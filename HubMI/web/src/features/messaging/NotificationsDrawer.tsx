import {
  ActionIcon,
  Badge,
  Button,
  Card,
  Drawer,
  Group,
  Loader,
  Stack,
  Switch,
  Text,
  Title,
  Tooltip,
} from '@mantine/core'
import {
  IconBell,
  IconBellCheck,
  IconCheck,
  IconFileCheck,
  IconInbox,
  IconInfoCircle,
  IconMessage,
  IconRefresh,
  IconRocket,
  IconSparkles,
} from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { NOTIFICATION_TYPE_CONFIG, formatPolishDateTime, formatRelativePolishTime } from './constants'
import { useNotifications } from './useNotifications'
import type { NotificationJs } from 'hubmi-client'

export interface NotificationsDrawerProps {
  opened: boolean
  onClose: () => void
}

function getNotificationIcon(type: string) {
  switch (type) {
    case 'IDEA_SUBMITTED':
      return <IconSparkles size={20} aria-hidden="true" />
    case 'IDEA_STATUS_CHANGED':
      return <IconFileCheck size={20} aria-hidden="true" />
    case 'CALL_PUBLISHED':
    case 'CALL_CHANGED':
      return <IconRocket size={20} aria-hidden="true" />
    case 'MESSAGE_RECEIVED':
      return <IconMessage size={20} aria-hidden="true" />
    default:
      return <IconInfoCircle size={20} aria-hidden="true" />
  }
}

export function NotificationsDrawer({ opened, onClose }: NotificationsDrawerProps) {
  const {
    notifications,
    unreadCount,
    unreadOnly,
    setUnreadOnly,
    isLoading,
    markRead,
    refresh,
  } = useNotifications()

  return (
    <Drawer
      opened={opened}
      onClose={onClose}
      position="right"
      size="md"
      title={
        <Group gap="sm" align="center">
          <IconBell size={24} aria-hidden="true" color="var(--mantine-color-blue-filled)" />
          <Text fw={700} style={{ fontSize: '1.4rem' }}>
            Powiadomienia
          </Text>
          {unreadCount > 0 && (
            <Badge color="red" size="lg" variant="filled">
              {unreadCount} nowe
            </Badge>
          )}
        </Group>
      }
      styles={{
        header: {
          padding: '16px 20px',
          borderBottom: '1px solid var(--mantine-color-gray-3)',
        },
        body: { padding: '20px' },
      }}
    >
      <Stack gap="md">
        <Text size="md" c="dimmed" style={{ fontSize: '1.05rem', lineHeight: 1.5 }}>
          Tutaj znajdziesz ważne informacje o Twoich pomysłach, naborach wniosków oraz wiadomościach od ROPS.
        </Text>

        <Group justify="space-between" align="center" wrap="wrap" gap="sm">
          <Switch
            label="Pokaż tylko nieprzeczytane"
            checked={unreadOnly}
            onChange={(event) => setUnreadOnly(event.currentTarget.checked)}
            size="md"
            styles={{ label: { fontSize: '1rem', fontWeight: 500 } }}
          />

          <Tooltip label="Odśwież listę powiadomień">
            <ActionIcon
              variant="subtle"
              color="gray"
              size="lg"
              onClick={() => void refresh()}
              aria-label="Odśwież powiadomienia"
              loading={isLoading}
            >
              <IconRefresh size={20} aria-hidden="true" />
            </ActionIcon>
          </Tooltip>
        </Group>

        {isLoading && notifications.length === 0 ? (
          <Group justify="center" py="xl">
            <Loader size="md" />
            <Text size="md" style={{ fontSize: '1.05rem' }}>
              Wczytuję powiadomienia...
            </Text>
          </Group>
        ) : notifications.length === 0 ? (
          <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }} mt="md">
            <Stack align="center" gap="sm">
              <IconBellCheck size={48} color="var(--mantine-color-gray-5)" aria-hidden="true" />
              <Title order={3} size="h4" style={{ fontSize: '1.2rem' }}>
                Brak nowych powiadomień
              </Title>
              <Text size="md" c="dimmed" style={{ fontSize: '1rem' }}>
                {unreadOnly
                  ? 'Nie masz żadnych nieprzeczytanych powiadomień.'
                  : 'Wszystkie sprawy masz już przeczytane.'}
              </Text>
            </Stack>
          </Card>
        ) : (
          <Stack
            component="ul"
            gap="md"
            p={0}
            m={0}
            style={{ listStyle: 'none' }}
            aria-label="Lista Twoich powiadomień"
          >
            {notifications.map((item: NotificationJs) => {
              const typeCfg = NOTIFICATION_TYPE_CONFIG[item.type] ?? {
                label: 'Informacja',
                color: 'blue',
              }
              const isUnread = !item.read

              return (
                <li key={item.id}>
                  <Card
                    withBorder
                    padding="md"
                    radius="md"
                    style={{
                      borderLeft: isUnread
                        ? '5px solid var(--mantine-color-blue-filled)'
                        : '1px solid var(--mantine-color-gray-3)',
                      backgroundColor: isUnread
                        ? 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))'
                        : undefined,
                    }}
                  >
                    <Stack gap="xs">
                      <Group justify="space-between" align="center" wrap="wrap" gap="xs">
                        <Badge
                          color={typeCfg.color}
                          variant={isUnread ? 'filled' : 'light'}
                          size="md"
                          leftSection={getNotificationIcon(item.type)}
                          styles={{ label: { fontSize: '0.85rem', fontWeight: 600 } }}
                        >
                          {typeCfg.label}
                        </Badge>

                        <Text
                          component="time"
                          dateTime={item.createdAt}
                          title={formatPolishDateTime(item.createdAt)}
                          size="xs"
                          c="dimmed"
                          style={{ fontSize: '0.9rem' }}
                        >
                          {formatRelativePolishTime(item.createdAt)}
                        </Text>
                      </Group>

                      <Title order={3} size="h5" style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                        {item.title}
                      </Title>

                      <Text size="md" style={{ fontSize: '1.05rem', lineHeight: 1.5 }}>
                        {item.body}
                      </Text>

                      <Group justify="space-between" align="center" mt="xs" wrap="wrap">
                        {item.type === 'MESSAGE_RECEIVED' && (
                          <Button
                            component={Link}
                            to="/wiadomosci"
                            onClick={onClose}
                            variant="subtle"
                            color="blue"
                            size="sm"
                            leftSection={<IconInbox size={18} aria-hidden="true" />}
                            styles={{ root: { fontSize: '0.95rem' } }}
                          >
                            Otwórz skrzynkę wiadomości
                          </Button>
                        )}

                        {isUnread && (
                          <Button
                            onClick={() => void markRead(item.id)}
                            variant="light"
                            color="gray"
                            size="sm"
                            leftSection={<IconCheck size={18} aria-hidden="true" />}
                            styles={{ root: { fontSize: '0.95rem', fontWeight: 600 } }}
                          >
                            Oznacz jako przeczytane
                          </Button>
                        )}
                      </Group>
                    </Stack>
                  </Card>
                </li>
              )
            })}
          </Stack>
        )}
      </Stack>
    </Drawer>
  )
}
