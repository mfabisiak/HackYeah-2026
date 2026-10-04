const dateFormat = new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'long', year: 'numeric' })
const dateTimeFormat = new Intl.DateTimeFormat('pl-PL', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})
const timeFormat = new Intl.DateTimeFormat('pl-PL', { hour: '2-digit', minute: '2-digit' })

/** `4 października 2026` */
export const formatDate = (iso: string): string => dateFormat.format(new Date(iso))

/** `4 października 2026, 12:03` */
export const formatDateTime = (iso: string): string => dateTimeFormat.format(new Date(iso))

/** `12:03` */
export const formatTime = (date: Date): string => timeFormat.format(date)

const MS_PER_DAY = 86_400_000

/** Whole days from `now` until `iso`, rounded up; negative when `iso` is in the past. */
export const daysUntil = (iso: string, now: Date = new Date()): number =>
  Math.ceil((new Date(iso).getTime() - now.getTime()) / MS_PER_DAY)
