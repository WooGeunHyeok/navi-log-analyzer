import { describe, it, expect, vi, afterEach } from 'vitest'
import { uploadLogFile, runParsing, runAnalysisFlow, MAX_FILE_SIZE_BYTES } from './logAnalyzer.js'

function mockFetchOnce(body, { ok = true, status = 200 } = {}) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok,
    status,
    json: async () => body,
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('MAX_FILE_SIZE_BYTES', () => {
  it('50MB이다', () => {
    expect(MAX_FILE_SIZE_BYTES).toBe(50 * 1024 * 1024)
  })
})

describe('uploadLogFile', () => {
  it('multipart로 업로드하고 fileId를 반환한다', async () => {
    const fetchMock = mockFetchOnce({ success: true, message: 'ok', data: 42 })
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })

    const fileId = await uploadLogFile({ title: '이슈 로그', jiraTicketKey: 'NAVI-1', file })

    expect(fileId).toBe(42)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/api/v1/logs/upload')
    expect(options.method).toBe('POST')
    expect(options.body).toBeInstanceOf(FormData)
    expect(options.body.get('file')).toBe(file)
  })

  it('success가 false면 서버 메시지로 에러를 던진다', async () => {
    mockFetchOnce({ success: false, message: '제목은 필수입니다.', data: null })
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })

    await expect(uploadLogFile({ title: '', file })).rejects.toThrow('제목은 필수입니다.')
  })
})

describe('runParsing', () => {
  it('fileId 경로로 POST 요청하고 결과를 반환한다', async () => {
    const fetchMock = mockFetchOnce({ success: true, message: 'ok', data: 42 })

    const result = await runParsing(42)

    expect(result).toBe(42)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/api/v1/parsing/42')
    expect(options.method).toBe('POST')
  })
})

describe('runAnalysisFlow', () => {
  it('fileId 경로로 GET 요청하고 트리 배열을 반환한다', async () => {
    const tree = [{ id: 1, functionName: 'onCreate', children: [] }]
    const fetchMock = mockFetchOnce({ success: true, message: 'ok', data: tree })

    const result = await runAnalysisFlow(42)

    expect(result).toEqual(tree)
    const [url] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/api/v1/logs/analysis/flow/42')
  })

  it('HTTP 응답이 실패(ok=false)이고 메시지가 없으면 상태 코드를 포함한 에러를 던진다', async () => {
    mockFetchOnce({ success: false, message: null, data: null }, { ok: false, status: 500 })

    await expect(runAnalysisFlow(42)).rejects.toThrow('500')
  })

  it('응답 본문이 JSON이 아니면 상태 코드를 포함한 에러를 던진다', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => {
        throw new SyntaxError('Unexpected token')
      },
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(runAnalysisFlow(42)).rejects.toThrow('500')
  })

  it('네트워크 연결 자체가 실패하면 사용자 친화적인 에러 메시지를 던진다', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))
    vi.stubGlobal('fetch', fetchMock)

    await expect(runAnalysisFlow(42)).rejects.toThrow(
      '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.',
    )
  })
})
