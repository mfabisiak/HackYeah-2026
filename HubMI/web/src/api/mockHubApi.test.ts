import { beforeEach, describe, expect, it } from 'vitest'
import { CreateIdeaJs, createMockHubApi, resetMockHubData, type SimilarPartJs } from 'hubmi-client'

// The demo backend runs on top of the real Kotlin/JS client, so these tests exercise exactly what the demo site runs.
describe('createMockHubApi', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('serves the library the server seeds, in pages', async () => {
    const api = createMockHubApi()

    const page = await api.innovations.list(undefined, undefined, undefined, 0, 5)

    expect(page.ok).toBe(true)
    expect(page.value?.items).toHaveLength(5)
    expect(page.value?.total).toBeGreaterThan(30)
  })

  it('finds the matching innovation for a described problem and admits when there is none', async () => {
    const api = createMockHubApi()

    const found = await api.matches.match('Samotni seniorzy nie mają jak zrobić zakupów i boją się wychodzić z domu')
    const nothing = await api.matches.match('Kosmiczne rakiety lecące na Marsa')

    expect(found.value?.noGoodMatch).toBe(false)
    expect(found.value?.matches[0].innovation.id).toBe('sasiad-dla-seniora')
    expect(nothing.value?.noGoodMatch).toBe(true)
  })

  it('reports a missing resource as an error value, not an exception', async () => {
    const api = createMockHubApi()

    const result = await api.innovations.myTestRequest('sasiad-dla-seniora')

    expect(result.ok).toBe(false)
    expect(result.error?.status).toBe(404)
    expect(result.error?.code).toBe('NOT_FOUND')
  })

  it('keeps what the visitor adds in the browser, until it is reset', async () => {
    const first = createMockHubApi()
    await first.innovations.sendFeedback('sasiad-dla-seniora', 5, 'Świetne', undefined)
    const created = await first.ideas.create(new CreateIdeaJs('Klub w bibliotece', 'Spotkania seniorów.', ['SENIORS'], 'IDEA'))
    expect(created.ok).toBe(true)

    // A new instance stands for a page reload: it reads the data back from localStorage.
    const reloaded = createMockHubApi()
    const rating = await reloaded.innovations.myFeedback('sasiad-dla-seniora')
    const mine = await reloaded.ideas.mine()
    expect(rating.value?.rating).toBe(5)
    expect(mine.value?.items.map((idea) => idea.title)).toContain('Klub w bibliotece')

    resetMockHubData()
    const fresh = await createMockHubApi().innovations.myFeedback('sasiad-dla-seniora')
    expect(fresh.ok).toBe(false)
  })

  it('rejects an invalid rating with field errors', async () => {
    const api = createMockHubApi()

    const result = await api.innovations.sendFeedback('sasiad-dla-seniora', 9, undefined, undefined)

    expect(result.error?.code).toBe('VALIDATION_FAILED')
    expect(result.error?.details[0].field).toBe('rating')
  })

  it('streams the assistant answer and settles with the whole of it', async () => {
    const api = createMockHubApi()
    const parts: SimilarPartJs[] = []

    const stream = api.assistant.assistDraft(
      new CreateIdeaJs('Klub seniorów w bibliotece', 'Seniorzy pomagają sobie w drobnych sprawach.', ['SENIORS'], 'IDEA'),
      'SIMILAR',
      { onSimilar: (part) => parts.push(part) },
    )
    const result = await stream.result

    expect(parts).toHaveLength(1)
    expect(result.value?.aiStatus).toBe('NOT_REQUESTED')
    expect(result.value?.similar.length).toBeGreaterThan(0)
  })

  it('signs in as each role and remembers the account across reloads', async () => {
    const api = createMockHubApi()

    const admin = api.signInAs('ADMIN')
    const me = await api.me()
    expect(Array.from(api.accounts()).map((account) => account.role)).toEqual(['USER', 'EXPERT', 'ADMIN'])
    expect(admin?.roles).toContain('admin')
    expect(me.value?.username).toBe(admin?.username)

    expect(createMockHubApi().currentAccount().role).toBe('ADMIN')
    expect(api.signInAs('NOBODY')).toBeNull()
    expect(api.currentAccount().role).toBe('ADMIN')
    expect(Array.from(api.signInAs('USER')?.roles ?? [])).toEqual(['user'])
  })

  it('lets one official handle a thread at a time and an admin free it', async () => {
    const api = createMockHubApi()
    const threads = await api.threads.list()
    const threadId = threads.value?.items[0].id ?? ''

    api.signInAs('EXPERT')
    const taken = await api.threads.assign(threadId)
    expect(taken.value?.assigneeName).toBe('Anna Nowak')
    expect(taken.value?.assignedToMe).toBe(true)

    api.signInAs('ADMIN')
    const rival = await api.threads.assign(threadId)
    expect(rival.error?.status).toBe(409)
    const seen = await api.threads.list()
    expect(seen.value?.items.find((thread) => thread.id === threadId)?.assignedToMe).toBe(false)

    const freed = await api.threads.unassign(threadId)
    expect(freed.value?.assigneeName).toBeNull()
  })

  it('serves the reply templates', async () => {
    const templates = await createMockHubApi().threads.replyTemplates()

    expect(templates.value?.length).toBeGreaterThan(3)
    expect(templates.value?.every((template) => template.text.length > 0)).toBe(true)
  })
})
