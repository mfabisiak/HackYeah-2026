// Form model of the ROPS application (Załącznik nr 3). The form keeps everything as text, because that is what the
// user types; `toRequest` / `fromContentJson` convert to and from the JSON of `SaveApplicationDraftRequest`.

export type ApplicantType = 'INDIVIDUAL' | 'ENTITY' | 'NON_FORMAL_GROUP'

export type PlanPhase = 'preparation' | 'testingPhase1' | 'testingPhase2'

export const PLAN_PHASES: readonly PlanPhase[] = ['preparation', 'testingPhase1', 'testingPhase2']

export interface AddressForm {
  street: string
  buildingNumber: string
  apartmentNumber: string
  postalCode: string
  city: string
}

export interface ContactPersonForm {
  function: string
  fullName: string
  phone: string
  email: string
}

export interface GroupRepresentativeForm {
  firstName: string
  lastName: string
  phone: string
  email: string
}

export interface IndividualPartnerForm {
  id: string
  kind: 'INDIVIDUAL'
  firstName: string
  lastName: string
  address: AddressForm
  phone: string
  email: string
}

export interface EntityPartnerForm {
  id: string
  kind: 'ENTITY'
  name: string
  krs: string
  regon: string
  nip: string
  address: AddressForm
  phone: string
  email: string
}

export type PartnerForm = IndividualPartnerForm | EntityPartnerForm

export interface IndividualApplicantForm {
  type: 'INDIVIDUAL'
  firstName: string
  lastName: string
  address: AddressForm
  phone: string
  email: string
}

export interface EntityApplicantForm {
  type: 'ENTITY'
  name: string
  krs: string
  regon: string
  nip: string
  address: AddressForm
  phone: string
  email: string
  representative: ContactPersonForm
  contactPerson: ContactPersonForm
}

export interface GroupApplicantForm {
  type: 'NON_FORMAL_GROUP'
  partners: PartnerForm[]
  representative: GroupRepresentativeForm
}

export type ApplicantForm = IndividualApplicantForm | EntityApplicantForm | GroupApplicantForm

export interface PlanRowForm {
  id: string
  action: string
  month: string
  year: string
  /** Cost in PLN exactly as typed, e.g. `1 200,50`. */
  cost: string
}

export type PlanForm = Record<PlanPhase, PlanRowForm[]>

export interface ApplicationForm {
  title: string
  applicant: ApplicantForm | null
  description: string
  innovativeness: string
  problemDiagnosis: string
  socialArea: string
  audienceDescription: string
  expectedChange: string
  futureVision: string
  plan: PlanForm
  /** Grant in PLN exactly as typed. */
  requestedGrant: string
  projectTeam: string
  declarations: string[]
}

// --- Limits mirrored from the server (RopsApplicationValidator) -------------------------------------------------

export const MAX_TITLE_LENGTH = 200
export const MAX_NARRATIVE_LENGTH = 5000
export const MAX_PREPARATION_MONTHS = 3
export const MAX_TESTING_MONTHS = 9
export const MAX_PARTNERS = 5
export const MAX_ITEM_COST_GROSZE = 10_000_000
export const MAX_GRANT_AMOUNT_GROSZE = 10_000_000
/** Rows per plan phase the server accepts in a draft. */
export const MAX_PLAN_ROWS = 50
/** Draft limits of the server (ApplicationService.validateDraftPayload), checked while the user types. */
export const MAX_DRAFT_TITLE_LENGTH = 300
/** Amounts travel as Kotlin `Int` grosze, so anything above this cannot be sent at all. */
const MAX_INT_GROSZE = 2_147_483_647

// --- JSON of SaveApplicationDraftRequest ------------------------------------------------------------------------

interface AddressJson {
  street: string
  buildingNumber: string
  apartmentNumber?: string | null
  postalCode: string
  city: string
}

interface ContactPersonJson {
  function: string
  fullName: string
  phone: string
  email: string
}

type PartnerJson =
  | {
      type: 'PARTNER_INDIVIDUAL'
      firstName: string
      lastName: string
      address: AddressJson
      phone: string
      email: string
    }
  | {
      type: 'PARTNER_ENTITY'
      name: string
      krs: string
      regon: string
      nip: string
      address: AddressJson
      phone: string
      email: string
    }

