package com.navi.loganalyzer.batch.job;

import com.navi.loganalyzer.batch.entity.NaviLogeData;
import com.navi.loganalyzer.batch.job.dto.NaviLogeDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NaviLogeDataProcessor implements ItemProcessor<NaviLogeDataDto, NaviLogeData> {

    @Override
    public NaviLogeData process(NaviLogeDataDto dto) throws Exception {
        if (dto.getFileName() == null || dto.getRawMessage() == null) {
            log.error("필수 로그 정보 누락으로 건너뜁니다 : {}", dto);
            return null;
        }

        // DTO -> Entity 변환
        return dto.toEntity();
    }
}
