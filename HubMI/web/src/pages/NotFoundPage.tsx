import { Anchor, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <Stack gap="md">
      <Title order={2}>Nie znaleziono strony</Title>
      <Text>Adres, który wpisano, nie istnieje.</Text>
      <Anchor component={Link} to="/">
        Wróć na stronę główną
      </Anchor>
    </Stack>
  )
}
