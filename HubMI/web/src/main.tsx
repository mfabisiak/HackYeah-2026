import '@mantine/core/styles.css'
import './index.css'

import { MantineProvider } from '@mantine/core'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './auth/AuthContext'
import { theme } from './theme'

import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './api/queryClient'

async function prepareApp() {
  if (import.meta.env.VITE_MOCK === 'true') {
    const { worker } = await import('./mocks/browser')
    await worker.start({ onUnhandledRequest: 'bypass' })
  }
}

prepareApp().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <MantineProvider theme={theme} defaultColorScheme="auto">
          <BrowserRouter>
            <AuthProvider>
              <App />
            </AuthProvider>
          </BrowserRouter>
        </MantineProvider>
      </QueryClientProvider>
    </StrictMode>,
  )
})
