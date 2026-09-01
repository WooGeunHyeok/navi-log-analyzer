package com.navi.loganalyzer.backend.domain.parsing.repository;

import com.navi.loganalyzer.backend.domain.parsing.entity.SystemLogKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemLogKeywordRepository extends JpaRepository<SystemLogKeyword, Long> {

    List<SystemLogKeyword> findAllByStatus(String status);
}
