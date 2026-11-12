import { useState } from 'react'
import { useAuthStore } from '../../stores/authStore'

export function AuthPage() {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const { login, register, isLoading, error, clearError } = useAuthStore()

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearError()
    if (mode === 'login') await login(email, password)
    else await register(fullName, email, password)
  }

  return (
    <main className="auth-page">
      <div className="auth-art"><div className="auth-orbit orbit-one" /><div className="auth-orbit orbit-two" /><div className="auth-art-copy"><span className="eyebrow"><span className="eyebrow-dot" /> NotifyX platform</span><h1>Communication that<br /><span>keeps moving.</span></h1><p>Reliable notification infrastructure for every important moment.</p></div></div>
      <section className="auth-panel">
        <a className="brand" href="#" onClick={() => window.location.reload()}><span className="brand-mark"><span /><span /><span /></span><span>notify<span className="brand-accent">x</span></span></a>
        <div className="auth-form-wrap">
          <div className="auth-tabs"><button className={mode === 'login' ? 'active' : ''} onClick={() => { setMode('login'); clearError() }}>Sign in</button><button className={mode === 'register' ? 'active' : ''} onClick={() => { setMode('register'); clearError() }}>Create account</button></div>
          <h2>{mode === 'login' ? 'Welcome back' : 'Start building with NotifyX'}</h2><p className="auth-subtitle">{mode === 'login' ? 'Sign in to manage your notification infrastructure.' : 'Create your workspace and ship your first message.'}</p>
          <form onSubmit={submit}>
            {mode === 'register' && <label>Full name<input required minLength={2} value={fullName} onChange={(event) => setFullName(event.target.value)} placeholder="Ada Lovelace" /></label>}
            <label>Email address<input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@company.com" /></label>
            <label>Password
              <span className="password-field">
                <input required minLength={4} type={showPassword ? 'text' : 'password'} value={password} onChange={(event) => setPassword(event.target.value)} placeholder="••••••••" />
                <button type="button" className="password-toggle" onClick={() => setShowPassword((visible) => !visible)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? 'Hide' : 'Show'}</button>
              </span>
            </label>
            {error && <div className="form-error" role="alert">{error}</div>}
            <button className="button auth-submit" disabled={isLoading}>{isLoading ? 'Connecting…' : mode === 'login' ? 'Sign in to dashboard' : 'Create account'} <span>→</span></button>
          </form>
          <p className="auth-legal">By continuing, you agree to NotifyX terms and privacy policy.</p>
        </div>
      </section>
    </main>
  )
}
