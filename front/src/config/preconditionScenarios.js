import { parseIgnValue } from './ignStatusParsers.js'

// 사전 조건 시나리오 테이블. 새 이슈 케이스의 사전 조건을 추가하려면 이 배열에
// { id, label, trigger, checks } 항목을 추가하면 된다.
// - trigger: 트리 노드 하나를 받아 "이 시나리오가 적용되는 기준 이벤트인지" 판단.
// - checks[].evaluate(triggerTimestamp, { allNodes, systemLogs }): 기준 이벤트
//   시점 이전의 로그를 근거로 그 조건이 충족됐는지 판단.

const DESTINATION_SET_MESSAGES = [
  'RP Normal Run!',
  'RP Offroute Run!',
  'RP TrafficDetour Run!',
  'RP DistanceDetour Run!',
  'RP PointDetoure Run!',
  'RP AreaDetour Run!',
  'RP RouteDetour Run!',
  'RP KSLinkDetour Run!',
  'rpSearchMozenGISP3WithThread run!',
]

function findLatestIgnValueBefore(referenceTimestamp, systemLogs) {
  for (let i = systemLogs.length - 1; i >= 0; i -= 1) {
    const log = systemLogs[i]
    if (log.timestamp >= referenceTimestamp) {
      continue
    }
    const value = parseIgnValue(log)
    if (value !== null) {
      return { value, log }
    }
  }
  return null
}

export const PRECONDITION_SCENARIOS = [
  {
    id: 'returnToMapAutoMove',
    label: 'ReturnToMap 자동 이동',
    trigger: (node) =>
      node.layer === 'CONTAINER' &&
      node.rawMessage.includes('GVwNaviSetting_ReturnToMap::OnProc') &&
      node.rawMessage.includes('setNaviAutoMoveTimer'),
    checks: [
      {
        id: 'ignOn',
        label: 'IGN On 상태',
        evaluate: (triggerTimestamp, { systemLogs }) => {
          const found = findLatestIgnValueBefore(triggerTimestamp, systemLogs)
          return {
            satisfied: found?.value === true,
            evidence: found ? found.log.rawMessage : null,
          }
        },
      },
      {
        id: 'destinationSet',
        label: '목적지 설정됨',
        evaluate: (triggerTimestamp, { allNodes }) => {
          const match = allNodes.find(
            (node) =>
              node.layer === 'CONTAINER' &&
              DESTINATION_SET_MESSAGES.includes(node.rawMessage) &&
              node.timestamp < triggerTimestamp,
          )
          return {
            satisfied: Boolean(match),
            evidence: match ? match.rawMessage : null,
          }
        },
      },
    ],
  },
]
