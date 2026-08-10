package com.navi.loganalyzer.backend.domain.parsing.repository;

import com.navi.loganalyzer.backend.domain.parsing.entity.ParsingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParsingLogRepository extends JpaRepository<ParsingLog, Long> {

    // 특정 로그 파일의 파싱 결과를 실행 순서(step) 오름차순으로 조회
    List<ParsingLog> findByLogFileIdOrderByStepAsc(Long logFileId);

    // 필요시 특정 파일의 파싱 결과 일괄 삭제 (재파싱 시)
    void deleteByLogFileId(Long logFileId);
}
