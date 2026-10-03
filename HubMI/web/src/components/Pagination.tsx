import { Box, Group, Pagination as MantinePagination, Text } from '@mantine/core'

export interface AccessiblePaginationProps {
  total: number
  value: number
  onChange: (page: number) => void
  totalCount?: number
  itemsPerPage?: number
  ariaLabel?: string
}

export function AccessiblePagination({
  total,
  value,
  onChange,
  totalCount,
  itemsPerPage,
  ariaLabel = 'Paginacja wyników',
}: AccessiblePaginationProps) {
  if (total <= 1) return null

  return (
    <Box component="nav" aria-label={ariaLabel} my="lg">
      <Group justify="space-between" align="center" wrap="wrap" gap="sm">
        {totalCount !== undefined ? (
          <Text size="sm" c="dimmed">
            {itemsPerPage !== undefined
              ? `Wyniki ${(value - 1) * itemsPerPage + 1}–${Math.min(value * itemsPerPage, totalCount)} z ${totalCount}`
              : `Strona ${value} z ${total}`}
          </Text>
        ) : (
          <Text size="sm" c="dimmed">
            Strona {value} z {total}
          </Text>
        )}

        <MantinePagination
          total={total}
          value={value}
          onChange={onChange}
          getItemProps={(page) => ({
            'aria-label': `Przejdź do strony ${page}`,
          })}
          getControlProps={(control) => {
            const labels: Record<string, string> = {
              first: 'Pierwsza strona',
              previous: 'Poprzednia strona',
              next: 'Następna strona',
              last: 'Ostatnia strona',
            }
            return {
              'aria-label': labels[control] ?? control,
            }
          }}
          nextIcon={() => <span aria-hidden="true">&rarr;</span>}
          previousIcon={() => <span aria-hidden="true">&larr;</span>}
          aria-label="Nawigacja stron"
        />
      </Group>
    </Box>
  )
}
