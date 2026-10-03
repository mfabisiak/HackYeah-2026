import { render, screen, fireEvent } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { describe, expect, it, vi } from 'vitest'
import { ErrorSummary } from './ErrorSummary'

function renderWithProviders(ui: React.ReactElement) {
  return render(<MantineProvider>{ui}</MantineProvider>)
}

describe('ErrorSummary', () => {
  it('renders nothing when errors array is empty', () => {
    renderWithProviders(<ErrorSummary errors={[]} />)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('renders alert box with error links and focuses container on mount', () => {
    const errors = [
      { fieldId: 'field-name', message: 'Imię jest wymagane' },
      { fieldId: 'field-email', message: 'Niepoprawny format adresu' },
    ]

    renderWithProviders(
      <>
        <ErrorSummary errors={errors} />
        <input id="field-name" data-testid="input-name" />
        <input id="field-email" data-testid="input-email" />
      </>,
    )

    const alertBox = screen.getByRole('alert')
    expect(alertBox).toBeInTheDocument()
    expect(alertBox).toHaveTextContent('W formularzu występują błędy')

    const nameLink = screen.getByRole('link', { name: 'Imię jest wymagane' })
    const emailLink = screen.getByRole('link', { name: 'Niepoprawny format adresu' })
    expect(nameLink).toBeInTheDocument()
    expect(emailLink).toBeInTheDocument()

    // Test clicking link focuses target input
    const inputName = screen.getByTestId('input-name')
    const focusSpy = vi.spyOn(inputName, 'focus')
    inputName.scrollIntoView = vi.fn()

    fireEvent.click(nameLink)
    expect(focusSpy).toHaveBeenCalled()
  })
})