type ApplicantJson =
  | {
      type: 'INDIVIDUAL'
      firstName: string
      lastName: string
      address: AddressJson
      phone: string
      email: string
    }
  | {
      type: 'ENTITY'
      name: string
      krs: string
      regon: string
      nip: string
      address: AddressJson
      phone: string
      email: string
      representative: ContactPersonJson
      contactPerson: ContactPersonJson
    }
  | {
      type: 'NON_FORMAL_GROUP'
      partners: PartnerJson[]
      representative: GroupRepresentativeForm
    }

interface PlanItemJson {
  action: string
  term: string
  costGrosze: number
}

interface PlanJson {
  preparation: PlanItemJson[]
  testingPhase1: PlanItemJson[]
  testingPhase2: PlanItemJson[]
}

export interface ApplicationContentJson {
  title: string | null
  applicant: ApplicantJson | null
  description: string | null
  innovativeness: string | null
  problemDiagnosis: string | null
  socialArea: string | null
  audienceDescription: string | null
  expectedChange: string | null
  futureVision: string | null
  plan: PlanJson | null
  requestedGrantAmountGrosze: number | null
  projectTeam: string | null
  declarations: string[]
}

// --- Money ------------------------------------------------------------------------------------------------------

/** Parses an amount in PLN (`1200`, `1 200,50`, `1200.5 zł`) to grosze; `null` when it is not a valid, sendable amount. */
export function parsePln(text: string): number | null {
  const cleaned = text
    .replace(/zł|pln/gi, '')
    .replace(/[\s\u00a0]/g, '')
    .trim()
  if (!/^\d+([.,]\d{1,2})?$/.test(cleaned)) return null
  const [whole, fraction = ''] = cleaned.split(/[.,]/)
  const grosze = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
  return grosze > MAX_INT_GROSZE ? null : grosze
}

const plnFormat = new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' })

export function formatGrosze(grosze: number): string {
  return plnFormat.format(grosze / 100)
}

/** Amount for a text input: `1200,50`, without the currency or thousands separators. */
export function groszeToInput(grosze: number | null | undefined): string {
  if (grosze === null || grosze === undefined) return ''
  const whole = Math.trunc(grosze / 100)
  const fraction = grosze % 100
  return fraction === 0 ? String(whole) : `${whole},${String(fraction).padStart(2, '0')}`
}

// --- Plan -------------------------------------------------------------------------------------------------------

export function newRowId(): string {
  return crypto.randomUUID()
}

export function emptyPlanRow(): PlanRowForm {
  return { id: newRowId(), action: '', month: '', year: '', cost: '' }
}

export const MONTH_NAMES = [
  'styczeń',
  'luty',
  'marzec',
  'kwiecień',
  'maj',
  'czerwiec',
  'lipiec',
  'sierpień',
  'wrzesień',
  'październik',
  'listopad',
  'grudzień',
] as const

export function rowTerm(row: PlanRowForm): string {
  return row.month && row.year ? `${row.year}-${row.month}` : ''
}

export function formatTerm(term: string): string {
  const match = /^(\d{4})-(0[1-9]|1[0-2])$/.exec(term)
  return match ? `${MONTH_NAMES[Number(match[2]) - 1]} ${match[1]}` : term
}

function monthIndex(term: string): number {
  const [year, month] = term.split('-').map(Number)
  return year * 12 + month
}

/** Number of calendar months spanned by the rows (first to last month, inclusive); 0 when no row has a term. */
export function planDurationMonths(rows: readonly PlanRowForm[]): number {
  const indices = rows.map(rowTerm).filter(Boolean).map(monthIndex)
  return indices.length === 0 ? 0 : Math.max(...indices) - Math.min(...indices) + 1
}

export function rowCostGrosze(row: PlanRowForm): number {
  return parsePln(row.cost) ?? 0
}

export function phaseTotalGrosze(rows: readonly PlanRowForm[]): number {
  return rows.reduce((sum, row) => sum + rowCostGrosze(row), 0)
}

export function planTotalGrosze(plan: PlanForm): number {
  return PLAN_PHASES.reduce((sum, phase) => sum + phaseTotalGrosze(plan[phase]), 0)
}

// --- Empty values and conversions ------------------------------------------------------------------------------

export const emptyAddress = (): AddressForm => ({
  street: '',
  buildingNumber: '',
  apartmentNumber: '',
  postalCode: '',
  city: '',
})

