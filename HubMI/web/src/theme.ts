import { createTheme } from '@mantine/core'

// Mantine's default palette and focus ring meet WCAG 2.1 AA contrast; keep custom colours in this file
// and verify them (>= 4.5:1 for text) before changing.
export const theme = createTheme({
  primaryColor: 'blue',
  primaryShade: { light: 7, dark: 5 },
  fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  defaultRadius: 'md',
  focusRing: 'always',
})
