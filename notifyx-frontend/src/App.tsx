import { useState } from 'react'
import './App.css'
import { AuthPage } from './features/auth/AuthPage'
import { DashboardPage } from './features/dashboard/DashboardPage'
import { useAuthStore } from './stores/authStore'

function ArrowIcon() {
  return <svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3 8h9M8.5 4.5 12 8l-3.5 3.5" /></svg>
}

function App() {
  const { user } = useAuthStore()
  const [screen, setScreen] = useState<'landing' | 'auth'>('landing')
  if (user) return <DashboardPage />
  if (screen === 'auth') return <AuthPage />
  return <Landing onStart={() => setScreen('auth')} />
}

function Landing({ onStart }: { onStart: () => void }) {
  return <main className="site-shell">
    <nav className="nav container" aria-label="Main navigation"><a className="brand" href="#"><span className="brand-mark"><span /><span /><span /></span><span>notify<span className="brand-accent">x</span></span></a><div className="nav-links"><a href="#platform">Platform</a><a href="#how-it-works">How it works</a><a href="#developers">Developers</a><a href="#pricing">Pricing</a></div><div className="nav-actions"><button className="sign-in" onClick={onStart}>Sign in</button><button className="button button-small" onClick={onStart}>Start building <ArrowIcon /></button></div></nav>
    <section className="hero container"><div className="hero-copy"><div className="eyebrow"><span className="eyebrow-dot" /> Notification infrastructure for modern teams</div><h1>Ship every message.<br /><span>Never miss a moment.</span></h1><p className="hero-lede">A reliable notification API for transactional email, SMS, and webhooks. Build the experience your users expect without building the infrastructure behind it.</p><div className="hero-actions"><button className="button" onClick={onStart}>Start building <ArrowIcon /></button><a className="text-button" href="#how-it-works">See how it works <ArrowIcon /></a></div><div className="trust-row"><div className="avatars"><span>AK</span><span>MJ</span><span>RS</span><span>+</span></div><p>Trusted by teams who<br /><strong>ship without limits.</strong></p></div></div><div className="hero-visual"><div className="glow glow-primary" /><div className="glow glow-secondary" /><div className="dashboard-card"><div className="dashboard-topbar"><div className="window-dots"><i /><i /><i /></div><span className="live-status"><span /> Live overview</span><span className="date-label">Last 24 hours <b>⌄</b></span></div><div className="dashboard-heading"><div><p>Delivery performance</p><strong>98.6%</strong><span className="positive">↗ 12.4%</span></div><div className="mini-stat"><span>Notifications sent</span><strong>24,892</strong></div></div><div className="chart-area"><div className="chart-y-labels"><span>150k</span><span>100k</span><span>50k</span><span>0</span></div><div className="chart"><div className="chart-grid"><i /><i /><i /><i /></div><svg viewBox="0 0 500 180" preserveAspectRatio="none" role="img" aria-label="Rising notification delivery chart"><path className="chart-fill" d="M0 148 C35 128 35 135 72 120 S105 135 140 98 S180 112 210 85 S247 100 278 67 S315 83 346 55 S380 70 410 35 S452 48 500 12 V180 H0Z" /><path className="chart-line" d="M0 148 C35 128 35 135 72 120 S105 135 140 98 S180 112 210 85 S247 100 278 67 S315 83 346 55 S380 70 410 35 S452 48 500 12" /><circle cx="410" cy="35" r="5" /></svg><div className="chart-x-labels"><span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span><span>Now</span></div></div></div><div className="dashboard-footer"><span>✉ Email <b>12,442</b></span><span>✦ SMS <b>8,230</b></span><span>↗ Webhooks <b>4,220</b></span></div></div><div className="flow-node flow-node-one"><span className="node-icon">✦</span><div><b>Event received</b><small>just now</small></div><strong>✓</strong></div><div className="flow-node flow-node-two"><span className="node-icon purple">↗</span><div><b>Webhook delivered</b><small>42ms latency</small></div><strong>✓</strong></div></div></section>
    <section className="metrics-strip" id="platform"><div className="container metrics"><div><strong>99.99%</strong><span>delivery uptime</span></div><div><strong>&lt; 50ms</strong><span>average latency</span></div><div><strong>10M+</strong><span>messages delivered</span></div><div><strong>24/7</strong><span>built-in reliability</span></div></div></section>
    <section className="feature-section container" id="how-it-works"><div className="section-heading"><div className="eyebrow">Built for momentum</div><h2>Everything your notifications<br /><span>need to move faster.</span></h2></div><div className="feature-grid"><article><span className="feature-icon mint">↯</span><h3>One API. Every channel.</h3><p>Send email, SMS, and webhooks through one consistent, developer-first API.</p><a href="#get-started">Explore the platform <ArrowIcon /></a></article><article><span className="feature-icon purple">⌁</span><h3>Reliable by default.</h3><p>Smart retries, idempotency, and real-time delivery status keep every message on track.</p><a href="#get-started">See reliability in action <ArrowIcon /></a></article><article><span className="feature-icon orange">◌</span><h3>Made for your workflow.</h3><p>Organize projects, manage API keys, and scale with clear usage and subscription controls.</p><a href="#get-started">Meet your new stack <ArrowIcon /></a></article></div></section>
    <section className="cta-section container" id="get-started"><div><div className="eyebrow">Ready when you are</div><h2>Make every message<br /><span>count.</span></h2></div><button className="button button-light" onClick={onStart}>Start building <ArrowIcon /></button></section>
    <footer className="container footer" id="developers"><a className="brand" href="#"><span className="brand-mark"><span /><span /><span /></span><span>notify<span className="brand-accent">x</span></span></a><span>© 2026 NotifyX. Built for reliable communication.</span></footer>
  </main>
}

export default App
