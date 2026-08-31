export function formatLogLine(node) {
  return `${node.timestamp}  ${node.threadId} ${node.logLevel} ${node.layer}: ${node.rawMessage}`
}
