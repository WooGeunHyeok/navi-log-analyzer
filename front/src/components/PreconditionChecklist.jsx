import { evaluatePreconditionScenarios } from '../utils/evaluatePreconditionScenarios.js'
import { PRECONDITION_SCENARIOS } from '../config/preconditionScenarios.js'
import styles from './PreconditionChecklist.module.css'

function PreconditionChecklist({ tree, systemLogs }) {
  const scenarioResults = evaluatePreconditionScenarios(tree, systemLogs, PRECONDITION_SCENARIOS)
  const applicableResults = scenarioResults.filter((result) => result.applicable)

  if (applicableResults.length === 0) {
    return null
  }

  return (
    <div className={styles.wrapper}>
      {applicableResults.map((result) => {
        const allSatisfied = result.checks.every((check) => check.satisfied)

        return (
          <div key={result.id} className={styles.scenario}>
            <h2 className={styles.title}>사전 조건 확인 — {result.label}</h2>
            <ul className={styles.list}>
              {result.checks.map((check) => (
                <li key={check.id} className={styles.item}>
                  <span aria-hidden="true">{check.satisfied ? '✅' : '❌'}</span>
                  <span className={styles.label}>{check.label}</span>
                  <span className={check.satisfied ? styles.statusPass : styles.statusFail}>
                    {check.satisfied ? '충족' : '미충족'}
                  </span>
                  {check.evidence && <span className={styles.evidence}>{check.evidence}</span>}
                </li>
              ))}
            </ul>
            {!allSatisfied && <p className={styles.warning}>⚠️ 사전 조건 중 일부가 확인되지 않았습니다.</p>}
          </div>
        )
      })}
    </div>
  )
}

export default PreconditionChecklist
