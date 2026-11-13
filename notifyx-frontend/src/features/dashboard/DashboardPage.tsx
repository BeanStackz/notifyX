import { useCallback, useEffect, useMemo, useState } from 'react'
import { Tooltip } from '../../components/Tooltip'
import { Toast } from '../../components/Toast'
import { useAuthStore } from '../../stores/authStore'
import { useDashboardStore } from '../../stores/dashboardStore'

type View = 'overview' | 'projects' | 'api-keys' | 'notifications' | 'billing' | 'analytics' | 'settings' | 'admin'

const navItems: { id: View; label: string; icon: string }[] = [
  { id: 'overview', label: 'Overview', icon: '⌂' },
  { id: 'projects', label: 'Projects', icon: '▦' },
  { id: 'api-keys', label: 'API keys', icon: '⌁' },
  { id: 'notifications', label: 'Send notification', icon: '↗' },
  { id: 'billing', label: 'Billing & plans', icon: '◈' },
  { id: 'analytics', label: 'Usage analytics', icon: '◒' },
  { id: 'settings', label: 'Settings', icon: '⚙' },
]

export function DashboardPage() {
  const { user, accessToken, logout, setUser } = useAuthStore()
  const { organizations, selectedOrganizationId, projects, plans, subscription, apiKeys, newlyCreatedApiKey, lastNotification, deliveryTracking, trackingLoading, notificationHistory, historyLoading, checkout, subscriptionMessage, health, analytics, adminDashboard, isLoading, error, load, selectOrganization, createOrganization, createProject, updateProject, archiveProject, loadApiKeys, createApiKey, revokeApiKey, clearNewApiKey, sendNotification, loadDeliveryTracking, loadNotificationHistory, retryNotification, createCheckout, verifyCheckout, clearCheckout, cancelSubscription, loadHealth, loadAnalytics, loadAdminDashboard, updateProfile } = useDashboardStore()
  const [view, setView] = useState<View>('overview')
  const [showOrganizationMenu, setShowOrganizationMenu] = useState(false)
  const [showCreateOrganization, setShowCreateOrganization] = useState(false)
  const organization = organizations.find((item) => item.id === selectedOrganizationId) ?? organizations[0]
  useEffect(() => { if (accessToken) void load(accessToken) }, [accessToken, load])
  useEffect(() => { if (accessToken) void loadHealth(accessToken) }, [accessToken, loadHealth])
  useEffect(() => { if (accessToken && organization) void loadAnalytics(accessToken, organization.id) }, [accessToken, organization, loadAnalytics])
  useEffect(() => { if (accessToken && user?.roles.includes('ROLE_ADMIN')) void loadAdminDashboard(accessToken) }, [accessToken, user, loadAdminDashboard])
  const dismissError = useCallback(() => useDashboardStore.setState({ error: null }), [])
  const usage = subscription ? Math.round((subscription.notificationsUsed / Math.max(subscription.monthlyQuota, 1)) * 100) : 0
  const initials = useMemo(() => user?.fullName.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase() ?? 'NX', [user])

  return (
    <main className="dashboard-shell">
      <aside className="sidebar">
        <a className="brand dashboard-brand" href="#"><span className="brand-mark"><span /><span /><span /></span><span>notify<span className="brand-accent">x</span></span></a>
        <div className="workspace-control">
          <button className="workspace-picker" onClick={() => setShowOrganizationMenu((visible) => !visible)} aria-expanded={showOrganizationMenu}><span className="workspace-avatar">{organization?.name[0] ?? 'N'}</span><div><b>{organization?.name ?? 'Your workspace'}</b><small>{organization ? organization.role.toLowerCase() : 'Set up workspace'}</small></div><span>⌄</span></button>
          {showOrganizationMenu && <div className="organization-menu">
            {organizations.map((item) => <button key={item.id} className={item.id === organization?.id ? 'selected' : ''} onClick={() => { if (accessToken) void selectOrganization(accessToken, item.id); setShowOrganizationMenu(false) }}><span className="workspace-avatar">{item.name[0]}</span><span>{item.name}</span><small>{item.role.toLowerCase()}</small></button>)}
            <button className="add-organization" onClick={() => { setShowCreateOrganization(true); setShowOrganizationMenu(false) }}>+ Create organization</button>
          </div>}
        </div>
        <nav className="side-nav">{navItems.map((item) => <button key={item.id} className={view === item.id ? 'active' : ''} onClick={() => setView(item.id)}><span>{item.icon}</span>{item.label}</button>)}{user?.roles.includes('ROLE_ADMIN') && <button className={view === 'admin' ? 'active' : ''} onClick={() => setView('admin')}><span>◈</span>Admin dashboard</button>}</nav>
        <div className="sidebar-bottom"><div className="help-card"><span>✦</span><b>Need a hand?</b><small>Read the API docs to get moving.</small><a href="#developers">View docs →</a></div><button className="logout" onClick={() => void logout()}>↪ <span>Sign out</span></button></div>
      </aside>
      <section className="dashboard-main">
        <header className="dashboard-header"><div><p className="breadcrumb">Workspace / <strong>{view}</strong></p><h1>{view === 'overview' ? 'Good morning, ' : ''}{user?.fullName.split(' ')[0] ?? 'there'} <span>✦</span></h1></div><div className="header-actions"><Tooltip label="Notifications are coming soon"><button className="icon-button">♧<i /></button></Tooltip><div className="user-chip"><span>{initials}</span><div><b>{user?.fullName}</b><small>{user?.email}</small></div><span>⌄</span></div></div></header>
        {isLoading && <div className="loading-banner">Syncing your workspace…</div>}
        {error && <Toast key={error} message={error} onClose={dismissError} />}
        {view === 'overview' && <Overview usage={usage} subscription={subscription} projects={projects.length} plans={plans.length} health={health} />}
        {view === 'projects' && organization && <Projects accessToken={accessToken} organizationId={organization.id} projects={projects} isLoading={isLoading} onCreate={createProject} onUpdate={updateProject} onArchive={archiveProject} />}
        {view === 'api-keys' && organization && <ApiKeys accessToken={accessToken} organizationId={organization.id} projects={projects} apiKeys={apiKeys} newlyCreatedApiKey={newlyCreatedApiKey} isLoading={isLoading} onLoad={loadApiKeys} onCreate={createApiKey} onRevoke={revokeApiKey} onClearNew={clearNewApiKey} />}
        {view === 'notifications' && organization && <NotificationComposer accessToken={accessToken} projects={projects} apiKeys={apiKeys} newlyCreatedApiKey={newlyCreatedApiKey} lastNotification={lastNotification} deliveryTracking={deliveryTracking} trackingLoading={trackingLoading} notificationHistory={notificationHistory} historyLoading={historyLoading} isLoading={isLoading} onSend={sendNotification} onTrack={loadDeliveryTracking} onLoadHistory={loadNotificationHistory} onRetry={retryNotification} />}
        {view === 'billing' && organization && <Billing accessToken={accessToken} organizationId={organization.id} plans={plans} subscription={subscription} checkout={checkout} subscriptionMessage={subscriptionMessage} isLoading={isLoading} onCheckout={createCheckout} onVerify={verifyCheckout} onClear={clearCheckout} onCancel={cancelSubscription} />}
        {view === 'analytics' && organization && <Analytics analytics={analytics} />}
        {view === 'settings' && <Settings user={user} accessToken={accessToken} organization={organization?.name ?? 'Your workspace'} onUpdate={async (name) => { const updated = await updateProfile(accessToken ?? '', name); setUser(updated) }} />}
        {view === 'admin' && user?.roles.includes('ROLE_ADMIN') && <AdminDashboard dashboard={adminDashboard} />}
        {!organization && <EmptyOrganization onCreate={() => setShowCreateOrganization(true)} />}
      </section>
      {showCreateOrganization && accessToken && <CreateOrganizationModal isLoading={isLoading} onClose={() => setShowCreateOrganization(false)} onCreate={async (name, slug) => { await createOrganization(accessToken, name, slug); setShowCreateOrganization(false) }} />}
    </main>
  )
}

