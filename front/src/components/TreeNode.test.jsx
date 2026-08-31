import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import TreeNode from './TreeNode.jsx'
import styles from './TreeNode.module.css'
import { formatLogLine } from '../utils/formatLogLine.js'

function buildNode(overrides) {
  return {
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
    ...overrides,
  }
}

describe('TreeNode', () => {
  it('함수명과 레이어 뱃지를 보여준다', () => {
    render(
      <TreeNode node={buildNode({})} collapsedIds={new Set()} forceExpanded={false} onToggle={() => {}} />,
    )

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('CONTAINER')).toBeInTheDocument()
    expect(screen.getByText('NaviMain.java:10')).toBeInTheDocument()
  })

  it('matchType이 NONE이면 매핑 안 됨 텍스트를 보여준다', () => {
    render(
      <TreeNode
        node={buildNode({ matchType: 'NONE', functionName: null, fileName: null, lineNumber: null })}
        collapsedIds={new Set()}
        forceExpanded={false}
        onToggle={() => {}}
      />,
    )

    const unmatchedText = screen.getByText('소스 위치 매핑 안 됨')
    expect(unmatchedText).toBeInTheDocument()

    const row = unmatchedText.closest(`.${styles.row}`)
    expect(row).toHaveClass(styles.rowUnmatched)
  })

  it('원본 로그 형식(타임스탬프/스레드ID/레벨/레이어/메시지)을 노드 아래에 보여준다', () => {
    const node = buildNode({
      timestamp: '01-01 15:23:17.910',
      threadId: 1542,
      logLevel: 'E',
      layer: 'JNI_LAYER',
      rawMessage: 'call_java_com_mnsoft_navi_NativeCall_getNaviAutoMoveTimer entered',
    })

    const { container } = render(
      <TreeNode node={node} collapsedIds={new Set()} forceExpanded={false} onToggle={() => {}} />,
    )

    const logLine = container.querySelector(`.${styles.logLine}`)
    expect(logLine).not.toBeNull()
    expect(logLine.textContent).toBe(formatLogLine(node))
  })

  it('matchType이 NONE이면 로그 줄도 흐리게 표시한다', () => {
    const node = buildNode({ matchType: 'NONE', functionName: null, fileName: null, lineNumber: null })

    const { container } = render(
      <TreeNode node={node} collapsedIds={new Set()} forceExpanded={false} onToggle={() => {}} />,
    )

    const logLine = container.querySelector(`.${styles.logLine}`)
    expect(logLine).toHaveClass(styles.logLineUnmatched)
    expect(logLine.textContent).toBe(formatLogLine(node))
  })

  it('자식이 있으면 토글 버튼을 클릭해 접고 펼 수 있다', async () => {
    const user = userEvent.setup()
    const onToggle = vi.fn()
    const child = buildNode({ id: 2, functionName: 'childFn' })
    const parent = buildNode({ id: 1, functionName: 'parentFn', children: [child] })

    render(
      <TreeNode node={parent} collapsedIds={new Set()} forceExpanded={false} onToggle={onToggle} />,
    )

    expect(screen.getByText('childFn')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '접기' }))

    expect(onToggle).toHaveBeenCalledWith(1)
  })

  it('collapsedIds에 포함돼 있어도 forceExpanded가 true이면 자식을 보여준다', () => {
    const child = buildNode({ id: 2, functionName: 'childFn' })
    const parent = buildNode({ id: 1, functionName: 'parentFn', children: [child] })

    render(
      <TreeNode
        node={parent}
        collapsedIds={new Set([1])}
        forceExpanded
        onToggle={() => {}}
      />,
    )

    expect(screen.getByText('childFn')).toBeInTheDocument()
  })

  it('collapsedIds에 포함되고 forceExpanded가 false이면 자식을 숨긴다', () => {
    const child = buildNode({ id: 2, functionName: 'childFn' })
    const parent = buildNode({ id: 1, functionName: 'parentFn', children: [child] })

    render(
      <TreeNode
        node={parent}
        collapsedIds={new Set([1])}
        forceExpanded={false}
        onToggle={() => {}}
      />,
    )

    expect(screen.queryByText('childFn')).not.toBeInTheDocument()
  })
})
