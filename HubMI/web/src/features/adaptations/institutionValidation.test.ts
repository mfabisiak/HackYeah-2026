import { describe, expect, it } from 'vitest'
import { validateInstitution } from './institutionValidation'

const valid = { type: 'NGO', staffCount: 4, budgetPln: 30_000, context: ' Małe miasto, mamy 3 busy. ' }

describe('validateInstitution', () => {
  it('accepts a complete profile and trims the context', () => {
    const { errors, institution } = validateInstitution(valid)

    expect(errors).toEqual({})
    expect(institution).toEqual({ type: 'NGO', staffCount: 4, budgetPln: 30_000, context: 'Małe miasto, mamy 3 busy.' })
  })

  it('reads numbers typed as text, as the input field gives them', () => {
    expect(validateInstitution({ ...valid, staffCount: '7', budgetPln: '12000' }).institution).toMatchObject({
      staffCount: 7,
      budgetPln: 12_000,
    })
  })

  it('rejects what the server would reject, all at once', () => {
    const { errors, institution } = validateInstitution({ type: null, staffCount: -1, budgetPln: 5, context: ' ' })

    expect(institution).toBeNull()
    expect(Object.keys(errors).sort()).toEqual(['budgetPln', 'context', 'staffCount', 'type'])
  })

  it('treats an empty or fractional number as missing', () => {
    expect(validateInstitution({ ...valid, staffCount: '' }).errors.staffCount).toBeDefined()
    expect(validateInstitution({ ...valid, budgetPln: 1500.5 }).errors.budgetPln).toBeDefined()
  })

  it('limits the length of the context', () => {
    expect(validateInstitution({ ...valid, context: 'x'.repeat(601) }).errors.context).toContain('600')
    expect(validateInstitution({ ...valid, context: 'x'.repeat(600) }).errors.context).toBeUndefined()
  })
})
