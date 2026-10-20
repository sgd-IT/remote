package com.enjoyiot.module.infra.controller.admin.logger.vo.apiaccesslog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - API 使用统计 Response VO")
@Data
public class ApiAccessLogStatisticsRespVO {

    @Schema(description = "汇总指标")
    private Summary summary;

    @Schema(description = "每日调用趋势")
    private List<TrendItem> trend;

    @Schema(description = "接口调用排行")
    private List<UrlRankItem> urlRank;

    @Schema(description = "接口调用最少排行")
    private List<UrlRankItem> urlRankBottom;

    @Schema(description = "用户活跃排行")
    private List<UserRankItem> userRank;

    @Schema(description = "汇总指标")
    @Data
    public static class Summary {

        @Schema(description = "接口调用总次数", example = "1024")
        private Long totalCount;

        @Schema(description = "活跃用户数（去重）", example = "32")
        private Long userCount;

        @Schema(description = "涉及接口数（去重）", example = "47")
        private Long urlCount;

        @Schema(description = "统计窗口内的日均调用次数", example = "146.3")
        private Double dailyAvg;
    }

    @Schema(description = "每日调用趋势项")
    @Data
    public static class TrendItem {

        @Schema(description = "日期", example = "2026-10-19")
        private LocalDate date;

        @Schema(description = "当日调用次数", example = "1024")
        private Long callCount;
    }

    @Schema(description = "接口调用排行项")
    @Data
    public static class UrlRankItem {

        @Schema(description = "请求地址", example = "/admin-api/eiot/firewatch/duty-schedule/page")
        private String requestUrl;

        @Schema(description = "调用次数", example = "1024")
        private Long callCount;

        @Schema(description = "涉及用户数（去重）", example = "8")
        private Long userCount;
    }

    @Schema(description = "用户活跃排行项")
    @Data
    public static class UserRankItem {

        @Schema(description = "用户编号", example = "1")
        private Long userId;

        @Schema(description = "用户昵称", example = "超级管理员")
        private String nickname;

        @Schema(description = "调用次数", example = "1024")
        private Long callCount;
    }
}
