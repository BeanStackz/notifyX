import { useEffect, useState } from 'react'

export function Toast({ message, onClose }: { message: string; onClose: () => void }) {
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    const timeout = window.setTimeout(() => setVisible(false), 4200)
    const remove = window.setTimeout(onClose, 4550)
    return () => {
      window.clearTimeout(timeout)
      window.clearTimeout(remove)
    }
  }, [message, onClose])

  function close() {
    setVisible(false)
    window.setTimeout(onClose, 350)
  }

  return (
    <div className={`toast toast-error ${visible ? 'toast-visible' : 'toast-hidden'}`} role="alert">
      <span className="toast-icon">!</span>
      <span className="toast-message">{message}</span>
      <button className="toast-close" onClick={close} aria-label="Dismiss error">×</button>
    </div>
  )
}
