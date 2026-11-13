import { create } from 'zustand'
import { apiRequest } from '../lib/api'
import { clearStoredAuth, getStoredAccessToken, getStoredRefreshToken, getStoredUser, saveAuthResponse } from '../lib/authStorage'
import type { AuthResponse, User } from '../lib/types'

type AuthState = {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  isLoading: boolean
  error: string | null
  login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  clearError: () => void
}

function saveAuth(auth: AuthResponse) {
  saveAuthResponse(auth)
  return { user: auth.user, accessToken: auth.accessToken, refreshToken: auth.refreshToken }
}

export const useAuthStore = create<AuthState>((set) => ({
  user: getStoredUser(),
  accessToken: getStoredAccessToken(),
  refreshToken: getStoredRefreshToken(),
  isLoading: false,
  error: null,
  login: async (email, password) => {
    set({ isLoading: true, error: null })
    try {
      const auth = await apiRequest<AuthResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
      set({ ...saveAuth(auth), isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to sign in.' })
      throw error
    }
  },
  register: async (fullName, email, password) => {
    set({ isLoading: true, error: null })
    try {
      await apiRequest<User>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify({ fullName, email, password }) })
      const auth = await apiRequest<AuthResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
      set({ ...saveAuth(auth), isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to create your account.' })
      throw error
    }
  },
  logout: async () => {
    const refreshToken = useAuthStore.getState().refreshToken
    try {
      if (refreshToken) await apiRequest('/api/v1/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) })
    } finally {
      clearStoredAuth()
      set({ user: null, accessToken: null, refreshToken: null, error: null })
    }
  },
  clearError: () => set({ error: null }),
}))

window.addEventListener('notifyx:auth-refreshed', (event) => {
  const auth = (event as CustomEvent<AuthResponse>).detail
  useAuthStore.setState({ ...saveAuth(auth), error: null })
})
window.addEventListener('notifyx:auth-expired', () => {
  clearStoredAuth()
  useAuthStore.setState({ user: null, accessToken: null, refreshToken: null, error: 'Your session has expired. Please sign in again.' })
})
