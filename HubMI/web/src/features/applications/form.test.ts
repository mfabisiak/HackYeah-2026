import { describe, expect, it } from 'vitest'
import {
  applicantHasData,
  emptyApplicant,
  emptyForm,
  emptyPlanRow,
  formatGrosze,
  fromContentJson,
  groszeToInput,
  parsePln,
  planDurationMonths,
  toRequest,
  type ApplicationForm,
  type EntityApplicantForm,
  type PlanRowForm,
} from './form'
import { fieldContext, fieldId, fieldIdOrStep, stepForField } from './steps'
import { krsError, nipError, phoneError, postalCodeError, regonError, validateForm } from './validation'

const row = (month: string, year: string, cost: string, action = 'Zadanie'): PlanRowForm => ({
  ...emptyPlanRow(),
  action,
  month,
  year,
  cost,
})

function validIndividualForm(): ApplicationForm {
  return {
    ...emptyForm(),
    title: 'Sąsiedzka pomoc',
    applicant: {
      type: 'INDIVIDUAL',
      firstName: 'Jan',
      lastName: 'Testowy',
      address: { street: 'Długa', buildingNumber: '5', apartmentNumber: '', postalCode: '30-001', city: 'Kraków' },
      phone: '123 456 789',
      email: 'jan@example.com',
    },
    description: 'Opis',
    innovativeness: 'Innowacyjność',
    problemDiagnosis: 'Diagnoza',
    socialArea: 'AGING',
    audienceDescription: 'Odbiorcy',
    expectedChange: 'Zmiana',
    futureVision: 'Wizja',
    plan: {
      preparation: [row('03', '2027', '1000')],
      testingPhase1: [row('05', '2027', '2000,50')],
      testingPhase2: [],
    },
    requestedGrant: '3000,50',
    projectTeam: 'Zespół',
    declarations: ['A', 'B'],
  }
}

describe('money', () => {
  it('parses złoty amounts to grosze', () => {
    expect(parsePln('1200')).toBe(120000)
    expect(parsePln('1 200,5')).toBe(120050)
    expect(parsePln('1200.05 zł')).toBe(120005)
    expect(parsePln('')).toBeNull()
    expect(parsePln('12,345')).toBeNull()
    expect(parsePln('abc')).toBeNull()
    expect(parsePln('-5')).toBeNull()
  })

  it('formats grosze for inputs and for reading', () => {
    expect(groszeToInput(120000)).toBe('1200')
    expect(groszeToInput(120005)).toBe('1200,05')
    expect(groszeToInput(null)).toBe('')
    expect(formatGrosze(120050).replace(/\s/g, ' ')).toBe('1200,50 zł')
    expect(formatGrosze(1234550).replace(/\s/g, ' ')).toBe('12 345,50 zł')
  })
})

describe('plan', () => {
  it('counts calendar months from the first to the last task', () => {
    expect(planDurationMonths([])).toBe(0)
    expect(planDurationMonths([row('03', '2027', '1')])).toBe(1)
    expect(planDurationMonths([row('11', '2026', '1'), row('02', '2027', '1')])).toBe(4)
  })
})

describe('conversion to and from the server JSON', () => {
  it('round-trips a full form', () => {
    const form = validIndividualForm()
    const json = JSON.stringify(toRequest(form))
    const back = fromContentJson(json)
    expect(toRequest(back)).toEqual(toRequest(form))
    expect(back.requestedGrant).toBe('3000,50')
    expect(back.plan.testingPhase1[0]).toMatchObject({ month: '05', year: '2027', cost: '2000,50' })
  })

  it('sends null for empty text and no plan', () => {
    const request = toRequest(emptyForm())
    expect(request.title).toBeNull()
    expect(request.applicant).toBeNull()
    expect(request.plan).toBeNull()
    expect(request.requestedGrantAmountGrosze).toBeNull()
    expect(request.declarations).toEqual([])
  })

  it('maps the group applicant with typed partners', () => {
    const form: ApplicationForm = { ...emptyForm(), applicant: emptyApplicant('NON_FORMAL_GROUP') }
    const request = toRequest(form)
    expect(request.applicant).toMatchObject({
      type: 'NON_FORMAL_GROUP',
      partners: [{ type: 'PARTNER_INDIVIDUAL' }],
    })
    expect(fromContentJson(JSON.stringify(request)).applicant).toMatchObject({
      type: 'NON_FORMAL_GROUP',
      partners: [{ kind: 'INDIVIDUAL' }],
    })
  })

  it('knows when the applicant variant holds typed data', () => {
    const blank = emptyApplicant('ENTITY') as EntityApplicantForm
    expect(applicantHasData(null)).toBe(false)
    expect(applicantHasData(blank)).toBe(false)
    expect(applicantHasData({ ...blank, name: 'Fundacja' })).toBe(true)
  })
})

