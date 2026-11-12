import { create } from 'zustand'
import { apiRequest } from '../lib/api'
import type { AuthResponse, User } from '../lib/types'

const ACCESS_TOKEN_KEY = 'notifyx.accessToken'
const REFRESH_TOKEN_KEY = 'notifyx.refreshToken'
const USER_KEY = 'notifyx.user'

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

const storedUser = localStorage.getItem(USER_KEY)

function saveAuth(auth: AuthResponse) {
  localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, auth.refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
  return { user: auth.user, accessToken: auth.accessToken, refreshToken: auth.refreshToken }
}

export const useAuthStore = create<AuthState>((set) => ({
  user: storedUser ? (JSON.parse(storedUser) as User) : null,
  accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
  refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
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
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      set({ user: null, accessToken: null, refreshToken: null, error: null })
    }
  },
  clearError: () => set({ error: null }),
}))
