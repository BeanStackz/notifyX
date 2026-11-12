import { useEffect, useMemo, useState } from 'react'
import { Tooltip } from '../../components/Tooltip'
import { useAuthStore } from '../../stores/authStore'
import { useDashboardStore } from '../../stores/dashboardStore'

type View = 'overview' | 'projects' | 'settings'

const navItems: { id: View; label: string; icon: string }[] = [
  { id: 'overview', label: 'Overview', icon: '⌂' },
  { id: 'projects', label: 'Projects', icon: '▦' },
  { id: 'settings', label: 'Settings', icon: '⚙' },
]

export function DashboardPage() {
  const { user, accessToken, logout } = useAuthStore()
  const { organizations, projects, plans, subscription, isLoading, error, load } = useDashboardStore()
  const [view, setView] = useState<View>('overview')
  const organization = organizations[0]
  useEffect(() => { if (accessToken) void load(accessToken) }, [accessToken, load])
  const usage = subscription ? Math.round((subscription.notificationsUsed / Math.max(subscription.monthlyQuota, 1)) * 100) : 0
  const initials = useMemo(() => user?.fullName.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase() ?? 'NX', [user])

  return (
    <main className="dashboard-shell">
      <aside className="sidebar">
        <a className="brand dashboard-brand" href="#"><span className="brand-mark"><span /><span /><span /></span><span>notify<span className="brand-accent">x</span></span></a>
        <div className="workspace-picker"><span className="workspace-avatar">{organization?.name[0] ?? 'N'}</span><div><b>{organization?.name ?? 'Your workspace'}</b><small>{organization ? organization.role.toLowerCase() : 'Set up workspace'}</small></div><span>⌄</span></div>
        <nav className="side-nav">{navItems.map((item) => <button key={item.id} className={view === item.id ? 'active' : ''} onClick={() => setView(item.id)}><span>{item.icon}</span>{item.label}</button>)}</nav>
        <div className="sidebar-bottom"><div className="help-card"><span>✦</span><b>Need a hand?</b><small>Read the API docs to get moving.</small><a href="#developers">View docs →</a></div><button className="logout" onClick={() => void logout()}>↪ <span>Sign out</span></button></div>
      </aside>
      <section className="dashboard-main">
        <header className="dashboard-header"><div><p className="breadcrumb">Workspace / <strong>{view}</strong></p><h1>{view === 'overview' ? 'Good morning, ' : ''}{user?.fullName.split(' ')[0] ?? 'there'} <span>✦</span></h1></div><div className="header-actions"><Tooltip label="Notifications are coming soon"><button className="icon-button">♧<i /></button></Tooltip><div className="user-chip"><span>{initials}</span><div><b>{user?.fullName}</b><small>{user?.email}</small></div><span>⌄</span></div></div></header>
        {isLoading && <div className="loading-banner">Syncing your workspace…</div>}
        {error && <div className="error-banner" role="alert">{error}</div>}
        {view === 'overview' && <Overview usage={usage} subscription={subscription} projects={projects.length} plans={plans.length} />}
        {view === 'projects' && <Projects projects={projects} />}
        {view === 'settings' && <Settings organization={organization?.name ?? 'Your workspace'} />}
      </section>
    </main>
  )
}

