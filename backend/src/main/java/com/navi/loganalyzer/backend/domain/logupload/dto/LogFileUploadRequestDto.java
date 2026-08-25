package com.navi.loganalyzer.backend.domain.logupload.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogFileUploadRequestDto {

    /**
     * 로그 파일 Upload Request Dto
     */

    @NotBlank(message = "로그 제목은 필수 입력값입니다.")
    private String title;                               // 로그 제목
    private String jiraTicketKey;                       // 티켓 번호 (예 : INDIAPIOUP-4922)
}
