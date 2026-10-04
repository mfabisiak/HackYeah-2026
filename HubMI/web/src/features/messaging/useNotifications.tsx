import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react'
import { hubApi } from '../../api/hubApi'
import { useAuth } from '../../auth/AuthContext'
import type { NotificationJs } from 'hubmi-client'

interface NotificationsContextValue {
  notifications: NotificationJs[]
  unreadCount: number
  unreadOnly: boolean
  setUnreadOnly: (unreadOnly: boolean) => void
  isLoading: boolean
  liveAnnouncement: string | null
  markRead: (id: string) => Promise<void>
  refresh: () => Promise<void>
}

const NotificationsContext = createContext<NotificationsContextValue | null>(null)

export function NotificationsProvider({ children }: { children: ReactNode }) {
  const { authenticated } = useAuth()
  const [notifications, setNotifications] = useState<NotificationJs[]>([])
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [liveAnnouncement, setLiveAnnouncement] = useState<string | null>(null)
  const knownNotificationIds = useRef<Set<string>>(new Set())
  const isFirstLoad = useRef(true)

  const refresh = async () => {
    setIsLoading(true)
    try {
      const res = await hubApi.notifications.list(unreadOnly, 0, 50)
      if (res.value) {
        const items = res.value.items
        setNotifications(items)
        setUnreadCount(items.filter((n) => !n.read).length)
      }
    } catch {
      // Ignore
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    if (!authenticated) {
      return
    }

    let cancelled = false

    const poll = async () => {
      try {
        const res = await hubApi.notifications.list(unreadOnly, 0, 50)
        if (cancelled) return
        if (res.value) {
          const items = res.value.items
          setNotifications(items)

          const count = items.filter((n) => !n.read).length
          setUnreadCount(count)

          const newItems = items.filter((n) => !knownNotificationIds.current.has(n.id))
          if (!isFirstLoad.current && newItems.length > 0) {
            setLiveAnnouncement(`Nowe powiadomienie: ${newItems[0].title}`)
          }

          items.forEach((n) => knownNotificationIds.current.add(n.id))
          isFirstLoad.current = false
        }
      } catch {
        // Quietly fail during background polling
      }
    }

    void poll()

    const interval = setInterval(() => {
      void poll()
    }, 20000)

    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [authenticated, unreadOnly])

  const markRead = async (id: string) => {
    try {
      const res = await hubApi.notifications.markRead(id)
      if (!res.error) {
        setNotifications((prev) =>
          prev.map((item) => (item.id === id ? { ...item, read: true } as unknown as NotificationJs : item)),
        )
        setUnreadCount((prev) => Math.max(0, prev - 1))
      }
    } catch {
      // Ignore
    }
  }

  return (
    <NotificationsContext.Provider
      value={{
        notifications,
        unreadCount,
        unreadOnly,
        setUnreadOnly,
        isLoading,
        liveAnnouncement,
        markRead,
        refresh,
      }}
    >
      {children}
    </NotificationsContext.Provider>
  )
}

export function useNotifications() {
  const context = useContext(NotificationsContext)
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationsProvider')
  }
  return context
}
