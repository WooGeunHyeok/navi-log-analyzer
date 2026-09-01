package com.navi.loganalyzer.backend.domain.parsing.entity;

import com.navi.loganalyzer.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Table(name = "TBL_SYSTEM_LOG_KEYWORD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SystemLogKeyword extends BaseEntity {

    /**
     * Container/JNI_Layer/Java_Layer가 아닌 로그 중 사전 조건 판별에 필요한 줄을 골라내는 키워드 목록.
     * 재배포 없이 이 테이블에 행만 추가하면 다음 파싱부터 바로 반영된다 (재파싱 전까지는 소급 적용 안 됨).
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    // RAW_MESSAGE 안에서 대소문자 구분 없이 찾을 키워드. 짧은 조각("ign")이 아니라
    // "ignition"처럼 구체적인 단어로 등록해야 무관한 단어("design" 등)에 우연히 걸리지 않는다.
    @Column(name = "KEYWORD", nullable = false, unique = true)
    private String keyword;

    // 관리용 설명 (API 응답에는 노출하지 않음). 예: "IGN 상태"
    @Column(name = "LABEL")
    private String label;

    @Column(name = "STATUS", nullable = false)
    private String status;
}
