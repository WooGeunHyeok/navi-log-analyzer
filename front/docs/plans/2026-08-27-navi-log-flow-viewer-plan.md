# 내비 로그 소스코드 흐름도 뷰어 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 내비게이션 단말기 로그 파일을 업로드하면 업로드→파싱→분석 API를 순차 호출하고, 결과로 받은 함수 호출 트리를 아코디언 형태로 렌더링하는 단일 플로우 React 화면을 만든다.

**Architecture:** `App.jsx`가 `useReducer` 기반 상태 머신으로 `upload → processing → result` (또는 `error`) 4개 화면을 전환한다. 각 화면은 `src/components/`의 독립 컴포넌트이며, 트리 렌더링은 `TreeNode.jsx` 재귀 컴포넌트 + `src/utils/treeFilter.js` 순수 함수(검색/필터)로 분리한다. API 호출은 `src/api/logAnalyzer.js` 하나에 모은다.

**Tech Stack:** Vite + React 19 (JSX), 순수 CSS Modules, 네이티브 `fetch`. 테스트: Vitest + @testing-library/react + jsdom (이번 계획에서 새로 도입).

**설계 문서:** [front/docs/navi-log-flow-viewer-design.md](../navi-log-flow-viewer-design.md)

**작업 디렉터리:** 아래 모든 명령어는 `front/` 디렉터리에서 실행한다고 가정한다. 커밋 시 파일 경로도 `front/` 기준 상대경로로 표기했다 (`git add` 시 리포지토리 루트에서 실행한다면 `front/` 접두사를 붙일 것).

---

## Task 1: 테스트 환경 구축 (Vitest + Testing Library)

**Files:**
- Modify: `package.json`
- Modify: `vite.config.js`
- Create: `src/test/setup.js`

- [ ] **Step 1: 테스트 관련 패키지 설치**

Run:
```bash
npm install -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event
```

- [ ] **Step 2: `package.json`에 `test` 스크립트 추가**

`scripts` 블록을 아래와 같이 수정한다 (기존 `dev`/`build`/`lint`/`preview`는 유지하고 `test`만 추가):

```json
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "lint": "oxlint",
    "preview": "vite preview",
    "test": "vitest run"
  }
}
```

- [ ] **Step 3: `vite.config.js`에 Vitest 설정 추가**

`vite.config.js` 전체를 아래 내용으로 교체한다 (기존 `defineConfig` import 출처를 `vite`에서 `vitest/config`로 바꿔야 `test` 옵션 타입이 인식된다):

```js
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.js',
  },
})
```

- [ ] **Step 4: 테스트 셋업 파일 작성**

Create `src/test/setup.js`:

```js
import '@testing-library/jest-dom/vitest'
```

- [ ] **Step 5: 설정이 정상적으로 로드되는지 확인**

Run: `npx vitest run`
Expected: 에러 없이 실행되고 "No test files found" (또는 이에 준하는 메시지)로 종료됨. jsdom/설정 관련 에러가 나면 Step 3~4를 다시 확인한다.

- [ ] **Step 6: 커밋**

```bash
git add package.json package-lock.json vite.config.js src/test/setup.js
git commit -m "[FRONT] 테스트 환경 구축 (Vitest + Testing Library)"
```

---

## Task 2: API 클라이언트 — `src/api/logAnalyzer.js`

**Files:**
- Create: `src/api/logAnalyzer.js`
- Test: `src/api/logAnalyzer.test.js`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `src/api/logAnalyzer.test.js`:

```js
import { describe, it, expect, vi, afterEach } from 'vitest'
import { uploadLogFile, runParsing, runAnalysisFlow, MAX_FILE_SIZE_BYTES } from './logAnalyzer.js'

function mockFetchOnce(body, { ok = true, status = 200 } = {}) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok,
    status,
    json: async () => body,
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('MAX_FILE_SIZE_BYTES', () => {
  it('50MB이다', () => {
    expect(MAX_FILE_SIZE_BYTES).toBe(50 * 1024 * 1024)
  })
})

describe('uploadLogFile', () => {
  it('multipart로 업로드하고 fileId를 반환한다', async () => {
    const fetchMock = mockFetchOnce({ success: true, message: 'ok', data: 42 })
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })

    const fileId = await uploadLogFile({ title: '이슈 로그', jiraTicketKey: 'NAVI-1', file })

    expect(fileId).toBe(42)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/api/v1/logs/upload')
    expect(options.method).toBe('POST')
    expect(options.body).toBeInstanceOf(FormData)
    expect(options.body.get('file')).toBe(file)
  })

  it('success가 false면 서버 메시지로 에러를 던진다', async () => {
    mockFetchOnce({ success: false, message: '제목은 필수입니다.', data: null })
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })

    await expect(uploadLogFile({ title: '', file })).rejects.toThrow('제목은 필수입니다.')
  })
})

describe('runParsing', () => {
  it('fileId 경로로 POST 요청하고 결과를 반환한다', async () => {
    const fetchMock = mockFetchOnce({ success: true, message: 'ok', data: 42 })

    const result = await runParsing(42)

    expect(result).toBe(42)
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/api/v1/parsing/42')
    expect(options.method).toBe('POST')
  })
})

describe('runAnalysisFlow', () => {
  it('fileId 경로로 GET 요청하고 트리 배열을 반환한다', async () => {
    const tree = [{ id: 1, functionName: 'onCreate', children: [] }]
    const fetchMock = mockFetchOnce({ success: true, message: 'ok', data: tree })

    const result = await runAnalysisFlow(42)

    expect(result).toEqual(tree)
    const [url] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8081/api/v1/logs/analysis/flow/42')
  })

  it('HTTP 응답이 실패(ok=false)이고 메시지가 없으면 상태 코드를 포함한 에러를 던진다', async () => {
    mockFetchOnce({ success: false, message: null, data: null }, { ok: false, status: 500 })

    await expect(runAnalysisFlow(42)).rejects.toThrow('500')
  })
})
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/api/logAnalyzer.test.js`
Expected: FAIL — `src/api/logAnalyzer.js`가 없어서 import 에러 발생.

