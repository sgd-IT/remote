package com.enjoyiot.module.eiot.service.devicealert;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertConfig;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertRecord;
import com.enjoyiot.module.eiot.controller.admin.devicealert.vo.*;

import com.enjoyiot.module.eiot.controller.app.cameraalert.vo.CameraAlertReportReqVO;

import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import jakarta.validation.Valid;
import java.util.List;

public interface DeviceAlertConfigService {

    Long createDeviceAlertConfig(@Valid DeviceAlertConfigSaveReqVO createReqVO);

    void updateDeviceAlertConfig(@Valid DeviceAlertConfigSaveReqVO updateReqVO);

    void deleteDeviceAlertConfig(Long id);

    DeviceAlertConfig getDeviceAlertConfig(Long id);

    PageResult<DeviceAlertConfig> getDeviceAlertConfigPage(DeviceAlertConfigPageReqVO pageReqVO);

    List<DeviceAlertConfig> getDeviceAlertConfigListByDeviceId(Long deviceId);

    List<DeviceAlertConfig> getDeviceAlertConfigListByProductKey(String productKey);

    void addDeviceAlertRecord(DeviceAlertRecord record);

    void recoverDeviceAlertRecord(Long deviceId, String alertName);

    /**
     * 摄像头报警上报
     * 根据 devSno 查找设备，写入报警记录（含报警图片）
     *
     * @param reqVO       解析后的报警事件数据
     */
    void reportCameraAlert(CameraAlertReportReqVO reqVO);

}
