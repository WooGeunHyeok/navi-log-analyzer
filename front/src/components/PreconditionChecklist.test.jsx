import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import PreconditionChecklist from './PreconditionChecklist.jsx'

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

function systemLog(overrides) {
  return {
    id: 1,
    fileId: 1,
    timestamp: '01-01 00:00:00.000',
    threadId: 1,
    logLevel: 'D',
    tag: 'GeneralSettings',
    rawMessage: '',
    ...overrides,
  }
}

const TRIGGER_NODE = node({
  id: 274,
  timestamp: '01-01 15:25:07.630',
  rawMessage: '[GVwNaviSetting_ReturnToMap::OnProc()] setNaviAutoMoveTimer(10)',
})

describe('PreconditionChecklist', () => {
  it('트리거 이벤트가 로그에 없으면 아무것도 보여주지 않는다', () => {
    const { container } = render(
      <PreconditionChecklist tree={[node({ rawMessage: '상관없는 로그' })]} systemLogs={[]} />,
    )

    expect(container).toBeEmptyDOMElement()
  })

  it('모든 조건이 충족되면 충족으로 표시하고 근거 로그를 함께 보여준다, 경고 문구는 없다', () => {
    const tree = [
      node({ id: 1, timestamp: '01-01 15:01:01.021', rawMessage: 'RP Normal Run!' }),
      TRIGGER_NODE,
    ]
    const systemLogs = [
      systemLog({ timestamp: '01-01 15:00:00.000', rawMessage: 'ignStatus is true' }),
    ]

    render(<PreconditionChecklist tree={tree} systemLogs={systemLogs} />)

    expect(screen.getByText('목적지 설정됨')).toBeInTheDocument()
    expect(screen.getByText('IGN On 상태')).toBeInTheDocument()
    expect(screen.getAllByText('충족')).toHaveLength(2)
    expect(screen.getByText('RP Normal Run!')).toBeInTheDocument()
    expect(screen.getByText('ignStatus is true')).toBeInTheDocument()
    expect(screen.queryByText('⚠️ 사전 조건 중 일부가 확인되지 않았습니다.')).not.toBeInTheDocument()
  })

  it('조건 중 하나라도 미충족이면 미충족 표시와 경고 문구를 보여준다', () => {
    // 목적지 설정 로그가 없음 -> destinationSet 미충족
    render(<PreconditionChecklist tree={[TRIGGER_NODE]} systemLogs={[]} />)

    expect(screen.getByText('목적지 설정됨')).toBeInTheDocument()
    expect(screen.getByText('IGN On 상태')).toBeInTheDocument()
    expect(screen.getAllByText('미충족')).toHaveLength(2)
    expect(screen.getByText('⚠️ 사전 조건 중 일부가 확인되지 않았습니다.')).toBeInTheDocument()
  })
})
