import type { AuthResponse } from './types'
import { clearStoredAuth, getStoredRefreshToken, saveAuthResponse } from './authStorage'

const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? 'https://webhook.200630.xyz'
).replace(/\/+$/, '')
let refreshRequest: Promise<string> | null = null

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

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getStoredRefreshToken()
  if (!refreshToken) throw new ApiError('Your session has expired. Please sign in again.', 401)

  if (!refreshRequest) {
    refreshRequest = fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    }).then(async (response) => {
      const body = (await response.json().catch(() => null)) as ApiResponse<AuthResponse> | { message?: string } | null
      if (!response.ok || !body || !('data' in body)) {
        const message = body && 'message' in body && body.message ? body.message : 'Your session has expired. Please sign in again.'
        throw new ApiError(message, response.status)
      }
      saveAuthResponse(body.data)
      window.dispatchEvent(new CustomEvent('notifyx:auth-refreshed', { detail: body.data }))
      return body.data.accessToken
    }).catch((error) => {
      clearStoredAuth()
      window.dispatchEvent(new Event('notifyx:auth-expired'))
      throw error
    }).finally(() => {
      refreshRequest = null
    })
  }
  return refreshRequest
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  accessToken?: string,
): Promise<T> {
  const request = async (token?: string) => {
    const headers = new Headers(options.headers)
    headers.set('Content-Type', 'application/json')
    if (token) headers.set('Authorization', `Bearer ${token}`)

    const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
    const body = (await response.json().catch(() => null)) as ApiResponse<T> | { message?: string } | null
    return { response, body }
  }

  const isAuthEndpoint = path.startsWith('/api/v1/auth/')
  let { response, body } = await request(accessToken)

  if (response.status === 401 && accessToken && !isAuthEndpoint) {
    const newAccessToken = await refreshAccessToken()
    ;({ response, body } = await request(newAccessToken))
  }

  if (!response.ok || (body && 'success' in body && !body.success)) {
    const message = body && 'message' in body && body.message ? body.message : `Request failed (${response.status})`
    throw new ApiError(message, response.status)
  }

  if (!body || !('data' in body)) throw new ApiError('The server returned an invalid response.', response.status)
  return body.data
}
