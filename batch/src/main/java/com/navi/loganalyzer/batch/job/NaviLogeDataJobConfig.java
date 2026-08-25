package com.navi.loganalyzer.batch.job;

import com.navi.loganalyzer.batch.entity.NaviLogeData;
import com.navi.loganalyzer.batch.job.dto.NaviLogeDataDto;
import com.navi.loganalyzer.batch.mapper.NaviLogeDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NaviLogeDataJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final JsonItemReader<NaviLogeDataDto> reader;
    private final NaviLogeDataProcessor processor;
    private final NaviLogeDataWriter writer;
    private final NaviLogeDataMapper mapper;

    @Bean
    public Job naviLogeDataJob(Step naviLogeDataStep, Step updateOrphanLogeStep) {
        return new JobBuilder("naviLogeDataJob", jobRepository)
                // 1. JSON 파일 읽어서 INSERT/UPDATE
                .start(naviLogeDataStep)

                // 2. 고아 LOGE STATUS -> DELETED 처리
                .next(updateOrphanLogeStep)
                .build();
    }

    @Bean
    public Step naviLogeDataStep() {
        return new StepBuilder("naviLogeDataStep", jobRepository)
                .<NaviLogeDataDto, NaviLogeData>chunk(1000)
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Step updateOrphanLogeStep(Tasklet updateOrphanLogsTasklet) {
        return new StepBuilder("updateOrphanLogeStep", jobRepository)
                .tasklet(updateOrphanLogsTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet updateOrphanLogsTasklet(@Value("#{jobParameters['fileName']}") String jobExecutionTimeStr) {
        return (contribution, chunkContext) -> {
            log.info("고아 로그 STATUS -> DELETED 상태 변경 작업 시작");

            LocalDateTime jobExecutionTime = (jobExecutionTimeStr != null)
                    ? LocalDateTime.parse(jobExecutionTimeStr)
                    : LocalDateTime.now().minusMinutes(5);

            int updatedCount = mapper.updateOrphanLogsToDeleted(jobExecutionTime);
            log.info("총 {}건의 DELETED 상태로 변경", updatedCount);

            return RepeatStatus.FINISHED;
        };
    }
}
