package com.navi.loganalyzer.backend.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 모든 테이블에서 공통으로 쓰이는 생성일시 (INSDATE), 수정일시(UPDDATE)를 관리하는 Base MappedSuperClass
     */

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime INSDDATE;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime UPPDATE;
}
