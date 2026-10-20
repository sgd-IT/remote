package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.api.enums.ErrorCodeConstants;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space.FireSpaceDTO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space.FireSpaceUpdateDTO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireSpaceDO;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FireSpaceMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 空间管理 Service 实现类
 * <p>
 * 数据按当前租户隔离；同一租户下 楼栋/楼层/区域/详细位置 组合不可重复。
 *
 * @author sgd
 */
@Slf4j
@Service
public class FireSpaceServiceImpl implements FireSpaceService {

    @Resource
    private FireSpaceMapper fireSpaceMapper;

    @Resource
    private EiotDeviceInfoMapper deviceInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFireSpace(FireSpaceDTO fireSpaceDTO) {
        //获取当前租户id
        Long tenantId = TenantContextHolder.getTenantId();

        // 获取楼栋、楼层、区域、详细位置
        // 对值进行空格处理
        String building = fireSpaceDTO.getBuilding().trim();
        String floor = fireSpaceDTO.getFloor().trim();
        String area = fireSpaceDTO.getArea() == null ? "" : fireSpaceDTO.getArea().trim();
        String position = fireSpaceDTO.getPosition() == null ? "" : fireSpaceDTO.getPosition().trim();

        // 组装校验数据
        LambdaQueryWrapper<FireSpaceDO> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(FireSpaceDO::getTenantId, tenantId)
                .eq(FireSpaceDO::getBuilding, building)
                .eq(FireSpaceDO::getFloor, floor)
                .eq(FireSpaceDO::getArea, area)
                .eq(FireSpaceDO::getPosition, position);

        // 查询是否存在重复的空间
        if (fireSpaceMapper.selectCount(checkWrapper) > 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.SPACE_EXISTS);
        }

        //组装空间信息
        FireSpaceDO space = FireSpaceDO.builder()
                .building(building)
                .floor(floor)
                .area(area)
                .position(position)
                .remark(fireSpaceDTO.getRemark())
                .build();
        space.setTenantId(tenantId);
        // 插入空间信息
        fireSpaceMapper.insert(space);

        log.info("[createFireSpace] 创建空间成功，ID：{}，楼栋：{}，楼层：{}，区域：{}，详细位置：{}", space.getId(), building, floor, area, position);
        return space.getId();
    }

    /**
     * 更新空间信息（保持原租户不变；同组合不可重复）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFireSpace(FireSpaceUpdateDTO updateDTO) {
        // 查询要更新的空间
        FireSpaceDO space = fireSpaceMapper.selectById(updateDTO.getId());
        if (space == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.SPACE_NOT_EXISTS);
        }

        // 对值进行空格处理
        String building = updateDTO.getBuilding().trim();
        String floor = updateDTO.getFloor().trim();
        String area = updateDTO.getArea() == null ? "" : updateDTO.getArea().trim();
        String position = updateDTO.getPosition() == null ? "" : updateDTO.getPosition().trim();

        // 组装校验数据（排除自身）
        LambdaQueryWrapper<FireSpaceDO> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(FireSpaceDO::getTenantId, space.getTenantId())
                .eq(FireSpaceDO::getBuilding, building)
                .eq(FireSpaceDO::getFloor, floor)
                .eq(FireSpaceDO::getArea, area)
                .eq(FireSpaceDO::getPosition, position)
                .ne(FireSpaceDO::getId, space.getId());
        if (fireSpaceMapper.selectCount(checkWrapper) > 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.SPACE_EXISTS);
        }

        // 按 id 更新
        FireSpaceDO updateObj = FireSpaceDO.builder()
                .building(building)
                .floor(floor)
                .area(area)
                .position(position)
                .remark(updateDTO.getRemark())
                .build();
        updateObj.setId(space.getId());
        fireSpaceMapper.updateById(updateObj);

        log.info("[updateFireSpace] 更新空间成功，ID：{}，楼栋：{}，楼层：{}，区域：{}，详细位置：{}", space.getId(), building, floor, area, position);
    }

    /**
     * 删除空间信息（存在设备引用时不允许删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFireSpace(Long id) {
        // 查询要删除的空间
        FireSpaceDO space = fireSpaceMapper.selectById(id);
        if (space == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.SPACE_NOT_EXISTS);
        }

        // 如果当前空间id被设备使用，无法删除
        Long deviceCount = deviceInfoMapper.selectCount(
                new LambdaQueryWrapper<EiotDeviceInfoDO>().eq(EiotDeviceInfoDO::getSpaceId, id));
        if (deviceCount > 0) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.SPACE_USED_BY_DEVICE);
        }

        // 物理删除（软删会导致同组合无法重新录入）
        fireSpaceMapper.deleteByIdPhysically(id);

        log.info("[deleteFireSpace] 删除空间成功，ID：{}，楼栋：{}，楼层：{}，区域：{}，详细位置：{}", space.getId(), space.getBuilding(), space.getFloor(), space.getArea(), space.getPosition());
    }

    /**
     * 查询空间列表（不分页，按录入顺序）
     * <p>
     * 系统租户管理员：tenantId 为空查全部，否则查指定租户；企业租户：仅本租户
     *
     * @param tenantId 目标租户（仅系统租户管理员生效）
     */
    @Override
    public List<FireSpaceDO> listFireSpace(Long tenantId) {
        LambdaQueryWrapper<FireSpaceDO> wrapper = new LambdaQueryWrapper<>();

        if (isSystemTenant()) {
            // 系统租户管理员：带 tenantId 查指定租户，不带查全部
            if (tenantId != null) {
                wrapper.eq(FireSpaceDO::getTenantId, tenantId);
            }
        } else {
            // 企业租户：只看本租户
            wrapper.eq(FireSpaceDO::getTenantId, TenantContextHolder.getTenantId());
        }
        wrapper.orderByAsc(FireSpaceDO::getId);

        return fireSpaceMapper.selectList(wrapper);
    }


    /**
     * 判断当前是否为系统租户（tenant_id=1，即admin）
     */
    private boolean isSystemTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 1L;
    }
}
