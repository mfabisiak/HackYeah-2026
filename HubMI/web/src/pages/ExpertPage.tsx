import { RequireRole } from '../auth/RequireRole'
import { AdminPanel } from '../features/admin/AdminPanel'

export function ExpertPage() {
  return (
    <RequireRole requiredRole="expert">
      <AdminPanel mode="expert" />
    </RequireRole>
  )
}
