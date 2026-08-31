import { LAYER_META } from '../constants/layers.js'
import { formatLogLine } from '../utils/formatLogLine.js'
import styles from './TreeNode.module.css'

function TreeNode({ node, collapsedIds, forceExpanded, onToggle }) {
  const hasChildren = node.children.length > 0
  const isExpanded = forceExpanded || !collapsedIds.has(node.id)
  const isUnmatched = node.matchType === 'NONE'
  const layerMeta = LAYER_META[node.layer]

  return (
    <li className={styles.node}>
      <div className={isUnmatched ? `${styles.row} ${styles.rowUnmatched}` : styles.row}>
        {hasChildren ? (
          <button
            type="button"
            className={styles.toggle}
            onClick={() => onToggle(node.id)}
            aria-label={isExpanded ? '접기' : '펼치기'}
          >
            {isExpanded ? '▼' : '▶'}
          </button>
        ) : (
          <span className={styles.toggleSpacer} />
        )}
        {layerMeta && (
          <span className={`${styles.badge} ${styles[layerMeta.badgeClass]}`}>{layerMeta.label}</span>
        )}
        <span className={isUnmatched ? styles.unmatched : styles.functionName}>
          {isUnmatched ? '소스 위치 매핑 안 됨' : node.functionName}
        </span>
        {node.fileName && (
          <span className={styles.location}>
            {node.fileName}
            {node.lineNumber != null ? `:${node.lineNumber}` : ''}
          </span>
        )}
      </div>
      <p className={isUnmatched ? `${styles.logLine} ${styles.logLineUnmatched}` : styles.logLine}>
        {formatLogLine(node)}
      </p>
      {hasChildren && isExpanded && (
        <ul className={styles.children}>
          {node.children.map((child) => (
            <TreeNode
              key={child.id}
              node={child}
              collapsedIds={collapsedIds}
              forceExpanded={forceExpanded}
              onToggle={onToggle}
            />
          ))}
        </ul>
      )}
    </li>
  )
}

export default TreeNode
