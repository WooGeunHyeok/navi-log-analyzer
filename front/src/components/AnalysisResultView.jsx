import { useMemo, useState } from 'react'
import TreeNode from './TreeNode.jsx'
import { collectExpandableIds, filterTree } from '../utils/treeFilter.js'
import styles from './AnalysisResultView.module.css'

function AnalysisResultView({ treeData }) {
  const [searchTerm, setSearchTerm] = useState('')
  const [unmatchedOnly, setUnmatchedOnly] = useState(false)
  const [collapsedIds, setCollapsedIds] = useState(() => new Set())

  const isFiltering = searchTerm.trim() !== '' || unmatchedOnly

  const filteredTree = useMemo(
    () => filterTree(treeData, { searchTerm, unmatchedOnly }),
    [treeData, searchTerm, unmatchedOnly],
  )

  const expandableIds = useMemo(() => collectExpandableIds(treeData), [treeData])
  const allCollapsed = expandableIds.length > 0 && expandableIds.every((id) => collapsedIds.has(id))

  function handleToggle(id) {
    setCollapsedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  function handleToggleAll() {
    setCollapsedIds(allCollapsed ? new Set() : new Set(expandableIds))
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.controls}>
        <input
          type="text"
          className={styles.search}
          placeholder="함수명 또는 파일명 검색"
          aria-label="함수명 또는 파일명 검색"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />
        <label className={styles.checkboxLabel}>
          <input
            type="checkbox"
            checked={unmatchedOnly}
            onChange={(event) => setUnmatchedOnly(event.target.checked)}
          />
          매핑 안 된 노드만
        </label>
        <button type="button" className={styles.toggleAll} onClick={handleToggleAll}>
          {allCollapsed ? '전체 펼치기' : '전체 접기'}
        </button>
      </div>

      {filteredTree.length === 0 ? (
        <p className={styles.empty}>검색 결과가 없습니다.</p>
      ) : (
        <ul className={styles.tree}>
          {filteredTree.map((node) => (
            <TreeNode
              key={node.id}
              node={node}
              collapsedIds={collapsedIds}
              forceExpanded={isFiltering}
              onToggle={handleToggle}
            />
          ))}
        </ul>
      )}
    </div>
  )
}

export default AnalysisResultView
