import { useMemo, useState } from 'react'
import TreeNode from './TreeNode.jsx'
import PreconditionChecklist from './PreconditionChecklist.jsx'
import { collectExpandableIds, filterTree } from '../utils/treeFilter.js'
import styles from './AnalysisResultView.module.css'

const MATCH_FILTER_OPTIONS = [
  { value: 'matched', label: '매핑된 노드만' },
  { value: 'unmatched', label: '매핑 안 된 노드만' },
  { value: 'all', label: '전체' },
]

function AnalysisResultView({ tree, systemLogs }) {
  const [searchTerm, setSearchTerm] = useState('')
  const [matchFilter, setMatchFilter] = useState('matched')
  const [collapsedIds, setCollapsedIds] = useState(() => new Set())

  // 검색어가 있을 때만 매치된 노드로 가는 조상 경로를 강제로 펼친다.
  // "매핑된/매핑 안 된 노드만" 필터는 상시 켜둔 채로 개별 노드를 접고 펼 수 있어야 하므로
  // 이 필터들은 강제 펼침 조건에 포함하지 않는다.
  const isSearching = searchTerm.trim() !== ''

  const filteredTree = useMemo(
    () =>
      filterTree(tree, {
        searchTerm,
        matchedOnly: matchFilter === 'matched',
        unmatchedOnly: matchFilter === 'unmatched',
      }),
    [tree, searchTerm, matchFilter],
  )

  const expandableIds = useMemo(() => collectExpandableIds(filteredTree), [filteredTree])
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
      <PreconditionChecklist tree={tree} systemLogs={systemLogs} />
      <div className={styles.controls}>
        <input
          type="text"
          className={styles.search}
          placeholder="함수명 또는 파일명 검색"
          aria-label="함수명 또는 파일명 검색"
          value={searchTerm}
          onChange={(event) => setSearchTerm(event.target.value)}
        />
        <div className={styles.matchFilterGroup} role="radiogroup" aria-label="매핑 상태 필터">
          {MATCH_FILTER_OPTIONS.map((option) => (
            <label key={option.value} className={styles.filterOption}>
              <input
                type="radio"
                name="matchFilter"
                checked={matchFilter === option.value}
                onChange={() => setMatchFilter(option.value)}
              />
              {option.label}
            </label>
          ))}
        </div>
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
              forceExpanded={isSearching}
              onToggle={handleToggle}
            />
          ))}
        </ul>
      )}
    </div>
  )
}

export default AnalysisResultView
