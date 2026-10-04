import { createTheme, DEFAULT_THEME, type MantineColorsTuple } from '@mantine/core'

// Mantine's default palette and focus ring meet WCAG 2.1 AA contrast; keep custom colours in this file
// and verify them (>= 4.5:1 for text) before changing.
// Shade 8 is the primary of both schemes; the defaults give 4.0:1 (teal) on white and 4.1:1 (blue) on the light alert
// background, so both are darkened (axe color-contrast, checked on every view).
const darken = (palette: MantineColorsTuple, overrides: Record<number, string>): MantineColorsTuple =>
  palette.map((shade, index) => overrides[index] ?? shade) as unknown as MantineColorsTuple

export const theme = createTheme({
  primaryColor: 'blue',
  // Filled buttons and avatars carry white text, so the dark scheme needs the dark shade too (2.99:1 with shade 5).
  primaryShade: 8,
  colors: {
    blue: darken(DEFAULT_THEME.colors.blue, { 8: '#1864ab' }),
    teal: darken(DEFAULT_THEME.colors.teal, { 8: '#087f5b', 9: '#066649' }),
    // Text of the light variant is shade 9: 3.8:1 by default on the pale green of an alert.
    green: darken(DEFAULT_THEME.colors.green, { 9: '#237032' }),
    // Same for the text of orange outline and yellow light badges (3.2:1 and 2.7:1 by default).
    orange: darken(DEFAULT_THEME.colors.orange, { 7: '#c2410c', 8: '#b53a0a', 9: '#9a3412' }),
    yellow: darken(DEFAULT_THEME.colors.yellow, { 9: '#8f4a00' }),
  },
  fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  headings: { fontWeight: '700' },
  defaultRadius: 'md',
  focusRing: 'always',
  components: {
    // A link inside text must not rely on colour alone (WCAG 1.4.1), and buttons keep a 42px target (WCAG 2.5.8).
    Anchor: { defaultProps: { underline: 'always' } },
    Button: { defaultProps: { size: 'md' } },
    Card: { defaultProps: { radius: 'lg' } },
    Paper: { defaultProps: { radius: 'lg' } },
  },
})