function Overview({ usage, subscription, projects, plans, health }: { usage: number; subscription: { planName: string; notificationsUsed: number; monthlyQuota: number; remainingNotifications: number } | null; projects: number; plans: number; health: { rabbitMqStatus: string; brevoStatus: string; outboxBacklog: number; deadLetterQueueCount: number } | null }) {
  const bars = [42, 55, 45, 70, 60, 78, 64, 86, 80, 95, 88, 100]
  return <div className="dashboard-content"><div className="dashboard-grid">
    <article className="stat-card stat-card-primary"><div className="card-label">Notifications sent <Tooltip label="Messages accepted by the NotifyX ingestion API">ⓘ</Tooltip></div><strong>{subscription?.notificationsUsed.toLocaleString() ?? '0'}</strong><span className="trend">↗ Live from your subscription</span><div className="spark-bars">{bars.map((height, index) => <i key={index} style={{ height: `${height}%` }} />)}</div></article>
    <article className="stat-card"><div className="card-label">Delivery rate <Tooltip label="Successful delivery percentage across channels">ⓘ</Tooltip></div><strong>98.6%</strong><span className="trend">↗ 12.4% this period</span><div className="ring"><span>98<span>%</span></span></div></article>
    <article className="stat-card"><div className="card-label">Active projects</div><strong>{projects}</strong><span className="muted">Organize your integrations</span><div className="stat-icon mint">▦</div></article>
    <article className="usage-card"><div className="card-label">Plan usage <Tooltip label="Your monthly notification quota">ⓘ</Tooltip></div><div className="usage-title"><strong>{subscription?.planName ?? 'No plan yet'}</strong><span>{usage}% used</span></div><div className="progress"><i style={{ width: `${Math.min(usage, 100)}%` }} /></div><div className="usage-meta"><span>{(subscription?.notificationsUsed ?? 0).toLocaleString()} used</span><span>{(subscription?.monthlyQuota ?? 0).toLocaleString()} quota</span></div><a href="#pricing">View plans <span>→</span></a></article>
  </div><section className="panel health-panel"><div className="panel-heading"><div><p className="eyebrow">Platform health</p><h2>Delivery infrastructure</h2></div><span className="field-hint">Live backend indicators</span></div><div className="health-grid">{[['RabbitMQ', health?.rabbitMqStatus ?? 'Loading'], ['Brevo', health?.brevoStatus ?? 'Loading'], ['Outbox backlog', String(health?.outboxBacklog ?? '—')], ['Dead-letter queue', String(health?.deadLetterQueueCount ?? '—')]].map(([label, value]) => <div className="health-item" key={label}><span className={`health-dot ${value === 'UP' || value === 'CONFIGURED' || value === '0' ? 'healthy' : value === 'Loading' || value === '—' ? 'pending' : 'warning'}`} /><div><b>{label}</b><small>{value}</small></div></div>)}</div></section><div className="lower-grid"><article className="panel activity-panel"><div className="panel-heading"><div><p className="eyebrow">Activity</p><h2>Delivery performance</h2></div><select aria-label="Activity period"><option>Last 30 days</option><option>Last 7 days</option></select></div><div className="large-chart"><div className="chart-y-labels"><span>100k</span><span>75k</span><span>50k</span><span>25k</span><span>0</span></div><div className="large-chart-body"><div className="large-grid"><i /><i /><i /><i /><i /></div><svg viewBox="0 0 600 200" preserveAspectRatio="none"><path d="M0 168 C45 146 63 158 103 135 S163 142 201 112 S259 128 300 92 S361 110 401 71 S465 90 500 47 S550 52 600 20" /><path className="area" d="M0 168 C45 146 63 158 103 135 S163 142 201 112 S259 128 300 92 S361 110 401 71 S465 90 500 47 S550 52 600 20 V200 H0Z" /></svg><div className="large-x"><span>May 01</span><span>May 08</span><span>May 15</span><span>May 22</span><span>May 30</span></div></div></div></article><article className="panel quick-panel"><div className="panel-heading"><div><p className="eyebrow">Get started</p><h2>Build your first flow</h2></div></div><div className="quick-item"><span className="quick-number done">✓</span><div><b>Create a project</b><small>Projects keep your integrations organized.</small></div></div><div className="quick-item"><span className="quick-number">2</span><div><b>Generate an API key</b><small>Authenticate your first notification.</small></div><button>→</button></div><div className="quick-item"><span className="quick-number">3</span><div><b>Send a test notification</b><small>See delivery happen in real time.</small></div><button>→</button></div><div className="quick-footer">You have <strong>{plans}</strong> plans available to explore.</div></article></div></div>
}

