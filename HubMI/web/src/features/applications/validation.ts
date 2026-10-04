// Client-side validation mirroring RopsApplicationValidator (server), so errors show up before the request is sent.
// The server stays the authority: its `details` are mapped onto the same field paths.

import {
  MAX_GRANT_AMOUNT_GROSZE,
  MAX_DRAFT_TITLE_LENGTH,
  MAX_ITEM_COST_GROSZE,
  MAX_NARRATIVE_LENGTH,
  MAX_PARTNERS,
  MAX_PREPARATION_MONTHS,
  MAX_TESTING_MONTHS,
  MAX_TITLE_LENGTH,
  PLAN_PHASES,
  formatGrosze,
  formatTerm,
  parsePln,
  planDurationMonths,
  planTotalGrosze,
  rowTerm,
  type AddressForm,
  type ApplicationForm,
  type ContactPersonForm,
  type PartnerForm,
  type PlanRowForm,
} from './form'

/** What cannot be represented in a draft is reported while the user types, not only after a submit attempt. */
export interface FieldIssue {
  field: string
  message: string
}

const PHONE_FORMAT = /^\+?[0-9\s-]{9,18}$/
const EMAIL_FORMAT = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const POSTAL_CODE_FORMAT = /^[0-9]{2}-[0-9]{3}$/

const isBlank = (value: string): boolean => value.trim() === ''
const isDigits = (value: string): boolean => /^[0-9]+$/.test(value)
const digitsOf = (value: string): number[] => [...value].map(Number)
const compact = (value: string): string => value.replace(/[-\s]/g, '')

function weightedSum(digits: number[], weights: number[]): number {
  return weights.reduce((sum, weight, i) => sum + weight * digits[i], 0)
}

export function nipError(raw: string): string | null {
  if (isBlank(raw)) return 'NIP jest wymagany'
  const nip = compact(raw)
  if (nip.length !== 10 || !isDigits(nip)) return 'NIP musi składać się z 10 cyfr'
  const digits = digitsOf(nip)
  const checksum = weightedSum(digits, [6, 5, 7, 2, 3, 4, 5, 6, 7]) % 11
  return checksum === 10 || checksum !== digits[9] ? 'Nieprawidłowa suma kontrolna NIP' : null
}

export function regonError(raw: string): string | null {
  if (isBlank(raw)) return 'REGON jest wymagany'
  const regon = compact(raw)
  if (!isDigits(regon) || (regon.length !== 9 && regon.length !== 14)) {
    return 'REGON musi składać się z 9 lub 14 cyfr'
  }
  const digits = digitsOf(regon)
  if ((weightedSum(digits, [8, 9, 2, 3, 4, 5, 6, 7]) % 11) % 10 !== digits[8]) {
    return 'Nieprawidłowa suma kontrolna REGON'
  }
  if (
    regon.length === 14 &&
    (weightedSum(digits, [2, 4, 8, 5, 0, 9, 7, 3, 6, 1, 2, 4, 8]) % 11) % 10 !== digits[13]
  ) {
    return 'Nieprawidłowa suma kontrolna REGON-14'
  }
  return null
}

export function krsError(raw: string): string | null {
  if (isBlank(raw)) return 'Numer KRS jest wymagany'
  const krs = raw.trim()
  return krs.length === 10 && isDigits(krs) ? null : 'Numer KRS musi składać się z 10 cyfr'
}

export function postalCodeError(raw: string): string | null {
  if (isBlank(raw)) return 'Kod pocztowy jest wymagany'
  return POSTAL_CODE_FORMAT.test(raw.trim()) ? null : 'Kod pocztowy musi mieć format XX-XXX'
}

export function emailError(raw: string): string | null {
  if (isBlank(raw)) return 'Adres e-mail jest wymagany'
  return EMAIL_FORMAT.test(raw.trim()) ? null : 'Nieprawidłowy format adresu e-mail'
}

export function phoneError(raw: string): string | null {
  if (isBlank(raw)) return 'Numer telefonu jest wymagany'
  const trimmed = raw.trim()
  const digits = [...trimmed].filter((c) => c >= '0' && c <= '9').length
  return PHONE_FORMAT.test(trimmed) && digits >= 9 && digits <= 15 ? null : 'Nieprawidłowy format numeru telefonu'
}

const WHOLE_AMOUNT = /^\d+([.,]\d{1,2})?$/

