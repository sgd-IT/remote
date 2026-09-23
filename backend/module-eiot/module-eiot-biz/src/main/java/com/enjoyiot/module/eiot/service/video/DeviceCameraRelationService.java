package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.module.eiot.dal.dataobject.video.DeviceCameraRelationDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;

import java.util.List;

public interface DeviceCameraRelationService {

    void bind(Long deviceId, Long cameraId);

    void unbind(Long id);

    List<DeviceCameraRelationDO> getRelationsByDevice(Long deviceId);

    List<VideoCameraDO> getCamerasByDevice(Long deviceId);
}
