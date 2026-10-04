// Shared look of form inputs: large text and visible labels, because many users are seniors.
export const INPUT_STYLES = {
  label: { fontSize: '1rem', fontWeight: 600, marginBottom: 4 },
  description: { fontSize: '0.95rem', marginBottom: 6 },
  input: { fontSize: '1.05rem' },
  error: { fontSize: '0.95rem', fontWeight: 500 },
} as const

/** Error text colour with enough contrast (≥ 4.5:1) on both light and dark backgrounds. */
export const ERROR_TEXT_COLOR = 'light-dark(var(--mantine-color-red-9), var(--mantine-color-red-3))'
