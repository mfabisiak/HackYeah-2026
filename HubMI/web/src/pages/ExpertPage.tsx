import { RequireRole } from '../auth/RequireRole'
import { ExpertPanel } from '../features/expert/ExpertPanel'

export function ExpertPage() {
  return (
    <RequireRole requiredRole={['expert', 'admin']}>
      <ExpertPanel />
    </RequireRole>
  )
}