/** Why an amount could not be read: far too large, or not an amount in złoty at all. */
function amountFormatMessage(text: string, what: string): string {
  const cleaned = text.replace(/zł|pln/gi, '').replace(/[\s\u00a0]/g, '')
  return WHOLE_AMOUNT.test(cleaned)
    ? `Wpisana kwota jest za duża (${what})`
    : `Wpisz ${what} w złotych, np. 1200 lub 1200,50`
}

function required(field: string, label: string, value: string): FieldIssue[] {
  return isBlank(value) ? [{ field, message: `Pole „${label}” jest wymagane` }] : []
}

function check(field: string, message: string | null): FieldIssue[] {
  return message ? [{ field, message }] : []
}

function addressIssues(prefix: string, a: AddressForm): FieldIssue[] {
  return [
    ...required(`${prefix}.street`, 'Ulica', a.street),
    ...required(`${prefix}.buildingNumber`, 'Numer budynku', a.buildingNumber),
    ...check(`${prefix}.postalCode`, postalCodeError(a.postalCode)),
    ...required(`${prefix}.city`, 'Miejscowość', a.city),
  ]
}

function contactPersonIssues(prefix: string, label: string, p: ContactPersonForm): FieldIssue[] {
  return [
    ...required(`${prefix}.function`, `Funkcja ${label}`, p.function),
    ...required(`${prefix}.fullName`, `Imię i nazwisko ${label}`, p.fullName),
    ...check(`${prefix}.phone`, phoneError(p.phone)),
    ...check(`${prefix}.email`, emailError(p.email)),
  ]
}

function partnerIssues(prefix: string, p: PartnerForm): FieldIssue[] {
  return p.kind === 'INDIVIDUAL'
    ? [
        ...required(`${prefix}.firstName`, 'Imię partnera', p.firstName),
        ...required(`${prefix}.lastName`, 'Nazwisko partnera', p.lastName),
        ...addressIssues(`${prefix}.address`, p.address),
        ...check(`${prefix}.phone`, phoneError(p.phone)),
        ...check(`${prefix}.email`, emailError(p.email)),
      ]
    : [
        ...required(`${prefix}.name`, 'Nazwa podmiotu partnera', p.name),
        ...check(`${prefix}.krs`, krsError(p.krs)),
        ...check(`${prefix}.regon`, regonError(p.regon)),
        ...check(`${prefix}.nip`, nipError(p.nip)),
        ...addressIssues(`${prefix}.address`, p.address),
        ...check(`${prefix}.phone`, phoneError(p.phone)),
        ...check(`${prefix}.email`, emailError(p.email)),
      ]
}

function applicantIssues(form: ApplicationForm): FieldIssue[] {
  const a = form.applicant
  if (!a) return [{ field: 'applicant', message: 'Wybierz, kto składa wniosek' }]
  switch (a.type) {
    case 'INDIVIDUAL':
      return [
        ...required('applicant.firstName', 'Imię wnioskodawcy', a.firstName),
        ...required('applicant.lastName', 'Nazwisko wnioskodawcy', a.lastName),
        ...addressIssues('applicant.address', a.address),
        ...check('applicant.phone', phoneError(a.phone)),
        ...check('applicant.email', emailError(a.email)),
      ]
    case 'ENTITY':
      return [
        ...required('applicant.name', 'Nazwa podmiotu', a.name),
        ...check('applicant.krs', krsError(a.krs)),
        ...check('applicant.regon', regonError(a.regon)),
        ...check('applicant.nip', nipError(a.nip)),
        ...addressIssues('applicant.address', a.address),
        ...check('applicant.phone', phoneError(a.phone)),
        ...check('applicant.email', emailError(a.email)),
        ...contactPersonIssues('applicant.representative', 'osoby reprezentującej', a.representative),
        ...contactPersonIssues('applicant.contactPerson', 'osoby do kontaktu', a.contactPerson),
      ]
    case 'NON_FORMAL_GROUP':
      return [
        ...(a.partners.length < 1 || a.partners.length > MAX_PARTNERS
          ? [
              {
                field: 'applicant.partners',
                message: `Grupa nieformalna musi liczyć od 1 do ${MAX_PARTNERS} partnerów`,
              },
            ]
          : a.partners.flatMap((p, i) => partnerIssues(`applicant.partners[${i}]`, p))),
        ...required('applicant.representative.firstName', 'Imię reprezentanta grupy', a.representative.firstName),
        ...required('applicant.representative.lastName', 'Nazwisko reprezentanta grupy', a.representative.lastName),
        ...check('applicant.representative.phone', phoneError(a.representative.phone)),
        ...check('applicant.representative.email', emailError(a.representative.email)),
      ]
  }
}

