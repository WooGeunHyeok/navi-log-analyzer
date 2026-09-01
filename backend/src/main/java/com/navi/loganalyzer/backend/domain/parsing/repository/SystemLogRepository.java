package com.navi.loganalyzer.backend.domain.parsing.repository;

import com.navi.loganalyzer.backend.domain.parsing.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    // 필요시 특정 파일의 시스템 로그 일괄 삭제 (재파싱 시)
    void deleteByLogFileId(Long logFileId);

    @Query("SELECT s FROM SystemLog s WHERE s.logFile.id = :fileId ORDER BY s.timestamp ASC")
    List<SystemLog> findByFileIdOrderByTimestampAsc(Long fileId);
}
