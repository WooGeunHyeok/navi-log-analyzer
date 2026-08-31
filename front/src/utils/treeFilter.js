// JNI_LAYER 함수 호출 바로 아래에 나오는 JAVA_LAYER 로그(리턴값 등)는 소스가 없어
// matchType이 NONE이지만, 그 JNI 호출과 곧바로 이어지는 정상적인 로그 흐름이다.
// 필터링 목적으로는 "매핑 안 됨"이 아니라 "매핑됨"의 일부로 취급한다.
function isUnmapped(node, parentLayer) {
  if (node.matchType !== 'NONE') {
    return false
  }
  if (node.layer === 'JAVA_LAYER' && parentLayer === 'JNI_LAYER') {
    return false
  }
  return true
}

export function filterTree(
  nodes,
  { searchTerm = '', unmatchedOnly = false, matchedOnly = false } = {},
  parentLayer = null,
) {
  const term = searchTerm.trim().toLowerCase()
  if (!term && !unmatchedOnly && !matchedOnly) {
    return nodes
  }

  return nodes.reduce((kept, node) => {
    const filteredChildren = filterTree(
      node.children ?? [],
      { searchTerm, unmatchedOnly, matchedOnly },
      node.layer,
    )

    const matchesSearch =
      !term ||
      (node.functionName ?? '').toLowerCase().includes(term) ||
      (node.fileName ?? '').toLowerCase().includes(term)
    const unmapped = isUnmapped(node, parentLayer)
    const matchesUnmatchedOnly = !unmatchedOnly || unmapped
    const matchesMatchedOnly = !matchedOnly || !unmapped
    const selfMatches = matchesSearch && matchesUnmatchedOnly && matchesMatchedOnly

    if (selfMatches || filteredChildren.length > 0) {
      kept.push({ ...node, children: filteredChildren })
    }
    return kept
  }, [])
}

export function collectExpandableIds(nodes) {
  return nodes.reduce((ids, node) => {
    if (node.children && node.children.length > 0) {
      ids.push(node.id, ...collectExpandableIds(node.children))
    }
    return ids
  }, [])
}
