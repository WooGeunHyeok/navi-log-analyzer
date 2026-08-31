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

  it('매핑된 노드만 필터링하면 matchType이 NONE이 아닌 노드와 조상만 남긴다', () => {
    const result = filterTree(TREE, { searchTerm: '', matchedOnly: true })

    expect(result).toHaveLength(1)
    expect(result[0].children.map((child) => child.id)).toEqual([3])
  })

  it('검색어와 매핑된 노드만 필터를 동시에 적용하면 둘 다 만족하는 노드만 남긴다', () => {
    const result = filterTree(TREE, { searchTerm: 'routeInit', matchedOnly: true })

    expect(result).toHaveLength(1)
    expect(result[0].children.map((child) => child.id)).toEqual([3])
  })

  it('matchedOnly와 unmatchedOnly를 동시에 켜면(비정상 상태) 서로 모순되어 아무 노드도 남지 않는다', () => {
    const result = filterTree(TREE, { unmatchedOnly: true, matchedOnly: true })

    expect(result).toEqual([])
  })

  it('JNI_LAYER 바로 아래 자식인 JAVA_LAYER 노드는 matchType=NONE이어도 "매핑된 노드만"에 포함된다', () => {
    // JNI 함수 호출(call_java_...)에 바로 이어지는 JAVA_LAYER 로그(리턴값 등)는
    // 소스가 없어 매핑 대상은 아니지만, JNI 호출과 이어지는 정상적인 로그 흐름이라
    // '매핑 안 됨'으로 취급하지 않는다.
    const jniWithJavaChild = [
      node({
        id: 112,
        layer: 'JNI_LAYER',
        functionName: 'call_java_com_mnsoft_navi_NativeCall_getNaviAutoMoveTimer',
        fileName: 'app-jni.cpp',
        matchType: 'EXACT_KEY',
        children: [
          node({
            id: 113,
            layer: 'JAVA_LAYER',
            rawMessage: '[1542][NativeCall] getNaviAutoMoveTimer = 0',
            matchType: 'NONE',
            depth: 1,
          }),
        ],
      }),
    ]

    const result = filterTree(jniWithJavaChild, { matchedOnly: true })

    expect(result).toHaveLength(1)
    expect(result[0].children.map((child) => child.id)).toEqual([113])
  })

  it('JNI_LAYER 바로 아래 자식인 JAVA_LAYER 노드는 "매핑 안 된 노드만"에는 포함되지 않는다', () => {
    const jniWithJavaChild = [
      node({
        id: 112,
        layer: 'JNI_LAYER',
        functionName: 'call_java_com_mnsoft_navi_NativeCall_getNaviAutoMoveTimer',
        fileName: 'app-jni.cpp',
        matchType: 'EXACT_KEY',
        children: [
          node({
            id: 113,
            layer: 'JAVA_LAYER',
            rawMessage: '[1542][NativeCall] getNaviAutoMoveTimer = 0',
            matchType: 'NONE',
            depth: 1,
          }),
        ],
      }),
    ]

    const result = filterTree(jniWithJavaChild, { unmatchedOnly: true })

    expect(result).toEqual([])
  })

  it('JNI_LAYER가 아닌 다른 레이어 아래의 JAVA_LAYER 노드는 여전히 일반적인 매핑 안 됨 노드로 취급한다', () => {
    const containerWithJavaChild = [
      node({
        id: 1,
        layer: 'CONTAINER',
        functionName: 'onCreate',
        fileName: 'NaviMain.java',
        matchType: 'EXACT_KEY',
        children: [
          node({
            id: 2,
            layer: 'JAVA_LAYER',
            rawMessage: 'stray java log',
            matchType: 'NONE',
            depth: 1,
          }),
        ],
      }),
    ]

    const matchedResult = filterTree(containerWithJavaChild, { matchedOnly: true })
    expect(matchedResult[0].children).toEqual([])

    const unmatchedResult = filterTree(containerWithJavaChild, { unmatchedOnly: true })
    expect(unmatchedResult[0].children.map((child) => child.id)).toEqual([2])
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