function Projects({ accessToken, organizationId, projects, isLoading, onCreate, onUpdate, onArchive }: {
  accessToken: string | null
  organizationId: string
  projects: { id: string; name: string; slug: string; status: string; description: string; createdAt: string; updatedAt: string }[]
  isLoading: boolean
  onCreate: (accessToken: string, organizationId: string, name: string, slug: string, description: string) => Promise<void>
  onUpdate: (accessToken: string, organizationId: string, projectId: string, name: string, slug: string, description: string) => Promise<void>
  onArchive: (accessToken: string, organizationId: string, projectId: string) => Promise<void>
}) {
  const [editingProject, setEditingProject] = useState<{ id?: string; name: string; slug: string; description: string } | null>(null)
  const [selectedProject, setSelectedProject] = useState<string | null>(null)

  async function saveProject(name: string, slug: string, description: string) {
    if (!accessToken) return
    if (editingProject?.id) await onUpdate(accessToken, organizationId, editingProject.id, name, slug, description)
    else await onCreate(accessToken, organizationId, name, slug, description)
    setEditingProject(null)
  }

  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Workspace</p><h2>Projects</h2><p>Manage the notification environments connected to your organization.</p></div><button className="button" onClick={() => setEditingProject({ name: '', slug: '', description: '' })}>+ New project</button></div><div className="project-list">{projects.length ? projects.map((project) => <article className="project-row" key={project.id} onClick={() => setSelectedProject(selectedProject === project.id ? null : project.id)}><span className="project-logo">{project.name[0]}</span><div><b>{project.name}</b><small>{project.description || project.slug}</small></div><span className={`status ${project.status.toLowerCase()}`}>{project.status.toLowerCase()}</span><Tooltip label="Project details and actions"><button className="more-button" onClick={(event) => { event.stopPropagation(); setSelectedProject(selectedProject === project.id ? null : project.id) }}>•••</button></Tooltip>{selectedProject === project.id && <div className="project-actions"><div className="project-detail-summary"><small>Slug: <strong>{project.slug}</strong></small><small>Created: <strong>{new Date(project.createdAt).toLocaleDateString()}</strong></small><small>Updated: <strong>{new Date(project.updatedAt).toLocaleDateString()}</strong></small></div><button onClick={(event) => { event.stopPropagation(); setEditingProject(project); setSelectedProject(null) }}>Edit project</button><button className="danger-action" disabled={project.status === 'ARCHIVED' || isLoading} onClick={(event) => { event.stopPropagation(); if (window.confirm(`Archive ${project.name}?`)) void onArchive(accessToken ?? '', organizationId, project.id) }}>Archive project</button></div>}</article>) : <div className="empty-state"><span>▦</span><h3>No projects yet</h3><p>Create a project to start sending notifications.</p><button className="button" onClick={() => setEditingProject({ name: '', slug: '', description: '' })}>Create project</button></div>}</div>{editingProject && <ProjectModal project={editingProject} isLoading={isLoading} onClose={() => setEditingProject(null)} onSave={saveProject} />}</div>
}

