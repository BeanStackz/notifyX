import { create } from 'zustand'
import { apiRequest } from '../lib/api'
import type { Organization, Plan, Project, Subscription } from '../lib/types'

type DashboardState = {
  organizations: Organization[]
  projects: Project[]
  plans: Plan[]
  subscription: Subscription | null
  isLoading: boolean
  error: string | null
  load: (accessToken: string) => Promise<void>
}

export const useDashboardStore = create<DashboardState>((set) => ({
  organizations: [],
  projects: [],
  plans: [],
  subscription: null,
  isLoading: false,
  error: null,
  load: async (accessToken) => {
    set({ isLoading: true, error: null })
    try {
      const [organizations, plans] = await Promise.all([
        apiRequest<Organization[]>('/api/v1/organizations', {}, accessToken),
        apiRequest<Plan[]>('/api/v1/plans', {}, accessToken),
      ])
      const organization = organizations[0]
      const [projects, subscription] = organization
        ? await Promise.all([
            apiRequest<Project[]>(`/api/v1/organizations/${organization.id}/projects`, {}, accessToken),
            apiRequest<Subscription>(`/api/v1/organizations/${organization.id}/subscription`, {}, accessToken).catch(() => null),
          ])
        : [[], null]
      set({ organizations, plans, projects, subscription, isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to load dashboard data.' })
    }
  },
}))
