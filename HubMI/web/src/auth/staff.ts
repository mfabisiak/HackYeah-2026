/** Realm roles of ROPS officials: they moderate submissions and answer residents in the name of ROPS. */
export const STAFF_ROLES = ['admin', 'expert'] as const

export const isStaff = (hasRole: (role: string) => boolean): boolean => STAFF_ROLES.some(hasRole)
