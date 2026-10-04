import { RequireRole } from '../auth/RequireRole'
import { AdminPanel } from '../features/admin/AdminPanel'

export function AdminPage() {
  return (
    <RequireRole requiredRole="admin">
      <AdminPanel />
    </RequireRole>
  )
}
