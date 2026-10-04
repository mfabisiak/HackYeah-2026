import { vi } from 'vitest'
import type { ApiResult, StreamJs } from 'hubmi-client'

export interface FakeStream<T> {
  stream: StreamJs<T>
  /** Settles `result`, like the end of the real stream. */
  finish: (result: ApiResult<T>) => void
  cancel: ReturnType<typeof vi.fn>
}

/** A `StreamJs` whose end the test decides; what the listener is called with is up to the test as well. */
export function fakeStream<T>(): FakeStream<T> {
  const cancel = vi.fn()
  let finish: (result: ApiResult<T>) => void = () => {}
  const result = new Promise<ApiResult<T>>((resolve) => {
    finish = resolve
  })
  return { stream: { result, cancel } as unknown as StreamJs<T>, finish, cancel }
}
