const BASE_URL = 'http://localhost:8081'

export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024

async function parseApiResponse(response) {
  const body = await response.json()
  if (!response.ok || !body.success) {
    throw new Error(body.message || `요청이 실패했습니다. (status: ${response.status})`)
  }
  return body.data
}

export async function uploadLogFile({ title, jiraTicketKey, file }) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append(
    'request',
    new Blob([JSON.stringify({ title, jiraTicketKey })], { type: 'application/json' }),
  )

  const response = await fetch(`${BASE_URL}/api/v1/logs/upload`, {
    method: 'POST',
    body: formData,
  })
  return parseApiResponse(response)
}

export async function runParsing(fileId) {
  const response = await fetch(`${BASE_URL}/api/v1/parsing/${fileId}`, {
    method: 'POST',
  })
  return parseApiResponse(response)
}

export async function runAnalysisFlow(fileId) {
  const response = await fetch(`${BASE_URL}/api/v1/logs/analysis/flow/${fileId}`)
  return parseApiResponse(response)
}
