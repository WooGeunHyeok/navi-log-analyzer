import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import App from './App.jsx'
import * as api from './api/logAnalyzer.js'

vi.mock('./api/logAnalyzer.js')

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

async function fillAndSubmitUploadForm(user) {
  await user.type(screen.getByLabelText('제목'), '이슈 로그')
  const file = new File(['log content'], 'issue.log', { type: 'text/plain' })
  await user.upload(screen.getByLabelText('로그 파일'), file)
  await user.click(screen.getByRole('button', { name: '분석 시작' }))
}

describe('App', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('업로드 -> 파싱 -> 분석 순서로 화면이 전환되고 트리가 렌더링된다', async () => {
    const uploadDeferred = deferred()
    const parseDeferred = deferred()
    const analyzeDeferred = deferred()
    api.uploadLogFile.mockReturnValue(uploadDeferred.promise)
    api.runParsing.mockReturnValue(parseDeferred.promise)
    api.runAnalysisFlow.mockReturnValue(analyzeDeferred.promise)

    const user = userEvent.setup()
    render(<App />)

    await fillAndSubmitUploadForm(user)

    expect(await screen.findByText('업로드 중...')).toBeInTheDocument()
    uploadDeferred.resolve(1)

    expect(await screen.findByText('파싱 중...')).toBeInTheDocument()
    parseDeferred.resolve(1)

    expect(await screen.findByText('분석 중...')).toBeInTheDocument()
    analyzeDeferred.resolve([
      {
        id: 1,
        fileId: 1,
        step: 1,
        timestamp: '01-01 00:00:00.000',
        threadId: 1,
        logLevel: 'I',
        layer: 'CONTAINER',
        rawMessage: 'raw',
        fileName: 'NaviMain.java',
        filePath: '/NaviMain.java',
        functionName: 'onCreate',
        lineNumber: 10,
        matchType: 'EXACT_KEY',
        depth: 0,
        children: [],
      },
    ])

    expect(await screen.findByText('onCreate')).toBeInTheDocument()
  })

  it('중간에 실패하면 에러 배너를 보여주고, 다시 시도하면 업로드 폼으로 돌아간다', async () => {
    api.uploadLogFile.mockRejectedValue(new Error('파일 크기는 50MB를 초과할 수 없습니다.'))

    const user = userEvent.setup()
    render(<App />)

    await fillAndSubmitUploadForm(user)

    expect(await screen.findByText('파일 크기는 50MB를 초과할 수 없습니다.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '처음부터 다시 시도' }))

    expect(await screen.findByLabelText('제목')).toBeInTheDocument()
  })
})
