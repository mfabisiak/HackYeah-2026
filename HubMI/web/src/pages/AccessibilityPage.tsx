import { Anchor, Card, List, Stack, Text, Title } from '@mantine/core'
import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { PageHeader } from '../components'

const FEATURES = [
  'link „Przejdź do treści” na początku każdej strony i pełna obsługa klawiaturą z wyraźnie widocznym fokusem,',
  'semantyczny HTML: nagłówki, listy, regiony strony i etykiety pól formularzy,',
  'kontrast tekstu co najmniej 4,5:1 oraz jasny i ciemny motyw do wyboru,',
  'skalowanie tekstu i powiększenie strony do 200% bez utraty treści i bez przewijania w poziomie,',
  'komunikaty błędów podane tekstem, a nie samym kolorem, oraz komunikaty dynamiczne dla czytników ekranu,',
  'poszanowanie ustawienia systemu „ogranicz animacje”,',
  'wszystkie treści wygenerowane przez AI są wyraźnie oznaczone, a materiały wideo wymagają transkrypcji,',
  'brak zewnętrznych usług w przeglądarce (czcionki, analityka, reklamy): strona nie wysyła danych poza platformę.',
]

export function AccessibilityPage() {
  useEffect(() => {
    document.title = 'Deklaracja dostępności | HubMI'
  }, [])

  return (
    <Stack gap="xl" maw={800}>
      <PageHeader
        title="Deklaracja dostępności"
        subtitle="Platforma HubMI ma być dostępna dla każdego, w tym dla seniorów i osób z niepełnosprawnościami."
        breadcrumbs={[{ title: 'Strona główna', href: '/' }, { title: 'Deklaracja dostępności' }]}
      />

      <Card withBorder padding="lg">
        <Stack gap="sm">
          <Title order={2} size="h3">
            Stan zgodności
          </Title>
          <Text>
            Celem platformy jest zgodność z wytycznymi WCAG 2.1 na poziomie AA. Jest to prototyp przygotowany na
            HackYeah 2026: kluczowe widoki sprawdzano automatycznie (axe-core) i ręcznie, ale pełny audyt z
            czytnikami ekranu i innymi technologiami asystującymi jeszcze się nie odbył, dlatego platforma jest
            uznawana za częściowo zgodną.
          </Text>
        </Stack>
      </Card>

      <Card withBorder padding="lg">
        <Stack gap="sm">
          <Title order={2} size="h3">
            Co ułatwia korzystanie z platformy
          </Title>
          <List spacing="xs">
            {FEATURES.map((feature) => (
              <List.Item key={feature}>{feature}</List.Item>
            ))}
          </List>
        </Stack>
      </Card>

      <Card withBorder padding="lg">
        <Stack gap="sm">
          <Title order={2} size="h3">
            Zgłaszanie problemów z dostępnością
          </Title>
          <Text>
            Jeśli coś jest dla Ciebie niedostępne albo możesz poprosić o treść w innej formie, napisz do nas w
            zakładce{' '}
            <Anchor component={Link} to="/wiadomosci">
              Wiadomości
            </Anchor>
            . Wątek trafi do pracowników ROPS, którzy odpowiedzą w tej samej skrzynce.
          </Text>
          <Text size="sm" c="dimmed">
            Deklarację sporządzono 4 października 2026 r.
          </Text>
        </Stack>
      </Card>
    </Stack>
  )
}
