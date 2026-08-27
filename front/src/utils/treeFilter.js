export function filterTree(nodes, { searchTerm = '', unmatchedOnly = false } = {}) {
  const term = searchTerm.trim().toLowerCase()
  if (!term && !unmatchedOnly) {
    return nodes
  }

  return nodes.reduce((kept, node) => {
    const filteredChildren = filterTree(node.children ?? [], { searchTerm, unmatchedOnly })

    const matchesSearch =
      !term ||
      (node.functionName ?? '').toLowerCase().includes(term) ||
      (node.fileName ?? '').toLowerCase().includes(term)
    const matchesUnmatchedOnly = !unmatchedOnly || node.matchType === 'NONE'
    const selfMatches = matchesSearch && matchesUnmatchedOnly

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
