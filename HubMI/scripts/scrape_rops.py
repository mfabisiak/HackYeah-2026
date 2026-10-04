#!/usr/bin/env python3
"""Pobiera Bibliotekę innowacji społecznych ROPS Kraków i zapisuje ją jako seed:
server/src/main/resources/seed/rops-innovations.json (format opisany w seed/README.md).

Użycie (z katalogu HubMI):
    python3 scripts/scrape_rops.py [--cache-dir DIR] [--out PLIK]

Tylko biblioteka standardowa. Wynik jest deterministyczny (kolejność = kolejność na stronie ROPS), a
`--cache-dir` pozwala uruchamiać skrypt wielokrotnie bez ponownego pobierania. Nie zapisujemy autorów
innowacji (dane osobowe – wymóg wyzwania).
"""

import argparse
import json
import re
import sys
import time
import urllib.request
from html import unescape
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urljoin

BASE = "https://rops.krakow.pl"
LIBRARY = f"{BASE}/innowacje-spoleczne/biblioteka-innowacji-spolecznych"
USER_AGENT = "Mozilla/5.0 (compatible; HubMI-seed-scraper; HackYeah 2026)"
REGION = "Małopolska"
SUMMARY_MAX = 280
TEXT_MAX = 5000
MAX_AREAS = 3

# Kategoria ROPS -> (obszary, grupy docelowe, słowa kluczowe). Reszta jest dopisywana heurystykami z tekstu.
CATEGORIES = {
    "dla-seniorow": (["AGING"], ["SENIORS"], ["seniorzy", "osoby starsze"]),
    "dla-dzieci-mlodziezy-i-rodziny": (["OTHER"], ["YOUTH", "FAMILIES"], ["dzieci", "młodzież", "rodzina"]),
    "dla-rynku-pracy": (["OTHER"], ["RESIDENTS"], ["rynek pracy", "zatrudnienie"]),
    "dla-osob-o-ograniczonej-mobilnosci": (
        ["SERVICE_ACCESS"],
        ["PEOPLE_WITH_DISABILITIES"],
        ["ograniczona mobilność", "niepełnosprawność ruchowa"],
    ),
    "dla-osob-z-niepelnosprawnoscia-sensoryczna": (
        ["SERVICE_ACCESS"],
        ["PEOPLE_WITH_DISABILITIES"],
        ["niepełnosprawność sensoryczna", "wzrok", "słuch"],
    ),
    "dla-cudzoziemcow": (["OTHER"], ["RESIDENTS"], ["cudzoziemcy", "migranci"]),
    "dla-osob-z-niepelnosprawnoscia-intelektualna": (
        ["SERVICE_ACCESS"],
        ["PEOPLE_WITH_DISABILITIES"],
        ["niepełnosprawność intelektualna"],
    ),
    "dla-osob-w-kryzysie-bezdomnosci": (["OTHER"], ["RESIDENTS"], ["bezdomność", "kryzys bezdomności"]),
    "dla-zdrowia-i-medycyny": (["SERVICE_ACCESS"], ["RESIDENTS"], ["zdrowie", "medycyna"]),
}

# Heurystyki: wzorzec w tekście -> wartość enuma (kolejność = priorytet przy limicie MAX_AREAS).
AREA_PATTERNS = [
    ("LONELINESS", r"samotn|izolacj|osamotni"),
    ("MENTAL_HEALTH", r"psychiczn|psycholog|depresj|lęk|stres|emocj|wypaleni|zaburzeni"),
    ("DIGITAL_EXCLUSION", r"cyfrow|aplikacj|internet|komputer|smartfon|wirtualn|online"),
    ("DEPOPULATION", r"\bwsi\b|wiejsk|wyludni|małych miejscowości"),
    ("COORDINATION", r"koordynac|współprac|sieć współpracy|partnerstw"),
    ("SERVICE_ACCESS", r"dostępno|dostęp do"),
    ("AGING", r"senior|osób starszych|osoby starsze|starzeni"),
]
GROUP_PATTERNS = [
    ("SENIORS", r"senior|osób starszych|osoby starsze|starszych"),
    ("YOUTH", r"młodzież|młodzi|uczni|dzieci|nastolat"),
    ("FAMILIES", r"rodzin|rodzic|opiekun"),
    ("PEOPLE_WITH_DISABILITIES", r"niepełnospraw|dysfunk|autyzm|spektrum"),
    ("NGOS", r"organizacj\w* pozarządow|fundacj|stowarzyszen|\bNGO\b|ekonomii społecznej"),
    ("LOCAL_GOVERNMENTS", r"samorząd|\bJST\b|gmin|ośrodk\w* pomocy społecznej|\bOPS\b|powiat"),
]

