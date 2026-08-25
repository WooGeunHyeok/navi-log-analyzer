package com.navi.loganalyzer.batch.job;

import com.navi.loganalyzer.batch.job.dto.NaviLogeDataDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class NaviLogeDataReader {

    /**
     * JSON 파일을 읽어 NaviLogeDataDto 객체로 파싱하는 ItemReader
     * --filePath="C:/path/to/logs.json 배치 파라미터로 동작
     */
    @Bean
    @StepScope
    public JsonItemReader<NaviLogeDataDto> naviLogeDataDtoJsonItemReader (
            @Value("#{jobParameters['filePath']}") String filePath) {

        // Jackson 기반 레코드 리더 생성 및 ObjectMapper 바인딩
        JacksonJsonObjectReader<NaviLogeDataDto> recordReader =
                new JacksonJsonObjectReader<>(NaviLogeDataDto.class);

        return new JsonItemReaderBuilder<NaviLogeDataDto>()
                .name("naviLogeJsonReader")
                .resource(new FileSystemResource(filePath))
                .jsonObjectReader(recordReader)
                .build();
    }
}
