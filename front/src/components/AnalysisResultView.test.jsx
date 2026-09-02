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
  it('사전 조건 체크리스트에 필요한 tree/systemLogs를 그대로 전달한다', () => {
    const treeWithTrigger = [
      node({
        id: 1,
        rawMessage: '[GVwNaviSetting_ReturnToMap::OnProc()] setNaviAutoMoveTimer(10)',
        timestamp: '01-01 15:25:07.630',
      }),
    ]

    render(<AnalysisResultView tree={treeWithTrigger} systemLogs={[]} />)

    expect(screen.getByText('목적지 설정됨')).toBeInTheDocument()
  })

  it('기본값은 "매핑된 노드만"이며, 매핑된 노드는 펼쳐진 상태로 보여준다', () => {
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    expect(screen.getByLabelText('매핑된 노드만')).toBeChecked()
    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('routeInit')).toBeInTheDocument()
    expect(screen.queryByText('소스 위치 매핑 안 됨')).not.toBeInTheDocument()
  })

  it('"전체"를 선택하면 매핑 안 된 노드도 함께 보여준다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    await user.click(screen.getByLabelText('전체'))

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('routeInit')).toBeInTheDocument()
    expect(screen.getByText('소스 위치 매핑 안 됨')).toBeInTheDocument()
  })

  it('"매핑 안 된 노드만"을 선택하면 matchType=NONE 노드와 조상만 남긴다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    await user.click(screen.getByLabelText('매핑 안 된 노드만'))

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('소스 위치 매핑 안 됨')).toBeInTheDocument()
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()
  })

  it('세 필터는 서로 배타적이다 (하나만 항상 선택됨)', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    const allRadio = screen.getByLabelText('전체')
    const matchedRadio = screen.getByLabelText('매핑된 노드만')
    const unmatchedRadio = screen.getByLabelText('매핑 안 된 노드만')

    expect(matchedRadio).toBeChecked()

    await user.click(unmatchedRadio)
    expect(unmatchedRadio).toBeChecked()
    expect(matchedRadio).not.toBeChecked()
    expect(allRadio).not.toBeChecked()

    await user.click(allRadio)
    expect(allRadio).toBeChecked()
    expect(unmatchedRadio).not.toBeChecked()
    expect(matchedRadio).not.toBeChecked()

    await user.click(matchedRadio)
    expect(matchedRadio).toBeChecked()
    expect(allRadio).not.toBeChecked()
    expect(unmatchedRadio).not.toBeChecked()
  })

  it('검색어를 입력하면 매치되지 않는 형제는 숨기고 조상은 유지한다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    await user.type(screen.getByLabelText('함수명 또는 파일명 검색'), 'routeInit')

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('routeInit')).toBeInTheDocument()
    expect(screen.queryByText('소스 위치 매핑 안 됨')).not.toBeInTheDocument()
  })

  it('검색 결과가 없으면 안내 문구를 보여준다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    await user.type(screen.getByLabelText('함수명 또는 파일명 검색'), '존재하지-않음')

    expect(screen.getByText('검색 결과가 없습니다.')).toBeInTheDocument()
  })

  it('전체 접기/펼치기 버튼으로 모든 노드를 토글한다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    await user.click(screen.getByRole('button', { name: '전체 접기' }))
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '전체 펼치기' }))
    expect(screen.getByText('routeInit')).toBeInTheDocument()
  })

  it('매핑 필터가 켜져 있어도 개별 노드를 접고 펼 수 있다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    // 기본값인 "매핑된 노드만" 필터가 켜진 상태 그대로, 검색어 없이 개별 노드를 접는다.
    expect(screen.getByLabelText('매핑된 노드만')).toBeChecked()

    await user.click(screen.getByRole('button', { name: '접기' }))
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '펼치기' }))
    expect(screen.getByText('routeInit')).toBeInTheDocument()
  })

  it('검색어를 지우면 이전 펼침 상태로 복원된다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView tree={TREE} systemLogs={[]} />)

    await user.click(screen.getByRole('button', { name: '접기' }))
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()

    const searchInput = screen.getByLabelText('함수명 또는 파일명 검색')
    await user.type(searchInput, 'routeInit')
    expect(screen.getByText('routeInit')).toBeInTheDocument()

    await user.clear(searchInput)
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()
  })
})
