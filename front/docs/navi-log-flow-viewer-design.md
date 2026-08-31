# 내비 로그 소스코드 흐름도 뷰어 — 프론트엔드 설계

- 날짜: 2026-08-27
- 대상: `front/` (Vite + React 19)
- 배경: 내비게이션 단말기 이슈 로그 파일을 업로드하면 사전 DB(소스코드에서 추출한 함수/파일 정보)와 매칭해 "소스코드 흐름도"(함수 호출 흐름 트리)를 보여주는 백엔드가 완성되어 있음. 이 API를 소비하는 React 프론트엔드를 새로 만든다.

## 범위

- 업로드 → 파싱 → 분석 → 트리 결과 표시로 이어지는 **단일 플로우 화면 하나**만 만든다.
- 이력 목록 화면, 파일 재분석 트리거(저장된 결과 조회 API 재사용), 페이지 라우팅은 이번 범위에서 제외한다. (`GET /api/v1/logs/analysis/result/{logFileId}` 는 이번 스펙에서 사용하지 않음)

## API 계약 (백엔드, 변경 없음)

서버: `http://localhost:8081` (CORS 전체 오픈, context-path 없음)

공통 응답: `{ success: boolean, message: string, data: T }` (null 필드는 JSON에서 생략됨)

1. `POST /api/v1/logs/upload` — multipart. part `file`, part `request`(JSON: `{ title(필수), jiraTicketKey(선택) }`). 응답 `data: number`(fileId). 파일 크기 제한 50MB.
2. `POST /api/v1/parsing/{logFileId}` — 응답 `data: number`(fileId, 동일 값).
3. `GET /api/v1/logs/analysis/flow/{logFileId}` — 응답 `data: LogAnalysisResponseDto[]` (트리). 호출할 때마다 기존 분석 결과를 지우고 새로 저장 (재호출 안전).

`LogAnalysisResponseDto`:

```ts
interface LogAnalysisResponseDto {
  id: number;
  fileId: number;
  step: number;
  timestamp: string;
  threadId: number;
  logLevel: string;
  layer: "CONTAINER" | "JNI_LAYER" | "JAVA_LAYER";
  rawMessage: string;
  fileName: string | null;
  filePath: string | null;
  functionName: string | null;
  lineNumber: number | null;
  matchType: "EXACT_KEY" | "NONE";
  depth: number;
  children: LogAnalysisResponseDto[];
}
```

- 최상위 응답은 배열(root 노드가 여러 개 나란히 존재할 수 있음).
- `matchType: "NONE"`이고 `functionName/fileName/filePath/lineNumber`가 모두 `null`인 노드는 정상 케이스(예: `JAVA_LAYER`는 애초에 사전 DB 매칭 대상이 아님). 버그로 취급하지 않고 "소스 위치 매핑 안 됨" UI로 표시한다.

## 스택 & 의존성

- 기존 Vite + React 19 + JSX 그대로 사용. TypeScript, 라우터, 상태관리 라이브러리, axios, UI 컴포넌트 라이브러리는 추가하지 않는다.
- 스타일링: 순수 CSS(CSS Modules). 추가 의존성 없이 시작.
- HTTP: 네이티브 `fetch`를 얇게 감싼 함수만 작성.
- 표시 언어: 한국어 (라벨, 안내 문구, 에러 메시지 모두).

## 화면 흐름

단계 전환 방식 — **한 순간에 한 화면만 보인다** (이전 단계 화면은 사라짐):

1. **업로드 폼**: `title`(필수) + `jiraTicketKey`(선택) + 파일 선택 input. 제출 시 클라이언트에서 `title` 빈 값 검사, 파일 크기 50MB 초과 검사를 먼저 수행하고 통과하면 업로드 API 호출.
2. **처리 중 화면**: 업로드 성공 후, 사용자 입력 없이 파싱 → 분석을 순차 자동 호출. 하나의 로딩 화면 안에서 하위 상태 텍스트만 전환된다: `업로드 중 → 파싱 중 → 분석 중`. 취소 버튼 없음(흐름이 짧아 불필요하다고 판단; 필요해지면 추후 추가).
3. **트리 결과 화면**: 분석 API가 반환한 트리 배열을 렌더링.

