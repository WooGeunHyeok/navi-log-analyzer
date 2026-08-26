package com.navi.loganalyzer.backend.domain.analysis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navi.loganalyzer.backend.domain.analysis.dto.LogAnalysisResponseDto;
import com.navi.loganalyzer.backend.domain.analysis.entity.NaviLogeDataEntity;
import com.navi.loganalyzer.backend.domain.analysis.repository.LogAnalysisRepository;
import com.navi.loganalyzer.backend.domain.analysis.repository.NaviLogeDataRepository;
import com.navi.loganalyzer.backend.domain.analysis.util.LogStringUtils;
import com.navi.loganalyzer.backend.domain.parsing.entity.ParsingLog;
import com.navi.loganalyzer.backend.domain.analysis.entity.LogAnalysis;
import com.navi.loganalyzer.backend.domain.parsing.repository.ParsingLogRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisService {

    private final ParsingLogRepository parsingLogRepository;
    private final NaviLogeDataRepository naviLogeDataRepository;
    private final LogAnalysisRepository logAnalysisRepository;
    private final ObjectMapper objectMapper; // Spring 컨테이너의 ObjectMapper 주입

    /**
     * Service 로직 전체 실행 흐름 (Flow)
     * 1단계 : 파싱 로그 조회
     * 2단계 : 사전 DB 매칭하여 LogAnalysis엔티티 + calls 데이터 생성
     * 3단계 : Stack 기반 트리 구축
     * 4단계 : LogAnalysis 기반으로 LogAnalysisResponseDto 트리 조합하여 Controller 응답
     */

    /**
     * logFileId 로 로그 목록 및 사전 calls 정보를 조회하여 트리 구조로 변환 후 반환.
     * 분석 결과(functionName/fileName/depth 등)는 TBL_LOG_ANALYSIS에 저장된다.
     * 같은 logFileId로 다시 호출되면 기존 분석 결과를 지우고 새로 저장한다(항상 덮어쓰기).
     */
    @Transactional
    public List<LogAnalysisResponseDto> getLogFlowTree(Long logFileId) {

        // 1. logFileId 에 해당하는 로그 목록을 step 순으로 조회
        List<ParsingLog> ParsingLogList = parsingLogRepository.findByFileIdOrderByStepAsc(logFileId);

        if (ParsingLogList.isEmpty()) {
            return Collections.emptyList();
        }

        // 사전 DB 정밀 매칭 진행
        List<LogAnalysisWithCallsDto> logAnalysisList = ParsingLogList.stream()
                .map(this::matchWithDictionary)
                .collect(Collectors.toList());

        // Stack 기반 트리 변환 실행 (depth가 채워진 엔티티도 함께 계산됨)
        CallTreeResult result = buildCallTreeResult(logAnalysisList);

        // 분석 결과 저장 (재분석 기능은 따로 두지 않고, 호출할 때마다 항상 덮어쓴다 -
        // 이렇게 하면 같은 fileId로 실수로 두 번 호출되는 경우도 안전하게 처리된다)
        logAnalysisRepository.deleteByFileId(logFileId);
        logAnalysisRepository.saveAll(result.getAnalyzedEntities());

        return result.getRootNodes();
    }

    /**
     * 이미 분석/저장된 결과(TBL_LOG_ANALYSIS)를 재매칭 없이 조회하여 트리로 재조립.
     * step 순으로 읽으면서 저장된 depth 값만으로 트리를 복원하므로 사전 DB 조회가 필요 없다.
     */
    @Transactional(readOnly = true)
    public List<LogAnalysisResponseDto> getSavedLogFlowTree(Long logFileId) {
        List<LogAnalysis> savedList = logAnalysisRepository.findByFileIdOrderByStepAsc(logFileId);

        if (savedList.isEmpty()) {
            return Collections.emptyList();
        }

        return buildTreeFromSavedDepth(savedList);
    }

    /**
     * step 순으로 정렬된 (depth 포함) 저장 결과를 받아 depth만으로 트리를 재조립.
     * buildCallTree의 스택 로직과 반대로, depth가 이전 노드보다 작거나 같아질 때까지
     * 스택을 pop하면서 부모를 찾는 방식이다.
     */
    private List<LogAnalysisResponseDto> buildTreeFromSavedDepth(List<LogAnalysis> savedList) {
        List<LogAnalysisResponseDto> rootNodes = new ArrayList<>();
        Deque<LogAnalysisResponseDto> stack = new ArrayDeque<>();

        for (LogAnalysis entity : savedList) {
            int depth = entity.getDepth();
            LogAnalysisResponseDto currentDto = LogAnalysisResponseDto.fromEntity(entity, depth);

            while (!stack.isEmpty() && stack.peek().getDepth() >= depth) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                rootNodes.add(currentDto);
            } else {
                stack.peek().getChildren().add(currentDto);
            }

            stack.push(currentDto);
        }

        return rootNodes;
    }

    /**
     * [단계 1~3] 정제 -> 1차 후보군 조회 -> 2차 정규식 정밀 매칭
     */
    private LogAnalysisWithCallsDto matchWithDictionary(ParsingLog parsingLog) {
        if (parsingLog == null) {
            return new LogAnalysisWithCallsDto(null, Collections.emptyList());
        }

        // 1. 파싱 DB RAW_MESSAGE 접두사([BI3-0330] 등) 정제
        String cleanedRawMessage = LogStringUtils.cleanRawMessage(parsingLog.getRawMessage());

        // 2. CASE 1~3으로 1차 후보군(Candidates) 추출
        List<NaviLogeDataEntity> candidates = getCandidates(
                parsingLog.getFunctionName(),
                parsingLog.getLineNum(),
                cleanedRawMessage
        );

        // 3. 정규식(PATTERN_MESSAGE) 기반 최종 정밀 매칭
        Optional<NaviLogeDataEntity> matchedEntityOpt = findBestMatchByPattern(candidates, cleanedRawMessage);

        // 엔티티 및 결과 DTO 바인딩
        String fileName = null;
        String filePath = null;
        String functionName = parsingLog.getFunctionName();
        Integer lineNumber = parsingLog.getLineNum();
        String matchType = "NONE";
        List<String> callsList = Collections.emptyList();

        if (matchedEntityOpt.isPresent()) {
            NaviLogeDataEntity dict = matchedEntityOpt.get();
            fileName = dict.getFileName();
            filePath = dict.getFilePath();
            functionName = dict.getFunctionName(); // 사전 DB의 클래스명::함수명으로 보완
            lineNumber = dict.getLineNumber();
            matchType = "EXACT_KEY";
            callsList = parseCallsJson(dict.getCalls());
        }

        LogAnalysis logAnalysis = LogAnalysis.builder()
                .parsingLogId(parsingLog.getId())
                .fileId(parsingLog.getLogFile() != null ? parsingLog.getLogFile().getId() : null)
                .step(parsingLog.getStep())
                .timestamp(parsingLog.getTimestamp())
                .threadId(parsingLog.getThreadId())
                .logLevel(parsingLog.getLogLevel())
                .layer(parsingLog.getLayer() != null ? parsingLog.getLayer().name() : null)
                .rawMessage(parsingLog.getRawMessage()) // 원본 유지
                .fileName(fileName)
                .filePath(filePath)
                .functionName(functionName)
                .lineNumber(lineNumber)
                .matchType(matchType)
                .build();

        return new LogAnalysisWithCallsDto(logAnalysis, callsList);
    }

    /**
     * [2단계] 조건별 1차 후보군(Candidates) 조회
     */
    private List<NaviLogeDataEntity> getCandidates(String funcName, Integer lineNum, String cleanedRawMessage) {
        boolean hasFunc = funcName != null && !funcName.isBlank();
        boolean hasLine = lineNum != null && lineNum > 0;

        // [CASE 1] 함수명 + 라인번호 모두 있음
        if (hasFunc && hasLine) {
            List<NaviLogeDataEntity> list = naviLogeDataRepository.findCandidatesByFuncAndLine(funcName, lineNum);
            if (!list.isEmpty()) return list;
        }

        // [CASE 2] 함수명만 있음 (또는 CASE 1 실패 시 Fallback)
        if (hasFunc) {
            List<NaviLogeDataEntity> list = naviLogeDataRepository.findCandidatesByFunc(funcName);
            if (!list.isEmpty()) return list;
        }

        // [CASE 3] 함수명, 라인번호 모두 없음
        String keyword = LogStringUtils.extractFirstKeyword(cleanedRawMessage);
        if (!keyword.isBlank()) {
            return naviLogeDataRepository.findCandidatesByKeyword(keyword);
        }

        return Collections.emptyList();
    }

    /**
     * [3단계] 2차 정규식(Pattern) 정밀 매칭 (Matcher.find() 사용)
     */
    private Optional<NaviLogeDataEntity> findBestMatchByPattern(List<NaviLogeDataEntity> candidates, String cleanedRawMessage) {
        if (candidates.isEmpty() || cleanedRawMessage == null || cleanedRawMessage.isBlank()) {
            return Optional.empty();
        }

        for (NaviLogeDataEntity candidate : candidates) {
            String patternStr = candidate.getPatternMessage();

            // PATTERN_MESSAGE가 없는 경우 RAW_MESSAGE 단순 동등 비교
            if (patternStr == null || patternStr.isBlank()) {
                if (cleanedRawMessage.equals(candidate.getRawMessage())) {
                    return Optional.of(candidate);
                }
                continue;
            }

            try {
                Pattern pattern = Pattern.compile(patternStr);
                // find()를 사용하여 접두사 제외 부분 일치 검사
                if (pattern.matcher(cleanedRawMessage).find()) {
                    return Optional.of(candidate);
                }
            } catch (PatternSyntaxException e) {
                log.warn("유효하지 않은 정규식 패턴 ID {}: {}", candidate.getId(), patternStr);
            }
        }

        return Optional.empty();
    }

    /**
     * buildCallTree와 동일한 스택 로직을 수행하면서, 계산된 depth를 반영한 LogAnalysis 엔티티
     * 목록도 함께 만들어 반환한다 (저장용).
     */
    private CallTreeResult buildCallTreeResult(List<LogAnalysisWithCallsDto> itemList) {
        List<LogAnalysisResponseDto> rootNodes = new ArrayList<>();
        List<LogAnalysis> analyzedEntities = new ArrayList<>();
        Stack<StackNode> stack = new Stack<>();

        for (LogAnalysisWithCallsDto item : itemList) {
            LogAnalysis entity = item.getLogAnalysis();
            List<String> calls = item.getCallsList();

            while (!stack.isEmpty()) {
                StackNode parentNode = stack.peek();

                // 동일 함수가 연달아 출력된 경우(Sibling)를 최우선으로 판단한다.
                // 같은 함수의 다음 로그 줄이 우연히 부모 calls 목록의 텍스트를 포함하고 있어도
                // (예: "GetRouteState() = -1"처럼 이전에 조회한 값을 그대로 출력하는 경우),
                // "같은 함수의 후속 로그"라는 더 명확한 정보가 텍스트 우연 일치보다 우선해야 한다.
                if (isSameFunction(parentNode.getEntity().getFunctionName(), entity.getFunctionName())) {
                    stack.pop();
                    break;
                }

                // 부모의 calls 목록에 현재 함수명/메시지가 포함되어 있으면 부모 자식 관계
                if (isCalledByParent(parentNode.getCalls(), entity.getFunctionName(), entity.getRawMessage())) {
                    break;
                }

                // 내비게이션 단말기 로그 흐름 특성상 JNI_LAYER 진입 로그 다음에 그 결과를 출력하는
                // JAVA_LAYER 로그가 곧바로 이어지는 경우가 많음 (사전 DB에 등록되지 않아 매칭 실패해도
                // 부모 JNI 함수명의 핵심 키워드가 자식 메시지에 그대로 남아있으면 자식으로 판단)
                if (isJniToJavaFollowUp(parentNode.getEntity(), entity)) {
                    break;
                }

                // 3. 호출 관계가 성립하지 않으면 스택에서 Pop (함수 실행 종료)
                stack.pop();
            }

            int currentDepth = stack.isEmpty() ? 0 : stack.peek().getDto().getDepth() + 1;
            LogAnalysisResponseDto currentDto = LogAnalysisResponseDto.fromEntity(entity, currentDepth);

            if (stack.isEmpty()) {
                rootNodes.add(currentDto);
            } else {
                stack.peek().getDto().getChildren().add(currentDto);
            }

            stack.push(new StackNode(currentDto, entity, calls));
            analyzedEntities.add(entity.toBuilder().depth(currentDepth).build());
        }

        return new CallTreeResult(rootNodes, analyzedEntities);
    }

    /**
     * 부모의 calls 배열에 자식 함수명이 포함되어 있는지 검증
     */
    private boolean isCalledByParent(List<String> parentCalls, String currentFuncName, String rawMessage) {
        if (parentCalls == null || parentCalls.isEmpty()) return false;

        for (String call : parentCalls) {
            if (currentFuncName != null && (currentFuncName.equals(call) || currentFuncName.endsWith("::" + call))) {
                return true;
            }
            if (rawMessage != null && containsAsWord(rawMessage, call)) {
                return true;
            }
        }
        return false;
    }

    /**
     * rawMessage 안에 call이 "단어" 단위로 포함되어 있는지 검증.
     * 단순 String.contains()는 "Go"가 "nGoPase" 안에 우연히 포함된 경우처럼
     * 전혀 무관한 단어 중간에서 거짓 매칭이 발생한다. 단, "call_java_..._setNaviAutoMoveTimer"처럼
     * 밑줄(_)로 이어지는 함수명 관례가 있어 밑줄은 경계로 취급하고 영숫자만 "단어 문자"로 본다.
     */
    private boolean containsAsWord(String rawMessage, String call) {
        if (call == null || call.isBlank()) return false;
        Pattern boundaryPattern = Pattern.compile("(?<![a-zA-Z0-9])" + Pattern.quote(call) + "(?![a-zA-Z0-9])");
        return boundaryPattern.matcher(rawMessage).find();
    }

    // JNI 브릿지 함수명 접두사 (예: "call_java_com_mnsoft_navi_NativeCall_vcrmFunctionGetter")
    // 이 접두사를 뗀 나머지가 실제 처리 내용을 나타내는 핵심 키워드 (예: "vcrmFunctionGetter")
    private static final String JNI_NATIVE_CALL_PREFIX = "call_java_com_mnsoft_navi_NativeCall_";

    /**
     * JNI_LAYER 진입 로그(부모) -> JAVA_LAYER 결과 로그(자식) 흐름 판별.
     * 사전 DB에 없어 매칭 실패한 JAVA_LAYER 로그라도, 부모 JNI 함수명의 핵심 키워드가
     * 자식 로그 메시지 안에 그대로 남아있으면 그 JNI 호출의 결과 로그로 간주한다.
     */
    private boolean isJniToJavaFollowUp(LogAnalysis parentEntity, LogAnalysis entity) {
        if (!"JNI_LAYER".equals(parentEntity.getLayer()) || !"JAVA_LAYER".equals(entity.getLayer())) {
            return false;
        }

        String parentKeyword = extractJniCoreFunctionName(parentEntity.getFunctionName());
        if (parentKeyword == null || parentKeyword.isBlank()) {
            return false;
        }

        return entity.getRawMessage() != null && entity.getRawMessage().contains(parentKeyword);
    }

    /**
     * JNI 브릿지 함수명에서 고정 접두사를 제거해 실제 처리 내용을 나타내는 핵심 키워드만 추출
     */
    private String extractJniCoreFunctionName(String jniFunctionName) {
        if (jniFunctionName == null) {
            return null;
        }
        if (jniFunctionName.startsWith(JNI_NATIVE_CALL_PREFIX)) {
            return jniFunctionName.substring(JNI_NATIVE_CALL_PREFIX.length());
        }
        return jniFunctionName;
    }

    /**
     * 동일 함수 여부 판별 (클래스명::함수명 또는 pure 함수명 비교)
     */
    private boolean isSameFunction(String parentFuncName, String currentFuncName) {
        if (parentFuncName == null || currentFuncName == null) return false;
        return parentFuncName.equals(currentFuncName);
    }

    private List<String> parseCallsJson(String callsJsonStr) {
        if (callsJsonStr == null || callsJsonStr.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(callsJsonStr, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class LogAnalysisWithCallsDto {
        private final LogAnalysis logAnalysis;
        private final List<String> callsList;
    }

    @Getter
    @AllArgsConstructor
    private static class StackNode {
        private final LogAnalysisResponseDto dto;
        private final LogAnalysis entity;
        private final List<String> calls;
    }

    @Getter
    @AllArgsConstructor
    private static class CallTreeResult {
        private final List<LogAnalysisResponseDto> rootNodes;
        private final List<LogAnalysis> analyzedEntities;
    }
}