package com.navi.loganalyzer.backend.domain.parsing.repository;

import com.navi.loganalyzer.backend.domain.parsing.entity.ParsingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParsingLogRepository extends JpaRepository<ParsingLog, Long> {

    // 필요시 특정 파일의 파싱 결과 일괄 삭제 (재파싱 시)
    void deleteByLogFileId(Long logFileId);

    /**
     * fileId (TBL_LOGS_FILES.ID) 기준 로그 목록을 step ASC 조회
     */
    @Query("SELECT p FROM ParsingLog p WHERE p.logFile.id = :fileId ORDER BY p.step ASC")
    List<ParsingLog> findByFileIdOrderByStepAsc(Long fileId);
}