function narrative(field: string, label: string, value: string): FieldIssue[] {
  if (isBlank(value)) return [{ field, message: `Pole „${label}” jest wymagane` }]
  return value.trim().length > MAX_NARRATIVE_LENGTH
    ? [{ field, message: `Pole „${label}” nie może przekraczać ${MAX_NARRATIVE_LENGTH} znaków` }]
    : []
}

function rowIssues(prefix: string, row: PlanRowForm): FieldIssue[] {
  const cost = parsePln(row.cost)
  return [
    ...required(`${prefix}.action`, 'Opis zadania', row.action),
    ...(rowTerm(row) === '' ? [{ field: `${prefix}.term`, message: 'Wybierz miesiąc i rok zadania' }] : []),
    ...(cost === null
      ? [{ field: `${prefix}.costGrosze`, message: amountFormatMessage(row.cost, 'koszt zadania') }]
      : cost <= 0
        ? [{ field: `${prefix}.costGrosze`, message: 'Koszt zadania musi być większy od zera' }]
        : cost > MAX_ITEM_COST_GROSZE
          ? [
              {
                field: `${prefix}.costGrosze`,
                message: `Koszt pojedynczego zadania nie może przekraczać ${formatGrosze(MAX_ITEM_COST_GROSZE)}`,
              },
            ]
          : []),
  ]
}

function sequenceIssues(plan: ApplicationForm['plan']): FieldIssue[] {
  const terms = (rows: PlanRowForm[]) =>
    rows.map((row, index) => ({ index, term: rowTerm(row) })).filter((entry) => entry.term !== '')
  const preparation = terms(plan.preparation)
  const phase1 = terms(plan.testingPhase1)
  const phase2 = terms(plan.testingPhase2)
  const firstTesting = [...phase1, ...phase2].map((e) => e.term).sort()[0]
  const firstPhase2 = phase2.map((e) => e.term).sort()[0]

  return [
    ...(firstTesting
      ? preparation
          .filter((e) => e.term >= firstTesting)
          .map((e) => ({
            field: `plan.preparation[${e.index}].term`,
            message: `Okres przygotowawczy (${formatTerm(e.term)}) musi poprzedzać okres testowania (${formatTerm(firstTesting)})`,
          }))
      : []),
    ...(firstPhase2
      ? phase1
          .filter((e) => e.term > firstPhase2)
          .map((e) => ({
            field: `plan.testingPhase1[${e.index}].term`,
            message: `Faza I testowania (${formatTerm(e.term)}) musi poprzedzać Fazę II (${formatTerm(firstPhase2)})`,
          }))
      : []),
  ]
}

function planIssues(form: ApplicationForm): FieldIssue[] {
  const { plan } = form
  const preparationMonths = planDurationMonths(plan.preparation)
  const testingMonths = planDurationMonths([...plan.testingPhase1, ...plan.testingPhase2])
  return [
    ...(plan.preparation.length === 0
      ? [{ field: 'plan.preparation', message: 'Dodaj co najmniej jedno zadanie w okresie przygotowawczym' }]
      : []),
    ...(plan.testingPhase1.length === 0
      ? [{ field: 'plan.testingPhase1', message: 'Dodaj co najmniej jedno zadanie w Fazie I testowania' }]
      : []),
    ...PLAN_PHASES.flatMap((phase) => plan[phase].flatMap((row, i) => rowIssues(`plan.${phase}[${i}]`, row))),
    ...(preparationMonths > MAX_PREPARATION_MONTHS
      ? [
          {
            field: 'plan.preparation',
            message: `Okres przygotowawczy nie może przekraczać ${MAX_PREPARATION_MONTHS} miesięcy (obecnie: ${preparationMonths})`,
          },
        ]
      : []),
    ...(testingMonths > MAX_TESTING_MONTHS
      ? [
          {
            field: 'plan.testingPhase1',
            message: `Łączny okres testowania (Faza I + Faza II) nie może przekraczać ${MAX_TESTING_MONTHS} miesięcy (obecnie: ${testingMonths})`,
          },
        ]
      : []),
    ...sequenceIssues(plan),
  ]
}

