package com.enjoyiot.module.infra.service.logger;

import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.infra.controller.admin.logger.vo.apiaccesslog.ApiAccessLogStatisticsRespVO;
import com.enjoyiot.module.infra.dal.mysql.logger.ApiAccessLogMapper;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API 使用统计 Service 实现类
 */
@Service
@Validated
public class ApiAccessLogStatisticsServiceImpl implements ApiAccessLogStatisticsService {

    /** 页面不传时间时默认看最近 7 天 */
    private static final int DEFAULT_DAYS = 7;
    /** 排行榜默认条数，上限防止一次拉太多拖慢查询 */
    private static final int DEFAULT_TOP = 20;
    private static final int MAX_TOP = 100;

    @Resource
    private ApiAccessLogMapper apiAccessLogMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @TenantIgnore // 超管要看全租户的调用量；不加会因租户插件只剩自己租户的数据
    public ApiAccessLogStatisticsRespVO getStatistics(LocalDateTime beginTime, LocalDateTime endTime, Integer top) {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        LocalDateTime begin = beginTime != null ? beginTime : end.minusDays(DEFAULT_DAYS);
        int limit = top == null || top <= 0 ? DEFAULT_TOP : Math.min(top, MAX_TOP);

        ApiAccessLogStatisticsRespVO resp = new ApiAccessLogStatisticsRespVO();
        ApiAccessLogStatisticsRespVO.Summary summary = apiAccessLogMapper.selectStatisticsSummary(begin, end);
        if (summary == null) {
            summary = new ApiAccessLogStatisticsRespVO.Summary();
        }
        // 日均按自然天数算，最少算 1 天：查当天时 between 为 0 天，直接除会除零
        long days = Math.max(ChronoUnit.DAYS.between(begin.toLocalDate(), end.toLocalDate()) + 1, 1);
        summary.setDailyAvg(summary.getTotalCount() == null ? 0D
                : Math.round(summary.getTotalCount() * 10.0 / days) / 10.0);
        resp.setSummary(summary);
        resp.setTrend(apiAccessLogMapper.selectDailyTrend(begin, end));
        resp.setUrlRank(apiAccessLogMapper.selectUrlRank(begin, end, limit));
        resp.setUrlRankBottom(apiAccessLogMapper.selectUrlRankBottom(begin, end, limit));

        List<ApiAccessLogStatisticsRespVO.UserRankItem> userRank = apiAccessLogMapper.selectUserRank(begin, end, limit);
        fillNicknames(userRank);
        resp.setUserRank(userRank);
        return resp;
    }

    /**
     * 用户排行只存了 userId，这里批量换昵称；查不到（用户被删）就留空，前端兜底显示编号
     */
    private void fillNicknames(List<ApiAccessLogStatisticsRespVO.UserRankItem> userRank) {
        if (userRank == null || userRank.isEmpty()) {
            return;
        }
        Set<Long> userIds = userRank.stream()
                .map(ApiAccessLogStatisticsRespVO.UserRankItem::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        userRank.forEach(item -> {
            AdminUserRespDTO user = userMap.get(item.getUserId());
            item.setNickname(user != null ? user.getNickname() : null);
        });
    }

}
