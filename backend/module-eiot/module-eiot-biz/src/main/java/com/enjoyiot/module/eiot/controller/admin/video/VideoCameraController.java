package com.enjoyiot.module.eiot.controller.admin.video;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoCameraBatchCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoCameraCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoCameraTreeRespVO;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoCameraUpdateReqVO;
import com.enjoyiot.module.eiot.controller.admin.video.vo.VideoLiveUrlRespVO;
import com.enjoyiot.module.eiot.controller.admin.video.vo.WsPlayerStreamRespVO;
import com.enjoyiot.module.eiot.controller.admin.video.vo.WsPlayerTalkReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.video.IccMediaServerDO;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.service.video.MediaServerService;
import com.enjoyiot.module.eiot.service.video.VideoCameraService;
import com.enjoyiot.module.eiot.service.video.icc.IccOpenApiClient;
import com.enjoyiot.module.eiot.service.video.icc.dto.IccStartVideoRespDTO;
import com.enjoyiot.module.eiot.service.video.icc.dto.IccTalkRespDTO;
import com.enjoyiot.module.system.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import com.enjoyiot.module.system.dal.dataobject.tenant.TenantDO;
import com.enjoyiot.module.system.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.enjoyiot.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.enjoyiot.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 摄像头管理")
@RestController
@RequestMapping("/eiot/video/camera")
@Validated
public class VideoCameraController {

    @Resource
    private VideoCameraService videoCameraService;

    @Resource
    private MediaServerService mediaServerService;

    @Resource
    private IccOpenApiClient iccOpenApiClient;

    @Resource
    private TenantService tenantService;

    @PostMapping("/create")
    @Operation(summary = "创建摄像头")
    public CommonResult<Long> create(@RequestBody @Valid VideoCameraCreateReqVO reqVO) {
        return success(videoCameraService.create(convertToDO(reqVO)));
    }

    @PutMapping("/update")
    @Operation(summary = "更新摄像头")
    public CommonResult<Boolean> update(@RequestBody VideoCameraUpdateReqVO reqVO) {
        VideoCameraDO camera = new VideoCameraDO();
        camera.setId(reqVO.getId());
        camera.setDeviceId(reqVO.getDeviceId());
        camera.setChannelId(reqVO.getChannelId());
        camera.setChannelName(reqVO.getChannelName());
        camera.setLongitude(reqVO.getLongitude());
        camera.setLatitude(reqVO.getLatitude());
        camera.setAddress(reqVO.getAddress());
        camera.setRemark(reqVO.getRemark());
        camera.setQuality(reqVO.getQuality());
        camera.setPullType(reqVO.getPullType());
        camera.setType(reqVO.getType());
        camera.setSupportReplay(reqVO.getSupportReplay());
        videoCameraService.update(camera);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除摄像头")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        videoCameraService.delete(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取摄像头详情")
    public CommonResult<VideoCameraDO> get(@RequestParam("id") Long id) {
        return success(videoCameraService.getById(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取摄像头列表")
    public CommonResult<List<VideoCameraDO>> list() {
        return success(videoCameraService.getList());
    }

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

    @GetMapping("/wsplayer/start-video")
    @Operation(summary = "WSPlayer 实时预览取流（RTSP + 一次性 token）")
    public CommonResult<WsPlayerStreamRespVO> wsPlayerStartVideo(
            @RequestParam(value = "cameraId", required = false) Long cameraId,
            @RequestParam(value = "channelId", required = false) String directChannelId,
            @RequestParam(value = "streamType", required = false) String directStreamType) {
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }

        String channelCode;
        String deviceCode;
        String streamType;
        if (directChannelId != null && !directChannelId.isEmpty()) {
            // 直接传入 4 段式通道编码（WSPlayer 内置回调场景）
            channelCode = directChannelId;
            deviceCode = directChannelId.contains("$") ? directChannelId.substring(0, directChannelId.indexOf("$")) : directChannelId;
            streamType = directStreamType != null && !directStreamType.isEmpty() ? directStreamType : "2";
        } else {
            VideoCameraDO camera = getCameraForVideo(cameraId);
            // 通道编码兼容处理：若 channelId 已是 4 段式则直接使用，否则拼接 deviceId$1$0$channelId
            channelCode = camera.getChannelId() != null && camera.getChannelId().contains("$")
                    ? camera.getChannelId()
                    : camera.getDeviceId() + "$1$0$" + camera.getChannelId();
            deviceCode = camera.getDeviceId();
            streamType = camera.getQuality() != null ? camera.getQuality() : "2";
        }

        IccStartVideoRespDTO resp = iccOpenApiClient.startVideo(config, deviceCode, channelCode, streamType);

        WsPlayerStreamRespVO vo = new WsPlayerStreamRespVO();
        vo.setUrl(resp.getUrl());
        vo.setToken(resp.getToken());
        vo.setSession(resp.getSession());
        vo.setChannelId(channelCode);
        vo.setDeviceCode(deviceCode);
        vo.setStreamType(streamType);
        vo.setInnerIp(resp.getInnerIp());
        vo.setWssDirect(resp.getWssDirect());
        vo.setNetFlag(resp.getNetFlag());
        return success(vo);
    }

    @PostMapping("/wsplayer/talk/start")
    @Operation(summary = "WSPlayer 开始对讲（转发 ICC StartTalk）")
    public CommonResult<IccTalkRespDTO> wsPlayerStartTalk(@RequestBody WsPlayerTalkReqVO reqVO) {
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }
        String talkType = reqVO.getTalkType() != null ? reqVO.getTalkType() : "1";
        String audioType = reqVO.getAudioType() != null ? reqVO.getAudioType() : "2";
        IccTalkRespDTO resp = iccOpenApiClient.startTalk(config, reqVO.getDeviceCode(), talkType, audioType);
        return success(resp);
    }

    @PostMapping("/wsplayer/talk/stop")
    @Operation(summary = "WSPlayer 停止对讲（转发 ICC StopTalk）")
    public CommonResult<Boolean> wsPlayerStopTalk(@RequestBody WsPlayerTalkReqVO reqVO) {
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }
        if (reqVO.getSession() == null || reqVO.getSession().isEmpty()) {
            throw new RuntimeException("停止对讲缺少 session");
        }
        String talkType = reqVO.getTalkType() != null ? reqVO.getTalkType() : "1";
        boolean resp = iccOpenApiClient.stopTalk(config, reqVO.getDeviceCode(), talkType, reqVO.getSession());
        return success(resp);
    }

