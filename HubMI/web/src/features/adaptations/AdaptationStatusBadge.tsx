import { Badge } from '@mantine/core'
import { IconCircleCheck, IconCircleX, IconClock } from '@tabler/icons-react'
import { ADAPTATION_STATUS } from './constants'

const ICONS: Record<string, typeof IconClock> = {
  PENDING_REVIEW: IconClock,
  APPROVED: IconCircleCheck,
  REJECTED: IconCircleX,
}

/** The status as text and an icon, never as colour alone. */
export function AdaptationStatusBadge({ status }: { status: string }) {
  const info = ADAPTATION_STATUS[status] ?? { label: status, color: 'gray' }
  const Icon = ICONS[status] ?? IconClock
  return (
    <Badge color={info.color} variant="light" size="lg" tt="none" leftSection={<Icon size={16} aria-hidden="true" />}>
      {info.label}
    </Badge>
  )
}
