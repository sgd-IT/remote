package com.enjoyiot.module.eiot.controller.app.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FirePatrolTaskPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.FirePatrolTaskRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FirePatrolTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 用户 App - 巡检任务（移动端入口）
 *
 * 直调 Service（与 PC 共用实现）：若经 admin 版 Controller 转发，方法级安全会触发它自己的
 * @PreAuthorize 二次拦截（要求菜单权限点），移动端账号会被挡下。
 * 权限口径：账号允许移动端即可访问（system_users.user_type 含移动位，即 2/3），不校验菜单权限点。
 */
@Tag(name = "用户 App - 巡检任务")
@RestController
@RequestMapping("/eiot/firewatch/patrol-task")
@Validated
public class AppFirePatrolTaskController {

    @Resource
    private FirePatrolTaskService firePatrolTaskService;

    @GetMapping("/page")
    @Operation(summary = "分页查询任务列表")
    @PreAuthorize("@ss.hasMobilePlatformAccess()")
    public CommonResult<PageResult<FirePatrolTaskRespVO>> pageTasks(@Valid FirePatrolTaskPageReqVO pageReqVO) {
        return success(firePatrolTaskService.pageTasks(pageReqVO));
    }

}
