package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FireHazardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 隐患管理 Controller
 *
 * 功能概述：
 * - 隐患上报：支持移动端快速上报隐患信息
 * - 隐患等级：一般隐患、较大隐患、重大隐患三级分类
 * - 整改跟踪：整改进度实时跟踪，超期预警
 * - 验收复查流程：整改完成后申请验收，确认闭环
 * - 隐患统计：按类型、区域、时间等维度统计分析
 * - 关联设备：隐患可关联相关设备信息
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 隐患管理")
@RestController
@RequestMapping("/eiot/firewatch/hazard")
@Validated
public class FireHazardController {

    @Resource
    private FireHazardService fireHazardService;

    @PostMapping("/create")
    @Operation(summary = "创建隐患", description = "上报新的消防隐患")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:create')")
    public CommonResult<Long> createHazard(@Valid @RequestBody FireHazardCreateReqVO createReqVO) {
        return success(fireHazardService.createHazard(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新隐患", description = "更新隐患信息")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:update')")
    public CommonResult<Boolean> updateHazard(@Valid @RequestBody FireHazardUpdateReqVO updateReqVO) {
        fireHazardService.updateHazard(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除隐患", description = "删除隐患记录")
    @Parameter(name = "id", description = "隐患ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:delete')")
    public CommonResult<Boolean> deleteHazard(@RequestParam("id") Long id) {
        fireHazardService.deleteHazard(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取隐患详情", description = "获取隐患详细信息及整改记录")
    @Parameter(name = "id", description = "隐患ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:query')")
    public CommonResult<FireHazardRespVO> getHazard(@RequestParam("id") Long id) {
        return success(fireHazardService.getHazard(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询隐患列表", description = "分页查询隐患列表")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:query')")
    public CommonResult<PageResult<FireHazardRespVO>> pageHazards(@Valid FireHazardPageReqVO pageReqVO) {
        return success(fireHazardService.pageHazards(pageReqVO));
    }

    @PutMapping("/submit-rectification")
    @Operation(summary = "提交整改进度", description = "提交隐患整改进度和措施")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:rectify')")
    public CommonResult<Boolean> submitRectification(@Valid @RequestBody FireHazardRectificationReqVO reqVO) {
        fireHazardService.submitRectification(reqVO);
        return success(true);
    }

    @PutMapping("/review-rectification")
    @Operation(summary = "验收复查", description = "对整改完成后的隐患进行验收复查")
    @Parameter(name = "id", description = "隐患ID", required = true)
    @Parameter(name = "result", description = "验收结果: approved-通过, rejected-不通过", required = true)
    @Parameter(name = "remark", description = "验收意见")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:review')")
    public CommonResult<Boolean> reviewRectification(
            @RequestParam("id") Long id,
            @RequestParam("result") String result,
            @RequestParam(value = "remark", required = false) String remark) {
        fireHazardService.reviewRectification(id, result, remark);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭隐患", description = "关闭隐患记录")
    @Parameter(name = "id", description = "隐患ID", required = true)
    @Parameter(name = "remark", description = "关闭原因")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:close')")
    public CommonResult<Boolean> closeHazard(
            @RequestParam("id") Long id,
            @RequestParam(value = "remark", required = false) String remark) {
        fireHazardService.closeHazard(id, remark);
        return success(true);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取隐患统计", description = "获取隐患统计数据，按等级、状态、趋势等维度分析")
    @Parameter(name = "propertyId", description = "物业ID")
    @Parameter(name = "startDate", description = "开始日期", required = true)
    @Parameter(name = "endDate", description = "结束日期", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:query')")
    public CommonResult<Map<String, Object>> getHazardStatistics(
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        return success(fireHazardService.getHazardStatistics(propertyId, startDate, endDate));
    }

    @GetMapping("/export")
    @Operation(summary = "导出隐患", description = "导出隐患列表到Excel")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:query')")
    public void exportHazards(@Valid FireHazardPageReqVO pageReqVO,
                               HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(com.enjoyiot.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        PageResult<FireHazardRespVO> pageResult = fireHazardService.pageHazards(pageReqVO);
        ExcelUtils.write(response, "隐患数据.xls", "隐患数据", FireHazardRespVO.class, pageResult.getList());
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "下载隐患导入模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<FireHazardImportVO> list = Arrays.asList(
                FireHazardImportVO.builder()
                        .hazardLevel("general")
                        .hazardDesc("示例隐患描述")
                        .location("A栋1楼")
                        .foundUserName("张三")
                        .foundDate(LocalDate.now())
                        .deadline(LocalDate.now().plusDays(7))
                        .suggestion("及时整改")
                        .remark("示例备注")
                        .build()
        );
        ExcelUtils.write(response, "隐患导入模板.xls", "隐患数据", FireHazardImportVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入隐患")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:hazard:create')")
    public CommonResult<Map<String, Object>> importHazards(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        List<FireHazardImportVO> list = ExcelUtils.read(file, FireHazardImportVO.class);
        return success(fireHazardService.importHazards(list));
    }

}
