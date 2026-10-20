package com.enjoyiot.module.eiot.service.remote;

import com.enjoyiot.module.eiot.controller.admin.remote.vo.*;

import java.util.List;

public interface RemoteConsoleService {

    /**
     * 获取设备树
     */
    List<RemoteConsoleDeviceTreeRespVO> getDeviceTree();

    /**
     * 获取设备详情
     */
    RemoteConsoleDeviceDetailRespVO getDeviceDetail(Long deviceId);

    /**
     * 获取设备参数
     */
    List<RemoteConsoleDeviceParamRespVO> getDeviceParams(Long deviceId);

    /**
     * 获取设备状态统计
     */
    RemoteConsoleDeviceStatusStatsRespVO getDeviceStatusStats();

    /**
     * 获取远程控制中心右上角展示人员列表
     *
     * 展示顺序：1. 展示岗位下的人员（按岗位顺序）；2. 当前时间值班人员（去重）
     *
     * @param tenantId 租户ID
     * @return 展示人员列表
     */
    List<ConsolePersonRespVO> getConsolePersons(Long tenantId);

    /**
     * 切换手自动模式
     */
    Boolean switchDeviceMode(Long deviceId, SwitchDeviceModeReqVO reqVO);

    /**
     * 智能空开控制（开闸/合闸）
     *
     * @param action 操作类型：open=开闸, close=合闸
     * @param reqVO  请求参数
     * @return 是否成功
     */
    Boolean valveControl(String action, ValveControlReqVO reqVO);

    /**
     * 拉取并消费智能空开控制命令
     *
     * @param deviceId 设备 ID
     * @return 待执行命令；不存在时返回 {@code null}
     */
    ValveCommandRespVO consumeValveCommand(Long deviceId);
}
