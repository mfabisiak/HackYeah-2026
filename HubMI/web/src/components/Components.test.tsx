import { render, screen, fireEvent } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { PageHeader } from './PageHeader'
import { LoadingState } from './LoadingState'
import { EmptyState } from './EmptyState'
import { ErrorAlert } from './ErrorAlert'
import { FormField } from './FormField'
import { AccessiblePagination } from './Pagination'

function renderWithProviders(ui: React.ReactElement) {
  return render(
    <MantineProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </MantineProvider>,
  )
}

describe('Shared Accessible Components', () => {
  describe('PageHeader', () => {
    it('renders title, subtitle, and accessible breadcrumbs', () => {
      renderWithProviders(
        <PageHeader
          title="Katalog wyzwań"
          subtitle="Przeglądaj zgłoszone wyzwania"
          breadcrumbs={[
            { title: 'Start', href: '/' },
            { title: 'Wyzwania' },
          ]}
        />,
      )

      expect(screen.getByRole('heading', { level: 1, name: 'Katalog wyzwań' })).toBeInTheDocument()
      expect(screen.getByText('Przeglądaj zgłoszone wyzwania')).toBeInTheDocument()
      expect(screen.getByRole('navigation', { name: 'Okruszki chleba' })).toBeInTheDocument()
      expect(screen.getByText('Start')).toBeInTheDocument()
    })
  })

  describe('LoadingState', () => {
    it('renders with role="status" and aria-live="polite"', () => {
      renderWithProviders(<LoadingState message="Trwa przetwarzanie zapytania..." />)

      const statusEl = screen.getByRole('status')
      expect(statusEl).toBeInTheDocument()
      expect(statusEl).toHaveAttribute('aria-live', 'polite')
      expect(screen.getByText('Trwa przetwarzanie zapytania...')).toBeInTheDocument()
    })
  })

  describe('EmptyState', () => {
    it('renders accessible empty state with title and action', () => {
      renderWithProviders(
        <EmptyState
          title="Brak wyników wyszukiwania"
          description="Spróbuj zmienić kryteria filtrów."
          action={<button type="button">Wyczyść filtry</button>}
        />,
      )

      expect(screen.getByRole('region', { name: 'Brak wyników wyszukiwania' })).toBeInTheDocument()
      expect(screen.getByText('Spróbuj zmienić kryteria filtrów.')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Wyczyść filtry' })).toBeInTheDocument()
    })
  })

  describe('ErrorAlert', () => {
    it('renders alert with role="alert" and triggers onRetry', () => {
      const handleRetry = vi.fn()
      renderWithProviders(
        <ErrorAlert
          title="Błąd ładowania"
          message="Nie udało się pobrać danych"
          onRetry={handleRetry}
          retryLabel="Odśwież"
        />,
      )

      expect(screen.getByRole('alert')).toBeInTheDocument()
      expect(screen.getByText('Nie udało się pobrać danych')).toBeInTheDocument()

      const retryBtn = screen.getByRole('button', { name: 'Odśwież' })
      fireEvent.click(retryBtn)
      expect(handleRetry).toHaveBeenCalledTimes(1)
    })
  })

  describe('FormField', () => {
    it('properly associates label, description, and error with input', () => {
      renderWithProviders(
        <FormField
          id="custom-input"
          label="Adres e-mail"
          description="Wprowadź służbowy adres e-mail"
          error="Adres e-mail jest nieprawidłowy"
          required
        >
          {(props) => <input type="email" {...props} />}
        </FormField>,
      )

      const input = screen.getByLabelText(/Adres e-mail/)
      expect(input).toBeInTheDocument()
      expect(input).toHaveAttribute('id', 'custom-input')
      expect(input).toHaveAttribute('aria-invalid', 'true')
      expect(input).toHaveAttribute(
        'aria-describedby',
        'custom-input-description custom-input-error',
      )

      const errorEl = screen.getByRole('alert')
      expect(errorEl).toHaveTextContent('Adres e-mail jest nieprawidłowy')
    })
  })

  describe('AccessiblePagination', () => {
    it('renders pagination with accessible navigation', () => {
      const handleChange = vi.fn()
      renderWithProviders(
        <AccessiblePagination
          total={5}
          value={2}
          onChange={handleChange}
          totalCount={50}
          itemsPerPage={10}
        />,
      )

      const nav = screen.getByRole('navigation', { name: 'Paginacja wyników' })
      expect(nav).toBeInTheDocument()
      expect(screen.getByText('Wyniki 11–20 z 50')).toBeInTheDocument()
    })
  })
})
