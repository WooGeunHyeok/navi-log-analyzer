const BASE_URL = 'http://localhost:8081'

export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024

async function parseApiResponse(response) {
  let body
  try {
    body = await response.json()
  } catch {
    throw new Error(`요청이 실패했습니다. (status: ${response.status})`)
  }
  if (!response.ok || !body.success) {
    throw new Error(body.message || `요청이 실패했습니다. (status: ${response.status})`)
  }
  return body.data
}

async function fetchJson(url, options) {
  let response
  try {
    response = await fetch(url, options)
  } catch {
    throw new Error('서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.')
  }
  return parseApiResponse(response)
}

export async function uploadLogFile({ title, jiraTicketKey, file }) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append(
    'request',
    new Blob([JSON.stringify({ title, jiraTicketKey })], { type: 'application/json' }),
  )

  return fetchJson(`${BASE_URL}/api/v1/logs/upload`, {
    method: 'POST',
    body: formData,
  })
}

export async function runParsing(fileId) {
  return fetchJson(`${BASE_URL}/api/v1/parsing/${fileId}`, {
    method: 'POST',
  })
}

export async function runAnalysisFlow(fileId) {
  return fetchJson(`${BASE_URL}/api/v1/logs/analysis/flow/${fileId}`)
}