function ProjectModal({ project, isLoading, onClose, onSave }: { project: { id?: string; name: string; slug: string; description: string }; isLoading: boolean; onClose: () => void; onSave: (name: string, slug: string, description: string) => Promise<void> }) {
  const [name, setName] = useState(project.name)
  const [slug, setSlug] = useState(project.slug)
  const [description, setDescription] = useState(project.description)
  const [validationError, setValidationError] = useState('')

  function updateName(value: string) {
    setName(value)
    if (!project.id && (!slug || slug === name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''))) setSlug(value.toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''))
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
      setValidationError('Use lowercase letters, numbers, and hyphens only.')
      return
    }
    setValidationError('')
    await onSave(name.trim(), slug, description.trim())
  }

  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target) onClose() }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="project-modal-title"><button className="modal-close" onClick={onClose} aria-label="Close">×</button><p className="eyebrow">Project configuration</p><h2 id="project-modal-title">{project.id ? 'Edit project' : 'Create a project'}</h2><p className="modal-description">Projects isolate notification keys, channels, and delivery traffic.</p><form onSubmit={submit}><label>Project name<input required maxLength={150} value={name} onChange={(event) => updateName(event.target.value)} placeholder="Customer notifications" /></label><label>Project slug<input required maxLength={100} pattern="^[a-z0-9]+(?:-[a-z0-9]+)*$" value={slug} onChange={(event) => setSlug(event.target.value)} placeholder="customer-notifications" /><small className="field-hint">Lowercase letters, numbers, and hyphens.</small></label><label>Description<input maxLength={500} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Transactional messages for our app" /></label>{validationError && <div className="form-error" role="alert">{validationError}</div>}<div className="modal-actions"><button type="button" className="text-button" onClick={onClose}>Cancel</button><button className="button" disabled={isLoading}>{isLoading ? 'Saving…' : project.id ? 'Save changes' : 'Create project'} <span>→</span></button></div></form></section></div>
}

function Settings({ user, accessToken, organization, onUpdate }: { user: { fullName: string; email: string } | null; accessToken: string | null; organization: string; onUpdate: (name: string) => Promise<void> }) {
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [saved, setSaved] = useState(false)
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accessToken || !fullName.trim()) return
    await onUpdate(fullName.trim())
    setSaved(true)
    window.setTimeout(() => setSaved(false), 2500)
  }
  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Workspace</p><h2>Settings</h2><p>Manage your {organization} configuration.</p></div></div><form className="settings-card" onSubmit={submit}><h3>Profile</h3><p>Update the name shown across your NotifyX workspace.</p><label>Full name<input required maxLength={100} value={fullName} onChange={(event) => setFullName(event.target.value)} /></label><label>Email address<input value={user?.email ?? ''} readOnly /></label><div className="composer-actions"><span>{saved ? 'Profile saved.' : 'Email changes require support.'}</span><button className="button" disabled={!accessToken}>Save profile <span>→</span></button></div></form><div className="settings-card"><h3>API and integrations</h3><p>API key management and channel configuration are scoped to each project.</p><div className="settings-placeholder">⌁ <span>Secure by default with scoped API keys and idempotent delivery.</span></div></div></div>
}

function Analytics({ analytics }: { analytics: { totalNotifications: number; projectCount: number; byStatus: Record<string, number>; byChannel: Record<string, number> } | null }) {
  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Workspace insights</p><h2>Usage analytics</h2><p>Understand volume and delivery state across this organization.</p></div></div>{analytics ? <><div className="analytics-summary"><article className="stat-card"><span className="card-label">Total notifications</span><strong>{analytics.totalNotifications.toLocaleString()}</strong></article><article className="stat-card"><span className="card-label">Projects tracked</span><strong>{analytics.projectCount}</strong></article></div><div className="analytics-grid"><article className="panel"><div className="panel-heading"><h3>By status</h3></div>{Object.entries(analytics.byStatus).map(([key, value]) => <div className="analytics-row" key={key}><span>{key.toLowerCase()}</span><strong>{value.toLocaleString()}</strong></div>)}</article><article className="panel"><div className="panel-heading"><h3>By channel</h3></div>{Object.entries(analytics.byChannel).map(([key, value]) => <div className="analytics-row" key={key}><span>{key.toLowerCase()}</span><strong>{value.toLocaleString()}</strong></div>)}</article></div></> : <div className="empty-state"><span>◒</span><h3>Analytics unavailable</h3><p>We could not load usage data for this workspace.</p></div>}</div>
}

function AdminDashboard({ dashboard }: { dashboard: { userCount: number; organizationCount: number; projectCount: number; notificationCount: number } | null }) {
  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Platform administration</p><h2>Admin dashboard</h2><p>High-level NotifyX platform activity.</p></div></div>{dashboard ? <div className="analytics-summary"><article className="stat-card"><span className="card-label">Users</span><strong>{dashboard.userCount.toLocaleString()}</strong></article><article className="stat-card"><span className="card-label">Organizations</span><strong>{dashboard.organizationCount.toLocaleString()}</strong></article><article className="stat-card"><span className="card-label">Projects</span><strong>{dashboard.projectCount.toLocaleString()}</strong></article><article className="stat-card"><span className="card-label">Notifications</span><strong>{dashboard.notificationCount.toLocaleString()}</strong></article></div> : <div className="empty-state"><span>◈</span><h3>Admin data unavailable</h3><p>Your administrator metrics could not be loaded.</p></div>}</div>
}

