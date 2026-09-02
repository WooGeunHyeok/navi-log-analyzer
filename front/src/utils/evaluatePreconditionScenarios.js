function flattenNodes(nodes) {
  return nodes.reduce((flat, node) => {
    flat.push(node)
    if (node.children && node.children.length > 0) {
      flat.push(...flattenNodes(node.children))
    }
    return flat
  }, [])
}

export function evaluatePreconditionScenarios(tree, systemLogs, scenarios) {
  const allNodes = flattenNodes(tree)

  return scenarios.map((scenario) => {
    const triggerNode = allNodes.find((node) => scenario.trigger(node)) ?? null

    if (!triggerNode) {
      return {
        id: scenario.id,
        label: scenario.label,
        applicable: false,
        triggerNode: null,
        checks: [],
      }
    }

    const checks = scenario.checks.map((check) => {
      const result = check.evaluate(triggerNode.timestamp, { allNodes, systemLogs })
      return {
        id: check.id,
        label: check.label,
        satisfied: result.satisfied,
        evidence: result.evidence,
      }
    })

    return {
      id: scenario.id,
      label: scenario.label,
      applicable: true,
      triggerNode,
      checks,
    }
  })
}
