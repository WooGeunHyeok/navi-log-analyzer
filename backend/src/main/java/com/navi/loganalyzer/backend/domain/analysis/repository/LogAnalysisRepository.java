package com.navi.loganalyzer.backend.domain.analysis.repository;

import com.navi.loganalyzer.backend.domain.analysis.entity.LogAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogAnalysisRepository extends JpaRepository<LogAnalysis, Long> {

    // 저장된 분석 결과를 step 순으로 조회 (조회 API용 - 재매칭 없이 depth만으로 트리 재조립)
    List<LogAnalysis> findByFileIdOrderByStepAsc(Long fileId);

    // 재분석 시 기존 결과를 지우고 새로 저장하기 위한 삭제
    void deleteByFileId(Long fileId);
}