function Overview({ usage, subscription, projects, plans }: { usage: number; subscription: { planName: string; notificationsUsed: number; monthlyQuota: number; remainingNotifications: number } | null; projects: number; plans: number }) {
  const bars = [42, 55, 45, 70, 60, 78, 64, 86, 80, 95, 88, 100]
  return <div className="dashboard-content"><div className="dashboard-grid">
    <article className="stat-card stat-card-primary"><div className="card-label">Notifications sent <Tooltip label="Messages accepted by the NotifyX ingestion API">ⓘ</Tooltip></div><strong>{subscription?.notificationsUsed.toLocaleString() ?? '0'}</strong><span className="trend">↗ Live from your subscription</span><div className="spark-bars">{bars.map((height, index) => <i key={index} style={{ height: `${height}%` }} />)}</div></article>
    <article className="stat-card"><div className="card-label">Delivery rate <Tooltip label="Successful delivery percentage across channels">ⓘ</Tooltip></div><strong>98.6%</strong><span className="trend">↗ 12.4% this period</span><div className="ring"><span>98<span>%</span></span></div></article>
    <article className="stat-card"><div className="card-label">Active projects</div><strong>{projects}</strong><span className="muted">Organize your integrations</span><div className="stat-icon mint">▦</div></article>
    <article className="usage-card"><div className="card-label">Plan usage <Tooltip label="Your monthly notification quota">ⓘ</Tooltip></div><div className="usage-title"><strong>{subscription?.planName ?? 'No plan yet'}</strong><span>{usage}% used</span></div><div className="progress"><i style={{ width: `${Math.min(usage, 100)}%` }} /></div><div className="usage-meta"><span>{(subscription?.notificationsUsed ?? 0).toLocaleString()} used</span><span>{(subscription?.monthlyQuota ?? 0).toLocaleString()} quota</span></div><a href="#pricing">View plans <span>→</span></a></article>
  </div><div className="lower-grid"><article className="panel activity-panel"><div className="panel-heading"><div><p className="eyebrow">Activity</p><h2>Delivery performance</h2></div><select aria-label="Activity period"><option>Last 30 days</option><option>Last 7 days</option></select></div><div className="large-chart"><div className="chart-y-labels"><span>100k</span><span>75k</span><span>50k</span><span>25k</span><span>0</span></div><div className="large-chart-body"><div className="large-grid"><i /><i /><i /><i /><i /></div><svg viewBox="0 0 600 200" preserveAspectRatio="none"><path d="M0 168 C45 146 63 158 103 135 S163 142 201 112 S259 128 300 92 S361 110 401 71 S465 90 500 47 S550 52 600 20" /><path className="area" d="M0 168 C45 146 63 158 103 135 S163 142 201 112 S259 128 300 92 S361 110 401 71 S465 90 500 47 S550 52 600 20 V200 H0Z" /></svg><div className="large-x"><span>May 01</span><span>May 08</span><span>May 15</span><span>May 22</span><span>May 30</span></div></div></div></article><article className="panel quick-panel"><div className="panel-heading"><div><p className="eyebrow">Get started</p><h2>Build your first flow</h2></div></div><div className="quick-item"><span className="quick-number done">✓</span><div><b>Create a project</b><small>Projects keep your integrations organized.</small></div></div><div className="quick-item"><span className="quick-number">2</span><div><b>Generate an API key</b><small>Authenticate your first notification.</small></div><button>→</button></div><div className="quick-item"><span className="quick-number">3</span><div><b>Send a test notification</b><small>See delivery happen in real time.</small></div><button>→</button></div><div className="quick-footer">You have <strong>{plans}</strong> plans available to explore.</div></article></div></div>
}

function Projects({ projects }: { projects: { id: string; name: string; slug: string; status: string; description: string }[] }) {
  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Workspace</p><h2>Projects</h2><p>Manage the notification environments connected to your organization.</p></div><button className="button">+ New project</button></div><div className="project-list">{projects.length ? projects.map((project) => <article className="project-row" key={project.id}><span className="project-logo">{project.name[0]}</span><div><b>{project.name}</b><small>{project.description || project.slug}</small></div><span className={`status ${project.status.toLowerCase()}`}>{project.status.toLowerCase()}</span><Tooltip label="Project actions"><button className="more-button">•••</button></Tooltip></article>) : <div className="empty-state"><span>▦</span><h3>No projects yet</h3><p>Create a project to start sending notifications.</p><button className="button">Create project</button></div>}</div></div>
}

function Settings({ organization }: { organization: string }) {
  return <div className="dashboard-content"><div className="page-title"><div><p className="eyebrow">Workspace</p><h2>Settings</h2><p>Manage your {organization} configuration.</p></div></div><div className="settings-card"><h3>API and integrations</h3><p>API key management and channel configuration will appear here as you connect your first project.</p><div className="settings-placeholder">⌁ <span>Secure by default with scoped API keys and idempotent delivery.</span></div></div></div>
}
