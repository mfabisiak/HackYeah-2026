import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { Shell } from './layout/Shell'
import { AccountPage } from './pages/AccountPage'
import { AdminPage } from './pages/AdminPage'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { StatusPage } from './pages/StatusPage'
import { RequireAuth } from './auth/RequireAuth'
import { LoadingState } from './components'

import {
  ChallengesListPage,
  InnovationDetailPage,
  InnovationsListPage,
  MaterialsListPage,
} from './features/knowledge'
import {
  NotificationsProvider,
  ThreadDetailPage,
  ThreadsListPage,
} from './features/messaging'

// The idea and application screens are only needed by people who submit something, so they load on demand.
const IdeaWizardPage = lazy(() => import('./features/ideas').then((m) => ({ default: m.IdeaWizardPage })))
const MyIdeasPage = lazy(() => import('./features/ideas').then((m) => ({ default: m.MyIdeasPage })))
const IdeaDetailPage = lazy(() => import('./features/ideas').then((m) => ({ default: m.IdeaDetailPage })))
const CallsListPage = lazy(() => import('./features/applications').then((m) => ({ default: m.CallsListPage })))
const CallDetailPage = lazy(() => import('./features/applications').then((m) => ({ default: m.CallDetailPage })))
const ApplicationsListPage = lazy(() =>
  import('./features/applications').then((m) => ({ default: m.ApplicationsListPage })),
)
const ApplicationWizardPage = lazy(() =>
  import('./features/applications').then((m) => ({ default: m.ApplicationWizardPage })),
)

const signedIn = (page: React.ReactNode) => (
  <RequireAuth>
    <Suspense fallback={<LoadingState message="Ładowanie..." />}>{page}</Suspense>
  </RequireAuth>
)

const onDemand = (page: React.ReactNode) => (
  <Suspense fallback={<LoadingState message="Ładowanie..." />}>{page}</Suspense>
)

export default function App() {
  return (
    <NotificationsProvider>
      <Routes>
        <Route element={<Shell />}>
          <Route index element={<HomePage />} />
          <Route path="dopasuj" element={<HomePage />} />
          <Route path="innowacje" element={<InnovationsListPage />} />
          <Route path="innowacje/:id" element={<InnovationDetailPage />} />
          <Route path="wyzwania" element={<ChallengesListPage />} />
          <Route path="materialy" element={<MaterialsListPage />} />
          <Route path="pomysly" element={signedIn(<MyIdeasPage />)} />
          <Route path="pomysly/nowy" element={signedIn(<IdeaWizardPage />)} />
          <Route path="pomysly/:id" element={signedIn(<IdeaDetailPage />)} />
          <Route path="nabory" element={onDemand(<CallsListPage />)} />
          <Route path="nabory/:id" element={onDemand(<CallDetailPage />)} />
          <Route path="wnioski" element={signedIn(<ApplicationsListPage />)} />
          <Route path="wnioski/:id" element={signedIn(<ApplicationWizardPage />)} />
          <Route path="status" element={<StatusPage />} />
          <Route
            path="wiadomosci"
            element={
              <RequireAuth>
                <ThreadsListPage />
              </RequireAuth>
            }
          />
          <Route
            path="wiadomosci/:threadId"
            element={
              <RequireAuth>
                <ThreadDetailPage />
              </RequireAuth>
            }
          />
          <Route
            path="konto"
            element={
              <RequireAuth>
                <AccountPage />
              </RequireAuth>
            }
          />
          <Route path="admin" element={<AdminPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </NotificationsProvider>
  )
}

