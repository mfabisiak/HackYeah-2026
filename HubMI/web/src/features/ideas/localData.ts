// Small bits of idea data kept in this browser: the unfinished idea card and the last seen status of each idea.
// Neither holds personal data from the application form, but both belong to the signed-in user, so they are
// wiped on logout (shared computers in libraries).

const DRAFT_KEY = 'hubmi.ideaDraft'
const SEEN_KEY = 'hubmi.ideaSeen'

export interface IdeaDraft {
  title: string
  essence: string
  targetGroups: string[]
  stage: string
}

function read(key: string): unknown {
  try {
    const raw = window.localStorage.getItem(key)
    return raw === null ? null : JSON.parse(raw)
  } catch {
    return null
  }
}

function write(key: string, value: unknown): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // Storage can be unavailable (private mode, full); the draft is a convenience only.
  }
}

function isDraft(value: unknown): value is IdeaDraft {
  if (typeof value !== 'object' || value === null) return false
  const v = value as Record<string, unknown>
  return (
    typeof v.title === 'string' &&
    typeof v.essence === 'string' &&
    typeof v.stage === 'string' &&
    Array.isArray(v.targetGroups) &&
    v.targetGroups.every((g) => typeof g === 'string')
  )
}

export function loadIdeaDraft(): IdeaDraft | null {
  const value = read(DRAFT_KEY)
  return isDraft(value) ? value : null
}

export function saveIdeaDraft(draft: IdeaDraft): void {
  write(DRAFT_KEY, draft)
}

export function clearIdeaDraft(): void {
  try {
    window.localStorage.removeItem(DRAFT_KEY)
  } catch {
    // see `write`
  }
}

/** Last seen `status|comment` per idea id, used to flag changes since the previous visit. */
export function loadSeenIdeas(): Record<string, string> {
  const value = read(SEEN_KEY)
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return {}
  return Object.fromEntries(Object.entries(value).filter((entry): entry is [string, string] => typeof entry[1] === 'string'))
}

export function saveSeenIdeas(seen: Record<string, string>): void {
  write(SEEN_KEY, seen)
}

export function clearIdeaLocalData(): void {
  clearIdeaDraft()
  try {
    window.localStorage.removeItem(SEEN_KEY)
  } catch {
    // see `write`
  }
}
