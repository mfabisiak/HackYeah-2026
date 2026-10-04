import { ActionIcon, Badge, Box } from '@mantine/core'
import { IconBell, IconBellRinging } from '@tabler/icons-react'
import { useState } from 'react'
import { NotificationsDrawer } from './NotificationsDrawer'
import { useNotifications } from './useNotifications'

export function NotificationBell() {
  const { unreadCount, liveAnnouncement } = useNotifications()
  const [drawerOpened, setDrawerOpened] = useState(false)

  const label =
    unreadCount > 0
      ? `Powiadomienia: ${unreadCount} nieprzeczytanych`
      : 'Powiadomienia: brak nowych'

  return (
    <>
      {/* Screen-reader polite live announcer for incoming notifications */}
      <div
        aria-live="polite"
        aria-atomic="true"
        style={{
          position: 'absolute',
          width: 1,
          height: 1,
          padding: 0,
          margin: -1,
          overflow: 'hidden',
          clip: 'rect(0, 0, 0, 0)',
          whiteSpace: 'nowrap',
          border: 0,
        }}
      >
        {liveAnnouncement}
      </div>

      <Box style={{ position: 'relative', display: 'inline-block' }}>
        <ActionIcon
          onClick={() => setDrawerOpened(true)}
          variant="default"
          size="lg"
          radius="md"
          aria-label={label}
          title={label}
          style={{ width: 42, height: 42 }}
        >
          {unreadCount > 0 ? (
            <IconBellRinging size={22} color="var(--mantine-color-blue-filled)" aria-hidden="true" />
          ) : (
            <IconBell size={22} aria-hidden="true" />
          )}
        </ActionIcon>

        {unreadCount > 0 && (
          <Badge
            size="sm"
            color="red"
            variant="filled"
            circle
            style={{
              position: 'absolute',
              top: -4,
              right: -4,
              pointerEvents: 'none',
              fontWeight: 700,
            }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </Badge>
        )}
      </Box>

      <NotificationsDrawer
        opened={drawerOpened}
        onClose={() => setDrawerOpened(false)}
      />
    </>
  )
}
