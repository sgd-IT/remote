package com.enjoyiot.module.eiot.controller.admin.firewatch;

import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord.CallRecordPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord.CallRecordRespVO;
import com.enjoyiot.module.eiot.service.firewatch.FireCallRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.enjoyiot.framework.common.pojo.CommonResult.success;

/**
 * 话单通话记录 Controller
 * <p>
 * 系统租户可查全部（按租户/号码/方向筛选）；企业租户仅看本租户通话记录。
 * 录音文件通过后端代理拉取（/voice/{id}），避免浏览器直接访问内网录音地址的跨域问题。
 *
 * @author EnjoyIot
 */
@Tag(name = "管理后台 - 话单通话记录")
@RestController
@RequestMapping("/eiot/firewatch/call-record")
@Validated
public class FireCallRecordController {

    @Autowired
    private FireCallRecordService fireCallRecordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询通话记录", description = "分页查询通话记录，系统租户可按租户/号码/方向筛选")
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:call-record:query')")
    public CommonResult<PageResult<CallRecordRespVO>> pageRecords(@Valid CallRecordPageReqVO pageReqVO) {
        return success(fireCallRecordService.pageRecords(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取通话记录详情", description = "获取通话记录详细信息")
    @Parameter(name = "id", description = "通话记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:call-record:query')")
    public CommonResult<CallRecordRespVO> getRecord(@RequestParam("id") Long id) {
        return success(fireCallRecordService.getRecord(id));
    }

    @GetMapping("/voice/{id}")
    @Operation(summary = "下载/播放录音文件（代理）", description = "通过后端代理拉取能力开放平台录音文件")
    @Parameter(name = "id", description = "通话记录ID", required = true)
    @Parameter(name = "side", description = "录音侧：mixed=混合 in=主叫 out=被叫", required = false)
    @PreAuthorize("@ss.hasPermission('eiot:firewatch:call-record:voice')")
    public ResponseEntity<Resource> downloadVoice(@PathVariable("id") Long id,
                                                  @RequestParam(value = "side", required = false) String side) {
        Resource resource = fireCallRecordService.downloadVoice(id, side);
        String filename = resource.getFilename() == null ? "voice.wav" : resource.getFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(resource);
    }
}