const emptyContactPerson = (): ContactPersonForm => ({ function: '', fullName: '', phone: '', email: '' })

export function emptyPartner(kind: PartnerForm['kind']): PartnerForm {
  return kind === 'INDIVIDUAL'
    ? {
        id: newRowId(),
        kind,
        firstName: '',
        lastName: '',
        address: emptyAddress(),
        phone: '',
        email: '',
      }
    : {
        id: newRowId(),
        kind,
        name: '',
        krs: '',
        regon: '',
        nip: '',
        address: emptyAddress(),
        phone: '',
        email: '',
      }
}

export function emptyApplicant(type: ApplicantType): ApplicantForm {
  switch (type) {
    case 'INDIVIDUAL':
      return { type, firstName: '', lastName: '', address: emptyAddress(), phone: '', email: '' }
    case 'ENTITY':
      return {
        type,
        name: '',
        krs: '',
        regon: '',
        nip: '',
        address: emptyAddress(),
        phone: '',
        email: '',
        representative: emptyContactPerson(),
        contactPerson: emptyContactPerson(),
      }
    case 'NON_FORMAL_GROUP':
      return {
        type,
        partners: [emptyPartner('INDIVIDUAL')],
        representative: { firstName: '', lastName: '', phone: '', email: '' },
      }
  }
}

export function emptyForm(): ApplicationForm {
  return {
    title: '',
    applicant: null,
    description: '',
    innovativeness: '',
    problemDiagnosis: '',
    socialArea: '',
    audienceDescription: '',
    expectedChange: '',
    futureVision: '',
    plan: { preparation: [], testingPhase1: [], testingPhase2: [] },
    requestedGrant: '',
    projectTeam: '',
    declarations: [],
  }
}

const withoutIds = (value: ApplicantForm): string =>
  JSON.stringify(value, (key, v: unknown) => (key === 'id' ? undefined : v))

/** Has the user typed anything into the applicant's data? Used to warn before the variant is changed. */
export function applicantHasData(applicant: ApplicantForm | null): boolean {
  return applicant !== null && withoutIds(applicant) !== withoutIds(emptyApplicant(applicant.type))
}

const address = (json: AddressJson | null | undefined): AddressForm => ({
  street: json?.street ?? '',
  buildingNumber: json?.buildingNumber ?? '',
  apartmentNumber: json?.apartmentNumber ?? '',
  postalCode: json?.postalCode ?? '',
  city: json?.city ?? '',
})

const contact = (json: ContactPersonJson | null | undefined): ContactPersonForm => ({
  function: json?.function ?? '',
  fullName: json?.fullName ?? '',
  phone: json?.phone ?? '',
  email: json?.email ?? '',
})

function partnerFromJson(p: PartnerJson): PartnerForm {
  return p.type === 'PARTNER_INDIVIDUAL'
    ? {
        id: newRowId(),
        kind: 'INDIVIDUAL',
        firstName: p.firstName,
        lastName: p.lastName,
        address: address(p.address),
        phone: p.phone,
        email: p.email,
      }
    : {
        id: newRowId(),
        kind: 'ENTITY',
        name: p.name,
        krs: p.krs,
        regon: p.regon,
        nip: p.nip,
        address: address(p.address),
        phone: p.phone,
        email: p.email,
      }
}

function applicantFromJson(json: ApplicantJson | null): ApplicantForm | null {
  if (!json) return null
  switch (json.type) {
    case 'INDIVIDUAL':
      return {
        type: 'INDIVIDUAL',
        firstName: json.firstName,
        lastName: json.lastName,
        address: address(json.address),
        phone: json.phone,
        email: json.email,
      }
    case 'ENTITY':
      return {
        type: 'ENTITY',
        name: json.name,
        krs: json.krs,
        regon: json.regon,
        nip: json.nip,
        address: address(json.address),
        phone: json.phone,
        email: json.email,
        representative: contact(json.representative),
        contactPerson: contact(json.contactPerson),
      }
    case 'NON_FORMAL_GROUP':
      return {
        type: 'NON_FORMAL_GROUP',
        partners: json.partners.map(partnerFromJson),
        representative: { ...json.representative },
      }
  }
}

