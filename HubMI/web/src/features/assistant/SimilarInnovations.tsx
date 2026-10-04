import { Alert, Anchor, Badge, Button, Card, Group, Stack, Text, Title } from '@mantine/core'
import { IconBulb } from '@tabler/icons-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { SimilarInnovationJs } from 'hubmi-client'
import { closenessLabel, NOVELTY_NOTICES } from './constants'

export interface SimilarInnovationsProps {
  similar: SimilarInnovationJs[]
  noveltyHint: string | null
}

/** The closest ones are shown at once; the rest wait behind a button so that the assistant's own answer is not pushed off the screen. */
const SHOWN_AT_ONCE = 3

/** What the library already has for the idea, and what that says about it; none of it comes from the model. */
export function SimilarInnovations({ similar, noveltyHint }: SimilarInnovationsProps) {
  const [showAll, setShowAll] = useState(false)
  const notice = noveltyHint ? NOVELTY_NOTICES[noveltyHint] : undefined
  const visible = showAll ? similar : similar.slice(0, SHOWN_AT_ONCE)
  const hidden = similar.length - visible.length

  return (
    <Stack gap="md" component="section" aria-labelledby="similar-heading">
      <Title id="similar-heading" order={3} size="h4">
        Podobne rozwiązania w bazie
      </Title>

      {notice && (
        <Alert
          color={notice.color}
          variant="light"
          icon={<IconBulb size={22} aria-hidden="true" />}
          title={notice.title}
          data-testid="novelty-notice"
        >
          <Text size="md">{notice.text}</Text>
        </Alert>
      )}

      {similar.length > 0 && (
        <Stack component="ul" gap="sm" p={0} m={0} style={{ listStyle: 'none' }}>
          {visible.map((item) => (
            <li key={item.innovation.id}>
              <Card withBorder padding="md" radius="md">
                <Stack gap={6}>
                  <Group justify="space-between" align="flex-start" wrap="wrap" gap="xs">
                    <Anchor component={Link} to={`/innowacje/${item.innovation.id}`} fw={600} size="lg">
                      {item.innovation.title}
                    </Anchor>
                    <Badge variant="light" color="gray" size="lg">
                      {closenessLabel(item.score)}
                    </Badge>
                  </Group>
                  <Text size="md">{item.innovation.summary}</Text>
                </Stack>
              </Card>
            </li>
          ))}
        </Stack>
      )}

      {similar.length > SHOWN_AT_ONCE && (
        <div>
          <Button variant="subtle" onClick={() => setShowAll((all) => !all)} aria-expanded={showAll}>
            {showAll ? 'Pokaż mniej' : `Pokaż pozostałe (${hidden})`}
          </Button>
        </div>
      )}
    </Stack>
  )
}
