// IGN(점화) 상태를 로깅하는 태그별 파싱 규칙.
// 여러 안드로이드/벤더 컴포넌트가 같은 점화 상태 변경 브로드캐스트에 반응해 각자
// 다른 형식으로 로그를 남긴다 (소스코드로 매핑되는 대상이 아니라 텍스트로만 판별 가능).
// "onIgnitionChanged() X, mIgnStatus = Y" / "Cur[X],Prev[Y]" 형식은 로그를 찍은
// 시점에 필드가 아직 갱신되기 전이라 X(콜백 파라미터 = 새 값)를 사용한다.
const IGN_PARSERS = {
  GeneralSettings: [/ignStatus is (true|false)/, /onIgnitionChanged\(\)\s*(true|false)/],
  GeneralActivityCheckService: [/onIgnitionChanged isIGNStatus(true|false)/],
  USMActivityMonitorService: [/onIgnitionChanged isIGNStatus(true|false)/],
  LightService: [/onIgnitionChanged\(\)\s*(true|false)/],
  SeatWheelHeatService: [/onIgnitionChanged\(\)\s*(true|false)/],
  USBVideo: [/ign on\s*:\s*(true|false)/],
  OTAClient_OTAUpdateApp: [/Cur\[(true|false)\]/],
  DMClient_SWUpdateApp: [/(?<!prev)Ignition\[(true|false)\]/],
  SystemService: [{ regex: /ignStatus\s*=\s*\[(\d)\]/, toBool: (value) => value === '1' }],
  // BCMCanManager: getIGNStatus()는 조회 호출 로그일 뿐 값이 없어 의도적으로 매핑하지 않음.
  // 이런 태그/메시지는 parseIgnValue가 null을 반환하고, 호출부에서 더 과거 로그를 계속 탐색한다.
}

export function parseIgnValue(systemLog) {
  const patterns = IGN_PARSERS[systemLog.tag]
  if (!patterns) {
    return null
  }

  for (const pattern of patterns) {
    const regex = pattern.regex ?? pattern
    const toBool = pattern.toBool ?? ((value) => value === 'true')
    const match = systemLog.rawMessage.match(regex)
    if (match) {
      return toBool(match[1])
    }
  }

  return null
}
