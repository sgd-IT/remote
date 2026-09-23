package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractUpdateReqVO;
import com.enjoyiot.module.eiot.service.firewatch.FireMaintenanceContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 维保合同管理 Controller
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 维保合同管理")
@RestController
@RequestMapping("/eiot/firewatch/contract")
@Validated
@Slf4j
public class FireMaintenanceContractController {

    @Resource
    private FireMaintenanceContractService fireMaintenanceContractService;

    @PostMapping("/create")
    @Operation(summary = "创建维保合同", description = "创建维保合同")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:contract:create')")
    public CommonResult<Long> createContract(@Valid @RequestBody ContractCreateReqVO reqVO) {
        log.info("[createContract][Controller] 收到创建维保合同请求: {}", reqVO);
        try {
            Long id = fireMaintenanceContractService.createContract(reqVO);
            log.info("[createContract][Controller] 创建维保合同成功, id={}", id);
            return success(id);
        } catch (Exception e) {
            log.error("[createContract][Controller] 创建维保合同Controller异常", e);
            throw e;
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新维保合同", description = "更新维保合同信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:contract:update')")
    public CommonResult<Boolean> updateContract(@Valid @RequestBody ContractUpdateReqVO reqVO) {
        fireMaintenanceContractService.updateContract(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除维保合同", description = "删除维保合同")
    @Parameter(name = "id", description = "合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:contract:delete')")
    public CommonResult<Boolean> deleteContract(@RequestParam("id") Long id) {
        fireMaintenanceContractService.deleteContract(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取合同详情", description = "获取维保合同详细信息")
    @Parameter(name = "id", description = "合同ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:contract:query')")
    public CommonResult<ContractRespVO> getContract(@RequestParam("id") Long id) {
        return success(fireMaintenanceContractService.getContract(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询合同列表", description = "分页查询维保合同列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:contract:query')")
    public CommonResult<PageResult<ContractRespVO>> pageContracts(@Valid ContractPageReqVO pageReqVO) {
        return success(fireMaintenanceContractService.pageContracts(pageReqVO));
    }

    @GetMapping("/expiring")
    @Operation(summary = "获取即将到期合同", description = "获取30天内即将到期的合同列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:contract:query')")
    public CommonResult<List<ContractRespVO>> getExpiringContracts() {
        return success(fireMaintenanceContractService.getExpiringContracts());
    }

    @GetMapping("/expiring-count")
    @Operation(summary = "统计即将到期合同数", description = "统计30天内即将到期的合同数量")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:contract:query')")
    public CommonResult<Integer> countExpiring() {
        return success(fireMaintenanceContractService.countExpiring());
    }
}
