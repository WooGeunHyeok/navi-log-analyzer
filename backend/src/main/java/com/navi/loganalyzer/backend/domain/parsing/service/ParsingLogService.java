package com.navi.loganalyzer.backend.domain.parsing.service;

import com.navi.loganalyzer.backend.domain.log.entity.LogFileUpload;
import com.navi.loganalyzer.backend.domain.log.repository.LogFileUploadRepository;
import com.navi.loganalyzer.backend.domain.parsing.dto.ParsingLogFlowResponseDto;
import com.navi.loganalyzer.backend.domain.parsing.entity.LogLayer;
import com.navi.loganalyzer.backend.domain.parsing.entity.ParsingLog;
import com.navi.loganalyzer.backend.domain.parsing.repository.ParsingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParsingLogService {

    private final ParsingLogRepository parsingLogRepository;
    private final LogFileUploadRepository logFileUploadRepository;

    // 로그 정규식 패턴
    private static final Pattern LOG_PATTERN = Pattern.compile (
            "^(\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\d+)\\s+(\\d+)\\s+([A-Z])\\s+([^:]+):\\s*(.*?)(?:\\s*\\[([a-zA-Z0-9_]+),(\\d+)\\])?$"
    );

    // 업로드 된 로그 파일을 읽어 파싱 및 DB  저장 수행
    @Transactional
    public void parsingAndSaveLogFile(Long logFileId) {
        LogFileUpload logFile = logFileUploadRepository.findById(logFileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 로그 파일 입니다."));

        File file = new File(logFile.getStoredFilePath());
        if (!file.exists()) {
            throw new IllegalStateException("실제 파일이 저장 경로에 존재하지 않습니다. : " + logFile.getStoredFilePath());
        }

        List<ParsingLog> parsedLogs = new ArrayList<>();
        // Thread ID별 depth 관리
        Map<Long, Long> threadDepthMap = new HashMap<>();

        long step = 1L;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = LOG_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String timestamp = matcher.group(1);
                    Long threadId = Long.parseLong(matcher.group(3));
                    String logLevel = matcher.group(4);
                    String tag = matcher.group(5);
                    String rawMessage = matcher.group(6);
                    String functionName = matcher.group(7);
                    Long lineNum = matcher.group(8) != null ? Long.parseLong(matcher.group(8)) : null;

                    // 1. Layer 판별
                    LogLayer layer = determineLayer(tag);

                    if (layer == null) {
                        continue;
                    }
                    // 2. Thread ID별 Depth 계산
                    Long currentDepth = calculateDepth(threadDepthMap, threadId, rawMessage);

                    ParsingLog parsingLog = ParsingLog.builder()
                            .logFile(logFile)
                            .step(step++)
                            .timestamp(timestamp)
                            .threadId(threadId)
                            .logLevel(logLevel)
                            .layer(layer)
                            .functionName(functionName)
                            .lineNum(lineNum)
                            .rawMessage(rawMessage)
                            .depth(currentDepth)
                            .build();

                    parsedLogs.add(parsingLog);
                }
            }

            // 3. 파싱 결과 일괄 저장
            parsingLogRepository.saveAll(parsedLogs);
            log.info("로그 파일 파싱 완료 - 파일 ID: {}, 총 파싱 라인 수: {}", logFileId, parsedLogs.size());

        } catch (IOException e) {
            log.error("로그 파일 읽기 중 오류 발생", e);
            throw new RuntimeException("로그 파일 파싱 실패", e);
        }
    }

    // 메시지 키워드를 기반으로 LogLayer 판별
    private LogLayer determineLayer(String tag) {
        if (tag == null) {
            return null;
        }

        String trimmedTag = tag.trim();

        if ("Container".equalsIgnoreCase(trimmedTag)) {
            return LogLayer.CONTAINER;
        } else if ("JNI_Layer".equalsIgnoreCase(trimmedTag)) {
            return LogLayer.JNI_LAYER;
        } else if ("Java_Layer".equalsIgnoreCase(trimmedTag)) {
            return LogLayer.JAVA_LAYER;
        }
        return null;
    }

    // Thread ID별 키워드 기반 Depth 계산
    private Long calculateDepth(Map<Long, Long> threadDepthMap, Long threadId, String message) {
        Long depth = threadDepthMap.getOrDefault(threadId, 0L);

        // 함수 진입/시작 관련 키워드가 있으면 depth 증가
        if (message.contains("- start") || message.contains("ENTER") || message.contains("start :")) {
            depth = depth + 1L;
            threadDepthMap.put(threadId, depth);
        }
        // 함수 종료/반환 관련 키워드가 있으면 depth 감소
        else if ((message.contains("- ret") || message.contains("EXIT") || message.contains("End")) && depth > 0) {
            depth = depth - 1L;
            threadDepthMap.put(threadId, depth);
        }

        return depth;
    }

    // 파싱된 로그 목록을 조회하고, 소스코드 DB와 매핑하여 응답 DTO 리스트로 변환
    @Transactional(readOnly = true)
    public List<ParsingLogFlowResponseDto> getParsingLogFlow(Long logfileId) {
        List<ParsingLog> parsingLogs = parsingLogRepository.findByLogFileIdOrderByStepAsc(logfileId);

        return parsingLogs.stream().map(log -> {
            // TODO : 향후 소스코드 DB Repository를 통해 functionName과 lineNum으로 실제 위치 조회
            // String filePath = sourceCodeRepository.findFilePath(log.getFunctionName(), log.getLineNum());
            // String mappedClass = sourceCodeRepository.findClassName(log.getFunctionName(), log.getLineNum());

            String dummyMappedPath = null;
            String dummyMappedClass = null;

            // 예시: functionName이 존재할 경우 소스 위치가 매핑된 것으로 임시 가공
            if (log.getFunctionName() != null) {
                dummyMappedPath = "src/container/navigation/" + log.getFunctionName() + ".cpp";
                dummyMappedClass = log.getFunctionName();
            }

            return ParsingLogFlowResponseDto.from(log, dummyMappedPath, dummyMappedClass);
        }).toList();
    }
}

