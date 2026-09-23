package com.enjoyiot.module.eiot.controller.admin.alert;


import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.IdReqVo;
import com.enjoyiot.module.eiot.controller.admin.alert.vo.ChannelReqVO;
import com.enjoyiot.module.eiot.controller.admin.channelconfig.vo.Channel;
import com.enjoyiot.module.eiot.controller.admin.channelconfig.vo.ChannelConfig;
import com.enjoyiot.module.eiot.controller.admin.channelconfig.vo.ChannelConfigPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.channelconfig.vo.ChannelConfigReqVO;
import com.enjoyiot.module.eiot.controller.admin.channeltemplate.vo.ChannelTemplate;
import com.enjoyiot.module.eiot.controller.admin.channeltemplate.vo.ChannelTemplatePageReqVO;
import com.enjoyiot.module.eiot.controller.admin.channeltemplate.vo.ChannelTemplateSaveReqVO;
import com.enjoyiot.module.eiot.service.alert.ChannelConfigService;
import com.enjoyiot.module.eiot.service.alert.ChannelService;
import com.enjoyiot.module.eiot.service.alert.ChannelTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * author: 石恒
 * date: 2023-05-11 15:17
 * description:
 **/
@Tag(name = "消息通知")
@Slf4j
@RestController
@RequestMapping("/notify")
public class NotifyController {

    @Resource
    private ChannelService channelService;

    @Resource
    private ChannelConfigService channelConfigService;

    @Resource
    private ChannelTemplateService channelTemplateService;

    @Operation(summary = "获取通道类型列表")
    @PreAuthorize("@ss.hasPermission('iot:channel:query')")
    @PostMapping("/channel/getList")
    public CommonResult<List<Channel>> getChannelList(@RequestBody @Validated ChannelReqVO reqVO) {
        return CommonResult.success(channelService.getChannelList(reqVO));
    }

    @Operation(summary = "获取通道配置分页列表")
    @PreAuthorize("@ss.hasPermission('iot:channel:query')")
    @PostMapping("/channel/config/getList")
    public CommonResult<PageResult<ChannelConfig>> getChannelConfigList(@RequestBody @Validated ChannelConfigPageReqVO request) {
        return CommonResult.success(channelConfigService.getChannelConfigPage(request));
    }

    @Operation(summary = "获取通道配置列表")
    @PreAuthorize("@ss.hasPermission('iot:channel:query')")
    @PostMapping("/channel/config/getAll")
    public CommonResult<List<ChannelConfig>> getChannelConfigAll(@RequestBody @Validated ChannelConfigReqVO reqVO) {
        return CommonResult.success(channelConfigService.getChannelConfigAll(reqVO));
    }

    @Operation(summary = "新增通道配置")
    @PreAuthorize("@ss.hasPermission('iot:channel:add')")
    @PostMapping("/channel/config/add")
    public CommonResult<Long> addChannelConfig(@RequestBody @Validated ChannelConfig request) {
        return CommonResult.success(channelConfigService.createChannelConfig(request));
    }

    @Operation(summary = "根据ID获取通道配置")
    @PreAuthorize("@ss.hasPermission('iot:channel:query')")
    @PostMapping("/channel/config/getById")
    public CommonResult<ChannelConfig> getChannelConfigById(@RequestBody @Validated IdReqVo request) {
        return CommonResult.success(channelConfigService.getChannelConfig(request.getId()));
    }

    @Operation(summary = "修改通道配置")
    @PreAuthorize("@ss.hasPermission('iot:channel:edit')")
    @PostMapping("/channel/config/updateById")
    public CommonResult<Boolean> updateChannelConfigById(@RequestBody @Validated ChannelConfig request) {
        return CommonResult.success(channelConfigService.updateChannelConfig(request));
    }

    @Operation(summary = "删除通道配置")
    @PreAuthorize("@ss.hasPermission('iot:channel:remove')")
    @PostMapping("/channel/config/delById")
    public CommonResult<Boolean> delChannelConfigById(@RequestBody @Validated IdReqVo reqVo) {
        channelConfigService.deleteChannelConfig(reqVo.getId());
        return CommonResult.success(true);
    }

    @Operation(summary = "获取通道模板列表")
    @PreAuthorize("@ss.hasPermission('iot:channel:query')")
    @PostMapping("/channel/template/getList")
    public CommonResult<PageResult<ChannelTemplate>> getChannelTemplateList(@RequestBody @Validated ChannelTemplatePageReqVO request) {
        return CommonResult.success(channelTemplateService.getChannelTemplatePage(request));
    }

    @Operation(summary = "新增通道模板")
    @PreAuthorize("@ss.hasPermission('iot:channel:add')")
    @PostMapping("/channel/template/add")
    public CommonResult<Long> addChannelTemplate(@RequestBody @Validated ChannelTemplateSaveReqVO request) {
        return CommonResult.success(channelTemplateService.createChannelTemplate(request));
    }

    @Operation(summary = "根据ID获取通道模板")
    @PreAuthorize("@ss.hasPermission('iot:channel:query')")
    @PostMapping("/channel/template/getById")
    public CommonResult<ChannelTemplate> getChannelTemplateById(@RequestBody @Validated IdReqVo request) {
        return CommonResult.success(channelTemplateService.getChannelTemplate(request.getId()));
    }

    @Operation(summary = "修改通道模板")
    @PreAuthorize("@ss.hasPermission('iot:channel:update')")
    @PostMapping("/channel/template/updateById")
    public CommonResult<Boolean> updateChannelTemplateById(@RequestBody @Validated ChannelTemplateSaveReqVO request) {
        channelTemplateService.updateChannelTemplate(request);
        return CommonResult.success(true);
    }

    @Operation(summary = "删除通道模板")
    @PreAuthorize("@ss.hasPermission('iot:channel:remove')")
    @PostMapping("/channel/template/delById")
    public CommonResult<Boolean> delChannelTemplateById(@RequestBody @Validated IdReqVo request) {
        channelTemplateService.deleteById(request.getId());
        return CommonResult.success(true);
    }

//    @Operation(summary = "消息列表")
//    @PreAuthorize("@ss.hasPermission('iot:channelMsg:query')")
//    @PostMapping("/message/getList")
//    public CommonResult<PageResult<NotifyMessage>> messageList(@RequestBody @Validated NotifyMessagePageReq request) {
//        return CommonResult.success(notifyService.getNotifyMessageList(request));
//    }

}
