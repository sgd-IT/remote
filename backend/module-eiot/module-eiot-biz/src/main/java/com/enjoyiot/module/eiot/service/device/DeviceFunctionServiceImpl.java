package com.enjoyiot.module.eiot.service.device;

import cn.hutool.json.JSONUtil;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.DeviceFunctionSaveReqVO;
import com.enjoyiot.module.eiot.convert.device.DeviceFunctionConvert;
import com.enjoyiot.module.eiot.dal.dataobject.brand.BrandModelFunctionDO;
import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import com.enjoyiot.module.eiot.dal.mysql.brand.BrandModelFunctionMapper;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceFunctionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceFunctionServiceImpl implements DeviceFunctionService {

    @Resource
    private DeviceFunctionMapper deviceFunctionMapper;
    @Resource
    private BrandModelFunctionMapper brandModelFunctionMapper;

    @Override
    public List<DeviceFunctionRespVO> getDeviceFunctions(Long deviceId) {
        List<DeviceFunctionDO> list = deviceFunctionMapper.selectByDeviceId(deviceId);
        List<DeviceFunctionRespVO> result = DeviceFunctionConvert.INSTANCE.convertList(list);
        for (DeviceFunctionRespVO vo : result) {
            BrandModelFunctionDO func = brandModelFunctionMapper.selectById(vo.getBrandModelFunctionId());
            if (func != null) {
                vo.setFunctionName(func.getName());
            }
        }
        return result;
    }

    @Override
    public Long addDeviceFunction(Long deviceId, DeviceFunctionSaveReqVO reqVO) {
        DeviceFunctionDO function = new DeviceFunctionDO();
        function.setDeviceId(deviceId);
        function.setBrandModelFunctionId(reqVO.getBrandModelFunctionId());

        // 自动填充按钮名称
        String buttonName = reqVO.getButtonName();
        if (buttonName == null || buttonName.isEmpty()) {
            BrandModelFunctionDO func = brandModelFunctionMapper.selectById(reqVO.getBrandModelFunctionId());
            if (func != null) {
                buttonName = func.getName();
            }
        }
        function.setButtonName(buttonName);
        function.setSortOrder(reqVO.getSortOrder());
        if (reqVO.getParamValues() != null) {
            function.setParamValues(JSONUtil.toJsonStr(reqVO.getParamValues()));
        }
        function.setSubDeviceId(reqVO.getSubDeviceId());
        function.setFunctionType(reqVO.getFunctionType());
        function.setEnabled(true);
        deviceFunctionMapper.insert(function);
        return function.getId();
    }

    @Override
    public void updateDeviceFunction(Long id, DeviceFunctionSaveReqVO reqVO) {
        DeviceFunctionDO function = deviceFunctionMapper.selectById(id);
        if (function == null) {
            return;
        }
        String buttonName = reqVO.getButtonName();
        if (buttonName != null && !buttonName.isEmpty()) {
            function.setButtonName(buttonName);
        }
        if (reqVO.getSortOrder() != null) {
            function.setSortOrder(reqVO.getSortOrder());
        }
        if (reqVO.getParamValues() != null) {
            function.setParamValues(JSONUtil.toJsonStr(reqVO.getParamValues()));
        }
        function.setSubDeviceId(reqVO.getSubDeviceId());
        function.setFunctionType(reqVO.getFunctionType());
        deviceFunctionMapper.updateById(function);
    }

    @Override
    public void deleteDeviceFunction(Long id) {
        deviceFunctionMapper.deleteById(id);
    }
}
