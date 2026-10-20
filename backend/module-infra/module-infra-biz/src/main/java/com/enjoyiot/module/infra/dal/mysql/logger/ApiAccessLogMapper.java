
/*
 *
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
 *  * +----------------------------------------------------------------------
 *
 *  Copyright [2025] [Enjoy-iot] | Tel: 19918996474
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.enjoyiot.module.infra.dal.mysql.logger;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.infra.controller.admin.logger.vo.apiaccesslog.ApiAccessLogPageReqVO;
import com.enjoyiot.module.infra.controller.admin.logger.vo.apiaccesslog.ApiAccessLogStatisticsRespVO;
import com.enjoyiot.module.infra.dal.dataobject.logger.ApiAccessLogDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API 访问日志 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface ApiAccessLogMapper extends BaseMapperX<ApiAccessLogDO> {

    default PageResult<ApiAccessLogDO> selectPage(ApiAccessLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ApiAccessLogDO>()
                .eqIfPresent(ApiAccessLogDO::getUserId, reqVO.getUserId())
                .eqIfPresent(ApiAccessLogDO::getUserType, reqVO.getUserType())
                .eqIfPresent(ApiAccessLogDO::getApplicationName, reqVO.getApplicationName())
                .likeIfPresent(ApiAccessLogDO::getRequestUrl, reqVO.getRequestUrl())
                .betweenIfPresent(ApiAccessLogDO::getBeginTime, reqVO.getBeginTime())
                .geIfPresent(ApiAccessLogDO::getDuration, reqVO.getDuration())
                .eqIfPresent(ApiAccessLogDO::getResultCode, reqVO.getResultCode())
                .orderByDesc(ApiAccessLogDO::getId)
        );
    }

    /**
     * 物理删除指定时间之前的日志
     *
     * @param createTime 最大时间
     * @param limit 删除条数，防止一次删除太多
     * @return 删除条数
     */
    @Delete("DELETE FROM infra_api_access_log WHERE create_time < #{createTime} LIMIT #{limit}")
    Integer deleteByCreateTimeLt(@Param("createTime") LocalDateTime createTime, @Param("limit") Integer limit);

    // ========== 使用统计（超管页面）：统计口径按 create_time 走索引，别改成 begin_time ==========

    @Select("""
            SELECT COUNT(*) AS totalCount, COUNT(DISTINCT user_id) AS userCount, COUNT(DISTINCT request_url) AS urlCount
            FROM infra_api_access_log
            WHERE deleted = 0 AND create_time BETWEEN #{beginTime} AND #{endTime}
            """)
    ApiAccessLogStatisticsRespVO.Summary selectStatisticsSummary(@Param("beginTime") LocalDateTime beginTime,
                                                                 @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT DATE(create_time) AS date, COUNT(*) AS callCount
            FROM infra_api_access_log
            WHERE deleted = 0 AND create_time BETWEEN #{beginTime} AND #{endTime}
            GROUP BY DATE(create_time)
            ORDER BY date
            """)
    List<ApiAccessLogStatisticsRespVO.TrendItem> selectDailyTrend(@Param("beginTime") LocalDateTime beginTime,
                                                                  @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT request_url AS requestUrl, COUNT(*) AS callCount, COUNT(DISTINCT user_id) AS userCount
            FROM infra_api_access_log
            WHERE deleted = 0 AND create_time BETWEEN #{beginTime} AND #{endTime}
            GROUP BY request_url
            ORDER BY callCount DESC
            LIMIT #{limit}
            """)
    List<ApiAccessLogStatisticsRespVO.UrlRankItem> selectUrlRank(@Param("beginTime") LocalDateTime beginTime,
                                                                 @Param("endTime") LocalDateTime endTime,
                                                                 @Param("limit") Integer limit);

    /**
     * 调用最少的接口：只能看到“有调用记录”的接口，0 调用的接口在日志里不存在，统计不到
     */
    @Select("""
            SELECT request_url AS requestUrl, COUNT(*) AS callCount, COUNT(DISTINCT user_id) AS userCount
            FROM infra_api_access_log
            WHERE deleted = 0 AND create_time BETWEEN #{beginTime} AND #{endTime}
            GROUP BY request_url
            ORDER BY callCount ASC, requestUrl
            LIMIT #{limit}
            """)
    List<ApiAccessLogStatisticsRespVO.UrlRankItem> selectUrlRankBottom(@Param("beginTime") LocalDateTime beginTime,
                                                                       @Param("endTime") LocalDateTime endTime,
                                                                       @Param("limit") Integer limit);

    @Select("""
            SELECT user_id AS userId, COUNT(*) AS callCount
            FROM infra_api_access_log
            WHERE deleted = 0 AND user_id IS NOT NULL AND create_time BETWEEN #{beginTime} AND #{endTime}
            GROUP BY user_id
            ORDER BY callCount DESC
            LIMIT #{limit}
            """)
    List<ApiAccessLogStatisticsRespVO.UserRankItem> selectUserRank(@Param("beginTime") LocalDateTime beginTime,
                                                                   @Param("endTime") LocalDateTime endTime,
                                                                   @Param("limit") Integer limit);

}
