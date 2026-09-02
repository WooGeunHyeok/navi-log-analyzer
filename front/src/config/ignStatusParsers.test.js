import { describe, it, expect } from 'vitest'
import { parseIgnValue } from './ignStatusParsers.js'

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

describe('parseIgnValue', () => {
  it('GeneralSettings의 "ignStatus is true" 형식을 인식한다', () => {
    expect(parseIgnValue(systemLog({ tag: 'GeneralSettings', rawMessage: 'ignStatus is true' }))).toBe(true)
  })

  it('GeneralSettings의 "onIgnitionChanged() X, mIgnStatus = Y" 형식은 첫 번째(콜백 파라미터) 값을 사용한다', () => {
    expect(
      parseIgnValue(
        systemLog({ tag: 'GeneralSettings', rawMessage: 'onIgnitionChanged() true, mIgnStatus = false' }),
      ),
    ).toBe(true)
  })

  it('GeneralActivityCheckService의 공백 없는 형식을 인식한다', () => {
    expect(
      parseIgnValue(
        systemLog({ tag: 'GeneralActivityCheckService', rawMessage: 'onIgnitionChanged isIGNStatusfalse' }),
      ),
    ).toBe(false)
  })

  it('USMActivityMonitorService도 같은 형식을 인식한다', () => {
    expect(
      parseIgnValue(
        systemLog({ tag: 'USMActivityMonitorService', rawMessage: 'onIgnitionChanged isIGNStatustrue' }),
      ),
    ).toBe(true)
  })

  it('LightService의 "onIgnitionChanged() X, mIgnStatus = Y" 형식도 첫 번째 값을 사용한다', () => {
    expect(
      parseIgnValue(
        systemLog({ tag: 'LightService', rawMessage: 'onIgnitionChanged() false, mIgnStatus = true' }),
      ),
    ).toBe(false)
  })

  it('SeatWheelHeatService도 같은 형식을 인식한다', () => {
    expect(
      parseIgnValue(
        systemLog({ tag: 'SeatWheelHeatService', rawMessage: 'onIgnitionChanged() false, mIgnStatus = true' }),
      ),
    ).toBe(false)
  })

  it('USBVideo의 "ign on : X, acc on : Y" 형식에서 ign on 값만 사용한다 (acc on 값과 헷갈리지 않음)', () => {
    expect(
      parseIgnValue(
        systemLog({ tag: 'USBVideo', rawMessage: '[E][onIgnitionChanged] ign on : false, acc on : true' }),
      ),
    ).toBe(false)
  })

  it('OTAClient_OTAUpdateApp의 "Cur[X],Prev[Y]" 형식에서 Cur 값을 사용한다', () => {
    expect(
      parseIgnValue(
        systemLog({
          tag: 'OTAClient_OTAUpdateApp',
          rawMessage: 'OTAUpdateApp.java(246)::onIgnitionChanged - Cur[false],Prev[true]',
        }),
      ),
    ).toBe(false)
  })

  it('DMClient_SWUpdateApp의 "Ignition[X], prevIgnition[Y]" 형식에서 Ignition 값만 사용한다 (prevIgnition과 헷갈리지 않음)', () => {
    expect(
      parseIgnValue(
        systemLog({
          tag: 'DMClient_SWUpdateApp',
          rawMessage: 'SWUpdateApp.java(214)::onIgnitionChanged - Ignition[false], prevIgnition[true]',
        }),
      ),
    ).toBe(false)
  })

  it('SystemService의 "ignStatus = [0|1]" 형식을 boolean으로 변환한다', () => {
    expect(
      parseIgnValue(systemLog({ tag: 'SystemService', rawMessage: '[onIGNStatus] ignStatus = [0]' })),
    ).toBe(false)
    expect(
      parseIgnValue(systemLog({ tag: 'SystemService', rawMessage: '[onIGNStatus] ignStatus = [1]' })),
    ).toBe(true)
  })

  it('값을 알 수 없는 태그/메시지는 null을 반환한다 (예: BCMCanManager의 조회 알림 로그)', () => {
    expect(parseIgnValue(systemLog({ tag: 'BCMCanManager', rawMessage: 'getIGNStatus()' }))).toBeNull()
  })

  it('등록되지 않은 태그는 null을 반환한다', () => {
    expect(parseIgnValue(systemLog({ tag: 'UnknownTag', rawMessage: 'ignStatus is true' }))).toBeNull()
  })
})