- [ ] **Step 3: 최소 구현 작성**

Create `src/api/logAnalyzer.js`:

```js
const BASE_URL = 'http://localhost:8081'

export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024

async function parseApiResponse(response) {
  const body = await response.json()
  if (!response.ok || !body.success) {
    throw new Error(body.message || `요청이 실패했습니다. (status: ${response.status})`)
  }
  return body.data
}

export async function uploadLogFile({ title, jiraTicketKey, file }) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append(
    'request',
    new Blob([JSON.stringify({ title, jiraTicketKey })], { type: 'application/json' }),
  )

  const response = await fetch(`${BASE_URL}/api/v1/logs/upload`, {
    method: 'POST',
    body: formData,
  })
  return parseApiResponse(response)
}

export async function runParsing(fileId) {
  const response = await fetch(`${BASE_URL}/api/v1/parsing/${fileId}`, {
    method: 'POST',
  })
  return parseApiResponse(response)
}

export async function runAnalysisFlow(fileId) {
  const response = await fetch(`${BASE_URL}/api/v1/logs/analysis/flow/${fileId}`)
  return parseApiResponse(response)
}
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/api/logAnalyzer.test.js`
Expected: PASS (6 tests)

- [ ] **Step 5: 커밋**

```bash
git add src/api/logAnalyzer.js src/api/logAnalyzer.test.js
git commit -m "[FRONT] 로그 분석 API 클라이언트 추가"
```

---

## Task 3: 트리 검색/필터 유틸 — `src/utils/treeFilter.js`

**Files:**
- Create: `src/utils/treeFilter.js`
- Test: `src/utils/treeFilter.test.js`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `src/utils/treeFilter.test.js`:

```js
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
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/utils/treeFilter.test.js`
Expected: FAIL — `src/utils/treeFilter.js`가 없어서 import 에러 발생.

- [ ] **Step 3: 최소 구현 작성**

Create `src/utils/treeFilter.js`:

```js
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
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/utils/treeFilter.test.js`
Expected: PASS (8 tests)

- [ ] **Step 5: 커밋**

```bash
git add src/utils/treeFilter.js src/utils/treeFilter.test.js
git commit -m "[FRONT] 트리 검색/필터 유틸 추가"
```

---

## Task 4: 레이어 상수 + `TreeNode` 컴포넌트

**Files:**
- Create: `src/constants/layers.js`
- Create: `src/components/TreeNode.jsx`
- Create: `src/components/TreeNode.module.css`
- Test: `src/components/TreeNode.test.jsx`

- [ ] **Step 1: 레이어 메타데이터 상수 작성**

Create `src/constants/layers.js`:

```js
export const LAYER_META = {
  CONTAINER: { label: 'CONTAINER', badgeClass: 'badgeContainer' },
  JNI_LAYER: { label: 'JNI', badgeClass: 'badgeJni' },
  JAVA_LAYER: { label: 'JAVA', badgeClass: 'badgeJava' },
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

Create `src/components/TreeNode.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import TreeNode from './TreeNode.jsx'

function buildNode(overrides) {
  return {
    id: 1,
    fileId: 1,
    step: 1,
    timestamp: '01-01 00:00:00.000',
    threadId: 1,
    logLevel: 'I',
    layer: 'CONTAINER',
    rawMessage: 'raw',
    fileName: 'NaviMain.java',
    filePath: '/NaviMain.java',
    functionName: 'onCreate',
    lineNumber: 10,
    matchType: 'EXACT_KEY',
    depth: 0,
    children: [],
    ...overrides,
  }
}

