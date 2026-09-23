package com.enjoyiot.module.eiot.controller.admin.remote;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageParam;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.excel.core.util.ExcelUtils;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.remote.vo.RemoteOperationLogRespVO;
import com.enjoyiot.module.eiot.service.remote.RemoteOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 远程控制日志")
@RestController
@RequestMapping("/eiot/remote-console/operation-log")
public class RemoteOperationLogController {

    @Resource
    private RemoteOperationLogService remoteOperationLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询操作日志")
    @PreAuthorize("@ss.hasPermission('iot:remote-operation-log:query')")
    public CommonResult<PageResult<RemoteOperationLogRespVO>> getOperationLogPage(
            @Valid RemoteOperationLogPageReqVO pageReqVO) {
        return success(remoteOperationLogService.getOperationLogPage(pageReqVO));
    }

    @GetMapping("/export")
    @Operation(summary = "导出操作日志")
    @PreAuthorize("@ss.hasPermission('iot:remote-operation-log:export')")
    public void exportOperationLog(HttpServletResponse response,
                                    @Valid RemoteOperationLogPageReqVO pageReqVO) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<RemoteOperationLogRespVO> pageResult = remoteOperationLogService.getOperationLogPage(pageReqVO);
        ExcelUtils.write(response, "远程控制操作日志.xlsx", "日志",
                RemoteOperationLogRespVO.class, pageResult.getList());
    }
}
