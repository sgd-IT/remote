package com.enjoyiot.module.infra.service.logger;

import com.enjoyiot.module.infra.controller.admin.logger.vo.apiaccesslog.ApiAccessLogStatisticsRespVO;

import java.time.LocalDateTime;

/**
 * API 使用统计 Service 接口
 *
 * 统计口径是"接口调用次数"，数据源就是 API 访问日志本身；
 * 纯前端行为（不发请求的点击）统计不到，需要埋点表另行采集。
 */
public interface ApiAccessLogStatisticsService {

    /**
     * 获得使用统计：汇总 + 每日趋势 + 接口排行 + 用户排行
     *
     * @param beginTime 开始时间，为空默认最近 7 天
     * @param endTime   结束时间，为空默认当前时间
     * @param top       排行榜条数，默认 20、上限 100
     */
    ApiAccessLogStatisticsRespVO getStatistics(LocalDateTime beginTime, LocalDateTime endTime, Integer top);

}
