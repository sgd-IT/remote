package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 交接班记录 Mapper（表 fire_handover_record）
 *
 * 注意：
 * - 历史代码中的 FireDutyRecordMapper 指向的 fire_duty_record 表在数据库中并不存在（存量缺陷，勿参照）；
 * - 本表无 property_id 列：物业下钻时只能按租户级（联网单位）统计，与告警/设备的近似口径一致；
 * - 租户过滤由租户拦截器自动追加 tenant_id。
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireHandoverRecordMapper {

    /**
     * 指定时间范围内交接班记录数（handover_time 左闭右开）
     */
    @Select("SELECT COUNT(*) FROM fire_handover_record WHERE deleted = 0 AND handover_time >= #{start} AND handover_time < #{end}")
    Integer countInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
