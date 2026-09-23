package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhonePageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneUpdateReqVO;
import com.enjoyiot.module.eiot.service.firewatch.FireDutyPhoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 值守中心座机号码管理 Controller
 * <p>
 * 值守中心座机归属固定为系统租户（tenantId = 1），仅系统租户可录入/维护。
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 值守中心座机号码")
@RestController
@RequestMapping("/eiot/firewatch/duty-phone")
@Validated
public class FireDutyPhoneController {

    @Resource
    private FireDutyPhoneService fireDutyPhoneService;

    @PostMapping("/create")
    @Operation(summary = "创建座机号码", description = "值守平台录入值守中心座机电话（归属固定为系统租户）")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty-phone:create')")
    public CommonResult<Long> createPhone(@Valid @RequestBody DutyPhoneCreateReqVO reqVO) {
        return success(fireDutyPhoneService.createPhone(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新座机号码", description = "更新座机号码信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty-phone:update')")
    public CommonResult<Boolean> updatePhone(@Valid @RequestBody DutyPhoneUpdateReqVO reqVO) {
        fireDutyPhoneService.updatePhone(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除座机号码", description = "删除座机号码")
    @Parameter(name = "id", description = "座机号码ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty-phone:delete')")
    public CommonResult<Boolean> deletePhone(@RequestParam("id") Long id) {
        fireDutyPhoneService.deletePhone(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取座机号码详情", description = "获取座机号码详细信息")
    @Parameter(name = "id", description = "座机号码ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty-phone:query')")
    public CommonResult<DutyPhoneRespVO> getPhone(@RequestParam("id") Long id) {
        return success(fireDutyPhoneService.getPhone(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询座机号码", description = "分页查询值守中心座机号码列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty-phone:query')")
    public CommonResult<PageResult<DutyPhoneRespVO>> pagePhones(@Valid DutyPhonePageReqVO pageReqVO) {
        return success(fireDutyPhoneService.pagePhones(pageReqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获取座机号码列表（不分页）", description = "获取值守中心座机号码列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:duty-phone:query')")
    public CommonResult<List<DutyPhoneRespVO>> listPhones() {
        return success(fireDutyPhoneService.listPhones());
    }
}
