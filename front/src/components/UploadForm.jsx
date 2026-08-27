import { useState } from 'react'
import { MAX_FILE_SIZE_BYTES } from '../api/logAnalyzer.js'
import styles from './UploadForm.module.css'

function UploadForm({ onSubmit }) {
  const [title, setTitle] = useState('')
  const [jiraTicketKey, setJiraTicketKey] = useState('')
  const [file, setFile] = useState(null)
  const [validationError, setValidationError] = useState(null)

  function handleSubmit(event) {
    event.preventDefault()

    if (title.trim() === '') {
      setValidationError('제목을 입력해주세요.')
      return
    }
    if (!file) {
      setValidationError('업로드할 로그 파일을 선택해주세요.')
      return
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setValidationError('파일 크기는 50MB를 초과할 수 없습니다.')
      return
    }

    setValidationError(null)
    onSubmit({
      title: title.trim(),
      jiraTicketKey: jiraTicketKey.trim() || undefined,
      file,
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.field}>
        <label htmlFor="title">제목</label>
        <input id="title" type="text" value={title} onChange={(event) => setTitle(event.target.value)} />
      </div>

      <div className={styles.field}>
        <label htmlFor="jiraTicketKey">Jira 티켓 키 (선택)</label>
        <input
          id="jiraTicketKey"
          type="text"
          value={jiraTicketKey}
          onChange={(event) => setJiraTicketKey(event.target.value)}
        />
      </div>

      <div className={styles.field}>
        <label htmlFor="file">로그 파일</label>
        <input id="file" type="file" onChange={(event) => setFile(event.target.files[0] ?? null)} />
      </div>

      {validationError && (
        <p role="alert" className={styles.error}>
          {validationError}
        </p>
      )}

      <button type="submit" className={styles.submit}>
        분석 시작
      </button>
    </form>
  )
}

export default UploadForm
