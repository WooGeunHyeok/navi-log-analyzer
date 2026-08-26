package com.navi.loganalyzer.backend.domain.analysis.util;

import java.util.regex.Pattern;

public class LogStringUtils {

    // 로그 시스템이 항상 붙이는 고정 노이즈 태그([BI3-0330])와, THREAD_ID를 그대로 echo하는
    // 숫자전용 태그([1529], [750] 등)만 제거 대상으로 한정.
    // 주의: "[CMainFrm]"처럼 소스코드/사전 DB 원문에 포함된 의미있는 태그는 제거 대상이 아님
    private static final Pattern PREFIX_TAG_PATTERN = Pattern.compile("^(\\[BI3-0330\\]\\s*|\\[\\d+\\]\\s*)+");

    /**
     * RAW_MESSAGE에서 앞쪽 고정 노이즈 태그([BI3-0330]) 및 THREAD_ID echo 태그([1529] 등)만 제거
     * 예: "[BI3-0330] PaneStack::Back() 2, rooping" -> "PaneStack::Back() 2, rooping"
     * 예: "[1529]resume activity -> ..." -> "resume activity -> ..."
     * 예: "[CMainFrm] setIndicatorType : ..." -> "[CMainFrm] setIndicatorType : ..." (그대로 유지)
     */
    public static String cleanRawMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return rawMessage;
        }
        String cleaned = PREFIX_TAG_PATTERN.matcher(rawMessage.trim()).replaceAll("");
        return cleaned.trim();
    }

    /**
     * CASE 3용: 정제된 메시지에서 LIKE 검색에 사용할 첫 단어(키워드) 추출
     * 예: "PaneStack::Back() 2, rooping" -> "PaneStack"
     */
    public static String extractFirstKeyword(String cleanedRawMessage) {
        if (cleanedRawMessage == null || cleanedRawMessage.isBlank()) {
            return "";
        }
        // 특수문자 전까지의 첫 단어 추출 (공백, ::, (, ), [, ] 등 기준)
        // 메시지가 구분자로 시작하는 경우(예: "[GVwNaviSetting_Map::...") tokens[0]이 빈 문자열이 되므로,
        // 첫 번째로 나오는 "빈 문자열이 아닌" 토큰을 사용한다.
        // 주의: "]"도 구분자에 포함해야 함 - "[CMainFrm] ..."처럼 태그 안에 "::"가 없으면
        // "]"까지 토큰에 붙어버려서("CMainFrm]") 사전 DB 매칭이 깨짐
        String[] tokens = cleanedRawMessage.split("[\\s::\\(\\)\\[\\]]+");
        for (String token : tokens) {
            if (!token.isBlank()) {
                return token;
            }
        }
        return cleanedRawMessage.length() > 5 ? cleanedRawMessage.substring(0, 5) : cleanedRawMessage;
    }
}
