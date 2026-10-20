package com.enjoyiot.module.eiot.controller.app.video;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoLiveUrlRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.video.IccMediaServerDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.service.video.MediaServerService;
import com.enjoyiot.module.eiot.service.video.VideoCameraService;
import com.enjoyiot.module.eiot.service.video.icc.IccOpenApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 用户 App - 摄像头（移动端入口）
 *
 * 直调 Service（与 PC 共用实现），不再注入 admin 版 Controller：避免以后 admin 侧补了
 * @PreAuthorize 时，移动端经代理被二次校验悄悄拦下。
 * 这两个端点 admin 侧本就没有权限注解，移动端保持“登录即可访问”不变。
 */
@Tag(name = "用户 App - 摄像头")
@RestController
@RequestMapping("/eiot/video/camera")
@Validated
public class AppVideoCameraController {

    @Resource
    private VideoCameraService videoCameraService;

    @Resource
    private MediaServerService mediaServerService;

    @Resource
    private IccOpenApiClient iccOpenApiClient;

    @GetMapping("/page")
    @Operation(summary = "分页查询摄像头")
    public CommonResult<PageResult<VideoCameraDO>> page(
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(videoCameraService.getPage(pageNo, pageSize, keyword));
    }

    @GetMapping("/live/url")
    @Operation(summary = "获取实时视频流地址")
    public CommonResult<VideoLiveUrlRespVO> getLiveUrl(@RequestParam("cameraId") Long cameraId) {
        // 平台租户（1）可跨租户取摄像头，普通租户只能取本租户的（与 admin 版逻辑保持一致）
        VideoCameraDO camera;
        Long currentTenantId = TenantUtils.getCurrentTenantId();
        if (currentTenantId == 1) {
            camera = TenantUtils.executeIgnoreResult(() -> videoCameraService.getById(cameraId));
        } else {
            camera = videoCameraService.getById(cameraId);
        }
        if (camera == null) {
            throw new RuntimeException("摄像头不存在");
        }
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }
        String iccChannelCode = camera.getDeviceId() + "$1$0$" + camera.getChannelId();
        String streamType = camera.getQuality() != null ? camera.getQuality() : "2";
        String hlsUrl = iccOpenApiClient.getRealtimeStreamUrl(config, iccChannelCode, streamType, "hls");
        String flvUrl = iccOpenApiClient.buildFlvStreamUrl(config, iccChannelCode, streamType);
        VideoLiveUrlRespVO vo = new VideoLiveUrlRespVO();
        vo.setUrl(hlsUrl);
        vo.setFlvUrl(flvUrl);
        return success(vo);
    }

}
