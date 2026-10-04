import { describe, expect, it } from 'vitest'
import { ApiClientError, extractFieldErrors, formatApiError, toFriendlyErrorMessage } from './errors'
import { ApiErrorJs, FieldErrorJs } from 'hubmi-client'

describe('API errors utility', () => {
  it('formats known error codes with friendly Polish messages', () => {
    const errorJs = new ApiErrorJs(400, 'VALIDATION_FAILED', 'Validation error')
    const error = new ApiClientError(errorJs)

    expect(formatApiError(error)).toBe('Wprowadzone dane są niepoprawne. Sprawdź zaznaczone pola.')
  })

  it('formats 404 NOT_FOUND error', () => {
    const errorJs = new ApiErrorJs(404, 'NOT_FOUND', 'Not found')
    const error = new ApiClientError(errorJs)

    expect(formatApiError(error)).toBe('Żądany zasób nie został odnaleziony.')
  })

  it('formats 403 FORBIDDEN error', () => {
    const errorJs = new ApiErrorJs(403, 'FORBIDDEN', 'Forbidden')
    const error = new ApiClientError(errorJs)

    expect(formatApiError(error)).toBe('Brak uprawnień do wykonania tej operacji.')
  })

  it('extracts field errors accurately', () => {
    const fieldErrors = [
      new FieldErrorJs('title', 'BLANK', 'Tytuł jest wymagany', null, null),
      new FieldErrorJs('email', 'INVALID', 'Niepoprawny format adresu e-mail', null, null),
    ]
    const errorJs = new ApiErrorJs(422, 'VALIDATION_FAILED', 'Validation error', fieldErrors)
    const error = new ApiClientError(errorJs)

    const extracted = extractFieldErrors(error)
    expect(extracted).toEqual({
      title: 'Tytuł jest wymagany',
      email: 'Niepoprawny format adresu e-mail',
    })
  })

  it('handles standard Error instances and unknown objects', () => {
    expect(formatApiError(new Error('Błąd połączenia'))).toBe('Błąd połączenia')
    expect(formatApiError('unexpected')).toBe('Wystąpił nieoczekiwany błąd. Spróbuj ponownie później.')
  })

  it('converts raw technical browser errors like Failed to fetch to friendly Polish', () => {
    expect(formatApiError(new Error('Failed to fetch'))).toBe(
      'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.',
    )
    expect(formatApiError('TypeError: Failed to fetch')).toBe(
      'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.',
    )
    expect(formatApiError(new ApiErrorJs(0, 'NETWORK_ERROR', 'Failed to fetch'))).toBe(
      'Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.',
    )
    expect(
      toFriendlyErrorMessage(
        new ApiErrorJs(0, 'NETWORK_ERROR', 'Failed to fetch'),
        'Nie udało się pobrać danych.',
      ),
    ).toBe('Nie udało się połączyć z serwerem. Sprawdź swoje połączenie internetowe lub spróbuj ponownie za chwilę.')
  })
})
