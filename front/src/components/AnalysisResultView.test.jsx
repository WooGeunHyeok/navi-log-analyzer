import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'
import AnalysisResultView from './AnalysisResultView.jsx'

function node(overrides) {
  return {
    id: 1,
    fileId: 1,
    step: 1,
    timestamp: '01-01 00:00:00.000',
    threadId: 1,
    logLevel: 'I',
    layer: 'CONTAINER',
    rawMessage: 'raw',
    fileName: null,
    filePath: null,
    functionName: null,
    lineNumber: null,
    matchType: 'NONE',
    depth: 0,
    children: [],
    ...overrides,
  }
}

const TREE = [
  node({
    id: 1,
    functionName: 'onCreate',
    fileName: 'NaviMain.java',
    matchType: 'EXACT_KEY',
    children: [
      node({ id: 2, functionName: null, fileName: null, matchType: 'NONE', depth: 1 }),
      node({
        id: 3,
        functionName: 'routeInit',
        fileName: 'RouteEngine.java',
        matchType: 'EXACT_KEY',
        depth: 1,
      }),
    ],
  }),
]

describe('AnalysisResultView', () => {
  it('기본적으로 트리가 전체 펼쳐진 상태로 렌더링된다', () => {
    render(<AnalysisResultView treeData={TREE} />)

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('routeInit')).toBeInTheDocument()
    expect(screen.getByText('소스 위치 매핑 안 됨')).toBeInTheDocument()
  })

  it('검색어를 입력하면 매치되지 않는 형제는 숨기고 조상은 유지한다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.type(screen.getByLabelText('함수명 또는 파일명 검색'), 'routeInit')

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('routeInit')).toBeInTheDocument()
    expect(screen.queryByText('소스 위치 매핑 안 됨')).not.toBeInTheDocument()
  })

  it('"매핑 안 된 노드만" 체크 시 matchType=NONE 노드만 남긴다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.click(screen.getByLabelText('매핑 안 된 노드만'))

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('소스 위치 매핑 안 됨')).toBeInTheDocument()
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()
  })

  it('검색 결과가 없으면 안내 문구를 보여준다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.type(screen.getByLabelText('함수명 또는 파일명 검색'), '존재하지-않음')

    expect(screen.getByText('검색 결과가 없습니다.')).toBeInTheDocument()
  })

  it('전체 접기/펼치기 버튼으로 모든 노드를 토글한다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.click(screen.getByRole('button', { name: '전체 접기' }))
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '전체 펼치기' }))
    expect(screen.getByText('routeInit')).toBeInTheDocument()
  })

  it('검색어를 지우면 이전 펼침 상태로 복원된다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.click(screen.getByRole('button', { name: '접기' }))
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()

    const searchInput = screen.getByLabelText('함수명 또는 파일명 검색')
    await user.type(searchInput, 'routeInit')
    expect(screen.getByText('routeInit')).toBeInTheDocument()

    await user.clear(searchInput)
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()
  })
})
