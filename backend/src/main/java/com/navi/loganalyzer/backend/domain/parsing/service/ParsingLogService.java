package com.navi.loganalyzer.backend.domain.parsing.service;

import com.navi.loganalyzer.backend.domain.logupload.entity.LogFileUpload;
import com.navi.loganalyzer.backend.domain.logupload.repository.LogFileUploadRepository;
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
    public Long parsingAndSaveLogFile(Long logFileId) {
        LogFileUpload logFile = logFileUploadRepository.findById(logFileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 로그 파일 입니다."));

        File file = new File(logFile.getStoredFilePath());
        if (!file.exists()) {
            throw new IllegalStateException("실제 파일이 저장 경로에 존재하지 않습니다. : " + logFile.getStoredFilePath());
        }

        // Thread ID별로 "아직 함수 태그를 못 받은 마지막 메시지 줄"을 기억해뒀다가,
        // 같은 스레드에서 뒤이어 나오는 "태그 전용 줄"을 만나면 그 줄에 소급 병합한다.
        // 예) "CPlatformAndroid::mPostMessage() - type(...)" 줄 다음에 " [mPostMessage,1343]" 줄만 따로 찍히는 케이스
        Map<Long, ParsedLine> pendingTagByThread = new HashMap<>();
        List<ParsedLine> parsedLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = LOG_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }

                String timestamp = matcher.group(1);
                Long threadId = Long.parseLong(matcher.group(3));
                String logLevel = matcher.group(4);
                String tag = matcher.group(5);
                String rawMessage = matcher.group(6);
                String functionName = matcher.group(7);
                Integer lineNum = matcher.group(8) != null ? Integer.parseInt(matcher.group(8)) : null;

                // 1. Layer 판별
                LogLayer layer = determineLayer(tag);
                if (layer == null) {
                    continue;
                }

                // 2. 메시지 없이 함수/라인 태그만 있는 줄인지 판별
                boolean isTagOnlyLine = (rawMessage == null || rawMessage.isBlank()) && functionName != null;

                if (isTagOnlyLine) {
                    ParsedLine pending = pendingTagByThread.get(threadId);
                    if (pending != null && pending.functionName == null) {
                        pending.functionName = functionName;
                        pending.lineNum = lineNum;
                    }
                    // 태그 전용 줄 자체는 별도 로그 row로 남기지 않음 (STEP 부여 안 함)
                    continue;
                }

                ParsedLine parsedLine = new ParsedLine(timestamp, threadId, logLevel, layer,
                        rawMessage, functionName, lineNum);
                parsedLines.add(parsedLine);
                pendingTagByThread.put(threadId, parsedLine);
            }

            // 4. 병합이 끝난 결과를 ParsingLog 엔티티로 변환 (STEP은 실제 저장되는 줄에만 순서대로 부여)
            List<ParsingLog> parsedLogs = new ArrayList<>();
            long step = 1L;
            for (ParsedLine parsedLine : parsedLines) {
                parsedLogs.add(ParsingLog.builder()
                        .logFile(logFile)
                        .step(step++)
                        .timestamp(parsedLine.timestamp)
                        .threadId(parsedLine.threadId)
                        .logLevel(parsedLine.logLevel)
                        .layer(parsedLine.layer)
                        .functionName(parsedLine.functionName)
                        .lineNum(parsedLine.lineNum)
                        .rawMessage(parsedLine.rawMessage)
                        .build());
            }

            // 5. 파싱 결과 일괄 저장
            parsingLogRepository.saveAll(parsedLogs);
            log.info("로그 파일 파싱 완료 - 파일 ID: {}, 총 파싱 라인 수: {}", logFileId, parsedLogs.size());

            return logFile.getId();

        } catch (IOException e) {
            log.error("로그 파일 읽기 중 오류 발생", e);
            throw new RuntimeException("로그 파일 파싱 실패", e);
        }
    }

    /**
     * 파싱 진행 중(태그 병합 완료 전) 상태를 담는 임시 객체.
     * functionName/lineNum은 같은 스레드의 뒤이은 태그 전용 줄에 의해 나중에 채워질 수 있어 가변으로 둔다.
     * ParsingLog 엔티티는 불변으로 유지하고, 병합이 끝난 뒤에만 엔티티로 변환한다.
     */
    private static class ParsedLine {
        private final String timestamp;
        private final Long threadId;
        private final String logLevel;
        private final LogLayer layer;
        private final String rawMessage;
        private String functionName;
        private Integer lineNum;

        private ParsedLine(String timestamp, Long threadId, String logLevel, LogLayer layer,
                            String rawMessage, String functionName, Integer lineNum) {
            this.timestamp = timestamp;
            this.threadId = threadId;
            this.logLevel = logLevel;
            this.layer = layer;
            this.rawMessage = rawMessage;
            this.functionName = functionName;
            this.lineNum = lineNum;
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
}

