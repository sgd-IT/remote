package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractUpdateReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenanceContractDO;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireMaintenanceContractMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * 维保合同管理 Service 实现类
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireMaintenanceContractServiceImpl implements FireMaintenanceContractService {

    @Resource
    private FireMaintenanceContractMapper fireMaintenanceContractMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContract(ContractCreateReqVO reqVO) {
        log.info("[createContract] ===== 开始创建维保合同 ===== 请求参数: contractNo={}, contractName={}, propertyId={}, propertyName={}, maintenanceUnit={}, maintenanceContact={}, maintenancePhone={}, startDate={}, endDate={}, contractAmount={}, contractFile={}, remark={}",
                reqVO.getContractNo(), reqVO.getContractName(), reqVO.getPropertyId(), reqVO.getPropertyName(),
                reqVO.getMaintenanceUnit(), reqVO.getMaintenanceContact(), reqVO.getMaintenancePhone(),
                reqVO.getStartDate(), reqVO.getEndDate(), reqVO.getContractAmount(), reqVO.getContractFile(), reqVO.getRemark());
        try {
            FireMaintenanceContractDO contract = FireMaintenanceContractDO.builder()
                    .contractNo(reqVO.getContractNo())
                    .contractName(reqVO.getContractName())
                    .propertyId(reqVO.getPropertyId())
                    .propertyName(reqVO.getPropertyName())
                    .maintenanceUnit(reqVO.getMaintenanceUnit())
                    .maintenanceContact(reqVO.getMaintenanceContact())
                    .maintenancePhone(reqVO.getMaintenancePhone())
                    .startDate(reqVO.getStartDate())
                    .endDate(reqVO.getEndDate())
                    .contractAmount(reqVO.getContractAmount())
                    .contractStatus("valid")
                    .remark(reqVO.getRemark())
                    .contractFile(reqVO.getContractFile())
                    .build();

            Long tenantId = TenantContextHolder.getTenantId();
            log.info("[createContract] 租户ID: {}, 待插入数据: {}", tenantId, contract);
            contract.setTenantId(tenantId);

            int insertResult = fireMaintenanceContractMapper.insert(contract);
            log.info("[createContract] 数据库insert结果: {}, 生成ID: {}, 合同编号: {}", insertResult, contract.getId(), contract.getContractNo());

            return contract.getId();
        } catch (Exception e) {
            log.error("[createContract] ===== 创建维保合同异常 ===== reqVO={}", reqVO, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(ContractUpdateReqVO reqVO) {
        tenantAware(() -> {
            FireMaintenanceContractDO existContract = fireMaintenanceContractMapper.selectById(reqVO.getId());
            if (existContract == null) {
                throw new IllegalArgumentException("合同不存在");
            }

            FireMaintenanceContractDO updateObj = FireMaintenanceContractDO.builder()
                    .id(reqVO.getId())
                    .contractNo(reqVO.getContractNo())
                    .contractName(reqVO.getContractName())
                    .propertyId(reqVO.getPropertyId())
                    .propertyName(reqVO.getPropertyName())
                    .maintenanceUnit(reqVO.getMaintenanceUnit())
                    .maintenanceContact(reqVO.getMaintenanceContact())
                    .maintenancePhone(reqVO.getMaintenancePhone())
                    .startDate(reqVO.getStartDate())
                    .endDate(reqVO.getEndDate())
                    .contractAmount(reqVO.getContractAmount())
                    .contractStatus(reqVO.getContractStatus())
                    .remark(reqVO.getRemark())
                    .contractFile(reqVO.getContractFile())
                    .build();

            fireMaintenanceContractMapper.updateById(updateObj);

            log.info("[updateContract] 更新维保合同成功，合同ID：{}", reqVO.getId());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        tenantAware(() -> {
            FireMaintenanceContractDO existContract = fireMaintenanceContractMapper.selectById(id);
            if (existContract == null) {
                throw new IllegalArgumentException("合同不存在");
            }

            fireMaintenanceContractMapper.deleteById(id);

            log.info("[deleteContract] 删除维保合同成功，合同ID：{}", id);
        });
    }

    @Override
    public ContractRespVO getContract(Long id) {
        return tenantAware(() -> {
            FireMaintenanceContractDO contract = fireMaintenanceContractMapper.selectById(id);
            if (contract == null) {
                return null;
            }
            return convertToRespVO(contract);
        });
    }

    @Override
    public PageResult<ContractRespVO> pageContracts(ContractPageReqVO pageReqVO) {
        return tenantAware(() -> {
            LambdaQueryWrapper<FireMaintenanceContractDO> wrapper = new LambdaQueryWrapper<>();

            wrapper.like(pageReqVO.getContractNo() != null, FireMaintenanceContractDO::getContractNo, pageReqVO.getContractNo());
            wrapper.like(pageReqVO.getContractName() != null, FireMaintenanceContractDO::getContractName, pageReqVO.getContractName());
            wrapper.like(pageReqVO.getMaintenanceUnit() != null, FireMaintenanceContractDO::getMaintenanceUnit, pageReqVO.getMaintenanceUnit());
            wrapper.eq(pageReqVO.getContractStatus() != null, FireMaintenanceContractDO::getContractStatus, pageReqVO.getContractStatus());
            wrapper.like(pageReqVO.getPropertyName() != null, FireMaintenanceContractDO::getPropertyName, pageReqVO.getPropertyName());
            wrapper.ge(pageReqVO.getCreateTimeStart() != null, FireMaintenanceContractDO::getCreateTime, pageReqVO.getCreateTimeStart());
            wrapper.le(pageReqVO.getCreateTimeEnd() != null, FireMaintenanceContractDO::getCreateTime, pageReqVO.getCreateTimeEnd());

            wrapper.orderByDesc(FireMaintenanceContractDO::getCreateTime);

            PageResult<FireMaintenanceContractDO> pageResult = fireMaintenanceContractMapper.selectPage(pageReqVO, wrapper);

            List<ContractRespVO> list = new ArrayList<>();
            for (FireMaintenanceContractDO contract : pageResult.getList()) {
                list.add(convertToRespVO(contract));
            }

            return new PageResult<>(list, pageResult.getTotal());
        });
    }

    @Override
    public List<ContractRespVO> getExpiringContracts() {
        return tenantAware(() -> {
            List<FireMaintenanceContractDO> list;
            if (isSystemTenant()) {
                list = fireMaintenanceContractMapper.selectAllExpiringContracts();
            } else {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                list = fireMaintenanceContractMapper.selectExpiringContracts(tenantId);
            }

            List<ContractRespVO> result = new ArrayList<>();
            for (FireMaintenanceContractDO contract : list) {
                result.add(convertToRespVO(contract));
            }
            return result;
        });
    }

    @Override
    public Integer countExpiring() {
        return tenantAware(() -> {
            if (isSystemTenant()) {
                return fireMaintenanceContractMapper.countAllExpiringContracts();
            } else {
                Long tenantId = TenantContextHolder.getRequiredTenantId();
                return fireMaintenanceContractMapper.countExpiringContracts(tenantId);
            }
        });
    }

    // ==================== 私有方法 ====================

    /**
     * 生成合同编号：CT-yyyyMMdd-xxxx
     */
    private String generateContractNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = IdUtil.randomUUID().substring(0, 4).toUpperCase();
        return "CT-" + datePart + "-" + randomPart;
    }

    /**
     * 获取合同状态名称
     */
    private String getContractStatusName(String contractStatus) {
        Map<String, String> map = new HashMap<>();
        map.put("valid", "有效");
        map.put("expired", "已到期");
        map.put("renewing", "续签中");
        return map.getOrDefault(contractStatus, contractStatus);
    }

    /**
     * 转换为响应VO
     */
    private ContractRespVO convertToRespVO(FireMaintenanceContractDO contract) {
        ContractRespVO vo = new ContractRespVO();
        vo.setId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setContractName(contract.getContractName());
        vo.setPropertyId(contract.getPropertyId());
        vo.setPropertyName(contract.getPropertyName());
        vo.setMaintenanceUnit(contract.getMaintenanceUnit());
        vo.setMaintenanceContact(contract.getMaintenanceContact());
        vo.setMaintenancePhone(contract.getMaintenancePhone());
        vo.setStartDate(contract.getStartDate());
        vo.setEndDate(contract.getEndDate());
        vo.setContractAmount(contract.getContractAmount());
        vo.setContractStatus(contract.getContractStatus());
        vo.setContractStatusName(getContractStatusName(contract.getContractStatus()));
        vo.setRemark(contract.getRemark());
        vo.setContractFile(contract.getContractFile());
        vo.setCreateTime(contract.getCreateTime());
        return vo;
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

    /** 无返回值的 tenantAware，用于写操作 */
    private void tenantAware(Runnable runnable) {
        if (isSystemTenant()) {
            TenantUtils.executeIgnore(runnable);
        } else {
            runnable.run();
        }
    }
}
