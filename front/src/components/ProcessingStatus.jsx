import styles from './ProcessingStatus.module.css'

const SUB_STEP_LABELS = {
  uploading: '업로드 중...',
  parsing: '파싱 중...',
  analyzing: '분석 중...',
}

function ProcessingStatus({ subStep }) {
  return (
    <div className={styles.wrapper}>
      <div className={styles.spinner} aria-hidden="true" />
      <p className={styles.label}>{SUB_STEP_LABELS[subStep] ?? '처리 중...'}</p>
    </div>
  )
}

export default ProcessingStatus
