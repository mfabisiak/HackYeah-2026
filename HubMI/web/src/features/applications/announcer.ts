import { createContext, useContext } from 'react'

/** Politely announces a message to screen-reader users (e.g. “Dodano pozycję 2”). */
export const AnnouncerContext = createContext<(message: string) => void>(() => undefined)

export const useAnnouncer = (): ((message: string) => void) => useContext(AnnouncerContext)
