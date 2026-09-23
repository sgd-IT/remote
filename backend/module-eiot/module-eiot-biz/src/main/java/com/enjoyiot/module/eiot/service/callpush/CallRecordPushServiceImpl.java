package com.enjoyiot.module.eiot.service.callpush;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.callpush.vo.CallRecordPushReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireCallRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyPhoneDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireCallRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyPhoneMapper;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 话单推送接收 Service 实现类
 * <p>
 * 采用与 AEP 回调一致的异步处理模式（@Async + @TenantIgnore），
 * 保证 Controller 立即返回 "success"，不阻塞能力开放平台。
 * <p>
 * 号码匹配与归属规则（参照《话单推送接口文档 V2.6》）：
 * <ul>
 *   <li>先匹配主叫/被叫是否命中值守中心座机，确定座机是主叫还是被叫</li>
 *   <li>再匹配另一方号码是否为某系统用户的手机号（system_users.mobile），
 *       命中则通话记录归属该用户所属租户</li>
 *   <li>未匹配到用户时归属值守中心（系统租户 1）</li>
 *   <li>通话方向以归属方视角判断：归属方是主叫→呼出(1)，归属方是被叫→呼入(2)；
 *       主/被叫均未命中座机→未匹配(0)，归属系统租户</li>
 * </ul>
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class CallRecordPushServiceImpl implements CallRecordPushService {

    /** 系统租户 ID（值守中心，未匹配企业时的兜底归属） */
    private static final Long SYSTEM_TENANT_ID = 1L;

    @Resource
    private FireCallRecordMapper fireCallRecordMapper;

    @Resource
    private FireDutyPhoneMapper fireDutyPhoneMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Async
    @TenantIgnore
    public void pushCallRecord(CallRecordPushReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getSessionid())) {
            log.warn("[话单推送] 缺少必填参数 sessionid，丢弃该话单");
            return;
        }
        try {
            // 1. 幂等校验：sessionid 已存在则跳过
            FireCallRecordDO exist = fireCallRecordMapper.selectOne(
                    FireCallRecordDO::getSessionId, reqVO.getSessionid());
            if (exist != null) {
                log.info("[话单推送] sessionid={} 已存在，忽略重复推送", reqVO.getSessionid());
                return;
            }

            // 2. 号码匹配：确定归属租户与通话方向（以归属方视角）
            MatchResult match = matchCall(reqVO.getCallerNbr(), reqVO.getCalledNbr());
            Long tenantId = match.getTenantId();

            // 3. 构建话单 DO
            FireCallRecordDO record = BeanUtils.toBean(reqVO, FireCallRecordDO.class, vo -> {
                vo.setSessionId(reqVO.getSessionid());
                vo.setMatchPhone(match.getDutyPhone());
                vo.setCallDirection(match.getDirection());
            });

            // 4. 在归属租户上下文中落库（触发租户字段自动填充）
            Long finalTenantId = tenantId;
            TenantUtils.execute(tenantId, () -> {
                try {
                    fireCallRecordMapper.insert(record);
                    log.info("[话单推送] 话单落库成功 sessionid={}, tenantId={}, direction={}, matchPhone={}",
                            record.getSessionId(), finalTenantId, match.getDirection(), match.getDutyPhone());
                } catch (DuplicateKeyException e) {
                    log.info("[话单推送] sessionid={} 唯一键冲突，忽略重复推送", record.getSessionId());
                }
            });
        } catch (Exception e) {
            log.error("[话单推送] 处理异常 sessionid={}", reqVO.getSessionid(), e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 匹配通话归属：
     * <ol>
     *   <li>主叫/被叫命中值守中心座机，确定座机是主叫还是被叫；</li>
     *   <li>另一方号码匹配企业租户物业联系电话（fire_property.contact_phone），
     *       命中则归属该企业租户；</li>
     *   <li>未命中企业则归属值守中心（系统租户 1）；</li>
     *   <li>方向以归属方视角：归属方为主叫→呼出(1)，归属方为被叫→呼入(2)；
     *       主/被叫均未命中座机→未匹配(0)，归属系统租户。</li>
     * </ol>
     * 当前为 @TenantIgnore 上下文，selectList 可查询全部租户数据。
     */
    private MatchResult matchCall(String callerNbr, String calledNbr) {
        List<FireDutyPhoneDO> phones = fireDutyPhoneMapper.selectList(new LambdaQueryWrapper<>());
        if (phones == null || phones.isEmpty()) {
            return new MatchResult(null, SYSTEM_TENANT_ID, 0);
        }
        // 1. 座机是主叫：另一方为被叫
        for (FireDutyPhoneDO phone : phones) {
            if (phoneMatch(phone.getPhoneNumber(), callerNbr)) {
                return matchEnterprise(normalizePhone(callerNbr), calledNbr, false);
            }
        }
        // 2. 座机是被叫：另一方为主叫
        for (FireDutyPhoneDO phone : phones) {
            if (phoneMatch(phone.getPhoneNumber(), calledNbr)) {
                return matchEnterprise(normalizePhone(calledNbr), callerNbr, true);
            }
        }
        // 3. 主/被叫均未命中座机：未匹配，归属系统租户
        return new MatchResult(null, SYSTEM_TENANT_ID, 0);
    }

    /**
     * 座机已命中后，匹配另一方号码归属的用户（system_users.mobile），
     * 命中则归属该用户所属租户，未命中归属值守中心（系统租户）。
     *
     * @param dutyPhone        归一化后的值守中心座机号码
     * @param otherNbr         另一方号码（对方号码）
     * @param otherIsCaller    另一方是否为主叫（座机为主叫时另一方为被叫）
     */
    private MatchResult matchEnterprise(String dutyPhone, String otherNbr, boolean otherIsCaller) {
        AdminUserRespDTO user = matchUser(otherNbr);
        if (user != null && user.getTenantId() != null) {
            // 命中用户：方向以归属方视角判断（归属方为主叫→呼出，归属方为被叫→呼入）
            int direction = otherIsCaller ? 1 : 2;
            return new MatchResult(dutyPhone, user.getTenantId(), direction);
        }
        // 未命中用户：归属值守中心（系统租户），方向以座机视角判断（座机为主叫→呼出，座机为被叫→呼入）
        int direction = otherIsCaller ? 2 : 1;
        return new MatchResult(dutyPhone, SYSTEM_TENANT_ID, direction);
    }

    /**
     * 匹配号码对应的系统用户：优先按归一化号码精确匹配，
     * 未命中且号码长于 11 位时，再尝试用后 11 位（手机号）匹配，兼容带区号场景。
     */
    private AdminUserRespDTO matchUser(String otherNbr) {
        String normalized = normalizePhone(otherNbr);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUserByMobile(normalized);
        if (user == null && normalized.length() > 11) {
            user = adminUserApi.getUserByMobile(normalized.substring(normalized.length() - 11));
        }
        return user;
    }

    /**
     * 号码归一化：去除空格、横线、括号、+86/86 前缀
     */
    private String normalizePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return null;
        }
        String p = phone.trim().replaceAll("[\\s\\-()（）]", "");
        if (p.startsWith("+86")) {
            p = p.substring(3);
        } else if (p.startsWith("86") && p.length() > 11) {
            p = p.substring(2);
        }
        return p;
    }

    /**
     * 号码匹配：归一化后完全相等；或一方为另一方后缀（座机未录入区号场景，后缀长度至少 7 位防止误匹配）
     */
    private boolean phoneMatch(String dutyPhone, String callNbr) {
        String a = normalizePhone(dutyPhone);
        String b = normalizePhone(callNbr);
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        int minLen = Math.min(a.length(), b.length());
        if (minLen >= 7 && (a.endsWith(b) || b.endsWith(a))) {
            return true;
        }
        return false;
    }

    /**
     * 匹配结果
     */
    @Data
    @AllArgsConstructor
    private static class MatchResult {

        /** 匹配到的值守中心座机号码（归一化，未匹配为 null） */
        private String dutyPhone;

        /** 归属租户 ID（企业租户或系统租户） */
        private Long tenantId;

        /** 通话方向（以归属方视角）：0=未匹配 1=呼出 2=呼入 */
        private Integer direction;
    }
}
