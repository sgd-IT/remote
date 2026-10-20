package com.enjoyiot.module.eiot.service.firewatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enjoyiot.framework.common.pojo.PageParam;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DeviceListItemRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.SubDeviceStatsRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 消防设备 Service 实现类（设备控制室专用）
 *
 * 租户处理：因 eiot_device_info 有租户拦截器，admin（tenant=1）直接查不到其他租户数据，
 * 故全程用 TenantUtils.executeIgnoreWithResult 包裹，手动按 tenantId 过滤。
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireDeviceServiceImpl implements FireDeviceService {

    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Override
    public List<DeviceListItemRespVO> listParentDevices(Long propertyId) {
        if (propertyId == null) {
            return Collections.emptyList();
        }
        // 1. 查物业获取 tenantId（忽略租户隔离，admin 可查任意企业）
        FirePropertyDO property = TenantUtils.executeIgnoreWithResult(
                () -> firePropertyMapper.selectById(propertyId));
        if (property == null || property.getTenantId() == null) {
            log.warn("[listParentDevices] 物业不存在或无租户ID, propertyId={}", propertyId);
            return Collections.emptyList();
        }
        Long tenantId = property.getTenantId();
        // 2. 按 tenantId 查父设备（parentId IS NULL），忽略租户隔离，过滤不可见设备
        List<EiotDeviceInfoDO> devices = TenantUtils.executeIgnoreWithResult(() ->
                eiotDeviceInfoMapper.selectList(new LambdaQueryWrapper<EiotDeviceInfoDO>()
                        .eq(EiotDeviceInfoDO::getTenantId, tenantId)
                        .and(w -> w.isNull(EiotDeviceInfoDO::getVisible).or().eq(EiotDeviceInfoDO::getVisible, true))
                        .orderByAsc(EiotDeviceInfoDO::getName)));
        return convertToList(devices, property.getName());
    }

    @Override
    public PageResult<DeviceListItemRespVO> pageSubDevices(Long parentId, List<Integer> lineTypes, Integer pageNo, Integer pageSize) {
        if (parentId == null) {
            return PageResult.empty();
        }
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageNo != null && pageNo > 0 ? pageNo : 1);
        pageParam.setPageSize(pageSize != null && pageSize > 0 ? pageSize : 10);
        // 按父设备ID分页查子设备，忽略租户隔离，过滤不可见设备
        PageResult<EiotDeviceInfoDO> pageResult = TenantUtils.executeIgnoreWithResult(() ->
                eiotDeviceInfoMapper.selectPage(pageParam, new LambdaQueryWrapper<EiotDeviceInfoDO>()
                        .eq(EiotDeviceInfoDO::getParentId, parentId)
                        .eq(EiotDeviceInfoDO::getVisible, false)
                        .in(!lineTypes.isEmpty(), EiotDeviceInfoDO::getLineType, lineTypes)
                        .orderByAsc(EiotDeviceInfoDO::getDn)));
        List<DeviceListItemRespVO> list = convertToList(pageResult.getList(), null);
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public SubDeviceStatsRespVO getSubDeviceStats(Long parentId) {
        SubDeviceStatsRespVO vo = new SubDeviceStatsRespVO();
        vo.setTotal(0L);
        vo.setOnline(0L);
        if (parentId == null) {
            return vo;
        }
        Long total = TenantUtils.executeIgnoreWithResult(() ->
                eiotDeviceInfoMapper.selectCount(new LambdaQueryWrapper<EiotDeviceInfoDO>()
                        .eq(EiotDeviceInfoDO::getParentId, parentId)
                        .and(w -> w.isNull(EiotDeviceInfoDO::getVisible).or().eq(EiotDeviceInfoDO::getVisible, true))));
        Long online = TenantUtils.executeIgnoreWithResult(() ->
                eiotDeviceInfoMapper.selectCount(new LambdaQueryWrapper<EiotDeviceInfoDO>()
                        .eq(EiotDeviceInfoDO::getParentId, parentId)
                        .eq(EiotDeviceInfoDO::getState, 1)
                        .and(w -> w.isNull(EiotDeviceInfoDO::getVisible).or().eq(EiotDeviceInfoDO::getVisible, true))));
        vo.setTotal(total != null ? total : 0L);
        vo.setOnline(online != null ? online : 0L);
        return vo;
    }

    /**
     * 将设备 DO 列表转为响应 VO 列表
     *
     * @param devices     设备 DO 列表
     * @param propertyName 所属企业名称（可空，子设备列表可为 null）
     * @return 响应 VO 列表
     */
    private List<DeviceListItemRespVO> convertToList(List<EiotDeviceInfoDO> devices, String propertyName) {
        List<DeviceListItemRespVO> list = new ArrayList<>();
        if (devices == null) {
            return list;
        }
        for (EiotDeviceInfoDO device : devices) {
            DeviceListItemRespVO vo = new DeviceListItemRespVO();
            vo.setId(device.getId());
            vo.setName(device.getName());
            vo.setDn(device.getDn());
            vo.setState(device.getState());
            vo.setAddr(device.getAddr());
            vo.setTenantId(device.getTenantId());
            vo.setProductKey(device.getProductKey());
            vo.setSerialNo(device.getSerialNo());
            vo.setDeviceType(device.getDeviceType());
            vo.setLineType(device.getLineType());
            vo.setPropertyName(propertyName);
            list.add(vo);
        }
        return list;
    }

}