describe('TreeNode', () => {
  it('함수명과 레이어 뱃지를 보여준다', () => {
    render(
      <TreeNode node={buildNode({})} collapsedIds={new Set()} forceExpanded={false} onToggle={() => {}} />,
    )

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('CONTAINER')).toBeInTheDocument()
    expect(screen.getByText('NaviMain.java:10')).toBeInTheDocument()
  })

  it('matchType이 NONE이면 매핑 안 됨 텍스트를 보여준다', () => {
    render(
      <TreeNode
        node={buildNode({ matchType: 'NONE', functionName: null, fileName: null, lineNumber: null })}
        collapsedIds={new Set()}
        forceExpanded={false}
        onToggle={() => {}}
      />,
    )

    expect(screen.getByText('소스 위치 매핑 안 됨')).toBeInTheDocument()
  })

  it('자식이 있으면 토글 버튼을 클릭해 접고 펼 수 있다', async () => {
    const user = userEvent.setup()
    const onToggle = vi.fn()
    const child = buildNode({ id: 2, functionName: 'childFn' })
    const parent = buildNode({ id: 1, functionName: 'parentFn', children: [child] })

    render(
      <TreeNode node={parent} collapsedIds={new Set()} forceExpanded={false} onToggle={onToggle} />,
    )

    expect(screen.getByText('childFn')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '접기' }))

    expect(onToggle).toHaveBeenCalledWith(1)
  })

  it('collapsedIds에 포함돼 있어도 forceExpanded가 true이면 자식을 보여준다', () => {
    const child = buildNode({ id: 2, functionName: 'childFn' })
    const parent = buildNode({ id: 1, functionName: 'parentFn', children: [child] })

    render(
      <TreeNode
        node={parent}
        collapsedIds={new Set([1])}
        forceExpanded
        onToggle={() => {}}
      />,
    )

    expect(screen.getByText('childFn')).toBeInTheDocument()
  })

  it('collapsedIds에 포함되고 forceExpanded가 false이면 자식을 숨긴다', () => {
    const child = buildNode({ id: 2, functionName: 'childFn' })
    const parent = buildNode({ id: 1, functionName: 'parentFn', children: [child] })

    render(
      <TreeNode
        node={parent}
        collapsedIds={new Set([1])}
        forceExpanded={false}
        onToggle={() => {}}
      />,
    )

    expect(screen.queryByText('childFn')).not.toBeInTheDocument()
  })
})
```

- [ ] **Step 3: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/components/TreeNode.test.jsx`
Expected: FAIL — `src/components/TreeNode.jsx`가 없어서 import 에러 발생.

- [ ] **Step 4: 최소 구현 작성**

Create `src/components/TreeNode.module.css`:

```css
.node {
  list-style: none;
}

.row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  font-family: var(--mono);
  font-size: 14px;
}

.toggle {
  width: 18px;
  height: 18px;
  border: none;
  background: transparent;
  color: var(--text);
  cursor: pointer;
  padding: 0;
  font-size: 11px;
}

.toggleSpacer {
  display: inline-block;
  width: 18px;
}

.badge {
  border-radius: 4px;
  padding: 1px 6px;
  font-size: 11px;
  color: #fff;
  flex-shrink: 0;
}

.badgeContainer {
  background: #6366f1;
}

.badgeJni {
  background: #f59e0b;
}

.badgeJava {
  background: #10b981;
}

.functionName {
  color: var(--text-h);
  font-weight: 600;
}

.unmatched {
  color: var(--text);
  font-style: italic;
}

.location {
  color: var(--text);
  opacity: 0.7;
  font-size: 12px;
}

.children {
  margin: 0;
  padding-left: 22px;
}
```

Create `src/components/TreeNode.jsx`:

```jsx
import { LAYER_META } from '../constants/layers.js'
import styles from './TreeNode.module.css'

function TreeNode({ node, collapsedIds, forceExpanded, onToggle }) {
  const hasChildren = node.children.length > 0
  const isExpanded = forceExpanded || !collapsedIds.has(node.id)
  const isUnmatched = node.matchType === 'NONE'
  const layerMeta = LAYER_META[node.layer]

  return (
    <li className={styles.node}>
      <div className={styles.row}>
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
```