function Billing({ accessToken, organizationId, plans, subscription, checkout, subscriptionMessage, isLoading, onCheckout, onVerify, onClear, onCancel }: {
  accessToken: string | null
  organizationId: string
  plans: { id: string; code: string; name: string; description: string; monthlyPriceInPaise: number; currency: string; monthlyQuota: number; rateLimitPerMinute: number; priority: string; allowedChannels: string[] }[]
  subscription: { planCode: string; planName: string; status: string; monthlyQuota: number; notificationsUsed: number; remainingNotifications: number; currentPeriodEnd: string } | null
  checkout: { billingOrderId: string; razorpayOrderId: string; razorpayKeyId: string; amountInPaise: number; currency: string; planName: string } | null
  subscriptionMessage: string | null
  isLoading: boolean
  onCheckout: (accessToken: string, organizationId: string, planCode: string) => Promise<void>
  onVerify: (accessToken: string, organizationId: string, orderId: string, paymentId: string, signature: string) => Promise<void>
  onClear: () => void
  onCancel: (accessToken: string, organizationId: string) => Promise<void>
}) {
  const [verificationOpen, setVerificationOpen] = useState(false)
  const [paymentId, setPaymentId] = useState('')
  const [signature, setSignature] = useState('')
  useEffect(() => {
    if (document.querySelector('script[src="https://checkout.razorpay.com/v1/checkout.js"]')) return
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.async = true
    document.body.appendChild(script)
    return () => { script.remove() }
  }, [])

  async function startCheckout(planCode: string) {
    if (!accessToken) return
    await onCheckout(accessToken, organizationId, planCode)
  }

  async function verify(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!accessToken || !checkout) return
    await onVerify(accessToken, organizationId, checkout.razorpayOrderId, paymentId.trim(), signature.trim())
  }

  async function cancel() {
    if (!accessToken || !subscription || subscription.status !== 'ACTIVE') return
    if (window.confirm('Schedule cancellation at the end of the current billing period?')) await onCancel(accessToken, organizationId)
  }

  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Workspace billing</p><h2>Plans & billing</h2><p>Choose the notification capacity that fits your next release.</p></div></div>{subscription && <div className="current-subscription"><div><span className="eyebrow">Current plan</span><h3>{subscription.planName}</h3><span className={`status ${subscription.status.toLowerCase()}`}>{subscription.status.toLowerCase()}</span></div><div><b>{subscription.notificationsUsed.toLocaleString()} / {subscription.monthlyQuota.toLocaleString()}</b><small>notifications used</small></div><div><b>{subscription.remainingNotifications.toLocaleString()}</b><small>remaining this period</small></div><div><b>{new Date(subscription.currentPeriodEnd).toLocaleDateString()}</b><small>period ends</small></div>{subscription.status === 'ACTIVE' && <button className="text-button" onClick={() => void cancel()}>Cancel at period end</button>}</div>}{subscriptionMessage && <div className="success-card billing-message"><span>✓</span><strong>{subscriptionMessage}</strong></div>}<div className="plan-grid">{plans.length ? plans.map((plan) => { const isCurrentPlan = subscription?.planCode === plan.code; return <article className={`plan-card ${isCurrentPlan ? 'current' : ''}`} key={plan.id}><div className="plan-card-top"><span className="eyebrow">{plan.priority} priority</span>{isCurrentPlan && <span className="current-pill">Current plan</span>}</div><h3>{plan.name}</h3><p>{plan.description}</p><strong className="plan-price">{new Intl.NumberFormat('en-IN', { style: 'currency', currency: plan.currency || 'INR', maximumFractionDigits: 0 }).format(plan.monthlyPriceInPaise / 100)}<small>/month</small></strong><div className="plan-details"><span>◉ {plan.monthlyQuota.toLocaleString()} notifications</span><span>↯ {plan.rateLimitPerMinute}/min rate limit</span><span>✓ {plan.allowedChannels.join(', ')}</span></div><button className="button" disabled={isLoading || isCurrentPlan} onClick={() => void startCheckout(plan.code)}>{isCurrentPlan ? 'Current plan' : isLoading ? 'Preparing…' : 'Choose plan'} <span>→</span></button></article> }) : <div className="empty-state"><span>◈</span><h3>No plans available</h3><p>Plans will appear here once billing is configured.</p></div>}</div>{checkout && <div className="modal-backdrop" role="presentation"><section className="modal-card checkout-card" role="dialog" aria-modal="true" aria-labelledby="checkout-title"><button className="modal-close" onClick={onClear} aria-label="Close">×</button><p className="eyebrow">Razorpay checkout</p><h2 id="checkout-title">{checkout.planName} checkout created</h2><p className="modal-description">Complete the one-time payment in Razorpay Checkout. After payment, paste the returned payment ID and signature if the callback did not verify automatically.</p><button className="button checkout-link" onClick={() => { const options = { key: checkout.razorpayKeyId, amount: checkout.amountInPaise, currency: checkout.currency, name: 'NotifyX', description: checkout.planName, order_id: checkout.razorpayOrderId, handler: (response: { razorpay_payment_id: string; razorpay_order_id: string; razorpay_signature: string }) => { setPaymentId(response.razorpay_payment_id); setSignature(response.razorpay_signature); void onVerify(accessToken!, organizationId, response.razorpay_order_id, response.razorpay_payment_id, response.razorpay_signature) } }; const Razorpay = (window as Window & { Razorpay?: new (options: unknown) => { open: () => void } }).Razorpay; if (Razorpay) new Razorpay(options).open(); else window.alert('Razorpay Checkout is still loading. Please try again.'); }}>{isLoading ? 'Processing…' : 'Pay securely with Razorpay'} ↗</button><button className="text-button checkout-toggle" onClick={() => setVerificationOpen((open) => !open)}>{verificationOpen ? 'Hide verification form' : 'Verify payment manually'}</button>{verificationOpen && <form onSubmit={verify} className="verification-form"><label>Payment ID<input required value={paymentId} onChange={(event) => setPaymentId(event.target.value)} placeholder="pay_…" /></label><label>Signature<input required value={signature} onChange={(event) => setSignature(event.target.value)} placeholder="Signature from Razorpay" /></label><button className="button" disabled={isLoading}>{isLoading ? 'Verifying…' : 'Verify payment'} <span>→</span></button></form>}</section></div>}</div>
}

