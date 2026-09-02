import { describe, it, expect } from 'vitest'
import { evaluatePreconditionScenarios } from '../utils/evaluatePreconditionScenarios.js'
import { PRECONDITION_SCENARIOS } from './preconditionScenarios.js'

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
  layer: 'CONTAINER',
  timestamp: '01-01 15:25:07.630',
  rawMessage: '[GVwNaviSetting_ReturnToMap::OnProc()] setNaviAutoMoveTimer(10)',
  functionName: 'GVwNaviSetting_ReturnToMap::OnProc',
})

function evaluate(tree, systemLogs) {
  const [result] = evaluatePreconditionScenarios(tree, systemLogs, PRECONDITION_SCENARIOS)
  return result
}

describe('returnToMapAutoMove 시나리오', () => {
  it('트리거 로그가 없으면 적용 대상이 아니다', () => {
    const result = evaluate([node({ rawMessage: '상관없는 로그' })], [])

    expect(result.applicable).toBe(false)
  })

  it('두 조건 모두 트리거 이전에 만족되면 둘 다 충족으로 표시한다', () => {
    const tree = [
      node({ id: 1, timestamp: '01-01 15:01:01.021', rawMessage: 'RP Normal Run!' }),
      TRIGGER_NODE,
    ]
    const systemLogs = [
      systemLog({ id: 1, timestamp: '01-01 15:00:00.000', tag: 'GeneralSettings', rawMessage: 'ignStatus is true' }),
    ]

    const result = evaluate(tree, systemLogs)

    expect(result.applicable).toBe(true)
    const [ignCheck, destinationCheck] = result.checks
    expect(ignCheck).toMatchObject({ id: 'ignOn', satisfied: true })
    expect(destinationCheck).toMatchObject({ id: 'destinationSet', satisfied: true, evidence: 'RP Normal Run!' })
  })

  it('목적지 설정 로그가 트리거 "이후"에만 있으면 미충족으로 본다', () => {
    const tree = [
      TRIGGER_NODE,
      node({ id: 1, timestamp: '01-01 15:30:00.000', rawMessage: 'RP Normal Run!' }), // 트리거보다 늦음
    ]

    const result = evaluate(tree, [])

    const destinationCheck = result.checks.find((c) => c.id === 'destinationSet')
    expect(destinationCheck.satisfied).toBe(false)
  })

  it('트리거 시점 이전 IGN 값이 false면 IGN 조건은 미충족이다', () => {
    const tree = [TRIGGER_NODE]
    const systemLogs = [
      systemLog({ timestamp: '01-01 15:00:00.000', tag: 'GeneralSettings', rawMessage: 'ignStatus is false' }),
    ]

    const result = evaluate(tree, systemLogs)

    const ignCheck = result.checks.find((c) => c.id === 'ignOn')
    expect(ignCheck.satisfied).toBe(false)
  })

  it('트리거 이전 IGN 값을 알 수 없으면(로그 없음) IGN 조건은 미충족이다', () => {
    const result = evaluate([TRIGGER_NODE], [])

    const ignCheck = result.checks.find((c) => c.id === 'ignOn')
    expect(ignCheck.satisfied).toBe(false)
    expect(ignCheck.evidence).toBeNull()
  })

  it('트리거 시점보다 늦게 찍힌 IGN 로그는 무시하고, 그 이전 중 가장 가까운 값을 사용한다', () => {
    const tree = [TRIGGER_NODE]
    const systemLogs = [
      systemLog({ timestamp: '01-01 15:00:00.000', tag: 'GeneralSettings', rawMessage: 'ignStatus is false' }),
      systemLog({ timestamp: '01-01 15:10:00.000', tag: 'GeneralSettings', rawMessage: 'ignStatus is true' }),
      systemLog({ timestamp: '01-01 15:30:00.000', tag: 'GeneralSettings', rawMessage: 'ignStatus is false' }), // 트리거(15:25:07.630)보다 늦음
    ]

    const result = evaluate(tree, systemLogs)

    const ignCheck = result.checks.find((c) => c.id === 'ignOn')
    expect(ignCheck.satisfied).toBe(true)
    expect(ignCheck.evidence).toBe('ignStatus is true')
  })

  it('값을 파싱할 수 없는 로그(BCMCanManager)는 건너뛰고 더 과거의 값을 사용한다', () => {
    const tree = [TRIGGER_NODE]
    const systemLogs = [
      systemLog({ timestamp: '01-01 15:00:00.000', tag: 'GeneralSettings', rawMessage: 'ignStatus is true' }),
      systemLog({ timestamp: '01-01 15:10:00.000', tag: 'BCMCanManager', rawMessage: 'getIGNStatus()' }),
    ]

    const result = evaluate(tree, systemLogs)

    const ignCheck = result.checks.find((c) => c.id === 'ignOn')
    expect(ignCheck.satisfied).toBe(true)
    expect(ignCheck.evidence).toBe('ignStatus is true')
  })
})
