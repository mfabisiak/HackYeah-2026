import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Divider,
  Group,
  Stack,
  Text,
  Textarea,
  ThemeIcon,
  Title,
} from '@mantine/core'
import {
  IconAlertCircle,
  IconArrowLeft,
  IconAward,
  IconBuildingCommunity,
  IconClock,
  IconRefresh,
  IconSend,
  IconUser,
} from '@tabler/icons-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import { PARTICIPANT_ROLE_CONFIG, formatPolishDateTime } from './constants'
import type { MessageJs, ThreadJs } from 'hubmi-client'

export function ThreadDetailPage() {
  const { threadId } = useParams<{ threadId: string }>()
  const { login } = useAuth()

  const [thread, setThread] = useState<ThreadJs | null>(null)
  const [messages, setMessages] = useState<MessageJs[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [is404, setIs404] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [replyText, setReplyText] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [replyError, setReplyError] = useState<string | null>(null)

  const messagesEndRef = useRef<HTMLDivElement>(null)

  const [refreshTrigger, setRefreshTrigger] = useState(0)

  useEffect(() => {
    if (!threadId) return
    let cancelled = false

    const load = async () => {
      try {
        const msgRes = await hubApi.threads.messages(threadId)
        if (cancelled) return

        if (msgRes.error) {
          if (msgRes.error.status === 404) {
            setIs404(true)
            return
          }
          setError(msgRes.error.message || 'Nie udało się pobrać wiadomości.')
          return
        }

        if (msgRes.value) {
          setMessages(msgRes.value)
        }

        const listRes = await hubApi.threads.list(0, 50)
        if (cancelled) return
        if (listRes.value) {
          const found = listRes.value.items.find((t) => t.id === threadId)
          if (found) {
            setThread(found)
            document.title = `${found.subject} | Wiadomości HubMI`
          }
        }
      } catch {
        if (!cancelled) {
          setError('Wystąpił błąd podczas ładowania rozmowy.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void load()

    return () => {
      cancelled = true
    }
  }, [threadId, refreshTrigger])

  const handleSendReply = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!threadId || !replyText.trim()) return

    if (replyText.trim().length < 2) {
      setReplyError('Odpowiedź musi zawierać co najmniej 2 znaki.')
      return
    }

    setIsSubmitting(true)
    setReplyError(null)

    try {
      const res = await hubApi.threads.postMessage(threadId, replyText.trim())
      if (res.error) {
        if (res.error.status === 401) {
          login()
          return
        }
        setReplyError(res.error.message || 'Nie udało się wysłać odpowiedzi. Spróbuj ponownie.')
      } else if (res.value) {
        setMessages((prev) => [...prev, res.value as MessageJs])
        setReplyText('')
        // Scroll to the latest message
        setTimeout(() => {
          messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
        }, 100)
      }
    } catch {
      setReplyError('Wystąpił błąd sieci. Spróbuj ponownie za chwilę.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <LoadingState message="Wczytuję historię rozmowy..." minHeight={300} />
  }

  if (is404) {
    return (
      <Stack gap="xl">
        <PageHeader
          title="Nie znaleziono rozmowy"
          breadcrumbs={[
            { title: 'Strona główna', href: '/' },
            { title: 'Wiadomości', href: '/wiadomosci' },
            { title: 'Brak rozmowy' },
          ]}
        />
        <Card withBorder padding="xl" radius="md" style={{ textAlign: 'center' }}>
          <Stack align="center" gap="md" py="xl">
            <ThemeIcon size={64} radius="xl" color="red" variant="light">
              <IconAlertCircle size={36} aria-hidden="true" />
            </ThemeIcon>
            <Title order={2} size="h3" style={{ fontSize: '1.4rem' }}>
              Rozmowa nie istnieje lub brak dostępu
            </Title>
            <Text size="lg" c="dimmed" style={{ maxWidth: 600, fontSize: '1.1rem', lineHeight: 1.6 }}>
              Wątek o podanym identyfikatorze nie został odnaleziony lub należy do innego
              użytkownika. Jeśli to błąd, upewnij się, że jesteś zalogowany na właściwe konto.
            </Text>
            <Button
              component={Link}
              to="/wiadomosci"
              size="lg"
              color="blue"
              leftSection={<IconArrowLeft size={20} aria-hidden="true" />}
              styles={{ root: { fontSize: '1.05rem', fontWeight: 600 } }}
            >
              Wróć do listy wiadomości
            </Button>
          </Stack>
        </Card>
      </Stack>
    )
  }

  const threadSubject = thread?.subject || 'Wątek rozmowy z ROPS'

  return (
    <Stack gap="xl">
      <PageHeader
        title={threadSubject}
        subtitle="Bezpośredni wątek rozmowy z pracownikami Regionalnego Ośrodka Polityki Społecznej."
        breadcrumbs={[
          { title: 'Strona główna', href: '/' },
          { title: 'Wiadomości', href: '/wiadomosci' },
          { title: threadSubject },
        ]}
      />

      <Group justify="space-between" align="center" wrap="wrap" gap="md">
        <Button
          component={Link}
          to="/wiadomosci"
          variant="subtle"
          color="gray"
          size="lg"
          leftSection={<IconArrowLeft size={22} aria-hidden="true" />}
          styles={{ root: { fontSize: '1.05rem' } }}
        >
          Wróć do listy wiadomości
        </Button>

        <Button
          variant="default"
          size="md"
          onClick={() => setRefreshTrigger((prev) => prev + 1)}
          leftSection={<IconRefresh size={18} aria-hidden="true" />}
          styles={{ root: { fontSize: '1rem' } }}
        >
          Odśwież wiadomości
        </Button>
      </Group>

      {thread?.relatedIdeaId && (
        <Alert color="blue" title="Rozmowa powiązana z pomysłem" radius="md">
          <Text size="md" style={{ fontSize: '1.05rem' }}>
            Ten wątek dotyczy zgłoszonego pomysłu innowacji społecznej o identyfikatorze:{' '}
            <strong>{thread.relatedIdeaId}</strong>.
          </Text>
        </Alert>
      )}

      {error && (
        <Alert
          icon={<IconAlertCircle size={20} />}
          color="red"
          title="Błąd wczytywania"
          radius="md"
        >
          {error}
        </Alert>
      )}

      {/* Messages history log */}
      <Stack gap="md">
        <Title order={2} size="h3" style={{ fontSize: '1.35rem', fontWeight: 700 }}>
          Historia wiadomości ({messages.length})
        </Title>

        <div
          role="log"
          aria-label="Historia wiadomości w wątku"
          aria-live="polite"
          style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}
        >
          {messages.map((msg, index) => {
            const roleCfg = PARTICIPANT_ROLE_CONFIG[msg.authorRole] ?? {
              label: msg.authorName || 'Użytkownik',
              badgeColor: 'gray',
              description: 'Uczestnik',
            }
            const isRops = msg.authorRole === 'ADMIN'
            const isExpert = msg.authorRole === 'EXPERT'
            const isAuthor = msg.authorRole === 'AUTHOR'

            return (
              <Card
                key={msg.id || index}
                withBorder
                padding="lg"
                radius="md"
                style={{
                  borderLeft: isRops
                    ? '6px solid var(--mantine-color-blue-filled)'
                    : isExpert
                    ? '6px solid var(--mantine-color-teal-filled)'
                    : '6px solid var(--mantine-color-gray-5)',
                  backgroundColor: isRops
                    ? 'light-dark(var(--mantine-color-blue-0), var(--mantine-color-dark-6))'
                    : isExpert
                    ? 'light-dark(var(--mantine-color-teal-0), var(--mantine-color-dark-6))'
                    : undefined,
                }}
              >
                <Stack gap="sm">
                  <Group justify="space-between" align="center" wrap="wrap" gap="sm">
                    <Group gap="sm" align="center">
                      <Avatar
                        radius="xl"
                        color={roleCfg.badgeColor}
                        size="md"
                      >
                        {isRops ? (
                          <IconBuildingCommunity size={20} aria-hidden="true" />
                        ) : isExpert ? (
                          <IconAward size={20} aria-hidden="true" />
                        ) : (
                          <IconUser size={20} aria-hidden="true" />
                        )}
                      </Avatar>

                      <div>
                        <Group gap="xs" align="center">
                          <Text fw={700} style={{ fontSize: '1.15rem' }}>
                            {msg.authorName || (isAuthor ? 'Ty' : 'Pracownik ROPS')}
                          </Text>
                          <Badge
                            color={roleCfg.badgeColor}
                            variant="filled"
                            size="md"
                            styles={{ label: { fontSize: '0.85rem', fontWeight: 600 } }}
                          >
                            {roleCfg.label}
                          </Badge>
                        </Group>
                        <Text size="xs" c="dimmed" style={{ fontSize: '0.9rem' }}>
                          {roleCfg.description}
                        </Text>
                      </div>
                    </Group>

                    <Group gap={6} align="center">
                      <IconClock size={16} color="var(--mantine-color-gray-6)" aria-hidden="true" />
                      <Text
                        component="time"
                        dateTime={msg.sentAt}
                        size="sm"
                        c="dimmed"
                        style={{ fontSize: '0.95rem' }}
                      >
                        {formatPolishDateTime(msg.sentAt)}
                      </Text>
                    </Group>
                  </Group>

                  <Divider />

                  <Text
                    size="lg"
                    style={{
                      fontSize: '1.15rem',
                      lineHeight: 1.6,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                    }}
                  >
                    {msg.text}
                  </Text>
                </Stack>
              </Card>
            )
          })}
          <div ref={messagesEndRef} />
        </div>
      </Stack>

      {/* Reply input section */}
      <Card withBorder padding="xl" radius="md">
        <form onSubmit={handleSendReply}>
          <Stack gap="md">
            <div>
              <Title order={3} size="h4" style={{ fontSize: '1.25rem', fontWeight: 700 }}>
                Napisz odpowiedź
              </Title>
              <Text size="md" c="dimmed" style={{ fontSize: '1.05rem', marginTop: 4 }}>
                Wpisz swoją odpowiedź w poniższym polu i naciśnij przycisk »Wyślij odpowiedź«.
              </Text>
            </div>

            {replyError && (
              <Alert
                icon={<IconAlertCircle size={20} />}
                color="red"
                title="Błąd wysyłania"
                radius="md"
                role="alert"
              >
                {replyError}
              </Alert>
            )}

            <Textarea
              label="Treść Twojej odpowiedzi"
              placeholder="Wpisz treść odpowiedzi dla pracownika ROPS..."
              minRows={4}
              required
              value={replyText}
              onChange={(e) => {
                setReplyText(e.currentTarget.value)
                if (replyError) setReplyError(null)
              }}
              size="md"
              styles={{
                label: { fontSize: '1.1rem', fontWeight: 600, marginBottom: 8 },
                input: { fontSize: '1.1rem', lineHeight: 1.6, padding: '12px' },
              }}
            />

            <Group justify="flex-end">
              <Button
                type="submit"
                size="lg"
                color="blue"
                loading={isSubmitting}
                disabled={!replyText.trim()}
                leftSection={<IconSend size={22} aria-hidden="true" />}
                styles={{ root: { fontSize: '1.1rem', fontWeight: 600, height: 48 } }}
              >
                Wyślij odpowiedź
              </Button>
            </Group>
          </Stack>
        </form>
      </Card>
    </Stack>
  )
}
