package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.DeviceListItemRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.SubDeviceStatsRespVO;

import java.util.List;

/**
 * 消防设备 Service（设备控制室专用）
 *
 * 提供按企业查询父设备、按父设备分页查询子设备的能力。
 *
 * @author EnjoyIot
 */
public interface FireDeviceService {

    /**
     * 按物业ID查询该企业的父设备列表（parentId 为 null 的设备）
     *
     * @param propertyId 物业ID
     * @return 父设备列表
     */
    List<DeviceListItemRespVO> listParentDevices(Long propertyId);

    /**
     * 分页查询父设备绑定的子设备
     *
     * @param parentId 父设备ID
     * @param lineTypes 线路类型列表（0=多线, 1=总线），空列表表示不过滤
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 子设备分页结果
     */
    PageResult<DeviceListItemRespVO> pageSubDevices(Long parentId, List<Integer> lineTypes, Integer pageNo, Integer pageSize);

    /**
     * 统计父设备下的子设备总数与在线数
     *
     * @param parentId 父设备ID
     * @return 子设备统计（total=总数, online=在线数）
     */
    SubDeviceStatsRespVO getSubDeviceStats(Long parentId);

}
