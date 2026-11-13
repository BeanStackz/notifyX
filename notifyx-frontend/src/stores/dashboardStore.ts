import { create } from 'zustand'
import { apiRequest } from '../lib/api'
import type { AdminDashboard, ApiKey, CreatedApiKey, DeliveryTracking, NotificationResponse, OperationalHealth, Organization, Plan, Project, Subscription, SubscriptionCheckout, UsageAnalytics, User } from '../lib/types'

type DashboardState = {
  organizations: Organization[]
  selectedOrganizationId: string | null
  projects: Project[]
  plans: Plan[]
  subscription: Subscription | null
  apiKeys: ApiKey[]
  newlyCreatedApiKey: CreatedApiKey | null
  lastNotification: NotificationResponse | null
  deliveryTracking: DeliveryTracking | null
  trackingLoading: boolean
  notificationHistory: NotificationResponse[]
  historyLoading: boolean
  checkout: SubscriptionCheckout | null
  subscriptionMessage: string | null
  health: OperationalHealth | null
  analytics: UsageAnalytics | null
  adminDashboard: AdminDashboard | null
  isLoading: boolean
  error: string | null
  load: (accessToken: string) => Promise<void>
  selectOrganization: (accessToken: string, organizationId: string) => Promise<void>
  createOrganization: (accessToken: string, name: string, slug: string) => Promise<void>
  createProject: (accessToken: string, organizationId: string, name: string, slug: string, description: string) => Promise<void>
  updateProject: (accessToken: string, organizationId: string, projectId: string, name: string, slug: string, description: string) => Promise<void>
  archiveProject: (accessToken: string, organizationId: string, projectId: string) => Promise<void>
  loadApiKeys: (accessToken: string, organizationId: string, projectId: string) => Promise<void>
  createApiKey: (accessToken: string, organizationId: string, projectId: string, name: string, expiresInDays: number | null) => Promise<void>
  revokeApiKey: (accessToken: string, organizationId: string, projectId: string, apiKeyId: string) => Promise<void>
  clearNewApiKey: () => void
  sendNotification: (apiKey: string, projectId: string, request: { channel: string; recipient: string; subject?: string; content: string; metadata?: Record<string, string>; scheduledAt?: string }) => Promise<NotificationResponse>
  loadDeliveryTracking: (accessToken: string, projectId: string, notificationId: string) => Promise<DeliveryTracking>
  loadNotificationHistory: (accessToken: string, projectId: string) => Promise<void>
  retryNotification: (accessToken: string, projectId: string, notificationId: string) => Promise<void>
  createCheckout: (accessToken: string, organizationId: string, planCode: string) => Promise<void>
  verifyCheckout: (accessToken: string, organizationId: string, orderId: string, paymentId: string, signature: string) => Promise<void>
  clearCheckout: () => void
  cancelSubscription: (accessToken: string, organizationId: string) => Promise<void>
  loadHealth: (accessToken: string) => Promise<void>
  loadAnalytics: (accessToken: string, organizationId: string, days?: number) => Promise<void>
  loadAdminDashboard: (accessToken: string) => Promise<void>
  updateProfile: (accessToken: string, fullName: string) => Promise<User>
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
  apiKeys: [],
  newlyCreatedApiKey: null,
  lastNotification: null,
  deliveryTracking: null,
  trackingLoading: false,
  notificationHistory: [],
  historyLoading: false,
  checkout: null,
  subscriptionMessage: null,
  health: null,
  analytics: null,
  adminDashboard: null,
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
  loadApiKeys: async (accessToken, organizationId, projectId) => {
    set({ isLoading: true, error: null })
    try {
      const apiKeys = await apiRequest<ApiKey[]>(`/api/v1/organizations/${organizationId}/projects/${projectId}/api-keys`, {}, accessToken)
      set({ apiKeys, isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to load API keys.' })
    }
  },
  createApiKey: async (accessToken, organizationId, projectId, name, expiresInDays) => {
    set({ isLoading: true, error: null, newlyCreatedApiKey: null })
    try {
      const createdApiKey = await apiRequest<CreatedApiKey>(`/api/v1/organizations/${organizationId}/projects/${projectId}/api-keys`, {
        method: 'POST',
        body: JSON.stringify({ name, expiresInDays }),
      }, accessToken)
      const listedKey: ApiKey = {
        ...createdApiKey,
        lastUsedAt: null,
        revokedAt: null,
      }
      set((state) => ({ apiKeys: [...state.apiKeys, listedKey], newlyCreatedApiKey: createdApiKey, isLoading: false }))
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to create API key.' })
      throw error
    }
  },
  revokeApiKey: async (accessToken, organizationId, projectId, apiKeyId) => {
    set({ isLoading: true, error: null })
    try {
      const revokedKey = await apiRequest<ApiKey>(`/api/v1/organizations/${organizationId}/projects/${projectId}/api-keys/${apiKeyId}`, { method: 'DELETE' }, accessToken)
      set((state) => ({ apiKeys: state.apiKeys.map((key) => key.id === apiKeyId ? revokedKey : key), isLoading: false }))
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to revoke API key.' })
      throw error
    }
  },
  clearNewApiKey: () => set({ newlyCreatedApiKey: null }),
  sendNotification: async (apiKey, projectId, request) => {
    set({ isLoading: true, error: null, lastNotification: null })
    try {
      const notification = await apiRequest<NotificationResponse>('/api/v1/ingest/notifications', {
        method: 'POST',
        headers: {
          'X-NotifyX-API-Key': apiKey,
          'Idempotency-Key': crypto.randomUUID(),
        },
        body: JSON.stringify(request),
      })
      if (notification.projectId !== projectId) throw new Error('The notification was returned for a different project.')
      set({ isLoading: false, lastNotification: notification, deliveryTracking: null })
      return notification
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to send notification.' })
      throw error
    }
  },
  loadDeliveryTracking: async (accessToken, projectId, notificationId) => {
    set({ trackingLoading: true })
    try {
      const tracking = await apiRequest<DeliveryTracking>(`/api/v1/projects/${projectId}/notifications/${notificationId}`, {}, accessToken)
      set({ deliveryTracking: tracking, trackingLoading: false })
      return tracking
    } catch (error) {
      set({ trackingLoading: false })
      throw error
    }
  },
  loadNotificationHistory: async (accessToken, projectId) => {
    set({ historyLoading: true })
    try {
      const notificationHistory = await apiRequest<NotificationResponse[]>(`/api/v1/projects/${projectId}/notifications`, {}, accessToken)
      set({ notificationHistory, historyLoading: false })
    } catch (error) {
      set({ historyLoading: false })
      throw error
    }
  },
  retryNotification: async (accessToken, projectId, notificationId) => {
    await apiRequest<void>(`/api/v1/projects/${projectId}/notifications/${notificationId}/retry`, {
      method: 'POST',
    }, accessToken)
    await useDashboardStore.getState().loadNotificationHistory(accessToken, projectId)
  },
  createCheckout: async (accessToken, organizationId, planCode) => {
    set({ isLoading: true, error: null, subscriptionMessage: null })
    try {
      const checkout = await apiRequest<SubscriptionCheckout>(`/api/v1/organizations/${organizationId}/billing/checkout`, {
        method: 'POST',
        body: JSON.stringify({ planCode }),
      }, accessToken)
      set({ checkout, isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to start checkout.' })
      throw error
    }
  },
  verifyCheckout: async (accessToken, organizationId, orderId, paymentId, signature) => {
    set({ isLoading: true, error: null })
    try {
      await apiRequest<void>(`/api/v1/organizations/${organizationId}/billing/verify`, {
        method: 'POST',
        body: JSON.stringify({ razorpayOrderId: orderId, razorpayPaymentId: paymentId, razorpaySignature: signature }),
      }, accessToken)
      set({ isLoading: false, subscriptionMessage: 'Payment signature verified. Refreshing your subscription…' })
      window.location.reload()
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to verify payment.' })
      throw error
    }
  },
  clearCheckout: () => set({ checkout: null, subscriptionMessage: null }),
  cancelSubscription: async (accessToken, organizationId) => {
    set({ isLoading: true, error: null })
    try {
      await apiRequest<void>(`/api/v1/organizations/${organizationId}/subscription/cancel`, { method: 'POST' }, accessToken)
      const subscription = await apiRequest<Subscription>(`/api/v1/organizations/${organizationId}/subscription`, {}, accessToken)
      set({ subscription, subscriptionMessage: 'Cancellation scheduled for the end of the current billing period.', isLoading: false })
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to cancel the subscription.' })
      throw error
    }
  },
  loadHealth: async (accessToken) => {
    try {
      const health = await apiRequest<OperationalHealth>('/api/v1/health/operations', {}, accessToken)
      set({ health })
    } catch (error) {
      set({ health: null, error: error instanceof Error ? error.message : 'Unable to load platform health.' })
    }
  },
  loadAnalytics: async (accessToken, organizationId, days = 30) => {
    try {
      const analytics = await apiRequest<UsageAnalytics>(`/api/v1/organizations/${organizationId}/analytics?days=${days}`, {}, accessToken)
      set({ analytics })
    } catch (error) {
      set({ analytics: null, error: error instanceof Error ? error.message : 'Unable to load usage analytics.' })
    }
  },
  loadAdminDashboard: async (accessToken) => {
    try {
      const adminDashboard = await apiRequest<AdminDashboard>('/api/v1/admin/dashboard', {}, accessToken)
      set({ adminDashboard })
    } catch (error) {
      set({ adminDashboard: null, error: error instanceof Error ? error.message : 'Unable to load admin dashboard.' })
    }
  },
  updateProfile: async (accessToken, fullName) => {
    try {
      const user = await apiRequest<User>('/api/v1/users/profile', {
        method: 'PUT',
        body: JSON.stringify({ fullName }),
      }, accessToken)
      set({ isLoading: false })
      return user
    } catch (error) {
      set({ isLoading: false, error: error instanceof Error ? error.message : 'Unable to update your profile.' })
      throw error
    }
  },
}))
