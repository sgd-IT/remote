package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceRemoteConfigSaveReqVO;
import com.enjoyiot.module.eiot.convert.device.DeviceRemoteConfigConvert;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandDO;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceRemoteConfigDO;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandMapper;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceRemoteConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DeviceRemoteConfigServiceImpl implements DeviceRemoteConfigService {

    @Resource
    private DeviceRemoteConfigMapper deviceRemoteConfigMapper;
    @Resource
    private BrandModelMapper brandModelMapper;
    @Resource
    private BrandMapper brandMapper;

    @Override
    public DeviceRemoteConfigRespVO getRemoteConfig(Long deviceId) {
        DeviceRemoteConfigDO config = deviceRemoteConfigMapper.selectByDeviceId(deviceId);
        if (config == null) {
            return null;
        }
        DeviceRemoteConfigRespVO vo = DeviceRemoteConfigConvert.INSTANCE.convert(config);

        BrandModelDO brandModel = brandModelMapper.selectById(config.getBrandModelId());
        if (brandModel != null) {
            vo.setModel(brandModel.getModel());
            vo.setVersion(brandModel.getVersion());
            BrandDO brand = brandMapper.selectById(brandModel.getBrandId());
            if (brand != null) {
                vo.setBrandName(brand.getName());
            }
        }
        vo.setHasPassword(config.getConfirmPassword() != null && !config.getConfirmPassword().isEmpty());
        return vo;
    }

    @Override
    public void saveRemoteConfig(Long deviceId, DeviceRemoteConfigSaveReqVO reqVO) {
        DeviceRemoteConfigDO existing = deviceRemoteConfigMapper.selectByDeviceId(deviceId);
        if (existing != null) {
            existing.setBrandModelId(reqVO.getBrandModelId());
            if (reqVO.getConfirmPassword() != null) {
                existing.setConfirmPassword(reqVO.getConfirmPassword());
            }
            deviceRemoteConfigMapper.updateById(existing);
        } else {
            DeviceRemoteConfigDO config = new DeviceRemoteConfigDO();
            config.setDeviceId(deviceId);
            config.setBrandModelId(reqVO.getBrandModelId());
            config.setConfirmPassword(reqVO.getConfirmPassword());
            deviceRemoteConfigMapper.insert(config);
        }
    }
}
