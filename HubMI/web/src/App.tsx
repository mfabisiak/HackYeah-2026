import { Route, Routes } from 'react-router-dom'
import { Shell } from './layout/Shell'
import { AccountPage } from './pages/AccountPage'
import { AdminPage } from './pages/AdminPage'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { StatusPage } from './pages/StatusPage'
import { RequireAuth } from './auth/RequireAuth'

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

