import { useReducer } from 'react'
import UploadForm from './components/UploadForm.jsx'
import ProcessingStatus from './components/ProcessingStatus.jsx'
import AnalysisResultView from './components/AnalysisResultView.jsx'
import ErrorBanner from './components/ErrorBanner.jsx'
import { uploadLogFile, runParsing, runAnalysisFlow } from './api/logAnalyzer.js'
import styles from './App.module.css'

const initialState = {
  step: 'upload',
  subStep: null,
  fileId: null,
  analysisResult: null,
  error: null,
}

function reducer(state, action) {
  switch (action.type) {
    case 'SUBMIT_UPLOAD':
      return { ...initialState, step: 'processing', subStep: 'uploading' }
    case 'UPLOAD_SUCCESS':
      return { ...state, fileId: action.fileId, subStep: 'parsing' }
    case 'PARSE_SUCCESS':
      return { ...state, subStep: 'analyzing' }
    case 'ANALYZE_SUCCESS':
      return { ...state, step: 'result', subStep: null, analysisResult: action.analysisResult }
    case 'FAIL':
      return { ...state, step: 'error', subStep: null, error: action.message }
    case 'RESET':
      return initialState
    default:
      return state
  }
}

function App() {
  const [state, dispatch] = useReducer(reducer, initialState)

  async function handleUploadSubmit(formValues) {
    dispatch({ type: 'SUBMIT_UPLOAD' })
    try {
      const fileId = await uploadLogFile(formValues)
      dispatch({ type: 'UPLOAD_SUCCESS', fileId })
      await runParsing(fileId)
      dispatch({ type: 'PARSE_SUCCESS' })
      const analysisResult = await runAnalysisFlow(fileId)
      dispatch({ type: 'ANALYZE_SUCCESS', analysisResult })
    } catch (err) {
      dispatch({ type: 'FAIL', message: err.message })
    }
  }

  function handleRetry() {
    dispatch({ type: 'RESET' })
  }

  return (
    <main className={styles.app}>
      <h1 className={styles.title}>내비 로그 소스코드 흐름도</h1>
      {state.step === 'upload' && <UploadForm onSubmit={handleUploadSubmit} />}
      {state.step === 'processing' && <ProcessingStatus subStep={state.subStep} />}
      {state.step === 'result' && (
        <AnalysisResultView tree={state.analysisResult.tree} systemLogs={state.analysisResult.systemLogs} />
      )}
      {state.step === 'error' && <ErrorBanner message={state.error} onRetry={handleRetry} />}
    </main>
  )
}

export default App