function NotificationComposer({ accessToken, projects, apiKeys, newlyCreatedApiKey, lastNotification, deliveryTracking, trackingLoading, notificationHistory, historyLoading, isLoading, onSend, onTrack, onLoadHistory, onRetry }: {
  accessToken: string | null
  projects: { id: string; name: string }[]
  apiKeys: { id: string; projectId: string; name: string; keyPrefix: string; status: string }[]
  newlyCreatedApiKey: { projectId: string; apiKey: string } | null
  lastNotification: { id: string; projectId: string; channel: string; recipient: string; status: string; scheduledAt: string | null } | null
  deliveryTracking: { notificationId: string; notificationStatus: string; attempts: { status: string; errorMessage: string | null }[]; events: { type: string; occurredAt: string }[] } | null
  trackingLoading: boolean
  notificationHistory: { id: string; projectId: string; channel: string; recipient: string; status: string; scheduledAt: string | null; createdAt: string }[]
  historyLoading: boolean
  isLoading: boolean
  onSend: (apiKey: string, projectId: string, request: { channel: string; recipient: string; subject?: string; content: string; metadata?: Record<string, string>; scheduledAt?: string }) => Promise<{ id: string; projectId: string; channel: string; recipient: string; status: string; scheduledAt: string | null }>
  onTrack: (accessToken: string, projectId: string, notificationId: string) => Promise<unknown>
  onLoadHistory: (accessToken: string, projectId: string) => Promise<void>
  onRetry: (accessToken: string, projectId: string, notificationId: string) => Promise<void>
}) {
  const [projectId, setProjectId] = useState(projects[0]?.id ?? '')
  const [apiKey, setApiKey] = useState(newlyCreatedApiKey?.projectId === projectId ? newlyCreatedApiKey.apiKey : '')
  const [channel, setChannel] = useState('EMAIL')
  const [recipient, setRecipient] = useState('')
  const [subject, setSubject] = useState('')
  const [content, setContent] = useState('')
  const [metadata, setMetadata] = useState('')
  const [scheduledAt, setScheduledAt] = useState('')
  const [formError, setFormError] = useState('')
  const [isSending, setIsSending] = useState(false)
  const projectKeys = apiKeys.filter((key) => key.projectId === projectId && key.status === 'ACTIVE')
  const [trackingError, setTrackingError] = useState('')
  const [retryingId, setRetryingId] = useState<string | null>(null)

  useEffect(() => {
    if (accessToken && projectId) void onLoadHistory(accessToken, projectId)
  }, [accessToken, projectId, onLoadHistory])

  useEffect(() => {
    if (!accessToken || !lastNotification) return
    let cancelled = false
    let pollCount = 0
    const poll = async () => {
      try {
        await onTrack(accessToken, lastNotification.projectId, lastNotification.id)
        const status = useDashboardStore.getState().deliveryTracking?.notificationStatus
        if (cancelled || pollCount >= 40 || status === 'SENT' || status === 'FAILED' || status === 'CANCELLED') return
        pollCount += 1
        window.setTimeout(() => { void poll() }, 3000)
      } catch (error) {
        if (!cancelled) setTrackingError(error instanceof Error ? error.message : 'Unable to refresh delivery status.')
      }
    }
    void poll()
    return () => { cancelled = true }
  }, [accessToken, lastNotification, onTrack])

  function chooseProject(value: string) {
    setProjectId(value)
    setApiKey(newlyCreatedApiKey?.projectId === value ? newlyCreatedApiKey.apiKey : '')
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSending) return
    setFormError('')
    let parsedMetadata: Record<string, string> | undefined
    if (metadata.trim()) {
      try {
        const parsed = JSON.parse(metadata) as unknown
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error()
        parsedMetadata = Object.fromEntries(Object.entries(parsed).map(([key, value]) => [key, String(value)]))
      } catch {
        setFormError('Metadata must be a valid JSON object, for example {"source":"dashboard"}.')
        return
      }

    }
    if (!apiKey.trim()) {
      setFormError('Enter the full API key secret. You can copy it when creating a key.')
      return
    }
    if (!projectId) {
      setFormError('Select a project before sending.')
      return
    }
    if (scheduledAt && new Date(scheduledAt).getTime() <= Date.now()) {
      setFormError('Scheduled time must be in the future.')
      return
    }
    setIsSending(true)
    try {
      await onSend(apiKey.trim(), projectId, { channel, recipient: recipient.trim(), ...(subject.trim() ? { subject: subject.trim() } : {}), content: content.trim(), ...(parsedMetadata ? { metadata: parsedMetadata } : {}), ...(scheduledAt ? { scheduledAt: new Date(scheduledAt).toISOString() } : {}) })
      if (accessToken) void onLoadHistory(accessToken, projectId)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Unable to submit the notification.')
    } finally {
      setIsSending(false)
    }
  }

  async function retry(notificationId: string) {
    if (!accessToken || retryingId) return
    setRetryingId(notificationId)
    try {
      await onRetry(accessToken, projectId, notificationId)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : 'Unable to queue notification retry.')
    } finally {
      setRetryingId(null)
    }
  }

  const status = deliveryTracking?.notificationStatus ?? lastNotification?.status ?? 'ACCEPTED'
  const statusLabel = status === 'SENT' ? 'Sent by provider' : status === 'FAILED' ? 'Delivery failed' : status === 'PROCESSING' ? 'Processing' : status === 'QUEUED' ? 'Queued for delivery' : 'Accepted'
  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Delivery playground</p><h2>Send a notification</h2><p>Send a real test message through your project API key.</p></div><Tooltip label="Every submission receives a unique Idempotency-Key automatically"><span className="idempotency-badge">↯ Idempotent delivery</span></Tooltip></div>{projects.length === 0 ? <div className="empty-state"><span>↗</span><h3>Create a project first</h3><p>Notifications are sent through a project-scoped API key.</p></div> : <><form className="composer-layout" onSubmit={submit}><section className="composer-card"><div className="composer-section"><label>Project<select value={projectId} onChange={(event) => chooseProject(event.target.value)}>{projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></label><label>Channel<select value={channel} onChange={(event) => setChannel(event.target.value)}><option value="EMAIL">Email</option><option value="SMS">SMS</option><option value="PUSH">Push</option><option value="IN_APP">In-app</option></select></label></div><label>API key secret  <Tooltip label="This is sent as the X-NotifyX-API-Key header and is never stored"><span className="field-info">ⓘ</span></Tooltip><input required type="password" value={apiKey} onChange={(event) => setApiKey(event.target.value)} placeholder="nx_live_••••••••" /><small className="field-hint">Use the full secret shown once after creating an API key.</small></label><label>Recipient<input required maxLength={320} value={recipient} onChange={(event) => setRecipient(event.target.value)} placeholder={channel === 'EMAIL' ? 'customer@example.com' : '+1 555 0100'} /></label>{channel === 'EMAIL' && <label>Subject<input maxLength={255} value={subject} onChange={(event) => setSubject(event.target.value)} placeholder="Your order is on its way" /></label>}<label>Message content<textarea required maxLength={10000} rows={7} value={content} onChange={(event) => setContent(event.target.value)} placeholder="Write the message you want to send…" /></label><div className="composer-section"><label>Schedule (optional)<input type="datetime-local" value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} /></label><label>Metadata (optional)<input value={metadata} onChange={(event) => setMetadata(event.target.value)} placeholder='{"source":"dashboard"}' /></label></div>{formError && <div className="form-error" role="alert">{formError}</div>}<div className="composer-actions"><span>Requires an active project API key</span><button className="button" disabled={isSending || isLoading}>{isSending ? 'Sending…' : 'Send notification'} <span>→</span></button></div></section><aside className="composer-side"><div className="panel"><p className="eyebrow">Active keys</p><h3>{projectKeys.length} available</h3>{projectKeys.length ? projectKeys.map((key) => <div className="composer-key" key={key.id}><span>⌁</span><div><b>{key.name}</b><small>{key.keyPrefix}••••••</small></div></div>) : <p className="side-muted">Create an active API key before sending.</p>}</div>{lastNotification && <div className="success-card"><span>✓</span><strong>{statusLabel}</strong><small>{lastNotification.channel} to {lastNotification.recipient}</small><code>{trackingLoading ? 'Refreshing delivery status…' : status}{trackingError ? ` · ${trackingError}` : ''}{deliveryTracking?.events.length ? ` · ${deliveryTracking.events.length} provider event(s)` : ''}</code>{deliveryTracking?.attempts.at(-1)?.errorMessage && <small>{deliveryTracking.attempts.at(-1)?.errorMessage}</small>}</div>}</aside></form><section className="panel notification-history"><div className="page-title"><div><p className="eyebrow">Project activity</p><h3>Notification history</h3></div><span className="field-hint">{historyLoading ? 'Refreshing…' : `${notificationHistory.length} notification(s)`}</span></div>{notificationHistory.length ? notificationHistory.map((notification) => <div className="project-row" key={notification.id}><div><b>{notification.channel} · {notification.recipient}</b><small>{new Date(notification.createdAt).toLocaleString()}</small></div><div className="history-row-actions"><span className={`status ${notification.status.toLowerCase()}`}>{notification.status.toLowerCase()}</span>{notification.status === 'FAILED' && <button type="button" className="text-button" disabled={retryingId === notification.id} onClick={() => void retry(notification.id)}>{retryingId === notification.id ? 'Retrying…' : 'Retry'}</button>}</div></div>) : <p className="side-muted">No notifications have been accepted for this project.</p>}</section></>}</div>
}

