package com.enjoyiot.module.eiot.service.brand;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.enjoyiot.module.eiot.dal.dataobject.protocol.VendorCommandDO;
import com.enjoyiot.module.eiot.dal.mysql.protocol.VendorCommandMapper;
import com.enjoyiot.module.eiot.service.remote.FourfaithRemoteControlClient;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandCascadeRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandModelFunctionRespVO;
import com.enjoyiot.module.eiot.controller.admin.brand.vo.BrandRespVO;
import com.enjoyiot.module.eiot.convert.brand.BrandConvert;
import com.enjoyiot.module.eiot.dal.dataobject.brand.*;
import com.enjoyiot.module.eiot.dal.mysql.brand.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enjoyiot.module.eiot.dal.dataobject.device.DeviceFunctionDO;
import com.enjoyiot.module.eiot.dal.mysql.device.DeviceFunctionMapper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BrandServiceImpl implements BrandService {

    @Resource
    private BrandMapper brandMapper;
    @Resource
    private BrandModelMapper brandModelMapper;
    @Resource
    private BrandModelFunctionMapper brandModelFunctionMapper;
    @Resource
    private BrandModelFunctionParamMapper brandModelFunctionParamMapper;
    @Resource
    private DeviceFunctionMapper deviceFunctionMapper;
    @Resource
    private FourfaithRemoteControlClient fourfaithClient;
    @Resource
    private VendorCommandMapper vendorCommandMapper;

    @Override
    public PageResult<BrandRespVO> getBrandPage(int pageNo, int pageSize) {
        List<BrandDO> list = brandMapper.selectList(new LambdaQueryWrapperX<BrandDO>()
                .eq(BrandDO::getDeleted, false));
        return new PageResult<>(BrandConvert.INSTANCE.convertList(list), (long) list.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBrandData() {
        log.info("开始同步四信品牌数据");
        JSONArray dataArr = fourfaithClient.getAllHardwareDetailed();
        if (dataArr == null || dataArr.isEmpty()) {
            log.warn("四信API返回数据为空");
            return;
        }

        for (int i = 0; i < dataArr.size(); i++) {
            JSONObject item = dataArr.getJSONObject(i);
            String brandCode = item.getStr("brandCode");
            String brandName = item.getStr("name");
            String model = item.getStr("model");
            String version = item.getStr("version");

            if (StrUtil.isBlank(brandCode) || StrUtil.isBlank(model) || StrUtil.isBlank(version)) {
                log.warn("跳过无效数据: brandCode={}, model={}, version={}", brandCode, model, version);
                continue;
            }

            // 1. 保存或更新品牌
            BrandDO brand = brandMapper.selectOne(new LambdaQueryWrapperX<BrandDO>()
                    .eq(BrandDO::getBrandCode, brandCode));
            if (brand == null) {
                brand = new BrandDO();
                brand.setBrandCode(brandCode);
                brand.setName(brandName);
                brandMapper.insert(brand);
                log.info("新增品牌: {}, id={}", brandCode, brand.getId());
            } else {
                brand.setName(brandName);
                brandMapper.updateById(brand);
            }

            // 2. 保存或更新型号版本
            BrandModelDO brandModel = brandModelMapper.selectOne(new LambdaQueryWrapperX<BrandModelDO>()
                    .eq(BrandModelDO::getBrandId, brand.getId())
                    .eq(BrandModelDO::getModel, model)
                    .eq(BrandModelDO::getVersion, version));
            if (brandModel == null) {
                brandModel = new BrandModelDO();
                brandModel.setBrandId(brand.getId());
                brandModel.setModel(model);
                brandModel.setVersion(version);
                brandModel.setSyncTime(LocalDateTime.now());
                brandModelMapper.insert(brandModel);
                log.info("新增型号版本: brandId={}, model={}, version={}", brand.getId(), model, version);
            } else {
                brandModel.setSyncTime(LocalDateTime.now());
                brandModelMapper.updateById(brandModel);
            }

            // 3. 增量同步功能和参数（以四信 functionId 为业务键，保持主键 id 稳定）
            syncBrandModelFunctions(brandModel.getId(), item.getJSONArray("hardwareFunctionList"));
        }

        log.info("品牌数据同步完成，共处理 {} 条记录", dataArr.size());
    }

    @Override
    public List<BrandCascadeRespVO> getBrandCascade() {
        List<BrandDO> brands = brandMapper.selectList(new LambdaQueryWrapperX<BrandDO>()
                .eq(BrandDO::getDeleted, false));

        List<BrandCascadeRespVO> result = new ArrayList<>();
        for (BrandDO brand : brands) {
            BrandCascadeRespVO vo = new BrandCascadeRespVO();
            vo.setId(brand.getId());
            vo.setBrandCode(brand.getBrandCode());
            vo.setName(brand.getName());

            List<BrandModelDO> models = brandModelMapper.selectByBrandId(brand.getId());
            List<BrandCascadeRespVO.ModelVO> modelVOs = new ArrayList<>();
            for (BrandModelDO model : models) {
                BrandCascadeRespVO.ModelVO mvo = new BrandCascadeRespVO.ModelVO();
                mvo.setId(model.getId());
                mvo.setModel(model.getModel());
                mvo.setVersions(List.of(model.getVersion()));
                modelVOs.add(mvo);
            }
            vo.setModels(modelVOs);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<BrandModelFunctionRespVO> getBrandModelFunctions(Long brandModelId) {
        List<BrandModelFunctionDO> functions = brandModelFunctionMapper.selectByBrandModelId(brandModelId);
        String vendorCode = resolveVendorCode(brandModelId);
        List<BrandModelFunctionRespVO> result = new ArrayList<>();
        for (BrandModelFunctionDO function : functions) {
            BrandModelFunctionRespVO vo = new BrandModelFunctionRespVO();
            vo.setId(function.getId());
            vo.setBrandModelId(function.getBrandModelId());
            vo.setFunctionId(function.getFunctionId());
            vo.setName(function.getName());
            vo.setRuleNum(function.getRuleNum());
            vo.setAwaitTime(function.getAwaitTime());

            List<BrandModelFunctionParamDO> params = brandModelFunctionParamMapper.selectByFunctionId(function.getId());
            List<BrandModelFunctionRespVO.FunctionParamRespVO> paramVOs = new ArrayList<>();
            for (BrandModelFunctionParamDO param : params) {
                BrandModelFunctionRespVO.FunctionParamRespVO pvo = new BrandModelFunctionRespVO.FunctionParamRespVO();
                pvo.setId(param.getId());
                pvo.setParamName(param.getParamName());
                pvo.setParamCode(param.getParamCode());
                pvo.setParamType(param.getParamType());
                paramVOs.add(pvo);
            }
            // 回退: LOCAL_COMMAND 类厂商(如YWN)无四信参数行, 从厂商指令字典 param_schema 解析参数定义
            if (paramVOs.isEmpty() && StrUtil.isNotBlank(function.getVendorCommandKey())
                    && StrUtil.isNotBlank(vendorCode)) {
                paramVOs = buildParamsFromSchema(vendorCode, function.getVendorCommandKey());
            }
            vo.setParams(paramVOs);
            result.add(vo);
        }
        return result;
    }

    /** 根据型号id反查品牌的厂商编码(eiot_brand.vendor_code) */
    private String resolveVendorCode(Long brandModelId) {
        BrandModelDO model = brandModelMapper.selectById(brandModelId);
        if (model == null) {
            return null;
        }
        BrandDO brand = brandMapper.selectById(model.getBrandId());
        return brand != null ? brand.getVendorCode() : null;
    }

    /** 从 eiot_vendor_command.param_schema 解析参数定义, 供前端渲染参数输入项 */
    private List<BrandModelFunctionRespVO.FunctionParamRespVO> buildParamsFromSchema(String vendorCode, String commandKey) {
        List<BrandModelFunctionRespVO.FunctionParamRespVO> paramVOs = new ArrayList<>();
        VendorCommandDO command = vendorCommandMapper.selectByVendorAndKey(vendorCode, commandKey);
        if (command == null || StrUtil.isBlank(command.getParamSchema())) {
            return paramVOs;
        }
        try {
            JSONArray params = JSONUtil.parseObj(command.getParamSchema()).getJSONArray("params");
            if (params == null) {
                return paramVOs;
            }
            long syntheticId = 1;
            for (Object item : params) {
                JSONObject def = (JSONObject) item;
                BrandModelFunctionRespVO.FunctionParamRespVO pvo = new BrandModelFunctionRespVO.FunctionParamRespVO();
                pvo.setId(syntheticId++);
                pvo.setParamCode(def.getStr("code"));
                pvo.setParamName(def.getStr("name"));
                pvo.setParamType("NUMBER");
                paramVOs.add(pvo);
            }
        } catch (Exception e) {
            log.warn("解析厂商指令参数模板失败: vendor={}, commandKey={}", vendorCode, commandKey, e);
        }
        return paramVOs;
    }

    private void syncBrandModelFunctions(Long brandModelId, JSONArray funcList) {
        // 查询该型号下现有功能，以四信 functionId 为业务键建立索引
        List<BrandModelFunctionDO> existingFuncs = brandModelFunctionMapper.selectByBrandModelId(brandModelId);
        Map<String, BrandModelFunctionDO> existingFuncMap = existingFuncs.stream()
                .collect(Collectors.toMap(BrandModelFunctionDO::getFunctionId, f -> f, (a, b) -> a));

        Set<String> syncedFunctionIds = new HashSet<>();
        if (funcList != null) {
            for (int j = 0; j < funcList.size(); j++) {
                JSONObject funcObj = funcList.getJSONObject(j);
                String functionId = funcObj.getStr("id");
                if (StrUtil.isBlank(functionId)) {
                    log.warn("跳过无效功能数据: brandModelId={}, functionId为空", brandModelId);
                    continue;
                }
                syncedFunctionIds.add(functionId);

                BrandModelFunctionDO func = existingFuncMap.get(functionId);
                boolean isNew = func == null;
                if (isNew) {
                    func = new BrandModelFunctionDO();
                    func.setBrandModelId(brandModelId);
                    func.setFunctionId(functionId);
                }
                func.setName(funcObj.getStr("name"));
                func.setRuleNum(funcObj.getInt("ruleNum", 1));
                func.setAwaitTime(funcObj.getInt("awaitTime", 0));

                if (isNew) {
                    brandModelFunctionMapper.insert(func);
                } else {
                    brandModelFunctionMapper.updateById(func);
                }

                // 同步该功能下的参数
                syncFunctionParams(func.getId(), funcObj.getJSONArray("functionCodeList"));
            }
        }

        // 清理四信侧已下架但本地仍存在的功能，并级联处理设备上已绑定的功能按钮
        // for (BrandModelFunctionDO oldFunc : existingFuncs) {
        //     if (!syncedFunctionIds.contains(oldFunc.getFunctionId())) {
        //         deviceFunctionMapper.delete(new LambdaQueryWrapperX<DeviceFunctionDO>()
        //                 .eq(DeviceFunctionDO::getBrandModelFunctionId, oldFunc.getId()));
        //         brandModelFunctionParamMapper.delete(new LambdaQueryWrapperX<BrandModelFunctionParamDO>()
        //                 .eq(BrandModelFunctionParamDO::getFunctionId, oldFunc.getId()));
        //         brandModelFunctionMapper.deleteById(oldFunc.getId());
        //         log.info("清理已下架型号功能: brandModelId={}, functionId={}", brandModelId, oldFunc.getFunctionId());
        //     }
        // }
    }

    private void syncFunctionParams(Long functionId, JSONArray paramList) {
        List<BrandModelFunctionParamDO> existingParams = brandModelFunctionParamMapper.selectByFunctionId(functionId);
        Map<String, BrandModelFunctionParamDO> existingParamMap = existingParams.stream()
                .collect(Collectors.toMap(BrandModelFunctionParamDO::getParamCode, p -> p, (a, b) -> a));

        Set<String> syncedParamCodes = new HashSet<>();
        if (paramList != null) {
            for (int k = 0; k < paramList.size(); k++) {
                JSONObject paramObj = paramList.getJSONObject(k);
                String paramCode = paramObj.getStr("code");
                if (StrUtil.isBlank(paramCode)) {
                    continue;
                }
                syncedParamCodes.add(paramCode);

                BrandModelFunctionParamDO param = existingParamMap.get(paramCode);
                boolean isNew = param == null;
                if (isNew) {
                    param = new BrandModelFunctionParamDO();
                    param.setFunctionId(functionId);
                    param.setParamCode(paramCode);
                }
                param.setParamName(paramObj.getStr("name"));
                param.setParamType(paramObj.getStr("type"));

                if (isNew) {
                    brandModelFunctionParamMapper.insert(param);
                } else {
                    brandModelFunctionParamMapper.updateById(param);
                }
            }
        }

        for (BrandModelFunctionParamDO oldParam : existingParams) {
            if (!syncedParamCodes.contains(oldParam.getParamCode())) {
                brandModelFunctionParamMapper.deleteById(oldParam.getId());
            }
        }
    }
}
