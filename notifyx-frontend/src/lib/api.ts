const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:4040'

export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  accessToken?: string,
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | { message?: string } | null

  if (!response.ok || (body && 'success' in body && !body.success)) {
    const message = body && 'message' in body && body.message ? body.message : `Request failed (${response.status})`
    throw new ApiError(message, response.status)
  }

  if (!body || !('data' in body)) throw new ApiError('The server returned an invalid response.', response.status)
  return body.data
}
