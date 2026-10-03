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
  NETWORK_ERROR: 'Nie udało się połączyć z serwerem. Sprawdź połączenie internetowe.',
}

export function formatApiError(error: unknown): string {
  if (error instanceof ApiClientError) {
    if (ERROR_CODE_MESSAGES[error.code]) {
      return ERROR_CODE_MESSAGES[error.code]
    }
    if (error.message) {
      return error.message
    }
  }

  if (typeof error === 'object' && error !== null && 'code' in error) {
    const code = String((error as { code: string }).code)
    if (ERROR_CODE_MESSAGES[code]) {
      return ERROR_CODE_MESSAGES[code]
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.'
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
