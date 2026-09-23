package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.framework.tenant.core.aop.TenantIgnore;
import com.enjoyiot.module.eiot.dal.dataobject.video.DeviceCameraRelationDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.dal.mysql.video.DeviceCameraRelationMapper;
import com.enjoyiot.module.eiot.dal.mysql.video.VideoCameraMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceCameraRelationServiceImpl implements DeviceCameraRelationService {

    @Resource
    private DeviceCameraRelationMapper relationMapper;

    @Resource
    private VideoCameraMapper videoCameraMapper;

    @Override
    public void bind(Long deviceId, Long cameraId) {
        List<DeviceCameraRelationDO> existing = relationMapper.selectListByDeviceId(deviceId);
        boolean alreadyBound = existing.stream().anyMatch(r -> r.getCameraId().equals(cameraId));
        if (alreadyBound) {
            throw new RuntimeException("该摄像头已与当前部件绑定");
        }
        DeviceCameraRelationDO relation = DeviceCameraRelationDO.builder()
                .deviceId(deviceId)
                .cameraId(cameraId)
                .relationType("manual")
                .build();
        relationMapper.insert(relation);
    }

    @Override
    public void unbind(Long id) {
        relationMapper.deleteByIdPhysically(id);
    }

    @Override
    @TenantIgnore
    public List<DeviceCameraRelationDO> getRelationsByDevice(Long deviceId) {
        return relationMapper.selectListByDeviceId(deviceId);
    }

    @Override
    @TenantIgnore
    public List<VideoCameraDO> getCamerasByDevice(Long deviceId) {
        List<DeviceCameraRelationDO> relations = relationMapper.selectListByDeviceId(deviceId);
        List<Long> cameraIds = relations.stream()
                .map(DeviceCameraRelationDO::getCameraId)
                .collect(Collectors.toList());
        if (cameraIds.isEmpty()) {
            return List.of();
        }
        return videoCameraMapper.selectBatchIds(cameraIds);
    }
}
