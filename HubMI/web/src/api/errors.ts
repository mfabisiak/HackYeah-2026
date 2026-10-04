import type { ApiErrorJs, FieldErrorJs } from 'hubmi-client'

export class ApiClientError extends Error {
  public status: number
  public code: string
  public details: FieldErrorJs[]

  constructor(error: ApiErrorJs) {
    super(error.message || 'Wystąpił błąd komunikacji z serwerem')
    this.name = 'ApiClientError'
    this.status = error.status
    this.code = error.code
    this.details = error.details ?? []
  }
}

const ERROR_CODE_MESSAGES: Record<string, string> = {
  VALIDATION_FAILED: 'Wprowadzone dane są niepoprawne. Sprawdź zaznaczone pola.',
  NOT_FOUND: 'Żądany zasób nie został odnaleziony.',
  CONFLICT: 'Wystąpił konflikt danych (np. zasób o tej nazwie już istnieje).',
  UNAUTHORIZED: 'Brak autoryzacji lub sesja wygasła. Zaloguj się ponownie.',
  FORBIDDEN: 'Brak uprawnień do wykonania tej operacji.',
  SERVICE_UNAVAILABLE: 'Serwis jest chwilowo niedostępny. Spróbuj ponownie za chwilę.',
  INTERNAL_ERROR: 'Wystąpił wewnętrzny błąd serwera. Skontaktuj się z administratorem.',
  NOT_IMPLEMENTED: 'Funkcjonalność nie została jeszcze zaimplementowana.',
  NETWORK_ERROR: 'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.',
}

function isRawTechnicalError(msg: string): boolean {
  const lower = msg.toLowerCase()
  return (
    lower.includes('failed to fetch') ||
    lower.includes('networkerror') ||
    lower.includes('network error') ||
    lower.includes('load failed') ||
    lower.includes('fetch') ||
    lower.includes('econnrefused') ||
    lower.includes('http_')
  )
}

export function formatApiError(error: unknown): string {
  if (error instanceof ApiClientError) {
    if (error.code && ERROR_CODE_MESSAGES[error.code]) {
      return ERROR_CODE_MESSAGES[error.code]
    }
    if (error.status === 401) {
      return 'Brak autoryzacji lub Twoja sesja wygasła. Zaloguj się ponownie.'
    }
    if (error.status === 403) {
      return 'Brak wymaganych uprawnień administratora do wykonania tej operacji.'
    }
    if (error.message && !isRawTechnicalError(error.message)) {
      return error.message
    }
    return 'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.'
  }

  if (typeof error === 'object' && error !== null) {
    const errObj = error as { code?: string; status?: number; message?: string }
    if (errObj.code && ERROR_CODE_MESSAGES[errObj.code]) {
      return ERROR_CODE_MESSAGES[errObj.code]
    }
    if (errObj.status === 401) {
      return 'Brak autoryzacji lub Twoja sesja wygasła. Zaloguj się ponownie.'
    }
    if (errObj.status === 403) {
      return 'Brak wymaganych uprawnień administratora do wykonania tej operacji.'
    }
    if (errObj.message) {
      if (isRawTechnicalError(errObj.message)) {
        return 'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.'
      }
      return errObj.message
    }
  }

  if (error instanceof Error && error.message) {
    if (isRawTechnicalError(error.message)) {
      return 'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.'
    }
    return error.message
  }

  if (typeof error === 'string') {
    if (isRawTechnicalError(error)) {
      return 'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.'
    }
    return 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.'
  }

  return 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.'
}

export function toFriendlyErrorMessage(error: unknown, fallback?: string): string {
  if (!error) return fallback || 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.'
  const formatted = formatApiError(error)
  if (formatted === 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.' && fallback) {
    return fallback
  }
  return formatted
}

export function extractFieldErrors(error: unknown): Record<string, string> {
  const result: Record<string, string> = {}
  if (error instanceof ApiClientError && error.details) {
    for (const detail of error.details) {
      if (detail.field) {
        result[detail.field] = detail.message || 'Pole zawiera niepoprawne dane.'
      }
    }
  }
  return result
}

/** The message to show for an error the client returns in an `ApiResult`: the usual wording per code, else the server's. */
export function describeApiError(error: ApiErrorJs): string {
  return ERROR_CODE_MESSAGES[error.code] ?? error.message ?? 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.'
}
