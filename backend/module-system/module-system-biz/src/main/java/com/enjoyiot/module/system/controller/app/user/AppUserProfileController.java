package com.enjoyiot.module.system.controller.app.user;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.datapermission.core.annotation.DataPermission;
import com.enjoyiot.module.system.controller.admin.user.UserProfileController;
import com.enjoyiot.module.system.controller.admin.user.vo.profile.UserProfileRespVO;
import com.enjoyiot.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户 App - 用户个人中心（移动端入口）
 *
 * 薄转发到 admin 版 Controller：业务逻辑与 PC 端共用同一实现，本类只负责把移动端请求
 * 收敛到 /app-api 前缀下（前缀由框架按 controller.app 包自动挂载，无需手工拼接）。
 * 权限与数据权限注解与 admin 版保持一致（/get 关闭数据权限，避免查询不到部门）。
 */
@Tag(name = "用户 App - 用户个人中心")
@RestController
@RequestMapping("/system/user/profile")
@Validated
public class AppUserProfileController {

    @Resource
    private UserProfileController userProfileController;

    @GetMapping("/get")
    @Operation(summary = "获得登录用户信息")
    @DataPermission(enable = false) // 关闭数据权限，避免只查看自己时，查询不到部门。
    public CommonResult<UserProfileRespVO> getUserProfile() {
        return userProfileController.getUserProfile();
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户个人信息")
    public CommonResult<Boolean> updateUserProfile(@Valid @RequestBody UserProfileUpdateReqVO reqVO) {
        return userProfileController.updateUserProfile(reqVO);
    }

    @RequestMapping(value = "/update-avatar",
            method = {RequestMethod.POST, RequestMethod.PUT}) // 解决 uni-app 不支持 Put 上传文件的问题
    @Operation(summary = "上传用户个人头像")
    public CommonResult<String> updateUserAvatar(@RequestParam("avatarFile") MultipartFile file) throws Exception {
        return userProfileController.updateUserAvatar(file);
    }

}
