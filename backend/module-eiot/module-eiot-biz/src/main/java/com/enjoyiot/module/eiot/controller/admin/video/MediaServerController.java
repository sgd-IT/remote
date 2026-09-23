package com.enjoyiot.module.eiot.controller.admin.video;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.dal.dataobject.video.IccMediaServerDO;
import com.enjoyiot.module.eiot.service.video.MediaServerService;
import com.enjoyiot.module.eiot.service.video.icc.IccOpenApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ICC平台配置")
@RestController
@RequestMapping("/eiot/video/media-server")
@Validated
public class MediaServerController {

    @Resource
    private MediaServerService mediaServerService;

    @Resource
    private IccOpenApiClient iccOpenApiClient;

    @GetMapping("/get")
    @Operation(summary = "获取当前租户ICC配置")
    @PreAuthorize("@ss.hasPermission('video:media-server:query')")
    public CommonResult<IccMediaServerDO> getConfig() {
        return success(mediaServerService.getCurrentTenantConfig());
    }

    @PostMapping("/save")
    @Operation(summary = "保存ICC配置")
    @PreAuthorize("@ss.hasAnyPermissions('video:media-server:create', 'video:media-server:update')")
    public CommonResult<IccMediaServerDO> save(@RequestBody IccMediaServerDO config) {
        Long id = mediaServerService.createOrUpdate(config);
        config.setId(id);
        return success(config);
    }

    @GetMapping("/public-key")
    @Operation(summary = "获取ICC平台RSA公钥")
    @PreAuthorize("@ss.hasPermission('video:media-server:query')")
    public CommonResult<String> getPublicKey(@RequestParam("baseUrl") String baseUrl) {
        String publicKey = iccOpenApiClient.getPublicKey(baseUrl);
        return success(publicKey);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除ICC配置")
    @PreAuthorize("@ss.hasPermission('video:media-server:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        mediaServerService.delete(id);
        return success(true);
    }
}
