import { describe, it, expect } from 'vitest'
import { filterTree, collectExpandableIds } from './treeFilter.js'

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
      node({
        id: 2,
        functionName: null,
        fileName: null,
        matchType: 'NONE',
        depth: 1,
        children: [],
      }),
      node({
        id: 3,
        functionName: 'routeInit',
        fileName: 'RouteEngine.java',
        matchType: 'EXACT_KEY',
        depth: 1,
        children: [],
      }),
    ],
  }),
]

describe('filterTree', () => {
  it('검색어와 필터가 모두 비어있으면 원본 트리를 그대로 반환한다', () => {
    expect(filterTree(TREE, { searchTerm: '', unmatchedOnly: false })).toBe(TREE)
  })

  it('검색어에 매치되는 노드와 그 조상만 남긴다', () => {
    const result = filterTree(TREE, { searchTerm: 'routeInit', unmatchedOnly: false })

    expect(result).toHaveLength(1)
    expect(result[0].id).toBe(1) // 조상(root)은 유지
    expect(result[0].children).toHaveLength(1)
    expect(result[0].children[0].id).toBe(3) // 매치된 노드만 남고 형제(id=2)는 제거
  })

  it('파일명으로도 검색된다', () => {
    const result = filterTree(TREE, { searchTerm: 'RouteEngine', unmatchedOnly: false })

    expect(result[0].children.map((child) => child.id)).toEqual([3])
  })

  it('매핑 안 된 노드만 필터링하면 matchType=NONE 노드와 조상만 남긴다', () => {
    const result = filterTree(TREE, { searchTerm: '', unmatchedOnly: true })

    expect(result).toHaveLength(1)
    expect(result[0].children.map((child) => child.id)).toEqual([2])
  })

  it('검색어와 매핑 필터를 동시에 적용하면 둘 다 만족하는 노드만 남긴다', () => {
    const bothConditionsTree = [
      node({
        id: 1,
        functionName: 'onCreate',
        matchType: 'EXACT_KEY',
        children: [
          // 검색어(route)에 매치되고 matchType도 NONE이라 두 조건 모두 만족 -> 유지
          node({ id: 2, functionName: 'jni_route_init', matchType: 'NONE', depth: 1 }),
          // 검색어에는 매치되지만 matchType이 EXACT_KEY라 unmatchedOnly 조건을 만족하지 않음 -> 제거
          node({ id: 3, functionName: 'routeInit', matchType: 'EXACT_KEY', depth: 1 }),
        ],
      }),
    ]

    const result = filterTree(bothConditionsTree, { searchTerm: 'route', unmatchedOnly: true })

    expect(result).toHaveLength(1)
    expect(result[0].children.map((child) => child.id)).toEqual([2])
  })

  it('아무것도 매치되지 않으면 빈 배열을 반환한다', () => {
    const result = filterTree(TREE, { searchTerm: '존재하지-않는-이름', unmatchedOnly: false })

    expect(result).toEqual([])
  })
})

describe('collectExpandableIds', () => {
  it('자식이 있는 노드의 id만 모은다', () => {
    expect(collectExpandableIds(TREE)).toEqual([1])
  })

  it('자식이 없으면 빈 배열을 반환한다', () => {
    expect(collectExpandableIds(TREE[0].children)).toEqual([])
  })
})
