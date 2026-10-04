import '@testing-library/jest-dom/vitest'

// Mock window.matchMedia for Mantine
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

// Mock ResizeObserver for Mantine components
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

window.ResizeObserver = ResizeObserverMock

// jsdom has no font loading API; Mantine's autosizing Textarea listens to it
Object.defineProperty(document, 'fonts', {
  writable: true,
  value: { addEventListener: () => {}, removeEventListener: () => {} },
})