# Nagłówek sekcji na stronie innowacji -> pole seedu. Autorów (dane osobowe) celowo pomijamy.
SECTION_FIELDS = [
    (r"polega", "description"),
    (r"problem", "problemDiagnosis"),
    (r"grupa docelowa", "audience"),
    (r"kto może skorzystać", "audience"),
    (r"działa", "expectedChange"),
]


# Podpisy załączników („… PDF 128 KB") i akapity z adresami e-mail to szum, nie treść innowacji.
ATTACHMENT_CAPTION = re.compile(r"(PDF|DOCX?|ZIP)\s*\d+\s*[KM]B\b|[\w.+-]+@[\w-]+\.[\w.-]+")


class TextExtractor(HTMLParser):
    """HTML -> zwykły tekst: akapity rozdzielone pustą linią, punkty list jako „- ”."""

    BLOCKS = {"p", "div", "br", "ul", "ol", "tr", "h1", "h2", "h3", "h4", "h5", "h6"}

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []

    def handle_starttag(self, tag, attrs):
        if tag == "li":
            self.parts.append("\n- ")
        elif tag in self.BLOCKS:
            self.parts.append("\n\n" if tag != "br" else "\n")

    def handle_endtag(self, tag):
        if tag in self.BLOCKS or tag == "li":
            self.parts.append("\n")

    def handle_data(self, data):
        self.parts.append(data)


def to_text(fragment):
    parser = TextExtractor()
    parser.feed(fragment)
    text = "".join(parser.parts).replace("\xa0", " ")
    lines = [re.sub(r"[ \t]+", " ", line).strip() for line in text.split("\n")]
    paragraphs, current = [], []
    for line in lines:
        if line:
            current.append(line)
        elif current:
            paragraphs.append(current)
            current = []
    if current:
        paragraphs.append(current)
    kept = [p for p in paragraphs if not ATTACHMENT_CAPTION.search(" ".join(p))]
    return "\n\n".join("\n".join(p) for p in kept).strip()


class Fetcher:
    def __init__(self, cache_dir):
        self.cache_dir = cache_dir
        self.last_request = 0.0

    def get(self, path_or_url):
        url = urljoin(BASE, path_or_url)
        cached = self.cache_dir / (re.sub(r"[^A-Za-z0-9]+", "_", url) + ".html") if self.cache_dir else None
        if cached and cached.exists():
            return cached.read_text(encoding="utf-8")
        wait = 0.3 - (time.monotonic() - self.last_request)
        if wait > 0:
            time.sleep(wait)
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=30) as response:
            html = response.read().decode("utf-8")
        self.last_request = time.monotonic()
        if cached:
            cached.parent.mkdir(parents=True, exist_ok=True)
            cached.write_text(html, encoding="utf-8")
        return html


def category_slugs(fetcher):
    html = fetcher.get(f"{LIBRARY}/kategorie")
    found = re.findall(r"biblioteka-innowacji-spolecznych/(dla-[a-z-]+)", html)
    return [slug for slug in dict.fromkeys(found)]


def list_items(html):
    """Pozycje z listy kategorii: (ścieżka strony, krótki opis)."""
    items = []
    for block in html.split('<div class="news-list__item">')[1:]:
        link = re.search(r'href="([^"]+)" class="news-list__title"', block)
        desc = re.search(r'<p class="news-list__desc">(.*?)(?=<p><strong>|<table|</div>)', block, re.S)
        if link:
            items.append((link.group(1), to_text(desc.group(1)) if desc else ""))
    return items


def content_of(html):
    start = html.index('class="text-content"')
    return html[start : html.index("btns-holder-justify", start)]


def title_of(html):
    return to_text(re.search(r'<h2 class="page-title">(.*?)</h2>', html, re.S).group(1))


