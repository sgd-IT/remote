package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.enums.ErrorCodeConstants;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhonePageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneUpdateReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireDutyPhoneDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireDutyPhoneMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.function.Supplier;

/**
 * 值守中心座机号码 Service 实现类
 * <p>
 * 值守中心座机归属固定为系统租户（tenantId = 1）；
 * 仅系统租户可录入/维护座机号码。
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireDutyPhoneServiceImpl implements FireDutyPhoneService {

    /** 系统租户 ID（值守中心座机固定归属） */
    private static final Long SYSTEM_TENANT_ID = 1L;

    @Resource
    private FireDutyPhoneMapper fireDutyPhoneMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPhone(DutyPhoneCreateReqVO reqVO) {
        if (!isSystemTenant()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.DUTY_PHONE_NOT_SYSTEM_TENANT);
        }
        return TenantUtils.executeIgnoreWithResult(() -> {
            // 座机号码全局唯一（座机归属固定为系统租户）
            FireDutyPhoneDO exist = fireDutyPhoneMapper.selectOne(
                    FireDutyPhoneDO::getPhoneNumber, reqVO.getPhoneNumber().trim());
            if (exist != null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.DUTY_PHONE_EXISTS);
            }
            FireDutyPhoneDO phone = FireDutyPhoneDO.builder()
                    .phoneNumber(reqVO.getPhoneNumber().trim())
                    .name(reqVO.getName())
                    .remark(reqVO.getRemark())
                    .build();
            phone.setTenantId(SYSTEM_TENANT_ID);
            fireDutyPhoneMapper.insert(phone);
            log.info("[createPhone] 创建座机号码成功，号码：{}，归属系统租户", phone.getPhoneNumber());
            return phone.getId();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhone(DutyPhoneUpdateReqVO reqVO) {
        if (!isSystemTenant()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.DUTY_PHONE_NOT_SYSTEM_TENANT);
        }
        TenantUtils.executeIgnore(() -> {
            FireDutyPhoneDO exist = fireDutyPhoneMapper.selectById(reqVO.getId());
            if (exist == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.DUTY_PHONE_NOT_EXISTS);
            }
            // 座机号码全局唯一（排除自身）
            FireDutyPhoneDO samePhone = fireDutyPhoneMapper.selectOne(
                    FireDutyPhoneDO::getPhoneNumber, reqVO.getPhoneNumber().trim());
            if (samePhone != null && !samePhone.getId().equals(reqVO.getId())) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.DUTY_PHONE_EXISTS);
            }
            FireDutyPhoneDO updateObj = FireDutyPhoneDO.builder()
                    .id(reqVO.getId())
                    .phoneNumber(reqVO.getPhoneNumber().trim())
                    .name(reqVO.getName())
                    .remark(reqVO.getRemark())
                    .build();
            updateObj.setTenantId(SYSTEM_TENANT_ID);
            fireDutyPhoneMapper.updateById(updateObj);
            log.info("[updatePhone] 更新座机号码成功，ID：{}", reqVO.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhone(Long id) {
        if (!isSystemTenant()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.DUTY_PHONE_NOT_SYSTEM_TENANT);
        }
        TenantUtils.executeIgnore(() -> {
            FireDutyPhoneDO exist = fireDutyPhoneMapper.selectById(id);
            if (exist == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.DUTY_PHONE_NOT_EXISTS);
            }
            fireDutyPhoneMapper.deleteById(id);
            log.info("[deletePhone] 删除座机号码成功，ID：{}", id);
        });
    }

    @Override
    public DutyPhoneRespVO getPhone(Long id) {
        return tenantAware(() -> {
            FireDutyPhoneDO phone = fireDutyPhoneMapper.selectById(id);
            return phone == null ? null : BeanUtils.toBean(phone, DutyPhoneRespVO.class);
        });
    }

    @Override
    public PageResult<DutyPhoneRespVO> pagePhones(DutyPhonePageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireDutyPhoneDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(pageReqVO.getPhoneNumber() != null && !pageReqVO.getPhoneNumber().isEmpty(),
                    FireDutyPhoneDO::getPhoneNumber, pageReqVO.getPhoneNumber());
            wrapper.like(pageReqVO.getName() != null && !pageReqVO.getName().isEmpty(),
                    FireDutyPhoneDO::getName, pageReqVO.getName());
            wrapper.orderByDesc(FireDutyPhoneDO::getCreateTime);

            PageResult<FireDutyPhoneDO> pageResult = fireDutyPhoneMapper.selectPage(pageReqVO, wrapper);
            return BeanUtils.toBean(pageResult, DutyPhoneRespVO.class);
        });
    }

    @Override
    public List<DutyPhoneRespVO> listPhones() {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireDutyPhoneDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(FireDutyPhoneDO::getCreateTime);
            return BeanUtils.toBean(fireDutyPhoneMapper.selectList(wrapper), DutyPhoneRespVO.class);
        });
    }

    // ==================== 私有方法 ====================

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
