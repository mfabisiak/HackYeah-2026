import { createTheme, DEFAULT_THEME, type MantineColorsTuple } from '@mantine/core'

// Mantine's default palette and focus ring meet WCAG 2.1 AA contrast; keep custom colours in this file
// and verify them (>= 4.5:1 for text) before changing.
// Shade 8 is the light-scheme primary; the defaults give 4.0:1 (teal) on white and 4.1:1 (blue) on the light alert
// background, so both are darkened (axe color-contrast, checked on every view).
const darken = (palette: MantineColorsTuple, overrides: Record<number, string>): MantineColorsTuple =>
  palette.map((shade, index) => overrides[index] ?? shade) as unknown as MantineColorsTuple

export const theme = createTheme({
  primaryColor: 'blue',
  primaryShade: { light: 8, dark: 5 },
  colors: {
    blue: darken(DEFAULT_THEME.colors.blue, { 8: '#1864ab' }),
    teal: darken(DEFAULT_THEME.colors.teal, { 8: '#087f5b', 9: '#066649' }),
  },
  fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  defaultRadius: 'md',
  focusRing: 'always',
})
