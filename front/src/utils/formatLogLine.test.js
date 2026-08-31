import { describe, it, expect } from 'vitest'
import { formatLogLine } from './formatLogLine.js'

describe('formatLogLine', () => {
  it('타임스탬프, 스레드ID, 로그레벨, 레이어, 원본 메시지를 로그 형식 문자열로 합친다', () => {
    const node = {
      timestamp: '01-01 15:23:17.910',
      threadId: 1542,
      logLevel: 'E',
      layer: 'JNI_LAYER',
      rawMessage: 'call_java_com_mnsoft_navi_NativeCall_getNaviAutoMoveTimer entered',
    }

    expect(formatLogLine(node)).toBe(
      '01-01 15:23:17.910  1542 E JNI_LAYER: call_java_com_mnsoft_navi_NativeCall_getNaviAutoMoveTimer entered',
    )
  })

  it('다른 레이어/레벨 값에도 같은 형식을 적용한다', () => {
    const node = {
      timestamp: '01-01 15:23:18.100',
      threadId: 750,
      logLevel: 'I',
      layer: 'CONTAINER',
      rawMessage: 'RouteEngine::init called',
    }

    expect(formatLogLine(node)).toBe('01-01 15:23:18.100  750 I CONTAINER: RouteEngine::init called')
  })
})
