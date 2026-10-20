package com.enjoyiot.module.eiot.controller.app.device;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.device.dto.DeviceShortInfo;
import com.enjoyiot.module.eiot.controller.admin.device.vo.DeviceInfoPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.DeviceShortRespVO;
import com.enjoyiot.module.eiot.service.device.DeviceInfoService;
import com.enjoyiot.module.system.dal.dataobject.tenant.TenantDO;
import com.enjoyiot.module.system.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 用户 App - 设备信息（移动端入口）
 *
 * 直调 Service（与 PC 共用实现）：若经 admin 版 Controller 转发，方法级安全会触发它自己的
 * @PreAuthorize 二次拦截（要求菜单权限点），移动端账号会被挡下。
 * 权限口径：账号允许移动端即可访问（system_users.user_type 含移动位，即 2/3），不校验菜单权限点。
 * 注意：分页结果的 VO 转换与企业名填充与 admin 版是两份拷贝，改这里时两边同步核对。
 */
@Tag(name = "用户 App - 设备信息")
@RestController
@RequestMapping("/eiot/device")
@Validated
public class AppDeviceInfoController {

    @Resource
    private DeviceInfoService deviceInfoService;

    @Resource
    private TenantService tenantService;

    @GetMapping("/page")
    @Operation(summary = "获得设备信息分页")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<PageResult<DeviceShortRespVO>> getDeviceInfoPage(@Valid DeviceInfoPageReqVO pageReqVO) {
        PageResult<DeviceShortInfo> pageResult = deviceInfoService.getDeviceInfoPage(pageReqVO);
        PageResult<DeviceShortRespVO> voPageResult = BeanUtils.toBean(pageResult, DeviceShortRespVO.class);
        // 批量填充所属企业名称（避免 N+1 查询）
        if (voPageResult.getList() != null && !voPageResult.getList().isEmpty()) {
            Set<Long> tenantIds = new HashSet<>();
            for (DeviceShortRespVO vo : voPageResult.getList()) {
                if (vo.getTenantId() != null) {
                    tenantIds.add(vo.getTenantId());
                }
            }
            if (!tenantIds.isEmpty()) {
                Map<Long, String> tenantNameMap = new HashMap<>();
                TenantUtils.executeIgnore(() -> {
                    for (Long tenantId : tenantIds) {
                        try {
                            TenantDO tenant = tenantService.getTenant(tenantId);
                            if (tenant != null) {
                                tenantNameMap.put(tenantId, tenant.getName());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
                for (DeviceShortRespVO vo : voPageResult.getList()) {
                    if (vo.getTenantId() != null) {
                        vo.setTenantName(tenantNameMap.get(vo.getTenantId()));
                    }
                }
            }
        }
        return success(voPageResult);
    }

}
