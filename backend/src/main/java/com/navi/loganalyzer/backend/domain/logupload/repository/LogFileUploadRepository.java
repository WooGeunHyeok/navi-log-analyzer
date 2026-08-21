package com.navi.loganalyzer.backend.domain.logupload.repository;

import com.navi.loganalyzer.backend.domain.logupload.entity.LogFileUpload;
import com.navi.loganalyzer.backend.domain.logupload.entity.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogFileUploadRepository extends JpaRepository<LogFileUpload, Long> {

    // 지라 티켓 번호로 로그 목록 조회
    List<LogFileUpload> findByJiraTicketKey(String jiraTicketKey);

    // 분석 상태별 로그 목록 조회 (예 : PENDING 상태인 파싱 대기 로그들만 가져오기)
    List<LogFileUpload> findByStatus(ProcessStatus status);

    // 제목 키워드 검색 (대소문자 구분 없이 포함 검색)
    List<LogFileUpload> findByTitleContainingIgnoreCase(String keyword);
}
