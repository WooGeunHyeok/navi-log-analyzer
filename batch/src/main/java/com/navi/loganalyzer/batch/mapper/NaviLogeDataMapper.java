package com.navi.loganalyzer.batch.mapper;

import com.navi.loganalyzer.batch.entity.NaviLogeData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NaviLogeDataMapper {

    // 단건 저장
    int insertNaviLogeData(NaviLogeData naviLogData);

    // 배치 처리용 다건 저장 (Bulk Insert)
    int insertNaviLogeDataList(@Param("list") List<NaviLogeData> naviLogeDataList);


    // 배치 스캔에 걸리지 않은 고아 LOGE DELETED 처리
    int updateOrphanLogsToDeleted(@Param("jobExecutionTime")LocalDateTime jobExecutionTime);
}