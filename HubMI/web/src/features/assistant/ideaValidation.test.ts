import { describe, expect, it } from 'vitest'
import { MAX_ESSENCE_LENGTH, MAX_TITLE_LENGTH, validateIdea } from './ideaValidation'

const valid = { title: ' Dowóz seniorów ', essence: ' Wolontariusze wożą seniorów. ', targetGroups: ['SENIORS'], stage: 'IDEA' }

describe('validateIdea', () => {
  it('trims the text and accepts a complete idea', () => {
    const { errors, idea } = validateIdea(valid)

    expect(errors).toEqual({})
    expect(idea).toEqual({
      title: 'Dowóz seniorów',
      essence: 'Wolontariusze wożą seniorów.',
      targetGroups: ['SENIORS'],
      stage: 'IDEA',
    })
  })

  it('reports every missing field at once', () => {
    const { errors, idea } = validateIdea({ title: '  ', essence: '', targetGroups: [], stage: null })

    expect(idea).toBeNull()
    expect(Object.keys(errors).sort()).toEqual(['essence', 'stage', 'targetGroups', 'title'])
  })

  it('enforces the same length limits as the server', () => {
    const long = validateIdea({
      ...valid,
      title: 'x'.repeat(MAX_TITLE_LENGTH + 1),
      essence: 'y'.repeat(MAX_ESSENCE_LENGTH + 1),
    })

    expect(long.idea).toBeNull()
    expect(long.errors.title).toContain(String(MAX_TITLE_LENGTH))
    expect(long.errors.essence).toContain(String(MAX_ESSENCE_LENGTH))
    expect(validateIdea({ ...valid, title: 'x'.repeat(MAX_TITLE_LENGTH) }).idea).not.toBeNull()
  })
})