- [ ] **Step 5: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/components/TreeNode.test.jsx`
Expected: PASS (5 tests)

- [ ] **Step 6: 커밋**

```bash
git add src/constants/layers.js src/components/TreeNode.jsx src/components/TreeNode.module.css src/components/TreeNode.test.jsx
git commit -m "[FRONT] 레이어 뱃지 + TreeNode 재귀 컴포넌트 추가"
```

---

## Task 5: `AnalysisResultView` 컴포넌트 (검색 / 필터 / 전체 펼치기·접기)

**Files:**
- Create: `src/components/AnalysisResultView.jsx`
- Create: `src/components/AnalysisResultView.module.css`
- Test: `src/components/AnalysisResultView.test.jsx`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `src/components/AnalysisResultView.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'
import AnalysisResultView from './AnalysisResultView.jsx'

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
      node({ id: 2, functionName: null, fileName: null, matchType: 'NONE', depth: 1 }),
      node({
        id: 3,
        functionName: 'routeInit',
        fileName: 'RouteEngine.java',
        matchType: 'EXACT_KEY',
        depth: 1,
      }),
    ],
  }),
]

describe('AnalysisResultView', () => {
  it('기본적으로 트리가 전체 펼쳐진 상태로 렌더링된다', () => {
    render(<AnalysisResultView treeData={TREE} />)

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('routeInit')).toBeInTheDocument()
    expect(screen.getByText('소스 위치 매핑 안 됨')).toBeInTheDocument()
  })

  it('검색어를 입력하면 매치되지 않는 형제는 숨기고 조상은 유지한다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.type(screen.getByLabelText('함수명 또는 파일명 검색'), 'routeInit')

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('routeInit')).toBeInTheDocument()
    expect(screen.queryByText('소스 위치 매핑 안 됨')).not.toBeInTheDocument()
  })

  it('"매핑 안 된 노드만" 체크 시 matchType=NONE 노드만 남긴다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.click(screen.getByLabelText('매핑 안 된 노드만'))

    expect(screen.getByText('onCreate')).toBeInTheDocument()
    expect(screen.getByText('소스 위치 매핑 안 됨')).toBeInTheDocument()
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()
  })

  it('검색 결과가 없으면 안내 문구를 보여준다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.type(screen.getByLabelText('함수명 또는 파일명 검색'), '존재하지-않음')

    expect(screen.getByText('검색 결과가 없습니다.')).toBeInTheDocument()
  })

  it('전체 접기/펼치기 버튼으로 모든 노드를 토글한다', async () => {
    const user = userEvent.setup()
    render(<AnalysisResultView treeData={TREE} />)

    await user.click(screen.getByRole('button', { name: '전체 접기' }))
    expect(screen.queryByText('routeInit')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '전체 펼치기' }))
    expect(screen.getByText('routeInit')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/components/AnalysisResultView.test.jsx`
Expected: FAIL — `src/components/AnalysisResultView.jsx`가 없어서 import 에러 발생.

- [ ] **Step 3: 최소 구현 작성**

Create `src/components/AnalysisResultView.module.css`:

```css
.wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search {
  flex: 1 1 240px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font: inherit;
}

.checkboxLabel {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  white-space: nowrap;
}

.toggleAll {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: transparent;
  color: var(--text-h);
  cursor: pointer;
  white-space: nowrap;
}

.empty {
  color: var(--text);
  opacity: 0.7;
}

.tree {
  margin: 0;
  padding: 0;
}
```

Create `src/components/AnalysisResultView.jsx`:

```jsx
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
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/components/AnalysisResultView.test.jsx`
Expected: PASS (5 tests)

- [ ] **Step 5: 커밋**

```bash
git add src/components/AnalysisResultView.jsx src/components/AnalysisResultView.module.css src/components/AnalysisResultView.test.jsx
git commit -m "[FRONT] 트리 결과 화면(검색/필터/전체 펼치기) 추가"
```

---

## Task 6: `UploadForm` 컴포넌트

**Files:**
- Create: `src/components/UploadForm.jsx`
- Create: `src/components/UploadForm.module.css`
- Test: `src/components/UploadForm.test.jsx`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `src/components/UploadForm.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import UploadForm from './UploadForm.jsx'

describe('UploadForm', () => {
  it('제목이 비어있으면 에러를 보여주고 onSubmit을 호출하지 않는다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    const file = new File(['content'], 'issue.log', { type: 'text/plain' })
    await user.upload(screen.getByLabelText('로그 파일'), file)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(screen.getByText('제목을 입력해주세요.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('파일을 선택하지 않으면 에러를 보여준다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '이슈 로그')
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(screen.getByText('업로드할 로그 파일을 선택해주세요.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('50MB를 초과하는 파일은 에러를 보여준다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '이슈 로그')
    const bigFile = new File(['content'], 'big.log', { type: 'text/plain' })
    Object.defineProperty(bigFile, 'size', { value: 51 * 1024 * 1024 })
    await user.upload(screen.getByLabelText('로그 파일'), bigFile)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(screen.getByText('파일 크기는 50MB를 초과할 수 없습니다.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('유효한 입력이면 onSubmit을 trim된 값으로 호출한다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '  이슈 로그  ')
    await user.type(screen.getByLabelText('Jira 티켓 키 (선택)'), 'NAVI-123')
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })
    await user.upload(screen.getByLabelText('로그 파일'), file)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(onSubmit).toHaveBeenCalledWith({
      title: '이슈 로그',
      jiraTicketKey: 'NAVI-123',
      file,
    })
  })

  it('jiraTicketKey를 비워두면 undefined로 전달한다', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UploadForm onSubmit={onSubmit} />)

    await user.type(screen.getByLabelText('제목'), '이슈 로그')
    const file = new File(['content'], 'issue.log', { type: 'text/plain' })
    await user.upload(screen.getByLabelText('로그 파일'), file)
    await user.click(screen.getByRole('button', { name: '분석 시작' }))

    expect(onSubmit).toHaveBeenCalledWith({ title: '이슈 로그', jiraTicketKey: undefined, file })
  })
})
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/components/UploadForm.test.jsx`
Expected: FAIL — `src/components/UploadForm.jsx`가 없어서 import 에러 발생.

- [ ] **Step 3: 최소 구현 작성**

Create `src/components/UploadForm.module.css`:

```css
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 480px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 14px;
  color: var(--text-h);
}

