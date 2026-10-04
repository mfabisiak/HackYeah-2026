import { Badge } from '@mantine/core'
import {
  IconCircleCheck,
  IconCircleX,
  IconFileText,
  IconHourglass,
  IconSend,
  type Icon,
} from '@tabler/icons-react'
import { IDEA_STATUS_LABELS } from './constants'

const STATUS_STYLE: Record<string, { color: string; icon: Icon }> = {
  DRAFT: { color: 'gray', icon: IconFileText },
  SUBMITTED: { color: 'blue', icon: IconSend },
  IN_REVIEW: { color: 'yellow', icon: IconHourglass },
  ACCEPTED: { color: 'teal', icon: IconCircleCheck },
  REJECTED: { color: 'red', icon: IconCircleX },
}

/** Status as text and icon; colour only reinforces it. */
export function IdeaStatusBadge({ status }: { status: string }) {
  const style = STATUS_STYLE[status] ?? STATUS_STYLE.DRAFT
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
      {IDEA_STATUS_LABELS[status] ?? status}
    </Badge>
  )
}
