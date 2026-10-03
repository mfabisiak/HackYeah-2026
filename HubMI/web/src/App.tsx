import { Route, Routes } from 'react-router-dom'
import { Shell } from './layout/Shell'
import { AccountPage } from './pages/AccountPage'
import { AdminPage } from './pages/AdminPage'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { StatusPage } from './pages/StatusPage'
import { RequireAuth } from './auth/RequireAuth'

export default function App() {
  return (
    <Routes>
      <Route element={<Shell />}>
        <Route index element={<HomePage />} />
        <Route path="status" element={<StatusPage />} />
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
  )
}