def media_urls(page_url, intro):
    """Strona źródłowa + materiały z paska ikon (broszura, film, paczka materiałów)."""
    hrefs = [unescape(h) for h in re.findall(r'<a href="([^"]+)"', intro)]
    wanted = [
        h
        for h in hrefs
        if re.search(r"\.(pdf|zip)$|youtube\.com|youtu\.be|vimeo\.com", h, re.I)
        and "Zasady_wykorzystania" not in h
    ]
    return list(dict.fromkeys([page_url] + [urljoin(BASE, h).replace("http://", "https://") for h in wanted]))


HEADING_AS_PARAGRAPH = re.compile(r"<p[^>]*>\s*(\d\.\s*(?:Na czym|Jakich|Grupa|Kto|Czy|Autor)[^<]*)</p>")


def sections_of(segment):
    segment = HEADING_AS_PARAGRAPH.sub(r"<h4>\1</h4>", segment)  # część stron ma nagłówki jako zwykłe akapity
    parts = re.split(r"<h4[^>]*>(.*?)</h4>", segment, flags=re.S)
    intro, rest = parts[0], parts[1:]
    return intro, [(to_text(rest[i]).lower(), to_text(rest[i + 1])) for i in range(0, len(rest), 2)]


def pick_fields(sections):
    fields = {}
    for heading, body in sections:
        field = next((f for pattern, f in SECTION_FIELDS if re.search(pattern, heading)), None)
        if field and body:
            fields[field] = f"{fields[field]}\n\n{body}" if field in fields else body
    return fields


def shorten(text, limit):
    flat = " ".join(text.split())
    if len(flat) <= limit:
        return flat
    cut = flat[: limit - 1].rsplit(" ", 1)[0].rstrip(",;:- ")
    return cut + "…"


def tag(patterns, text, base, limit):
    found = [value for value, pattern in patterns if re.search(pattern, text, re.I)]
    return list(dict.fromkeys(base + found))[:limit]


def build_item(category, page_path, list_desc, fetcher):
    html = fetcher.get(page_path)
    intro, sections = sections_of(content_of(html))
    fields = pick_fields(sections)
    title = title_of(html)
    description = fields.get("description") or list_desc.strip()
    if not description:
        return None
    base_areas, base_groups, keywords = CATEGORIES[category]
    summary = shorten(list_desc, SUMMARY_MAX) if list_desc.strip() else shorten(description, SUMMARY_MAX)
    problem_text = " ".join([title, description, fields.get("problemDiagnosis", ""), fields.get("audience", "")])
    page_url = urljoin(BASE, page_path)
    item = {
        "slug": "rops-" + page_path.rsplit(",", 1)[-1],
        "title": title,
        "summary": summary,
        "description": description,
        "areas": tag(AREA_PATTERNS, problem_text, base_areas, MAX_AREAS),
        "targetGroups": tag(GROUP_PATTERNS, fields.get("audience", ""), base_groups, len(GROUP_PATTERNS)),
        "stage": "TESTED",
        "region": REGION,
        "keywords": keywords,
        "mediaUrls": media_urls(page_url, intro),
    }
    for field in ("problemDiagnosis", "expectedChange"):
        if field in fields:
            item[field] = fields[field]
    if "audience" in fields:
        item["audienceDescription"] = fields["audience"]
    return item


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--cache-dir", type=Path, default=None)
    parser.add_argument(
        "--out", type=Path, default=Path("server/src/main/resources/seed/rops-innovations.json")
    )
    args = parser.parse_args()
    fetcher = Fetcher(args.cache_dir)

    slugs = category_slugs(fetcher)
    unknown = [s for s in slugs if s not in CATEGORIES]
    if unknown:
        sys.exit(f"Nowe kategorie ROPS bez mapowania w CATEGORIES: {unknown}")

    items, seen = [], set()
    for category in slugs:
        for page_path, desc in list_items(fetcher.get(f"{LIBRARY}/{category}")):
            item = build_item(category, page_path, desc, fetcher)
            if item is None:
                print(f"POMINIĘTO (brak opisu): {page_path}", file=sys.stderr)
            elif item["slug"] in seen:
                print(f"POMINIĘTO (duplikat): {page_path}", file=sys.stderr)
            else:
                seen.add(item["slug"])
                items.append(item)
        print(f"{category}: razem {len(items)}", file=sys.stderr)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(items, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Zapisano {len(items)} innowacji do {args.out}", file=sys.stderr)


if __name__ == "__main__":
    main()