describe('validation', () => {
  it('accepts a complete application', () => {
    expect(validateForm(validIndividualForm(), ['A', 'B'])).toEqual([])
  })

  it('reports every empty required field of an empty form', () => {
    const fields = validateForm(emptyForm(), []).map((i) => i.field)
    expect(fields).toEqual(
      expect.arrayContaining([
        'title',
        'applicant',
        'description',
        'socialArea',
        'plan.preparation',
        'plan.testingPhase1',
        'requestedGrantAmountGrosze',
        'projectTeam',
      ]),
    )
  })

  it('enforces the 3 and 9 month limits and the order of periods', () => {
    const form = validIndividualForm()
    const tooLong: ApplicationForm = {
      ...form,
      plan: {
        preparation: [row('01', '2027', '1000'), row('06', '2027', '1000')],
        testingPhase1: [row('03', '2027', '1000'), row('12', '2027', '1000')],
        testingPhase2: [],
      },
      requestedGrant: '4000',
    }
    const issues = validateForm(tooLong, [])
    expect(issues.map((i) => i.field)).toEqual(
      expect.arrayContaining(['plan.preparation', 'plan.testingPhase1', 'plan.preparation[1].term']),
    )
    expect(issues.find((i) => i.field === 'plan.preparation')?.message).toContain('3 miesięcy')
  })

  it('requires the grant to equal the sum of costs', () => {
    const issues = validateForm({ ...validIndividualForm(), requestedGrant: '1000' }, [])
    expect(issues.find((i) => i.field === 'requestedGrantAmountGrosze')?.message).toContain('musi być równa sumie')
  })

  it('requires every mandatory declaration', () => {
    const form: ApplicationForm = { ...validIndividualForm(), declarations: ['A'] }
    expect(validateForm(form, ['A', 'B']).map((i) => i.field)).toContain('declarations')
  })

  it('checks identifiers like the server does', () => {
    expect(nipError('526-104-08-28')).toBeNull()
    expect(nipError('1234567890')).toBe('Nieprawidłowa suma kontrolna NIP')
    expect(nipError('123')).toBe('NIP musi składać się z 10 cyfr')
    expect(regonError('123456785')).toBeNull()
    expect(regonError('123456789')).toBe('Nieprawidłowa suma kontrolna REGON')
    expect(krsError('0000123456')).toBeNull()
    expect(krsError('12')).toBe('Numer KRS musi składać się z 10 cyfr')
    expect(postalCodeError('30-001')).toBeNull()
    expect(postalCodeError('30001')).toBe('Kod pocztowy musi mieć format XX-XXX')
    expect(phoneError('+48 123 456 789')).toBeNull()
    expect(phoneError('12')).toBe('Nieprawidłowy format numeru telefonu')
  })
})

describe('field paths', () => {
  it('maps paths to steps and DOM ids', () => {
    expect(stepForField('plan.preparation[1].costGrosze')).toBe('plan')
    expect(stepForField('applicant.partners[0].email')).toBe('applicant')
    expect(stepForField('declarations')).toBe('summary')
    expect(stepForField('page')).toBeUndefined()
    expect(fieldId('plan.preparation[1].costGrosze')).toBe('f-plan-preparation-1-costGrosze')
    expect(fieldIdOrStep('plan.items[3].costGrosze')).toBe('f-plan')
  })

  it('describes where a field lives', () => {
    expect(fieldContext('plan.testingPhase1[1].action')).toBe('Testowanie – Faza I, pozycja 2')
    expect(fieldContext('applicant.partners[2].nip')).toBe('Wnioskodawca, partner 3')
    expect(fieldContext('title')).toBe('Tytuł innowacji')
  })
})