function grantIssues(form: ApplicationForm): FieldIssue[] {
  const field = 'requestedGrantAmountGrosze'
  const grant = parsePln(form.requestedGrant)
  const total = planTotalGrosze(form.plan)
  if (isBlank(form.requestedGrant)) return [{ field, message: 'Wpisz wnioskowaną kwotę grantu' }]
  if (grant === null) return [{ field, message: amountFormatMessage(form.requestedGrant, 'kwotę grantu') }]
  if (grant <= 0) return [{ field, message: 'Kwota grantu musi być większa od zera' }]
  if (grant > MAX_GRANT_AMOUNT_GROSZE) {
    return [{ field, message: `Kwota grantu nie może przekraczać ${formatGrosze(MAX_GRANT_AMOUNT_GROSZE)}` }]
  }
  return grant === total
    ? []
    : [
        {
          field,
          message: `Kwota grantu (${formatGrosze(grant)}) musi być równa sumie kosztów z planu (${formatGrosze(total)})`,
        },
      ]
}

/**
 * All problems that would block submission.
 *
 * @param requiredDeclarations ids of declarations the user has to accept (empty until the applicant type is known).
 */
export function validateForm(form: ApplicationForm, requiredDeclarations: readonly string[]): FieldIssue[] {
  const missing = requiredDeclarations.filter((id) => !form.declarations.includes(id))
  return [
    ...(isBlank(form.title)
      ? [{ field: 'title', message: 'Podaj tytuł innowacji' }]
      : form.title.trim().length > MAX_TITLE_LENGTH
        ? [{ field: 'title', message: `Tytuł nie może przekraczać ${MAX_TITLE_LENGTH} znaków` }]
        : []),
    ...applicantIssues(form),
    ...narrative('description', 'Opis innowacji', form.description),
    ...narrative('innovativeness', 'Innowacyjność rozwiązania', form.innovativeness),
    ...narrative('problemDiagnosis', 'Diagnoza problemu', form.problemDiagnosis),
    ...(form.socialArea === '' ? [{ field: 'socialArea', message: 'Wybierz obszar z Mapy Wyzwań Społecznych' }] : []),
    ...narrative('audienceDescription', 'Odbiorcy innowacji', form.audienceDescription),
    ...narrative('expectedChange', 'Zmiana, jaką wprowadza innowacja', form.expectedChange),
    ...narrative('futureVision', 'Wizja przyszłości innowacji', form.futureVision),
    ...planIssues(form),
    ...grantIssues(form),
    ...narrative('projectTeam', 'Zespół projektowy', form.projectTeam),
    ...(missing.length > 0
      ? [{ field: 'declarations', message: 'Zaznacz wszystkie wymagane oświadczenia' }]
      : []),
  ]
}

/**
 * Problems that stop a draft from being stored faithfully: the server rejects the save or the value would be dropped
 * and vanish after a reload. Unlike the submit checks they are shown immediately.
 */
export function draftIssues(form: ApplicationForm): FieldIssue[] {
  const rows = PLAN_PHASES.flatMap((phase) =>
    form.plan[phase].flatMap((row, i) => {
      const prefix = `plan.${phase}[${i}]`
      const typedCost = !isBlank(row.cost) && rowIssues(prefix, row).find((x) => x.field === `${prefix}.costGrosze`)
      const half = (row.month === '') !== (row.year === '')
      return [
        ...(typedCost ? [typedCost] : []),
        ...(half ? [{ field: `${prefix}.term`, message: 'Wybierz miesiąc i rok zadania' }] : []),
      ]
    }),
  )
  const grantTyped = !isBlank(form.requestedGrant) && parsePln(form.requestedGrant) === null
  return [
    ...(form.title.trim().length > MAX_DRAFT_TITLE_LENGTH
      ? [{ field: 'title', message: `Tytuł nie może przekraczać ${MAX_DRAFT_TITLE_LENGTH} znaków` }]
      : []),
    ...(['description', 'innovativeness', 'problemDiagnosis', 'audienceDescription', 'expectedChange', 'futureVision', 'projectTeam'] as const)
      .filter((name) => form[name].trim().length > MAX_NARRATIVE_LENGTH)
      .map((name) => ({ field: name, message: `Tekst nie może przekraczać ${MAX_NARRATIVE_LENGTH} znaków` })),
    ...rows,
    ...(grantTyped ? grantIssues(form) : []),
  ]
}