function ApiKeys({ accessToken, organizationId, projects, apiKeys, newlyCreatedApiKey, isLoading, onLoad, onCreate, onRevoke, onClearNew }: {
  accessToken: string | null
  organizationId: string
  projects: { id: string; name: string; slug: string }[]
  apiKeys: { id: string; projectId: string; name: string; keyPrefix: string; status: string; expiresAt: string | null; lastUsedAt: string | null; createdAt: string; revokedAt: string | null }[]
  newlyCreatedApiKey: { id: string; projectId: string; name: string; keyPrefix: string; apiKey: string; status: string; expiresAt: string | null; createdAt: string } | null
  isLoading: boolean
  onLoad: (accessToken: string, organizationId: string, projectId: string) => Promise<void>
  onCreate: (accessToken: string, organizationId: string, projectId: string, name: string, expiresInDays: number | null) => Promise<void>
  onRevoke: (accessToken: string, organizationId: string, projectId: string, apiKeyId: string) => Promise<void>
  onClearNew: () => void
}) {
  const [projectId, setProjectId] = useState(projects[0]?.id ?? '')
  const [showCreate, setShowCreate] = useState(false)
  useEffect(() => { if (accessToken && projectId) void onLoad(accessToken, organizationId, projectId) }, [accessToken, organizationId, projectId, onLoad])
  const projectKeys = apiKeys.filter((key) => key.projectId === projectId)

  async function create(name: string, expiresInDays: number | null) {
    if (!accessToken) return
    await onCreate(accessToken, organizationId, projectId, name, expiresInDays)
    setShowCreate(false)
  }

  function copyKey(value: string) {
    void navigator.clipboard.writeText(value)
  }

  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Project security</p><h2>API keys</h2><p>Authenticate notification ingestion without exposing your user session.</p></div>{projects.length > 0 && <button className="button" onClick={() => setShowCreate(true)}>+ Create API key</button>}</div>{projects.length === 0 ? <div className="empty-state"><span>⌁</span><h3>Create a project first</h3><p>API keys belong to a specific project.</p></div> : <><div className="key-toolbar"><label>Project<select value={projectId} onChange={(event) => setProjectId(event.target.value)}>{projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></label><span className="key-count">{projectKeys.length} {projectKeys.length === 1 ? 'key' : 'keys'}</span></div>{newlyCreatedApiKey && newlyCreatedApiKey.projectId === projectId && <div className="secret-banner"><div><strong>Copy this API key now</strong><p>For security, NotifyX will not show the full secret again.</p><code>{newlyCreatedApiKey.apiKey}</code></div><div className="secret-actions"><button className="button" onClick={() => copyKey(newlyCreatedApiKey.apiKey)}>Copy key</button><button className="text-button" onClick={onClearNew}>Dismiss</button></div></div>}<div className="key-list">{projectKeys.length ? projectKeys.map((key) => <article className="key-row" key={key.id}><span className="key-icon">⌁</span><div><b>{key.name}</b><small><code>{key.keyPrefix}••••••••</code> · created {new Date(key.createdAt).toLocaleDateString()}</small></div><span className={`status ${key.status.toLowerCase()}`}>{key.status.toLowerCase()}</span><Tooltip label={key.lastUsedAt ? `Last used ${new Date(key.lastUsedAt).toLocaleString()}` : 'This key has not been used yet'}><span className="key-last-used">{key.lastUsedAt ? 'Used' : 'Unused'} ⓘ</span></Tooltip><button className="revoke-button" disabled={key.status === 'REVOKED' || isLoading} onClick={() => { if (window.confirm(`Revoke ${key.name}? This cannot be undone.`) && accessToken) void onRevoke(accessToken, organizationId, projectId, key.id) }}>Revoke</button></article>) : <div className="empty-state"><span>⌁</span><h3>No API keys yet</h3><p>Create a scoped key to authenticate your notification requests.</p><button className="button" onClick={() => setShowCreate(true)}>Create API key</button></div>}</div></>}{showCreate && <CreateApiKeyModal isLoading={isLoading} onClose={() => setShowCreate(false)} onCreate={create} />}</div>
}

function CreateApiKeyModal({ isLoading, onClose, onCreate }: { isLoading: boolean; onClose: () => void; onCreate: (name: string, expiresInDays: number | null) => Promise<void> }) {
  const [name, setName] = useState('')
  const [expiresInDays, setExpiresInDays] = useState('90')
  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target) onClose() }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="create-key-title"><button className="modal-close" onClick={onClose} aria-label="Close">×</button><p className="eyebrow">Project security</p><h2 id="create-key-title">Create an API key</h2><p className="modal-description">Use separate keys for each environment and revoke them independently.</p><form onSubmit={(event) => { event.preventDefault(); void onCreate(name.trim(), expiresInDays ? Number(expiresInDays) : null) }}><label>Key name<input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} placeholder="Production server" /></label><label>Expires in days<input type="number" min="1" max="3650" value={expiresInDays} onChange={(event) => setExpiresInDays(event.target.value)} /><small className="field-hint">Leave blank for a non-expiring key.</small></label><div className="modal-actions"><button type="button" className="text-button" onClick={onClose}>Cancel</button><button className="button" disabled={isLoading}>{isLoading ? 'Creating…' : 'Create key'} <span>→</span></button></div></form></section></div>
}