.field input {
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font: inherit;
}

.error {
  margin: 0;
  color: var(--danger);
  font-size: 14px;
}

.submit {
  align-self: flex-start;
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
```

Create `src/components/UploadForm.jsx`:

```jsx
import { useState } from 'react'
import { MAX_FILE_SIZE_BYTES } from '../api/logAnalyzer.js'
import styles from './UploadForm.module.css'

function UploadForm({ onSubmit }) {
  const [title, setTitle] = useState('')
  const [jiraTicketKey, setJiraTicketKey] = useState('')
  const [file, setFile] = useState(null)
  const [validationError, setValidationError] = useState(null)

  function handleSubmit(event) {
    event.preventDefault()

    if (title.trim() === '') {
      setValidationError('제목을 입력해주세요.')
      return
    }
    if (!file) {
      setValidationError('업로드할 로그 파일을 선택해주세요.')
      return
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setValidationError('파일 크기는 50MB를 초과할 수 없습니다.')
      return
    }

    setValidationError(null)
    onSubmit({
      title: title.trim(),
      jiraTicketKey: jiraTicketKey.trim() || undefined,
      file,
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.field}>
        <label htmlFor="title">제목</label>
        <input id="title" type="text" value={title} onChange={(event) => setTitle(event.target.value)} />
      </div>

      <div className={styles.field}>
        <label htmlFor="jiraTicketKey">Jira 티켓 키 (선택)</label>
        <input
          id="jiraTicketKey"
          type="text"
          value={jiraTicketKey}
          onChange={(event) => setJiraTicketKey(event.target.value)}
        />
      </div>

      <div className={styles.field}>
        <label htmlFor="file">로그 파일</label>
        <input id="file" type="file" onChange={(event) => setFile(event.target.files[0] ?? null)} />
      </div>

      {validationError && <p className={styles.error}>{validationError}</p>}

      <button type="submit" className={styles.submit}>
        분석 시작
      </button>
    </form>
  )
}

export default UploadForm
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/components/UploadForm.test.jsx`
Expected: PASS (5 tests)

- [ ] **Step 5: 커밋**

```bash
git add src/components/UploadForm.jsx src/components/UploadForm.module.css src/components/UploadForm.test.jsx
git commit -m "[FRONT] 업로드 폼 컴포넌트 추가"
```

---

## Task 7: `ProcessingStatus` 컴포넌트

**Files:**
- Create: `src/components/ProcessingStatus.jsx`
- Create: `src/components/ProcessingStatus.module.css`
- Test: `src/components/ProcessingStatus.test.jsx`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `src/components/ProcessingStatus.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import ProcessingStatus from './ProcessingStatus.jsx'

describe('ProcessingStatus', () => {
  it.each([
    ['uploading', '업로드 중...'],
    ['parsing', '파싱 중...'],
    ['analyzing', '분석 중...'],
  ])('subStep=%s 이면 "%s"를 보여준다', (subStep, expectedLabel) => {
    render(<ProcessingStatus subStep={subStep} />)
    expect(screen.getByText(expectedLabel)).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/components/ProcessingStatus.test.jsx`
Expected: FAIL — `src/components/ProcessingStatus.jsx`가 없어서 import 에러 발생.

- [ ] **Step 3: 최소 구현 작성**

Create `src/components/ProcessingStatus.module.css`:

```css
.wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.label {
  color: var(--text-h);
  font-size: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
```

Create `src/components/ProcessingStatus.jsx`:

```jsx
import styles from './ProcessingStatus.module.css'

const SUB_STEP_LABELS = {
  uploading: '업로드 중...',
  parsing: '파싱 중...',
  analyzing: '분석 중...',
}

function ProcessingStatus({ subStep }) {
  return (
    <div className={styles.wrapper}>
      <div className={styles.spinner} aria-hidden="true" />
      <p className={styles.label}>{SUB_STEP_LABELS[subStep] ?? '처리 중...'}</p>
    </div>
  )
}

export default ProcessingStatus
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/components/ProcessingStatus.test.jsx`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add src/components/ProcessingStatus.jsx src/components/ProcessingStatus.module.css src/components/ProcessingStatus.test.jsx
git commit -m "[FRONT] 처리 중 화면(ProcessingStatus) 추가"
```

---

## Task 8: `ErrorBanner` 컴포넌트

**Files:**
- Create: `src/components/ErrorBanner.jsx`
- Create: `src/components/ErrorBanner.module.css`
- Test: `src/components/ErrorBanner.test.jsx`

- [ ] **Step 1: 실패하는 테스트 작성**

Create `src/components/ErrorBanner.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import ErrorBanner from './ErrorBanner.jsx'

describe('ErrorBanner', () => {
  it('에러 메시지를 보여준다', () => {
    render(<ErrorBanner message="분석에 실패했습니다." onRetry={() => {}} />)
    expect(screen.getByText('분석에 실패했습니다.')).toBeInTheDocument()
  })

  it('버튼을 클릭하면 onRetry가 호출된다', async () => {
    const onRetry = vi.fn()
    const user = userEvent.setup()
    render(<ErrorBanner message="분석에 실패했습니다." onRetry={onRetry} />)

    await user.click(screen.getByRole('button', { name: '처음부터 다시 시도' }))

    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
```

- [ ] **Step 2: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/components/ErrorBanner.test.jsx`
Expected: FAIL — `src/components/ErrorBanner.jsx`가 없어서 import 에러 발생.

- [ ] **Step 3: 최소 구현 작성**

Create `src/components/ErrorBanner.module.css`:

```css
.banner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 48px 24px;
  border: 1px solid var(--danger);
  background: var(--danger-bg);
  border-radius: 8px;
  text-align: center;
}

.message {
  margin: 0;
  color: var(--text-h);
}

.retry {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
```

Create `src/components/ErrorBanner.jsx`:

```jsx
import styles from './ErrorBanner.module.css'

function ErrorBanner({ message, onRetry }) {
  return (
    <div className={styles.banner} role="alert">
      <p className={styles.message}>{message}</p>
      <button type="button" className={styles.retry} onClick={onRetry}>
        처음부터 다시 시도
      </button>
    </div>
  )
}

export default ErrorBanner
```

- [ ] **Step 4: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/components/ErrorBanner.test.jsx`
Expected: PASS (2 tests)

- [ ] **Step 5: 커밋**

```bash
git add src/components/ErrorBanner.jsx src/components/ErrorBanner.module.css src/components/ErrorBanner.test.jsx
git commit -m "[FRONT] 에러 배너 컴포넌트 추가"
```

---

## Task 9: `App.jsx` 상태 머신 통합 + 템플릿 데모 코드 정리

**Files:**
- Modify: `src/App.jsx`
- Create: `src/App.module.css`
- Modify: `src/index.css`
- Delete: `src/App.css`
- Delete: `src/assets/hero.png`
- Delete: `src/assets/react.svg`
- Delete: `src/assets/vite.svg`
- Test: `src/App.test.jsx`

- [ ] **Step 1: 기본 Vite 템플릿 데모 자산 삭제**

```bash
git rm src/App.css src/assets/hero.png src/assets/react.svg src/assets/vite.svg
```

- [ ] **Step 2: `index.css`를 마케팅 데모 레이아웃에서 도구용 기본 스타일로 교체**

`src/index.css` 전체를 아래로 교체한다 (색상 변수/다크모드는 유지하고, `#root` 중앙 정렬 레이아웃과 데모 전용 규칙(`h1`, `code`, `.counter` 등)은 제거):

```css
:root {
  --text: #4b5563;
  --text-h: #08060d;
  --bg: #fff;
  --border: #e5e4e7;
  --accent: #6366f1;
  --accent-bg: rgba(99, 102, 241, 0.1);
  --danger: #dc2626;
  --danger-bg: rgba(220, 38, 38, 0.08);

  --sans: system-ui, 'Segoe UI', Roboto, sans-serif;
  --mono: ui-monospace, Consolas, monospace;

  font: 16px/1.5 var(--sans);
  color-scheme: light dark;
  color: var(--text);
  background: var(--bg);
  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

@media (prefers-color-scheme: dark) {
  :root {
    --text: #9ca3af;
    --text-h: #f3f4f6;
    --bg: #16171d;
    --border: #2e303a;
    --accent: #818cf8;
    --accent-bg: rgba(129, 140, 248, 0.15);
    --danger: #f87171;
    --danger-bg: rgba(248, 113, 113, 0.12);
  }
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
}
```

- [ ] **Step 3: `App.jsx` 통합 테스트 작성 (실패하는 테스트)**

Create `src/App.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import App from './App.jsx'
import * as api from './api/logAnalyzer.js'

vi.mock('./api/logAnalyzer.js')

function deferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

async function fillAndSubmitUploadForm(user) {
  await user.type(screen.getByLabelText('제목'), '이슈 로그')
  const file = new File(['log content'], 'issue.log', { type: 'text/plain' })
  await user.upload(screen.getByLabelText('로그 파일'), file)
  await user.click(screen.getByRole('button', { name: '분석 시작' }))
}

describe('App', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('업로드 -> 파싱 -> 분석 순서로 화면이 전환되고 트리가 렌더링된다', async () => {
    const uploadDeferred = deferred()
    const parseDeferred = deferred()
    const analyzeDeferred = deferred()
    api.uploadLogFile.mockReturnValue(uploadDeferred.promise)
    api.runParsing.mockReturnValue(parseDeferred.promise)
    api.runAnalysisFlow.mockReturnValue(analyzeDeferred.promise)

    const user = userEvent.setup()
    render(<App />)

    await fillAndSubmitUploadForm(user)

    expect(await screen.findByText('업로드 중...')).toBeInTheDocument()
    uploadDeferred.resolve(1)

    expect(await screen.findByText('파싱 중...')).toBeInTheDocument()
    parseDeferred.resolve(1)

    expect(await screen.findByText('분석 중...')).toBeInTheDocument()
    analyzeDeferred.resolve([
      {
        id: 1,
        fileId: 1,
        step: 1,
        timestamp: '01-01 00:00:00.000',
        threadId: 1,
        logLevel: 'I',
        layer: 'CONTAINER',
        rawMessage: 'raw',
        fileName: 'NaviMain.java',
        filePath: '/NaviMain.java',
        functionName: 'onCreate',
        lineNumber: 10,
        matchType: 'EXACT_KEY',
        depth: 0,
        children: [],
      },
    ])

    expect(await screen.findByText('onCreate')).toBeInTheDocument()
  })

  it('중간에 실패하면 에러 배너를 보여주고, 다시 시도하면 업로드 폼으로 돌아간다', async () => {
    api.uploadLogFile.mockRejectedValue(new Error('파일 크기는 50MB를 초과할 수 없습니다.'))

    const user = userEvent.setup()
    render(<App />)

    await fillAndSubmitUploadForm(user)

    expect(await screen.findByText('파일 크기는 50MB를 초과할 수 없습니다.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '처음부터 다시 시도' }))

    expect(await screen.findByLabelText('제목')).toBeInTheDocument()
  })
})
```

- [ ] **Step 4: 테스트 실행해서 실패 확인**

Run: `npx vitest run src/App.test.jsx`
Expected: FAIL — Step 1에서 `src/App.css`를 삭제했기 때문에 기존 `App.jsx`(아직 `./App.css`를 import하는 상태)가 모듈을 찾지 못해 에러가 나거나, 데모 화면이 렌더링되어 `제목` 라벨 등을 찾지 못해 실패한다. 둘 중 어느 쪽이든 FAIL이면 정상이다.

- [ ] **Step 5: `App.jsx`와 `App.module.css` 작성**

Create `src/App.module.css`:

```css
.app {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-h);
  margin: 0 0 24px;
}
```

`src/App.jsx` 전체를 아래 내용으로 교체한다:

```jsx
import { useReducer } from 'react'
import UploadForm from './components/UploadForm.jsx'
import ProcessingStatus from './components/ProcessingStatus.jsx'
import AnalysisResultView from './components/AnalysisResultView.jsx'
import ErrorBanner from './components/ErrorBanner.jsx'
import { uploadLogFile, runParsing, runAnalysisFlow } from './api/logAnalyzer.js'
import styles from './App.module.css'

const initialState = {
  step: 'upload',
  subStep: null,
  fileId: null,
  treeData: null,
  error: null,
}

function reducer(state, action) {
  switch (action.type) {
    case 'SUBMIT_UPLOAD':
      return { ...initialState, step: 'processing', subStep: 'uploading' }
    case 'UPLOAD_SUCCESS':
      return { ...state, fileId: action.fileId, subStep: 'parsing' }
    case 'PARSE_SUCCESS':
      return { ...state, subStep: 'analyzing' }
    case 'ANALYZE_SUCCESS':
      return { ...state, step: 'result', subStep: null, treeData: action.treeData }
    case 'FAIL':
      return { ...state, step: 'error', subStep: null, error: action.message }
    case 'RESET':
      return initialState
    default:
      return state
  }
}

function App() {
  const [state, dispatch] = useReducer(reducer, initialState)

  async function handleUploadSubmit(formValues) {
    dispatch({ type: 'SUBMIT_UPLOAD' })
    try {
      const fileId = await uploadLogFile(formValues)
      dispatch({ type: 'UPLOAD_SUCCESS', fileId })
      await runParsing(fileId)
      dispatch({ type: 'PARSE_SUCCESS' })
      const treeData = await runAnalysisFlow(fileId)
      dispatch({ type: 'ANALYZE_SUCCESS', treeData })
    } catch (err) {
      dispatch({ type: 'FAIL', message: err.message })
    }
  }

  function handleRetry() {
    dispatch({ type: 'RESET' })
  }

  return (
    <main className={styles.app}>
      <h1 className={styles.title}>내비 로그 소스코드 흐름도</h1>
      {state.step === 'upload' && <UploadForm onSubmit={handleUploadSubmit} />}
      {state.step === 'processing' && <ProcessingStatus subStep={state.subStep} />}
      {state.step === 'result' && <AnalysisResultView treeData={state.treeData} />}
      {state.step === 'error' && <ErrorBanner message={state.error} onRetry={handleRetry} />}
    </main>
  )
}

export default App
```

- [ ] **Step 6: 테스트 실행해서 통과 확인**

Run: `npx vitest run src/App.test.jsx`
Expected: PASS (2 tests)

- [ ] **Step 7: 전체 테스트 스위트 실행**

Run: `npm test`
Expected: 모든 테스트 파일 PASS (Task 1~9에서 작성한 테스트 전체)

- [ ] **Step 8: 커밋**

```bash
git add -A
git commit -m "[FRONT] App 상태 머신 통합 및 템플릿 데모 코드 정리"
```

---

## Task 10: 실제 백엔드 연동 수동 확인

이 태스크는 자동화 테스트가 아니라 브라우저에서 실제 흐름을 눈으로 확인하는 단계다.

- [ ] **Step 1: 백엔드 서버 기동 확인**

`backend/` 프로젝트를 `http://localhost:8081`에서 구동한다 (이미 떠 있다면 생략).

- [ ] **Step 2: 프론트 개발 서버 기동**

Run: `npm run dev`
브라우저에서 표시된 로컬 주소(기본 `http://localhost:5173`)를 연다.

- [ ] **Step 3: 정상 플로우 확인**

1. 제목/지라 티켓 키(선택)를 입력하고, 실제 내비 로그 파일을 선택해 "분석 시작" 클릭
2. "업로드 중... → 파싱 중... → 분석 중..." 이 순서대로 바뀌는지 확인
3. 최종적으로 트리 결과 화면이 뜨는지, 레이어 뱃지 색상(CONTAINER/JNI/JAVA)이 구분되는지 확인
4. `matchType: NONE` 노드가 "소스 위치 매핑 안 됨"으로 흐리게 표시되는지 확인
5. 검색창에 실제 함수명을 입력해 필터링이 조상 경로를 유지하며 동작하는지 확인
6. "매핑 안 된 노드만" 체크박스 동작 확인
7. "전체 접기" / "전체 펼치기" 버튼 동작 확인

- [ ] **Step 4: 에러 플로우 확인**

50MB를 초과하는 더미 파일을 선택해 클라이언트 검증 메시지가 뜨는지 확인. 백엔드를 잠시 내리거나 존재하지 않는 파일로 업로드를 시도해 에러 배너와 "처음부터 다시 시도" 버튼이 정상 동작하는지 확인.

- [ ] **Step 5: 콘솔 에러 확인**

브라우저 개발자 도구 콘솔에 에러/경고가 없는지 확인.

이 태스크는 코드 변경이 없으므로 커밋하지 않는다. 문제를 발견하면 해당 컴포넌트의 태스크로 돌아가 수정하고, 그 태스크의 테스트를 업데이트한 뒤 다시 커밋한다.

---

## 스펙 커버리지 체크 (자체 리뷰)

- 업로드 폼(title 필수/jiraTicketKey 선택, 파일 크기 50MB 클라이언트 검증) → Task 6
- 업로드→파싱→분석 순차 자동 호출 + 하위 상태 텍스트 전환 → Task 7, Task 9
- 단계 전환(한 순간에 한 화면) 상태 머신 → Task 9
- 에러 배너 + "처음부터 다시 시도" → Task 8, Task 9
- 레이어 뱃지 색상 구분(CONTAINER/JNI/JAVA) → Task 4
- matchType=NONE "소스 위치 매핑 안 됨" 표시 → Task 4
- 기본 전체 펼침 + 개별 접기/펼치기 → Task 4, Task 5
- 전체 펼치기/접기 버튼 → Task 5
- 함수명/파일명 검색 (조상 경로 유지) → Task 3, Task 5
- "매핑 안 된 노드만" 필터 → Task 3, Task 5
- API 클라이언트(공통 응답 처리, 3개 엔드포인트) → Task 2
- 템플릿 데모 코드/에셋 정리 → Task 9
