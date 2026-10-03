import { QueryClient } from '@tanstack/react-query'
import type { ApiResult } from 'hubmi-client'
import { ApiClientError } from './errors'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof ApiClientError) {
          if ([400, 401, 403, 404].includes(error.status)) return false
        }
        return failureCount < 2
      },
      staleTime: 1000 * 30, // 30s
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
})

export async function unwrapApiResult<T>(promise: Promise<ApiResult<T>>): Promise<T> {
  const res = await promise
  if (!res.ok || res.error) {
    if (res.error) {
      throw new ApiClientError(res.error)
    }
    throw new Error('Operacja nie powiodła się.')
  }
  return res.value as T
}