function planFromJson(json: PlanJson | null): PlanForm {
  const rows = (items: PlanItemJson[] | undefined): PlanRowForm[] =>
    (items ?? []).map((item) => {
      const match = /^(\d{4})-(0[1-9]|1[0-2])$/.exec(item.term.trim())
      return {
        id: newRowId(),
        action: item.action,
        month: match?.[2] ?? '',
        year: match?.[1] ?? '',
        cost: groszeToInput(item.costGrosze === 0 ? null : item.costGrosze),
      }
    })
  return {
    preparation: rows(json?.preparation),
    testingPhase1: rows(json?.testingPhase1),
    testingPhase2: rows(json?.testingPhase2),
  }
}

export function fromContentJson(contentJson: string): ApplicationForm {
  const content = JSON.parse(contentJson) as ApplicationContentJson
  return {
    title: content.title ?? '',
    applicant: applicantFromJson(content.applicant),
    description: content.description ?? '',
    innovativeness: content.innovativeness ?? '',
    problemDiagnosis: content.problemDiagnosis ?? '',
    socialArea: content.socialArea ?? '',
    audienceDescription: content.audienceDescription ?? '',
    expectedChange: content.expectedChange ?? '',
    futureVision: content.futureVision ?? '',
    plan: planFromJson(content.plan),
    requestedGrant: groszeToInput(content.requestedGrantAmountGrosze),
    projectTeam: content.projectTeam ?? '',
    declarations: content.declarations,
  }
}

const addressJson = (a: AddressForm): AddressJson => ({
  street: a.street,
  buildingNumber: a.buildingNumber,
  apartmentNumber: a.apartmentNumber.trim() || null,
  postalCode: a.postalCode,
  city: a.city,
})

function partnerToJson(p: PartnerForm): PartnerJson {
  return p.kind === 'INDIVIDUAL'
    ? {
        type: 'PARTNER_INDIVIDUAL',
        firstName: p.firstName,
        lastName: p.lastName,
        address: addressJson(p.address),
        phone: p.phone,
        email: p.email,
      }
    : {
        type: 'PARTNER_ENTITY',
        name: p.name,
        krs: p.krs,
        regon: p.regon,
        nip: p.nip,
        address: addressJson(p.address),
        phone: p.phone,
        email: p.email,
      }
}

function applicantToJson(applicant: ApplicantForm | null): ApplicantJson | null {
  if (!applicant) return null
  switch (applicant.type) {
    case 'INDIVIDUAL':
      return {
        type: 'INDIVIDUAL',
        firstName: applicant.firstName,
        lastName: applicant.lastName,
        address: addressJson(applicant.address),
        phone: applicant.phone,
        email: applicant.email,
      }
    case 'ENTITY':
      return {
        type: 'ENTITY',
        name: applicant.name,
        krs: applicant.krs,
        regon: applicant.regon,
        nip: applicant.nip,
        address: addressJson(applicant.address),
        phone: applicant.phone,
        email: applicant.email,
        representative: { ...applicant.representative },
        contactPerson: { ...applicant.contactPerson },
      }
    case 'NON_FORMAL_GROUP':
      return {
        type: 'NON_FORMAL_GROUP',
        partners: applicant.partners.map(partnerToJson),
        representative: { ...applicant.representative },
      }
  }
}

const textOrNull = (value: string): string | null => value.trim() || null

export function toRequest(form: ApplicationForm): ApplicationContentJson {
  const items = (rows: PlanRowForm[]): PlanItemJson[] =>
    rows.map((row) => ({ action: row.action, term: rowTerm(row), costGrosze: rowCostGrosze(row) }))
  const hasPlan = PLAN_PHASES.some((phase) => form.plan[phase].length > 0)
  return {
    title: textOrNull(form.title),
    applicant: applicantToJson(form.applicant),
    description: textOrNull(form.description),
    innovativeness: textOrNull(form.innovativeness),
    problemDiagnosis: textOrNull(form.problemDiagnosis),
    socialArea: form.socialArea || null,
    audienceDescription: textOrNull(form.audienceDescription),
    expectedChange: textOrNull(form.expectedChange),
    futureVision: textOrNull(form.futureVision),
    plan: hasPlan
      ? {
          preparation: items(form.plan.preparation),
          testingPhase1: items(form.plan.testingPhase1),
          testingPhase2: items(form.plan.testingPhase2),
        }
      : null,
    requestedGrantAmountGrosze: parsePln(form.requestedGrant),
    projectTeam: textOrNull(form.projectTeam),
    declarations: form.declarations,
  }
}
