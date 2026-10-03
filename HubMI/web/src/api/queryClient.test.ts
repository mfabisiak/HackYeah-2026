import { describe, expect, it } from 'vitest'
import { unwrapApiResult } from './queryClient'
import { ApiClientError } from './errors'
import { ApiErrorJs, ApiResult } from 'hubmi-client'

describe('unwrapApiResult', () => {
  it('unwraps successful ApiResult value', async () => {
    const result = new ApiResult<{ id: string }>({ id: '123' }, null)
    const data = await unwrapApiResult(Promise.resolve(result))

    expect(data).toEqual({ id: '123' })
  })

  it('throws ApiClientError when ApiResult contains error', async () => {
    const apiError = new ApiErrorJs(400, 'VALIDATION_FAILED', 'Invalid input')
    const result = new ApiResult<null>(null, apiError)

    await expect(unwrapApiResult(Promise.resolve(result))).rejects.toThrow(ApiClientError)
  })
})
