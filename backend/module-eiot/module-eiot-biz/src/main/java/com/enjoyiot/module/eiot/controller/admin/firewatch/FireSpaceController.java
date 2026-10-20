package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space.FireSpaceDTO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space.FireSpaceRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space.FireSpaceUpdateDTO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireSpaceDO;
import com.enjoyiot.module.eiot.service.firewatch.FireSpaceService;
import com.enjoyiot.module.system.dal.dataobject.tenant.TenantDO;
import com.enjoyiot.module.system.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 空间管理 Controller（楼栋/楼层/区域/详细位置）
 * <p>
 * 数据按当前租户隔离。
 *
 * @author sgd
 */
@Tag(name = "管理后台 - 空间管理")
@RestController
@RequestMapping("/eiot/firewatch/space")
@Validated
public class FireSpaceController {

    @Resource
    private FireSpaceService fireSpaceService;

    @Resource
    private TenantService tenantService;

    @PostMapping("/create")
    @Operation(summary = "创建空间", description = "创建当前租户下的空间（楼栋/楼层/区域/详细位置），同组合不可重复")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:space:create')")
    public CommonResult<Long> createFireSpace(@Valid @RequestBody FireSpaceDTO fireSpaceDTO) {
        return success(fireSpaceService.createFireSpace(fireSpaceDTO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新空间", description = "更新空间信息（楼栋/楼层/区域/详细位置），同组合不可重复")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:space:update')")
    public CommonResult<Void> updateFireSpace(@Valid @RequestBody FireSpaceUpdateDTO updateDTO) {
        fireSpaceService.updateFireSpace(updateDTO);
        return success();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除空间", description = "删除空间（楼栋/楼层/区域/详细位置）；存在设备引用时不允许删除")
    @Parameter(name = "id", description = "空间ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:space:delete')")
    public CommonResult<Void> deleteFireSpace(@RequestParam("id") Long id) {
        fireSpaceService.deleteFireSpace(id);
        return success();
    }
    @GetMapping("/list")
    @Operation(summary = "获取空间列表（不分页）", description = "系统租户管理员不带 tenantId 查全部、带 tenantId 查指定租户；企业租户仅查本租户。返回带租户名称（园区名），供设备地址下拉选择等场景使用")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:space:query')")
    public CommonResult<List<FireSpaceRespVO>> listFireSpace(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        List<FireSpaceDO> list = fireSpaceService.listFireSpace(tenantId);
        List<FireSpaceRespVO> voList = BeanUtils.toBean(list, FireSpaceRespVO.class);

        // 批量填充租户名称（避免 N+1 查询）
        Set<Long> tenantIds = new HashSet<>();
        for (FireSpaceRespVO vo : voList) {
            if (vo.getTenantId() != null) {
                tenantIds.add(vo.getTenantId());
            }
        }
        if (!tenantIds.isEmpty()) {
            Map<Long, String> tenantNameMap = new HashMap<>();
            TenantUtils.executeIgnore(() -> {
                for (Long tid : tenantIds) {
                    try {
                        TenantDO tenant = tenantService.getTenant(tid);
                        if (tenant != null) {
                            tenantNameMap.put(tid, tenant.getName());
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
            for (FireSpaceRespVO vo : voList) {
                if (vo.getTenantId() != null) {
                    vo.setTenantName(tenantNameMap.get(vo.getTenantId()));
                }
            }
        }

        return success(voList);
    }
}
