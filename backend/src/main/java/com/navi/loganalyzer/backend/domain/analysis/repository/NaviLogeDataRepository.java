package com.navi.loganalyzer.backend.domain.analysis.repository;

import com.navi.loganalyzer.backend.domain.analysis.entity.NaviLogeDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NaviLogeDataRepository extends JpaRepository<NaviLogeDataEntity, Long> {

    /**
     * [CASE 1] 함수명(Suffix) + 라인번호 매칭 후보군
     */
    @Query("SELECT n FROM NaviLogeDataEntity n " +
            "WHERE (n.functionName = :funcName OR n.functionName LIKE CONCAT('%::', :funcName)) " +
            "AND n.lineNumber = :lineNum")
    List<NaviLogeDataEntity> findCandidatesByFuncAndLine(@Param("funcName") String funcName,
                                                         @Param("lineNum") Integer lineNum);

    /**
     * [CASE 2] 함수명(Suffix) 단독 매칭 후보군
     */
    @Query("SELECT n FROM NaviLogeDataEntity n " +
            "WHERE n.functionName = :funcName OR n.functionName LIKE CONCAT('%::', :funcName)")
    List<NaviLogeDataEntity> findCandidatesByFunc(@Param("funcName") String funcName);

    /**
     * [CASE 3] 키워드로 RAW_MESSAGE/PATTERN_MESSAGE 시작부분 LIKE 매칭 후보군
     */
    @Query("SELECT n FROM NaviLogeDataEntity n " +
            "WHERE n.rawMessage LIKE CONCAT(:keyword, '%') " +
            "OR n.patternMessage LIKE CONCAT('%', :keyword, '%')")
    List<NaviLogeDataEntity> findCandidatesByKeyword(@Param("keyword") String keyword);
}
