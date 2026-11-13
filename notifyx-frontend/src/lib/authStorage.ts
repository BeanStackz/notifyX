import type { AuthResponse, User } from './types'

export const ACCESS_TOKEN_KEY = 'notifyx.accessToken'
export const REFRESH_TOKEN_KEY = 'notifyx.refreshToken'
export const USER_KEY = 'notifyx.user'

export function saveAuthResponse(auth: AuthResponse) {
  localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
}

export function clearStoredAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getStoredRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function getStoredAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getStoredUser(): User | null {
  const storedUser = localStorage.getItem(USER_KEY)
  return storedUser ? (JSON.parse(storedUser) as User) : null
}