function EmptyOrganization({ onCreate }: { onCreate: () => void }) {
  return <div className="empty-workspace"><span>✦</span><p className="eyebrow">Your workspace is ready</p><h2>Create your first organization</h2><p>Organizations keep projects, API keys, and billing neatly separated.</p><button className="button" onClick={onCreate}>Create organization</button></div>
}

function CreateOrganizationModal({ isLoading, onClose, onCreate }: { isLoading: boolean; onClose: () => void; onCreate: (name: string, slug: string) => Promise<void> }) {
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [validationError, setValidationError] = useState('')

  function updateName(value: string) {
    setName(value)
    if (!slug || slug === name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')) setSlug(value.toLowerCase().trim().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''))
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
      setValidationError('Use lowercase letters, numbers, and hyphens only.')
      return
    }
    setValidationError('')
    await onCreate(name.trim(), slug)
  }

  return <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.currentTarget === event.target) onClose() }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="create-organization-title"><button className="modal-close" onClick={onClose} aria-label="Close">×</button><p className="eyebrow">Workspace setup</p><h2 id="create-organization-title">Create an organization</h2><p className="modal-description">Use an organization to group projects, API keys, and subscriptions.</p><form onSubmit={submit}><label>Organization name<input required maxLength={100} value={name} onChange={(event) => updateName(event.target.value)} placeholder="Acme Inc." /></label><label>Organization slug<input required maxLength={100} pattern="^[a-z0-9]+(?:-[a-z0-9]+)*$" value={slug} onChange={(event) => setSlug(event.target.value)} placeholder="acme-inc" /><small className="field-hint">Lowercase letters, numbers, and hyphens.</small></label>{validationError && <div className="form-error" role="alert">{validationError}</div>}<div className="modal-actions"><button type="button" className="text-button" onClick={onClose}>Cancel</button><button className="button" disabled={isLoading}>{isLoading ? 'Creating…' : 'Create organization'} <span>→</span></button></div></form></section></div>
}
