package com.navi.loganalyzer.batch.job;

import com.navi.loganalyzer.batch.entity.NaviLogeData;
import com.navi.loganalyzer.batch.mapper.NaviLogeDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaviLogeDataWriter implements ItemWriter<NaviLogeData> {

    private final NaviLogeDataMapper naviLogeDataMapper;

    @Override
    public void write(Chunk<? extends NaviLogeData> chunk) throws Exception {
        if (!chunk.isEmpty()) {
            log.info("내비-로그-파싱 Data {}건 Bulk Insert/Update 실행", chunk.size());
            naviLogeDataMapper.insertNaviLogeDataList((java.util.List<NaviLogeData>) chunk.getItems());
        }
    }
}
