export type User = { id: string; fullName: string; email: string; roles: string[] }
export type AuthResponse = { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; user: User }
export type Organization = { id: string; name: string; slug: string; role: string; createdAt: string }
export type Project = { id: string; organizationId: string; name: string; slug: string; description: string; status: string; createdAt: string; updatedAt: string }
export type Plan = { id: string; code: string; name: string; description: string; monthlyPriceInPaise: number; currency: string; monthlyQuota: number; rateLimitPerMinute: number; priority: string; allowedChannels: string[] }
export type Subscription = { id: string; organizationId: string; planCode: string; planName: string; status: string; monthlyQuota: number; notificationsUsed: number; remainingNotifications: number; currentPeriodStart: string; currentPeriodEnd: string }
