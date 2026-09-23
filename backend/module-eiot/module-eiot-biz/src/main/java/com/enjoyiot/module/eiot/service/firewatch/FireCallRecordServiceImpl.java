package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.exception.ServiceException;
import com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.enums.ErrorCodeConstants;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord.CallRecordPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord.CallRecordRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireCallRecordDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireCallRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * 话单通话记录 Service 实现类
 * <p>
 * 系统租户：查询全部（可按租户筛选）；企业租户：租户拦截器自动隔离，仅看本租户。
 * 录音下载：通过后端代理拉取能力开放平台录音文件，避免浏览器跨域/内网可达性问题。
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireCallRecordServiceImpl implements FireCallRecordService {

    /** 呼叫开始时间格式 YYYYMMDDHH24MISS */
    private static final DateTimeFormatter START_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 录音代理拉取超时（毫秒） */
    private static final int VOICE_FETCH_TIMEOUT = 30_000;

    @Autowired
    private FireCallRecordMapper fireCallRecordMapper;

    @Override
    public PageResult<CallRecordRespVO> pageRecords(CallRecordPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireCallRecordDO> wrapper = new LambdaQueryWrapper<>();
            // 系统租户可按租户筛选；企业租户由租户拦截器自动隔离
            if (isSystemTenant() && pageReqVO.getTenantId() != null) {
                wrapper.eq(FireCallRecordDO::getTenantId, pageReqVO.getTenantId());
            }
            wrapper.eq(StrUtil.isNotBlank(pageReqVO.getMatchPhone()),
                    FireCallRecordDO::getMatchPhone, pageReqVO.getMatchPhone());
            wrapper.like(StrUtil.isNotBlank(pageReqVO.getCallerNbr()),
                    FireCallRecordDO::getCallerNbr, pageReqVO.getCallerNbr());
            wrapper.like(StrUtil.isNotBlank(pageReqVO.getCalledNbr()),
                    FireCallRecordDO::getCalledNbr, pageReqVO.getCalledNbr());
            wrapper.eq(pageReqVO.getCallDirection() != null,
                    FireCallRecordDO::getCallDirection, pageReqVO.getCallDirection());
            wrapper.eq(StrUtil.isNotBlank(pageReqVO.getDisposition()),
                    FireCallRecordDO::getDisposition, pageReqVO.getDisposition());
            wrapper.ge(StrUtil.isNotBlank(pageReqVO.getStartTimeBegin()),
                    FireCallRecordDO::getStartTime, pageReqVO.getStartTimeBegin());
            wrapper.le(StrUtil.isNotBlank(pageReqVO.getStartTimeEnd()),
                    FireCallRecordDO::getStartTime, pageReqVO.getStartTimeEnd());
            wrapper.orderByDesc(FireCallRecordDO::getStartTime);
            wrapper.orderByDesc(FireCallRecordDO::getId);

            PageResult<FireCallRecordDO> pageResult = fireCallRecordMapper.selectPage(pageReqVO, wrapper);
            return BeanUtils.toBean(pageResult, CallRecordRespVO.class);
        });
    }

    @Override
    public CallRecordRespVO getRecord(Long id) {
        return tenantAware(() -> {
            FireCallRecordDO record = fireCallRecordMapper.selectById(id);
            return record == null ? null : BeanUtils.toBean(record, CallRecordRespVO.class);
        });
    }

    @Override
    public Resource downloadVoice(Long id, String side) {
        return tenantAware(() -> {
            FireCallRecordDO record = fireCallRecordMapper.selectById(id);
            if (record == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.CALL_RECORD_NOT_EXISTS);
            }
            String voiceAddress = resolveVoiceAddress(record, side);
            if (StrUtil.isBlank(voiceAddress)) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.CALL_RECORD_NO_VOICE);
            }
            String url = normalizeVoiceUrl(voiceAddress);
            log.info("[downloadVoice] 代理拉取录音, recordId={}, side={}, url={}", id, side, url);
            try {
                HttpResponse response = HttpRequest.get(url)
                        .timeout(VOICE_FETCH_TIMEOUT)
                        .execute();
                if (!response.isOk()) {
                    log.warn("[downloadVoice] 拉取录音失败, recordId={}, side={}, httpStatus={}",
                            id, side, response.getStatus());
                    throw ServiceExceptionUtil.exception(ErrorCodeConstants.CALL_RECORD_VOICE_FETCH_FAILED);
                }
                return new ByteArrayResource(response.bodyBytes()) {
                    @Override
                    public String getFilename() {
                        return buildVoiceFileName(record, side);
                    }
                };
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.error("[downloadVoice] 拉取录音异常, recordId={}, side={}", id, side, e);
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.CALL_RECORD_VOICE_FETCH_FAILED);
            }
        });
    }

    // ==================== 私有方法 ====================

    /**
     * 按侧位解析录音地址
     *
     * @param side mixed=混合 in=主叫 out=被叫
     */
    private String resolveVoiceAddress(FireCallRecordDO record, String side) {
        if (StrUtil.isBlank(side)) {
            return record.getVoiceAddress();
        }
        return switch (side) {
            case "in" -> record.getVoiceAddressIn();
            case "out" -> record.getVoiceAddressOut();
            default -> record.getVoiceAddress();
        };
    }

    /**
     * 归一化录音URL：文档示例为 "xx.xx:xxxx/xx/xxxxx/xxxxxxx.wav"（无协议），补全为 http
     */
    private String normalizeVoiceUrl(String voiceAddress) {
        String address = voiceAddress.trim();
        if (address.startsWith("http://") || address.startsWith("https://")) {
            return address;
        }
        return "http://" + address;
    }

    private String buildVoiceFileName(FireCallRecordDO record, String side) {
        String time = StrUtil.isNotBlank(record.getStartTime())
                ? record.getStartTime() : LocalDateTime.now().format(START_TIME_FORMATTER);
        String phone = StrUtil.isNotBlank(record.getMatchPhone()) ? record.getMatchPhone() : "unknown";
        return "voice_" + phone + "_" + time + "_" + side + ".wav";
    }

    private boolean isSystemTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 1L;
    }

    private <T> T tenantAware(Supplier<T> supplier) {
        if (isSystemTenant()) {
            return TenantUtils.executeIgnoreWithResult(supplier::get);
        }
        return supplier.get();
    }
}
