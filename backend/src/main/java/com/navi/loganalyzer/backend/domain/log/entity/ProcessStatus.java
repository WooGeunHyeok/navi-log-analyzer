package com.navi.loganalyzer.backend.domain.log.entity;

public enum ProcessStatus {

    PENDING,                // 업로드 완료 (파싱 대기)
    PROCESSING,             // 파싱 진행 중
    COMPLETED,              // 파싱 완료
    FAILED                  // 파싱 실패
}