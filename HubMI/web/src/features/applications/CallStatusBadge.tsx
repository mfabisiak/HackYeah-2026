import { Badge } from '@mantine/core'
import { IconCircleCheck, IconClock, IconFileText, IconLock, type Icon } from '@tabler/icons-react'

const CALL_STYLE: Record<string, { label: string; color: string; icon: Icon }> = {
  OPEN: { label: 'Nabór otwarty', color: 'teal', icon: IconCircleCheck },
  UPCOMING: { label: 'Nabór wkrótce', color: 'blue', icon: IconClock },
  CLOSED: { label: 'Nabór zamknięty', color: 'gray', icon: IconLock },
}

const APPLICATION_STYLE: Record<string, { label: string; color: string; icon: Icon }> = {
  DRAFT: { label: 'Szkic – można edytować', color: 'yellow', icon: IconFileText },
  SUBMITTED: { label: 'Złożony', color: 'teal', icon: IconCircleCheck },
}

function StatusBadge({ style }: { style: { label: string; color: string; icon: Icon } }) {
  const StatusIcon = style.icon
  return (
    <Badge
      color={style.color}
      variant="light"
      size="lg"
      radius="md"
      leftSection={<StatusIcon size={16} aria-hidden="true" />}
      styles={{ root: { textTransform: 'none', fontSize: '0.95rem', color: 'var(--mantine-color-text)' } }}
    >
      {style.label}
    </Badge>
  )
}

export const CallStatusBadge = ({ status }: { status: string }) => (
  <StatusBadge style={CALL_STYLE[status] ?? CALL_STYLE.CLOSED} />
)

export const ApplicationStatusBadge = ({ status }: { status: string }) => (
  <StatusBadge style={APPLICATION_STYLE[status] ?? APPLICATION_STYLE.DRAFT} />
)