    /** 获取摄像头（兼容平台租户跨租户查询） */
    private VideoCameraDO getCameraForVideo(Long cameraId) {
        Long currentTenantId = TenantUtils.getCurrentTenantId();
        VideoCameraDO camera;
        if (currentTenantId == 1) {
            camera = TenantUtils.executeIgnoreResult(() -> videoCameraService.getById(cameraId));
        } else {
            camera = videoCameraService.getById(cameraId);
        }
        if (camera == null) {
            throw new RuntimeException("摄像头不存在");
        }
        return camera;
    }

    @GetMapping("/icc-devices")
    @Operation(summary = "获取 ICC 平台设备/通道列表（仅平台租户）")
    @PreAuthorize("@ss.hasPermission('video:camera-manage:create')")
    public CommonResult<List<Map<String, Object>>> getIccDevices() {
        // 仅平台租户（系统管理员）可获取 ICC 设备列表，防止企业端越权访问
        Long currentTenantId = TenantUtils.getCurrentTenantId();
        if (currentTenantId == null || currentTenantId != 1L) {
            throw exception(FORBIDDEN);
        }
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }
        List<Map<String, Object>> devices = iccOpenApiClient.getDeviceList(config);
        return success(devices);
    }

    @GetMapping("/tenant-options")
    @Operation(summary = "获取可分配摄像头的租户列表（仅平台租户）")
    @PreAuthorize("@ss.hasPermission('video:camera-manage:create')")
    public CommonResult<List<Map<String, Object>>> getTenantOptions() {
        // 仅平台租户（系统管理员）可获取租户列表，防止企业端越权访问
        Long currentTenantId = TenantUtils.getCurrentTenantId();
        if (currentTenantId == null || currentTenantId != 1L) {
            throw exception(FORBIDDEN);
        }
        List<Map<String, Object>> options = TenantUtils.executeIgnoreWithResult(() -> {
            TenantPageReqVO pageVO = new TenantPageReqVO();
            pageVO.setPageNo(1);
            pageVO.setPageSize(1000);
            return tenantService.getTenantPage(pageVO).getList().stream()
                    .map(t -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", t.getId());
                        m.put("name", t.getName());
                        return m;
                    })
                    .collect(Collectors.toList());
        });
        return success(options);
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建摄像头（ICC 自动获取，仅平台租户，可指定归属租户）")
    @PreAuthorize("@ss.hasPermission('video:camera-manage:create')")
    public CommonResult<Map<String, Object>> batchCreate(@Valid @RequestBody VideoCameraBatchCreateReqVO reqVO) {
        // 仅平台租户（系统管理员）可批量添加摄像头，防止企业端越权
        Long currentTenantId = TenantUtils.getCurrentTenantId();
        if (currentTenantId == null || currentTenantId != 1L) {
            throw exception(FORBIDDEN);
        }
        // 校验目标租户存在
        tenantService.validTenant(reqVO.getTenantId());
        List<VideoCameraDO> cameras = reqVO.getCameras().stream()
                .map(this::convertToDO)
                .collect(Collectors.toList());
        return success(videoCameraService.batchCreate(reqVO.getTenantId(), cameras));
    }

    /** 将创建请求转换为 DO（create 与 batch-create 共用） */
    private VideoCameraDO convertToDO(VideoCameraCreateReqVO reqVO) {
        VideoCameraDO camera = new VideoCameraDO();
        camera.setDeviceId(reqVO.getDeviceId());
        camera.setChannelId(reqVO.getChannelId());
        camera.setChannelName(reqVO.getChannelName());
        camera.setLongitude(reqVO.getLongitude());
        camera.setLatitude(reqVO.getLatitude());
        camera.setAddress(reqVO.getAddress());
        camera.setRemark(reqVO.getRemark());
        camera.setQuality(reqVO.getQuality());
        camera.setPullType(reqVO.getPullType());
        camera.setType(reqVO.getType());
        camera.setSupportReplay(reqVO.getSupportReplay());
        return camera;
    }

    @GetMapping("/tree")
    @Operation(summary = "获取摄像头树（按企业分组）")
    public CommonResult<List<VideoCameraTreeRespVO>> getCameraTree() {
        // 跨租户获取所有摄像头
        List<VideoCameraDO> allCameras = TenantUtils.executeIgnoreResult(
                () -> videoCameraService.getList());
        if (allCameras == null || allCameras.isEmpty()) {
            return success(Collections.emptyList());
        }
        // 按 tenantId 分组
        Map<Long, List<VideoCameraDO>> grouped = allCameras.stream()
                .collect(Collectors.groupingBy(c -> c.getTenantId() != null ? c.getTenantId() : 0L));

        // 批量查询租户名称
        Set<Long> tenantIds = grouped.keySet();
        Map<Long, String> tenantNameMap = new HashMap<>();
        TenantUtils.executeIgnore(() -> {
            for (Long tenantId : tenantIds) {
                try {
                    TenantDO tenant = tenantService.getTenant(tenantId);
                    if (tenant != null) {
                        tenantNameMap.put(tenantId, tenant.getName());
                    }
                } catch (Exception ignored) {}
            }
        });

        // 构建树
        List<VideoCameraTreeRespVO> result = new ArrayList<>();
        grouped.forEach((tenantId, cameras) -> {
            VideoCameraTreeRespVO node = new VideoCameraTreeRespVO();
            node.setId(tenantId);
            node.setName(tenantNameMap.getOrDefault(tenantId, "未知企业"));
            node.setCameraCount(cameras.size());
            node.setCameras(cameras.stream().map(c -> {
                VideoCameraTreeRespVO.CameraItem item = new VideoCameraTreeRespVO.CameraItem();
                item.setId(c.getId());
                item.setChannelName(c.getChannelName() != null ? c.getChannelName() : "未命名摄像头");
                item.setChannelId(c.getChannelId());
                item.setStatus(c.getStatus() != null ? c.getStatus() : 0);
                item.setAddress(c.getAddress());
                // WSPlayer 插件预览/对讲必填字段
                item.setDeviceId(c.getDeviceId());
                item.setQuality(c.getQuality());
                return item;
            }).collect(Collectors.toList()));
            result.add(node);
        });
        return success(result);
    }
}
