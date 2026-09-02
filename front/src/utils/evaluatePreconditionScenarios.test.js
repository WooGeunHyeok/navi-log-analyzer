import { describe, it, expect } from 'vitest'
import { evaluatePreconditionScenarios } from './evaluatePreconditionScenarios.js'

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
    tag: 'SomeTag',
    rawMessage: '',
    ...overrides,
  }
}

const triggerScenario = {
  id: 'scenarioA',
  label: '시나리오 A',
  trigger: (n) => n.rawMessage === 'TRIGGER',
  checks: [
    {
      id: 'checkAlwaysTrue',
      label: '항상 참',
      evaluate: () => ({ satisfied: true, evidence: 'evidence-a' }),
    },
    {
      id: 'checkAlwaysFalse',
      label: '항상 거짓',
      evaluate: () => ({ satisfied: false, evidence: null }),
    },
  ],
}

describe('evaluatePreconditionScenarios', () => {
  it('트리거 노드가 없으면 applicable: false를 반환한다', () => {
    const tree = [node({ rawMessage: '상관없음' })]

    const results = evaluatePreconditionScenarios(tree, [], [triggerScenario])

    expect(results).toEqual([
      { id: 'scenarioA', label: '시나리오 A', applicable: false, triggerNode: null, checks: [] },
    ])
  })

  it('트리거 노드를 찾으면 각 체크를 실행해서 결과를 담는다', () => {
    const triggerNode = node({ id: 5, rawMessage: 'TRIGGER', timestamp: '01-01 12:00:00.000' })
    const tree = [triggerNode]

    const results = evaluatePreconditionScenarios(tree, [], [triggerScenario])

    expect(results).toHaveLength(1)
    expect(results[0].applicable).toBe(true)
    expect(results[0].triggerNode.id).toBe(5)
    expect(results[0].checks).toEqual([
      { id: 'checkAlwaysTrue', label: '항상 참', satisfied: true, evidence: 'evidence-a' },
      { id: 'checkAlwaysFalse', label: '항상 거짓', satisfied: false, evidence: null },
    ])
  })

  it('트리거는 깊은 자식 노드에서도 찾을 수 있다', () => {
    const tree = [
      node({
        id: 1,
        rawMessage: 'root',
        children: [node({ id: 2, rawMessage: 'TRIGGER', timestamp: '01-01 12:00:00.000' })],
      }),
    ]

    const results = evaluatePreconditionScenarios(tree, [], [triggerScenario])

    expect(results[0].applicable).toBe(true)
    expect(results[0].triggerNode.id).toBe(2)
  })

  it('각 체크의 evaluate에 트리거 timestamp와 allNodes/systemLogs를 전달한다', () => {
    const triggerNode = node({ id: 1, rawMessage: 'TRIGGER', timestamp: '01-01 12:00:00.000' })
    const otherNode = node({ id: 2, rawMessage: 'other' })
    const tree = [triggerNode, otherNode]
    const logs = [systemLog({ id: 99, rawMessage: 'sys-log' })]

    let received = null
    const scenario = {
      id: 'scenarioB',
      label: '시나리오 B',
      trigger: (n) => n.rawMessage === 'TRIGGER',
      checks: [
        {
          id: 'inspect',
          label: '검사',
          evaluate: (triggerTimestamp, context) => {
            received = { triggerTimestamp, context }
            return { satisfied: true, evidence: null }
          },
        },
      ],
    }

    evaluatePreconditionScenarios(tree, logs, [scenario])

    expect(received.triggerTimestamp).toBe('01-01 12:00:00.000')
    expect(received.context.allNodes.map((n) => n.id)).toEqual([1, 2])
    expect(received.context.systemLogs).toBe(logs)
  })

  it('여러 시나리오를 각각 독립적으로 평가한다', () => {
    const tree = [node({ rawMessage: 'TRIGGER', timestamp: '01-01 12:00:00.000' })]
    const noTriggerScenario = {
      id: 'scenarioC',
      label: '시나리오 C',
      trigger: (n) => n.rawMessage === '없는 트리거',
      checks: [],
    }

    const results = evaluatePreconditionScenarios(tree, [], [triggerScenario, noTriggerScenario])

    expect(results).toHaveLength(2)
    expect(results[0].applicable).toBe(true)
    expect(results[1].applicable).toBe(false)
  })
})
