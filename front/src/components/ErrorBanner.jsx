import styles from './ErrorBanner.module.css'

function ErrorBanner({ message, onRetry }) {
  return (
    <div className={styles.banner} role="alert">
      <p className={styles.message}>{message}</p>
      <button type="button" className={styles.retry} onClick={onRetry}>
        처음부터 다시 시도
      </button>
    </div>
  )
}

export default ErrorBanner
