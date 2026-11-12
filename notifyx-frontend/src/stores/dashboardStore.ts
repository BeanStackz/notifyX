import { create } from 'zustand'
import { apiRequest } from '../lib/api'
import type { Organization, Plan, Project, Subscription } from '../lib/types'

type DashboardState = {
  organizations: Organization[]
  selectedOrganizationId: string | null
  projects: Project[]
  plans: Plan[]
  subscription: Subscription | null
  isLoading: boolean
  error: string | null
  load: (accessToken: string) => Promise<void>
  selectOrganization: (accessToken: string, organizationId: string) => Promise<void>
  createOrganization: (accessToken: string, name: string, slug: string) => Promise<void>
  createProject: (accessToken: string, organizationId: string, name: string, slug: string, description: string) => Promise<void>
  updateProject: (accessToken: string, organizationId: string, projectId: string, name: string, slug: string, description: string) => Promise<void>
  archiveProject: (accessToken: string, organizationId: string, projectId: string) => Promise<void>
}

async function loadOrganizationData(accessToken: string, organizationId: string) {
  const [projects, subscription] = await Promise.all([
    apiRequest<Project[]>(`/api/v1/organizations/${organizationId}/projects`, {}, accessToken),
    apiRequest<Subscription>(`/api/v1/organizations/${organizationId}/subscription`, {}, accessToken).catch(() => null),
  ])
  return { projects, subscription }
}

export const useDashboardStore = create<DashboardState>((set) => ({
  organizations: [],
  selectedOrganizationId: null,
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
      const currentOrganizationId = useDashboardStore.getState().selectedOrganizationId
      const organizationId = organizations.some((organization) => organization.id === currentOrganizationId)
        ? currentOrganizationId
        : organizations[0]?.id ?? null
      const organizationData = organizationId ? await loadOrganizationData(accessToken, organizationId) : { projects: [], subscription: null }
      set({ organizations, selectedOrganizationId: organizationId, plans, ...organizationData, isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to load dashboard data.' })
    }
  },
  selectOrganization: async (accessToken, organizationId) => {
    set({ isLoading: true, error: null })
    try {
      const organizationData = await loadOrganizationData(accessToken, organizationId)
      set({ selectedOrganizationId: organizationId, ...organizationData, isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to switch organization.' })
    }
  },
  createOrganization: async (accessToken, name, slug) => {
    set({ isLoading: true, error: null })
    try {
      const organization = await apiRequest<Organization>('/api/v1/organizations', {
        method: 'POST',
        body: JSON.stringify({ name, slug }),
      }, accessToken)
      const organizationData = await loadOrganizationData(accessToken, organization.id)
      set((state) => ({
        organizations: [...state.organizations, organization],
        selectedOrganizationId: organization.id,
        ...organizationData,
        isLoading: false,
      }))
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to create organization.' })
      throw error
    }
  },
  createProject: async (accessToken, organizationId, name, slug, description) => {
    set({ isLoading: true, error: null })
    try {
      const project = await apiRequest<Project>(`/api/v1/organizations/${organizationId}/projects`, {
        method: 'POST',
        body: JSON.stringify({ name, slug, description }),
      }, accessToken)
      set((state) => ({ projects: [...state.projects, project], isLoading: false }))
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to create project.' })
      throw error
    }
  },
  updateProject: async (accessToken, organizationId, projectId, name, slug, description) => {
    set({ isLoading: true, error: null })
    try {
      const project = await apiRequest<Project>(`/api/v1/organizations/${organizationId}/projects/${projectId}`, {
        method: 'PUT',
        body: JSON.stringify({ name, slug, description }),
      }, accessToken)
      set((state) => ({ projects: state.projects.map((item) => item.id === projectId ? project : item), isLoading: false }))
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to update project.' })
      throw error
    }
  },
  archiveProject: async (accessToken, organizationId, projectId) => {
    set({ isLoading: true, error: null })
    try {
      await apiRequest<void>(`/api/v1/organizations/${organizationId}/projects/${projectId}/archive`, { method: 'PATCH' }, accessToken)
      set((state) => ({ projects: state.projects.map((item) => item.id === projectId ? { ...item, status: 'ARCHIVED' } : item), isLoading: false }))
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to archive project.' })
      throw error
    }
  },
}))