어느 단계에서든 실패하면 에러 배너로 전환: 서버 `message`를 표시하고 "처음부터 다시 시도" 버튼으로 업로드 폼 단계로 리셋한다 (실패한 단계만 재시도하는 기능은 만들지 않음 — 흐름이 짧아 처음부터 다시 하는 부담이 적다고 판단).

## 상태 관리

`App.jsx`에서 `useReducer`로 아래 상태를 관리한다. 전역 상태 라이브러리는 쓰지 않는다.

```
{
  step: 'upload' | 'processing' | 'result' | 'error',
  subStep: 'uploading' | 'parsing' | 'analyzing' | null,  // step === 'processing'일 때만 사용
  fileId: number | null,
  treeData: LogAnalysisResponseDto[] | null,
  error: string | null,
}
```

## 컴포넌트 구성

```
src/
  api/
    logAnalyzer.js        // uploadLogFile / runParsing / runAnalysisFlow
  App.jsx                  // 단계 상태 머신, 위 3+1 화면 스위칭
  components/
    UploadForm.jsx          // 1단계
    ProcessingStatus.jsx     // 2단계 (하위 상태 텍스트 + 스피너)
    AnalysisResultView.jsx   // 3단계 컨테이너: 검색창 + 전체펼침/접기 버튼 + "매핑안됨만" 토글 + TreeNode 목록 렌더
      TreeNode.jsx             // 재귀 컴포넌트, 트리 노드 1개
    ErrorBanner.jsx           // 공통 에러 표시 + "처음부터 다시 시도"
```

### `api/logAnalyzer.js`

- `uploadLogFile({ title, jiraTicketKey, file })`: `FormData`에 `file`, `request`(JSON Blob)를 담아 POST.
- `runParsing(fileId)`: POST, 바디 없음.
- `runAnalysisFlow(fileId)`: GET.
- 공통 응답 파싱 헬퍼 하나: HTTP 에러이거나 `success: false`면 `message`를 담아 throw. 셋 다 이 헬퍼를 통해 결과를 반환한다.
- 서버 주소(`http://localhost:8081`)는 파일 상단 상수로 고정.

## 트리 결과 화면 상세

- **레이어 뱃지**: 함수명 옆에 작은 색상 칩으로 레이어 표시 — `CONTAINER`=보라, `JNI_LAYER`=주황, `JAVA_LAYER`=초록.
- **매핑 안 된 노드** (`matchType === "NONE"`): 행 전체를 흐리게(opacity) 표시하고, 함수명 자리에 "소스 위치 매핑 안 됨" 텍스트를 보여준다. `fileName`/`lineNumber`가 있으면 옆에 같이 표시(경로 클릭 등 별도 액션은 없음).
- **기본 펼침 상태**: 트리는 처음부터 전체 펼쳐진 상태로 렌더링된다. 노드별로 개별 접기/펼치기 가능.
- **전체 펼치기/접기 버튼**: 모든 노드의 펼침 상태를 한 번에 토글하는 버튼 하나.
- **검색(함수명/파일명)**: 입력한 검색어가 `functionName` 또는 `fileName`에 포함된 노드만 남기고 렌더링하되, 매치된 노드로 가는 **조상 경로는 항상 함께 표시**하고 자동으로 펼쳐진다(문맥 없이 매치 노드만 보이면 호출 흐름 파악이 안 되기 때문). 검색어를 지우면 이전 펼침 상태로 복원된다.
- **"매핑 안 된 노드만" 필터**: 검색과 동일한 조상-경로-유지 규칙으로 `matchType === "NONE"`인 노드만 필터링해서 보여준다. 검색어와 동시에 켤 수 있으며, 이 경우 두 조건을 모두 만족하는 노드만 남긴다.
- 검색/필터 로직(트리를 가지치기하면서 조상 경로를 유지하는 재귀 함수)은 `TreeNode.jsx` 밖에 순수 함수(`src/utils/treeFilter.js`)로 분리해 테스트하기 쉽게 한다.

## 제외한 것 (이번 범위 아님)

- 이력 목록, 재분석 버튼, 저장된 결과 빠른 조회 API 사용
- 라우팅/페이지 이동
- 업로드 취소, 실패 단계만 재시도
- TypeScript 전환, UI 라이브러리 도입
