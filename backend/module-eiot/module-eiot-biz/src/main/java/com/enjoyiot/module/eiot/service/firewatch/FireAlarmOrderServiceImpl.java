package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;
import com.enjoyiot.module.eiot.dal.dataobject.category.CategoryDO;
import com.enjoyiot.module.eiot.dal.dataobject.deviceinfo.EiotDeviceInfoDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FirePropertyDO;
import com.enjoyiot.module.eiot.dal.dataobject.product.ProductDO;
import com.enjoyiot.module.eiot.dal.mysql.AlertRecordMapper;
import com.enjoyiot.module.eiot.dal.mysql.category.CategoryMapper;
import com.enjoyiot.module.eiot.dal.mysql.deviceinfo.EiotDeviceInfoMapper;
import com.enjoyiot.module.eiot.dal.mysql.firewatch.FirePropertyMapper;
import com.enjoyiot.module.eiot.dal.mysql.product.ProductMapper;
import com.enjoyiot.module.eiot.service.UserTenantResolver;
import com.enjoyiot.module.system.api.sms.SmsSendApi;
import com.enjoyiot.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.enjoyiot.module.system.api.tenant.TenantApi;
import com.enjoyiot.module.system.api.user.AdminUserApi;
import com.enjoyiot.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 火警处置中心 Service 实现类
 * <p>
 * 数据源：eiot_alert_record（IoT 告警记录表）
 *
 * @author EnjoyIot
 */
@Slf4j
@Service
public class FireAlarmOrderServiceImpl implements FireAlarmOrderService {

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private FirePropertyMapper firePropertyMapper;

    @Resource
    private EiotDeviceInfoMapper eiotDeviceInfoMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private SmsSendApi smsSendApi;

    @Resource
    private TenantApi tenantApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private UserTenantResolver userTenantResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAlarmOrder(AlarmOrderCreateReqVO createReqVO) {
        long now = System.currentTimeMillis();
        long alertTimeMs = createReqVO.getAlarmTime() != null
                ? createReqVO.getAlarmTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : now;

        // 设置当前租户ID，确保告警记录有tenantId用于后续短信发送
        Long tenantId = TenantContextHolder.getTenantId();
        // 如果是系统租户(admin)，尝试从请求参数中获取物业ID再反查tenantId
        if (tenantId != null && tenantId == 1L && createReqVO.getPropertyId() != null) {
            // 忽略租户过滤查询，因为管理员需要查其他租户的物业
            FirePropertyDO property = TenantUtils.executeIgnoreWithResult(() ->
                    firePropertyMapper.selectById(createReqVO.getPropertyId()));
            if (property != null && property.getTenantId() != null) {
                tenantId = property.getTenantId();
            }
        }

        AlertRecordDO record = AlertRecordDO.builder()
                .alertTime(alertTimeMs)
                .level(createReqVO.getAlarmLevel() != null ? createReqVO.getAlarmLevel() : "1")
                .name(createReqVO.getAlarmDescription())
                .details(createReqVO.getAlarmDescription())
                .alarmType(0)
                .readFlg(false)
                .build();

        // 显式设置租户ID
        if (tenantId != null) {
            record.setTenantId(tenantId);
        }

        alertRecordMapper.insert(record);
        log.info("[createAlarmOrder] 创建告警记录成功，recordId：{}，tenantId：{}", record.getId(), record.getTenantId());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlarmOrder(AlarmOrderHandleReqVO handleReqVO) {
        if (isSystemTenant()) {
            TenantUtils.executeIgnore(() -> doHandleAlarmOrder(handleReqVO));
        } else {
            doHandleAlarmOrder(handleReqVO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmAlarm(Long id, String handler, String remark) {
        markReadAndAppend(id, handler, "确认火警", remark);
        log.info("[confirmAlarm] 确认火警，recordId：{}，处理人：{}", id, handler);

        // 发送火警确认短信通知（异常不阻塞主流程）
        try {
            sendConfirmSmsNotification(id, handler);
        } catch (Exception e) {
            log.error("[confirmAlarm] 发送火警确认短信异常，recordId：{}", id, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFalseAlarm(Long id, String handler, String remark) {
        markReadAndAppend(id, handler, "标记误报", remark);
        log.info("[markFalseAlarm] 标记为误报，recordId：{}，处理人：{}", id, handler);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRealFire(Long id, String handler, String remark) {
        markReadAndAppend(id, handler, "确认真实火警", remark);
        log.info("[confirmRealFire] 确认真实火警，recordId：{}，处理人：{}", id, handler);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Long id, String handler, String remark) {
        markReadAndAppend(id, handler, "关闭工单", remark);
        log.info("[closeOrder] 关闭工单，recordId：{}，处理人：{}", id, handler);
    }

    @Override
    public void resetDevice(Long id) {
        log.info("[resetDevice] 复位设备，recordId：{}", id);
    }

    @Override
    public void muteAlarm(Long id) {
        log.info("[muteAlarm] 消音，recordId：{}", id);
    }

    @Override
    public void makePhoneCall(Long id, String phone) {
        log.info("[makePhoneCall] 拨打电话，recordId：{}，电话：{}", id, phone);
    }

    @Override
    public void sendNotification(Long id, String[] channels) {
        log.info("[sendNotification] 发送通知，recordId：{}，渠道：{}", id, (Object) channels);
    }

    @Override
    public AlarmOrderRespVO getAlarmOrder(Long id) {
        // 按当前登录用户账号所属租户过滤；系统租户(admin)账号保留看全部
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        return userTenantId != null
                ? TenantUtils.execute(userTenantId, () -> doGetAlarmOrder(id))
                : doGetAlarmOrder(id);
    }

    @Override
    public PageResult<AlarmOrderRespVO> pageAlarmOrders(AlarmOrderPageReqVO pageReqVO) {
        // 按当前登录用户账号所属租户过滤；系统租户(admin)账号保留看全部（由租户拦截器规则保证）
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        return userTenantId != null
                ? TenantUtils.execute(userTenantId, () -> doPageFromAlertRecord(pageReqVO))
                : doPageFromAlertRecord(pageReqVO);
    }

    @Override
    public AlarmRecordStatisticsRespVO getAlarmRecordStatistics() {
        // 按当前登录用户账号所属租户统计；系统租户(admin)账号保留看全部
        Long userTenantId = userTenantResolver.resolveLoginUserTenantId();
        return userTenantId != null
                ? TenantUtils.execute(userTenantId, this::buildAlarmRecordStatistics)
                : buildAlarmRecordStatistics();
    }

    // ==================== PDF 导出 ====================

    @Override
    public void exportAlarmOrderPdf(String deviceType, String alarmType, String status,
                                    Long propertyId, String startTime, String endTime,
                                    HttpServletResponse response) throws IOException {
        // 1. 查询数据
        List<AlertRecordDO> records;
        if (isSystemTenant()) {
            records = TenantUtils.executeIgnoreWithResult(() ->
                    doQueryAlertRecordsForExport(deviceType, alarmType, status, propertyId, startTime, endTime));
        } else {
            records = doQueryAlertRecordsForExport(deviceType, alarmType, status, propertyId, startTime, endTime);
        }
        if (records == null) records = new ArrayList<>();

        // 2. 生成PDF
        try (PDDocument document = new PDDocument()) {
            PDType0Font font = loadChineseFont(document);

            // A4 横向
            float pageWidth = PDRectangle.A4.getHeight();
            float pageHeight = PDRectangle.A4.getWidth();
            float marginLeft = 36, marginRight = 36, marginTop = 48, marginBottom = 28;
            float usableWidth = pageWidth - marginLeft - marginRight;

            // 列定义
            String[] headers = {"报警时间", "所属单位", "设备名称", "设备类型", "报警类型", "安装位置", "处置状态", "处置人", "处置时间", "响应时长"};
            float[] colWidths = {118, 92, 86, 60, 56, 80, 52, 52, 118, 48};
            // 按比例缩放填满可用宽度
            float totalDef = 0;
            for (float w : colWidths) totalDef += w;
            float scale = usableWidth / totalDef;
            float[] widths = new float[colWidths.length];
            for (int i = 0; i < colWidths.length; i++) {
                widths[i] = colWidths[i] * scale;
            }

            float rowHeight = 17f;
            float headerRowHeight = 19f;
            float titleAreaHeight = 55f;
            float usableHeight = pageHeight - marginTop - marginBottom - titleAreaHeight;
            int rowsPerPage = Math.max(1, (int) (usableHeight / rowHeight));

            // 数据转换
            List<Map<String, String>> dataRows = new ArrayList<>();
            for (AlertRecordDO record : records) {
                dataRows.add(convertRecordToDisplayRow(record));
            }

            int totalRows = dataRows.size();
            int totalPages = (totalRows + rowsPerPage - 1) / rowsPerPage;
            if (totalPages == 0) totalPages = 1;

            for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                PDPage pdfPage = new PDPage(new PDRectangle(pageWidth, pageHeight));
                document.addPage(pdfPage);

                try (PDPageContentStream cs = new PDPageContentStream(document, pdfPage)) {
                    // 标题
                    cs.setFont(font, 15);
                    cs.beginText();
                    cs.newLineAtOffset(marginLeft, pageHeight - marginTop + 6);
                    cs.showText("报警记录报表");
                    cs.endText();

                    // 元信息
                    cs.setFont(font, 7.5f);
                    String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    StringBuilder filterInfo = new StringBuilder("生成时间: ").append(nowStr);
                    if (StrUtil.isNotBlank(alarmType) || StrUtil.isNotBlank(status) || propertyId != null) {
                        filterInfo.append("  筛选: ");
                        if (propertyId != null) filterInfo.append("物业ID=").append(propertyId).append(" ");
                        if (StrUtil.isNotBlank(alarmType))
                            filterInfo.append("类型=").append(getAlarmTypeName(alarmType)).append(" ");
                        if (StrUtil.isNotBlank(status)) filterInfo.append("状态=").append(status).append(" ");
                    }
                    cs.beginText();
                    cs.newLineAtOffset(marginLeft, pageHeight - marginTop - 18);
                    cs.showText(filterInfo.toString());
                    cs.endText();

                    // 页码
                    String pageText = "第 " + (pageNum + 1) + " / " + totalPages + " 页  共 " + totalRows + " 条记录";
                    cs.beginText();
                    cs.newLineAtOffset(pageWidth - marginRight - 150, pageHeight - marginTop - 18);
                    cs.showText(pageText);
                    cs.endText();

                    // 表格起始Y
                    float tableY = pageHeight - marginTop - titleAreaHeight;
                    float yPos = tableY;

                    // 画表头
                    drawPdfTableHeader(cs, font, headers, widths, marginLeft, yPos, headerRowHeight);
                    yPos -= headerRowHeight;

                    // 画数据行
                    int startIdx = pageNum * rowsPerPage;
                    int endIdx = Math.min(startIdx + rowsPerPage, totalRows);
                    for (int i = startIdx; i < endIdx; i++) {
                        Map<String, String> row = dataRows.get(i);
                        String[] values = {
                                row.getOrDefault("alarmTime", ""),
                                row.getOrDefault("propertyName", ""),
                                row.getOrDefault("deviceName", ""),
                                row.getOrDefault("deviceTypeName", ""),
                                row.getOrDefault("alarmTypeName", ""),
                                row.getOrDefault("location", ""),
                                row.getOrDefault("statusName", ""),
                                row.getOrDefault("handler", ""),
                                row.getOrDefault("handleTime", ""),
                                row.getOrDefault("responseTime", "")
                        };
                        drawPdfTableRow(cs, font, values, widths, marginLeft, yPos, rowHeight,
                                i % 2 == 0);
                        yPos -= rowHeight;
                    }

                    // 如果当前页数据不满，补齐底部横线
                    if (startIdx + rowsPerPage >= totalRows && yPos > marginBottom) {
                        cs.setStrokingColor(new Color(0.8f, 0.8f, 0.8f));
                        cs.moveTo(marginLeft, yPos);
                        cs.lineTo(marginLeft + usableWidth, yPos);
                        cs.stroke();
                        cs.setStrokingColor(Color.BLACK);
                    }
                }
            }

            // 3. 输出响应
            response.setContentType("application/pdf");
            String encodedFilename = java.net.URLEncoder.encode(
                    "报警记录_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            document.save(response.getOutputStream());
        }
    }

    // ==================== 私有方法（按调用顺序） ====================

    /**
     * 判断当前是否为系统租户（tenant_id=1，即admin）
     */
    private boolean isSystemTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 1L;
    }

    private void doHandleAlarmOrder(AlarmOrderHandleReqVO handleReqVO) {
        AlertRecordDO record = alertRecordMapper.selectById(handleReqVO.getId());
        if (record == null) {
            throw new IllegalArgumentException("告警记录不存在");
        }

        String handleType = handleReqVO.getHandleType();
        String action;
        switch (handleType) {
            case "confirm":
                action = "确认火警";
                break;
            case "false_alarm":
                action = "标记误报";
                break;
            case "real_fire":
                action = "确认真实火警";
                break;
            case "close":
                action = "关闭工单";
                break;
            case "manual":
                action = "手动处理";
                break;
            default:
                throw new IllegalArgumentException("未知的处理类型：" + handleType);
        }

        // 标记为已读 + 追加处置备注
        record.setReadFlg(true);
        String newDetails = appendRemarkToDetails(record.getDetails(), handleReqVO.getHandler(), action, handleReqVO.getHandleRemark());
        record.setDetails(newDetails);
        alertRecordMapper.updateById(record);
        log.info("[handleAlarmOrder] 处置告警，recordId：{}，action：{}", handleReqVO.getId(), action);
    }

    private String appendRemarkToDetails(String details, String handler, String action, String remark) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(details)) {
            sb.append(details).append("\n");
        }
        sb.append("[").append(LocalDateTime.now()).append("] ");
        if (StrUtil.isNotBlank(handler)) {
            sb.append(handler).append(" - ");
        }
        sb.append(action);
        if (StrUtil.isNotBlank(remark)) {
            sb.append("：").append(remark);
        }
        return sb.toString();
    }

    /**
     * 标记已读并追加处置备注
     */
    private void markReadAndAppend(Long id, String handler, String action, String remark) {
        if (isSystemTenant()) {
            TenantUtils.executeIgnore(() -> doMarkReadAndAppend(id, handler, action, remark));
        } else {
            doMarkReadAndAppend(id, handler, action, remark);
        }
    }

    private void doMarkReadAndAppend(Long id, String handler, String action, String remark) {
        AlertRecordDO record = alertRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("告警记录不存在: id=" + id);
        }
        record.setReadFlg(true);
        String newDetails = appendRemarkToDetails(record.getDetails(), handler, action, remark);
        record.setDetails(newDetails);
        alertRecordMapper.updateById(record);
    }

    /**
     * 发送火警确认短信通知
     * 通过 tenantId 查询企业联系人，逐个发送短信
     */
    private void sendConfirmSmsNotification(Long recordId, String handler) {
        // 短信发送不区分租户，直接执行（不受租户拦截器限制）
        if (isSystemTenant()) {
            TenantUtils.executeIgnore(() -> doSendConfirmSmsNotification(recordId, handler));
        } else {
            doSendConfirmSmsNotification(recordId, handler);
        }
    }

    private void doSendConfirmSmsNotification(Long recordId, String handler) {
        AlertRecordDO record = alertRecordMapper.selectById(recordId);
        if (record == null) {
            log.warn("[sendConfirmSmsNotification] 告警记录不存在，recordId：{}", recordId);
            return;
        }

        // 1. 定位告警所属企业 可能为空：未匹配到时仅发租户用户短信
        List<FirePropertyDO> properties = resolveEnterpriseByAlert(record, recordId);
        if (properties.isEmpty()) {
            log.warn("[sendConfirmSmsNotification] 未匹配到告警所属企业，recordId：{}", recordId);
        }

        // 2. 提取企业联系人手机号（contactPhone 可能逗号分隔多个号码，需拆分）
        List<String> proPhones = properties.stream()
                .map(FirePropertyDO::getContactPhone)
                .filter(StrUtil::isNotBlank)
                .flatMap(p -> Arrays.stream(p.split("[,，\\s]+")))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());

        //3.提取租户下启用用户的手机号
        List<String> userPhones = adminUserApi.getUserListByTenantId(record.getTenantId()).stream()
                .map(AdminUserRespDTO::getMobile)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());

        //4.合并手机号列表，trim 后按值去重（同一手机号只发一条）
        LinkedHashSet<String> phoneSet = new LinkedHashSet<>();
        proPhones.forEach(p -> phoneSet.add(p.trim()));
        userPhones.forEach(p -> phoneSet.add(p.trim()));
        List<String> phones = new ArrayList<>(phoneSet);
        //5.校验手机号
        if (phones.isEmpty()) {
            log.warn("[sendConfirmSmsNotification] 无有效接收手机号，recordId：{}，企业：{}",
                    recordId, properties.stream().map(FirePropertyDO::getName).collect(Collectors.joining(",")));
            return;
        }

        // 6. 发送短信
        sendSmsToProperties(record, phones);
        log.info("[sendConfirmSmsNotification] 短信发送完成，recordId：{}，企业：{}，手机号：{}",
                recordId,
                properties.isEmpty() ? "-" : properties.get(0).getName(),
                String.join(",", phones));
    }

    /**
     * 根据告警记录定位所属企业
     * 策略：tenantId → deviceId → 未匹配返回空（仅发租户用户短信）
     */
    private List<FirePropertyDO> resolveEnterpriseByAlert(AlertRecordDO record, Long recordId) {
        Long tenantId = record.getTenantId();

        // 策略1: 通过告警记录的 tenantId 直接查询
        if (tenantId != null && tenantId > 0) {
            List<FirePropertyDO> properties = firePropertyMapper.selectList(
                    new LambdaQueryWrapperX<FirePropertyDO>()
                            .eq(FirePropertyDO::getTenantId, tenantId)
            );
            if (!properties.isEmpty()) {
                log.info("[resolveEnterpriseByAlert] 通过tenantId={}找到{}个企业，recordId：{}",
                        tenantId, properties.size(), recordId);
                return properties;
            }
        }

        // 策略2: 通过 deviceId 反查设备→获取设备所属租户→查企业
        if (record.getDeviceId() != null && record.getDeviceId() > 0) {
            EiotDeviceInfoDO device = eiotDeviceInfoMapper.selectById(record.getDeviceId());
            if (device != null && device.getTenantId() != null && device.getTenantId() > 0) {
                List<FirePropertyDO> properties = firePropertyMapper.selectList(
                        new LambdaQueryWrapperX<FirePropertyDO>()
                                .eq(FirePropertyDO::getTenantId, device.getTenantId())
                );
                if (!properties.isEmpty()) {
                    log.info("[resolveEnterpriseByAlert] 通过deviceId={}→device.tenantId={}找到{}个企业，recordId：{}",
                            record.getDeviceId(), device.getTenantId(), properties.size(), recordId);
                    return properties;
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 发送短信通知
     *
     * @param record 告警记录
     * @param phones 去重后的接收手机号
     */
    private void sendSmsToProperties(AlertRecordDO record, List<String> phones) {
        // 防漂移——改模板占位符时同步改这里，或改为按模板 params 动态取值。
        // 设备/部件信息取自设备表（name=部件类型，addr=地址）
        EiotDeviceInfoDO device = record.getDeviceId() != null ? eiotDeviceInfoMapper.selectById(record.getDeviceId()) : null;
        EiotDeviceInfoDO subDevice = record.getSubDeviceId() != null ? eiotDeviceInfoMapper.selectById(record.getSubDeviceId()) : null;
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("event_name", StrUtil.blankToDefault(record.getName(), ""));
        templateParams.put("event_time", formatAlertTime(record.getAlertTime()));
        templateParams.put("deviceName", StrUtil.blankToDefault(record.getDeviceName(), ""));
        templateParams.put("deviceAddr", device != null ? StrUtil.blankToDefault(device.getAddr(), "") : "");
        templateParams.put("subDeviceName", subDevice != null ? StrUtil.blankToDefault(subDevice.getName(), "") : "");
        templateParams.put("subDeviceAddr", subDevice != null ? StrUtil.blankToDefault(subDevice.getAddr(), "") : "");

        int successCount = 0;
        int failCount = 0;
        for (String phone : phones) {
            try {
                SmsSendSingleToUserReqDTO reqDTO = new SmsSendSingleToUserReqDTO();
                reqDTO.setMobile(phone.trim());
                reqDTO.setTemplateCode("fire_alarm_confirmed_notify");
                reqDTO.setTemplateParams(templateParams);
                smsSendApi.sendSingleSmsToAdmin(reqDTO);
                successCount++;
                log.info("[sendConfirmSmsNotification] 确认短信发送成功，手机号：{}，recordId：{}", phone, record.getId());
            } catch (Exception e) {
                failCount++;
                log.error("[sendConfirmSmsNotification] 发送确认短信失败，手机号：{}，recordId：{}", phone, record.getId(), e);
            }
        }
        log.info("[sendConfirmSmsNotification] 短信发送完成，recordId：{}，成功：{}，失败：{}", record.getId(), successCount, failCount);
    }

    private String mapLevelToAlarmType(String level) {
        if (level == null) return "fire";
        switch (level) {
            case "1":
                return "fire";        // 火警
            case "2":
                return "fault";       // 故障
            case "3":
                return "risk";        // 隐患
            case "4":
                return "offline";     // 离线
            case "5":
                return "false_alarm"; // 误报
            case "6":
                return "info";        // 信息
            default:
                return "fire";
        }
    }

    private String getAlarmTypeName(String alarmType) {
        if (alarmType == null) return "";
        switch (alarmType) {
            case "fire":
                return "火警";
            case "fault":
                return "故障";
            case "risk":
                return "隐患";
            case "offline":
                return "离线";
            case "false_alarm":
                return "误报";
            default:
                return alarmType;
        }
    }

    private String formatAlertTime(Long alertTimeMs) {
        if (alertTimeMs == null) return "";
        LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(alertTimeMs), ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }


    private AlarmOrderRespVO doGetAlarmOrder(Long id) {
        AlertRecordDO record = alertRecordMapper.selectById(id);
        if (record == null) {
            return null;
        }
        AlarmOrderRespVO vo = convertToRespVO(record);
        // 通过多表关联获取设备类型
        fillDeviceType(vo, record);
        return vo;
    }

    /**
     * 从 eiot_alert_record 分页查询告警记录并转换为 AlarmOrderRespVO
     */
    private PageResult<AlarmOrderRespVO> doPageFromAlertRecord(AlarmOrderPageReqVO pageReqVO) {
        log.info("【报警记录搜索】搜索参数: pageNo={}, pageSize={}, alarmType={}, alarmLevel={}, orderStatus={}, propertyId={}, deviceType={}, timeBegin={}, timeEnd={}",
                pageReqVO.getPageNo(), pageReqVO.getPageSize(),
                pageReqVO.getAlarmType(), pageReqVO.getAlarmLevel(),
                pageReqVO.getOrderStatus(), pageReqVO.getPropertyId(),
                pageReqVO.getDeviceType(),
                pageReqVO.getAlarmTimeBegin(), pageReqVO.getAlarmTimeEnd());

        LambdaQueryWrapperX<AlertRecordDO> wrapper = new LambdaQueryWrapperX<AlertRecordDO>()
                .orderByDesc(AlertRecordDO::getAlertTime);

        // ====== 1. 处置状态筛选 ======
        // 筛选逻辑与 AlertOrderStatusResolver.resolveOrderStatus 保持一致（含 handle_status 处置状态），
        // 保证按状态筛选的结果与列表展示的状态一致
        if (StrUtil.isNotBlank(pageReqVO.getOrderStatus())) {
            log.info("【报警记录搜索】按处置状态筛选: {}", pageReqVO.getOrderStatus());
            switch (pageReqVO.getOrderStatus()) {
                case "pending":
                case "handling":
                case "handled":
                case "closed":
                    wrapper.apply(AlertOrderStatusResolver.ORDER_STATUS_CASE_SQL + " = {0}", pageReqVO.getOrderStatus());
                    break;
                default:
                    log.warn("【报警记录搜索】未知的处置状态: {}", pageReqVO.getOrderStatus());
            }
        }

        // ====== 2. 报警类型筛选（前端传 alarmType） ======
        if (StrUtil.isNotBlank(pageReqVO.getAlarmType())) {
            String level = mapAlarmTypeToLevel(pageReqVO.getAlarmType());
            log.info("【报警记录搜索】按报警类型筛选: alarmType={}, mappedLevel={}", pageReqVO.getAlarmType(), level);
            if (level != null) {
                wrapper.eq(AlertRecordDO::getLevel, level);
            } else {
                log.warn("【报警记录搜索】无法映射报警类型: {}", pageReqVO.getAlarmType());
            }
        }

        // ====== 3. 兼容 alarmLevel 参数（后端 VO 原有字段） ======
        if (StrUtil.isNotBlank(pageReqVO.getAlarmLevel())) {
            String recordLevel = mapAlarmLevelToRecordLevel(pageReqVO.getAlarmLevel());
            log.info("【报警记录搜索】按告警级别筛选: alarmLevel={}, mappedLevel={}", pageReqVO.getAlarmLevel(), recordLevel);
            if (recordLevel != null) {
                wrapper.eq(AlertRecordDO::getLevel, recordLevel);
            }
        }

        // ====== 4. 所属单位筛选：propertyId → tenantId ======
        if (pageReqVO.getPropertyId() != null) {
            log.info("【报警记录搜索】按所属单位筛选: propertyId={}", pageReqVO.getPropertyId());
            // 忽略租户过滤查询，因为需要按物业ID查到其所属租户
            FirePropertyDO property = TenantUtils.executeIgnoreWithResult(() ->
                    firePropertyMapper.selectById(pageReqVO.getPropertyId()));
            if (property != null && property.getTenantId() != null) {
                log.info("【报警记录搜索】propertyId={} 对应 tenantId={}", pageReqVO.getPropertyId(), property.getTenantId());
                wrapper.eq(AlertRecordDO::getTenantId, property.getTenantId());
            } else {
                log.warn("【报警记录搜索】未找到物业或物业无租户: propertyId={}", pageReqVO.getPropertyId());
            }
        }

        // ====== 5. 设备类型筛选：deviceType(即category_id) → device_id列表 ======
        if (StrUtil.isNotBlank(pageReqVO.getDeviceType())) {
            log.info("【报警记录搜索】按设备类型筛选: deviceType(categoryId)={}", pageReqVO.getDeviceType());
            // 子查询：通过category_id查找匹配的设备ID，再过滤告警记录
            wrapper.apply("device_id IN (SELECT d.id FROM eiot_device_info d " +
                    "JOIN eiot_product p ON d.product_key = p.product_key " +
                    "WHERE p.category_id = {0})", pageReqVO.getDeviceType());
        }

        // ====== 6. 告警时间范围 ======
        if (pageReqVO.getAlarmTimeBegin() != null) {
            long beginMs = pageReqVO.getAlarmTimeBegin()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            wrapper.ge(AlertRecordDO::getAlertTime, beginMs);
            log.info("【报警记录搜索】时间范围开始: {} -> {}ms", pageReqVO.getAlarmTimeBegin(), beginMs);
        }
        if (pageReqVO.getAlarmTimeEnd() != null) {
            long endMs = pageReqVO.getAlarmTimeEnd()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            wrapper.le(AlertRecordDO::getAlertTime, endMs);
            log.info("【报警记录搜索】时间范围结束: {} -> {}ms", pageReqVO.getAlarmTimeEnd(), endMs);
        }

        log.info("【报警记录搜索】开始执行分页查询...");

        // 分页查询
        PageResult<AlertRecordDO> pageResult = alertRecordMapper.selectPage(pageReqVO, wrapper);
        log.info("【报警记录搜索】查询完成: total={}, 本页数量={}", pageResult.getTotal(), pageResult.getList().size());

        // 转换为 VO 并填充扩展字段
        List<AlarmOrderRespVO> list = new ArrayList<>();
        for (AlertRecordDO record : pageResult.getList()) {
            AlarmOrderRespVO vo = convertToRespVO(record);
            // 填充设备类型名称
            fillDeviceType(vo, record);
            // 填充租户名称
            if (record.getTenantId() != null && record.getTenantId() > 0) {
                String tenantName = tenantApi.getTenantName(record.getTenantId());
                vo.setTenantName(tenantName);
                // 如果 propertyName 为空，用租户名称作为默认值
                if (vo.getPropertyName() == null) {
                    vo.setPropertyName(tenantName);
                }
            }
            list.add(vo);
        }

        return new PageResult<>(list, pageResult.getTotal());
    }

    /**
     * 将前端报警类型(fire/fault/risk/offline/false_alarm)映射到 eiot_alert_record.level 值
     */
    private String mapAlarmTypeToLevel(String alarmType) {
        if (alarmType == null) return null;
        switch (alarmType) {
            case "fire":
                return "1";
            case "fault":
                return "2";
            case "risk":
                return "3";
            case "offline":
                return "4";
            case "false_alarm":
                return "5";
            case "info":
                return "6";
            default:
                return null;
        }
    }

    /**
     * 将前端告警级别映射回 eiot_alert_record.level 值
     */
    private String mapAlarmLevelToRecordLevel(String alarmLevel) {
        if (alarmLevel == null) return null;
        switch (alarmLevel) {
            case "critical":
                return "1";
            case "major":
                return "2";
            case "warning":
                return "3";
            case "info":
                return "4";
            default:
                return null;
        }
    }

    private AlarmRecordStatisticsRespVO buildAlarmRecordStatistics() {
        AlarmRecordStatisticsRespVO result = new AlarmRecordStatisticsRespVO();

        // 1. 报警总数
        Integer total = alertRecordMapper.countTotalAlerts();
        result.setTotalAlarms(total != null ? total : 0);

        // 2. 已处置数（已读 = 已处置）
        Integer handled = alertRecordMapper.countReadAlerts();
        result.setHandledAlarms(handled != null ? handled : 0);

        // 3. 处置率
        if (result.getTotalAlarms() > 0) {
            result.setHandleRate(Math.round(result.getHandledAlarms() * 10000.0 / result.getTotalAlarms()) / 100.0);
        } else {
            result.setHandleRate(0.0);
        }

        // 4. 按等级统计各类报警数量
        List<Map<String, Object>> levelStats = alertRecordMapper.countAlertsByLevelAll();
        int fireAlarms = 0, faultAlarms = 0, riskAlarms = 0, offlineAlarms = 0, falseAlarmAlarms = 0;
        // level 映射: 1=火警, 2=故障, 3=隐患, 4=离线, 5=误报
        Map<String, Integer> levelCountMap = new HashMap<>();
        for (Map<String, Object> stat : levelStats) {
            String level = stat.get("level") != null ? stat.get("level").toString() : "1";
            int count = stat.get("count") != null ? Integer.parseInt(stat.get("count").toString()) : 0;
            levelCountMap.put(level, count);
            switch (level) {
                case "1":
                    fireAlarms = count;
                    break;
                case "2":
                    faultAlarms = count;
                    break;
                case "3":
                    riskAlarms = count;
                    break;
                case "4":
                    offlineAlarms = count;
                    break;
                case "5":
                    falseAlarmAlarms = count;
                    break;
                default:
                    break;
            }
        }
        result.setFireAlarms(fireAlarms);
        result.setFaultAlarms(faultAlarms);
        result.setRiskAlarms(riskAlarms);
        result.setOfflineAlarms(offlineAlarms);
        result.setFalseAlarmAlarms(falseAlarmAlarms);

        // 5. 平均响应时间（当前数据模型无此字段，暂为0）
        result.setAvgResponseTime(0);

        // 6. 报警类型分布（含各类已处置数）
        List<Map<String, Object>> levelReadStats = alertRecordMapper.countAlertsByLevelAndReadStatus();
        // 构建每个level的已处置数映射: level -> readCount
        Map<String, Integer> levelReadCountMap = new HashMap<>();
        for (Map<String, Object> stat : levelReadStats) {
            String level = stat.get("level") != null ? stat.get("level").toString() : "1";
            // read_flg 在 MySQL 中若为 TINYINT(1)，JDBC 驱动会映射为 Boolean
            // 此处兼容处理 Boolean 和 Integer 两种类型
            Object readFlgObj = stat.get("readFlg");
            boolean isRead = false;
            if (readFlgObj instanceof Boolean) {
                isRead = (Boolean) readFlgObj;
            } else if (readFlgObj != null) {
                isRead = Integer.parseInt(readFlgObj.toString()) == 1;
            }
            int count = stat.get("count") != null ? Integer.parseInt(stat.get("count").toString()) : 0;
            if (isRead) {
                levelReadCountMap.put(level, count);
            }
        }

        // level → alarmType 映射
        Map<String, String> levelToAlarmType = new HashMap<>();
        levelToAlarmType.put("1", "fire");
        levelToAlarmType.put("2", "fault");
        levelToAlarmType.put("3", "risk");
        levelToAlarmType.put("4", "offline");
        levelToAlarmType.put("5", "false_alarm");

        Map<String, String> alarmTypeNameMap = new HashMap<>();
        alarmTypeNameMap.put("fire", "火警");
        alarmTypeNameMap.put("fault", "故障");
        alarmTypeNameMap.put("risk", "隐患");
        alarmTypeNameMap.put("offline", "离线");
        alarmTypeNameMap.put("false_alarm", "误报");

        List<AlarmRecordStatisticsRespVO.AlarmTypeCountVO> typeDistribution = new ArrayList<>();
        for (Map.Entry<String, String> entry : levelToAlarmType.entrySet()) {
            String level = entry.getKey();
            String alarmType = entry.getValue();
            int count = levelCountMap.getOrDefault(level, 0);
            int handledCount = levelReadCountMap.getOrDefault(level, 0);
            if (count > 0) {
                AlarmRecordStatisticsRespVO.AlarmTypeCountVO vo = new AlarmRecordStatisticsRespVO.AlarmTypeCountVO();
                vo.setAlarmType(alarmType);
                vo.setTypeName(alarmTypeNameMap.get(alarmType));
                vo.setCount(count);
                vo.setHandledCount(handledCount);
                typeDistribution.add(vo);
            }
        }
        result.setAlarmTypeDistribution(typeDistribution);

        log.info("[报警记录统计] totalAlarms={}, fireAlarms={}, faultAlarms={}, handledAlarms={}, handleRate={}%",
                result.getTotalAlarms(), result.getFireAlarms(), result.getFaultAlarms(),
                result.getHandledAlarms(), result.getHandleRate());

        return result;
    }

    /**
     * 将告警记录转换为前端的 AlarmOrderRespVO
     */
    private AlarmOrderRespVO convertToRespVO(AlertRecordDO record) {
        if (record == null) {
            return null;
        }
        AlarmOrderRespVO vo = new AlarmOrderRespVO();
        vo.setId(record.getId());
        vo.setOrderNo("ALERT-" + record.getId());

        // level 映射到告警类型和级别
        String level = record.getLevel();
        vo.setAlarmLevel(mapLevel(level));
        vo.setAlarmType(mapLevelToAlarmType(level));

        // 时间
        if (record.getAlertTime() != null) {
            vo.setAlarmTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(record.getAlertTime()), ZoneId.systemDefault()));
        }

        // 告警信息
        vo.setAlarmDescription(record.getDetails() != null ? record.getDetails() : record.getName());
        vo.setAlarmLocation(record.getName());

        // 设备ID：用于后续 fillDeviceType 通过多表关联获取设备名称和类型
        vo.setDeviceId(record.getDeviceId());

        // 状态映射：通过 handle_status / details 处置记录 / readFlg 判断具体状态
        vo.setOrderStatus(AlertOrderStatusResolver.resolveOrderStatus(record));

        // 从 details 中提取处理人和处理时间
        resolveHandlerInfo(record, vo);

        return vo;
    }

    private String mapLevel(String level) {
        if (level == null) return "warning";
        switch (level) {
            case "1":
                return "critical";
            case "2":
                return "major";
            case "3":
                return "warning";
            case "4":
                return "info";
            case "5":
                return "info";
            default:
                return "warning";
        }
    }

    /**
     * 从 details 中提取最后一条处置记录的处理人和处理时间
     */
    private void resolveHandlerInfo(AlertRecordDO record, AlarmOrderRespVO vo) {
        String details = record.getDetails();
        if (details == null) {
            return;
        }
        int lastBracket = details.lastIndexOf('[');
        if (lastBracket < 0) {
            return;
        }
        String lastRecord = details.substring(lastBracket);
        // 格式: [yyyy-MM-ddTHH:mm] 处理人 - 动作：备注
        try {
            int bracketEnd = lastRecord.indexOf(']');
            if (bracketEnd > 0) {
                String timeStr = lastRecord.substring(1, bracketEnd);
                LocalDateTime handleTime = LocalDateTime.parse(timeStr);
                vo.setHandleTime(handleTime);
            }
            int dashIdx = lastRecord.indexOf(" - ");
            if (dashIdx > 0) {
                String handler = lastRecord.substring(bracketEnd + 1, dashIdx).trim();
                vo.setHandler(handler);
            }
        } catch (Exception e) {
            log.warn("[resolveHandlerInfo] 解析处理信息失败，不影响主流程", e);
        }
    }

    /**
     * 通过多表关联获取设备类型并填充到VO
     * 链路: device_id → eiot_device_info.product_key → eiot_product.category_id → eiot_category.name
     */
    private void fillDeviceType(AlarmOrderRespVO vo, AlertRecordDO record) {
        if (record == null) {
            return;
        }
        // 尝试从 record 中获取设备ID相关信息
        // 如果有设备ID，通过多表关联获取设备类型
        try {
            // 从 AlarmOrderRespVO 中获取设备信息
            Long deviceId = vo.getDeviceId();
            if (deviceId == null) {
                return;
            }
            // 1. 根据deviceId查询设备信息，获取deviceName和productKey
            EiotDeviceInfoDO device = eiotDeviceInfoMapper.selectById(deviceId);
            if (device == null || StrUtil.isBlank(device.getProductKey())) {
                return;
            }
            // 填充设备名称
            if (StrUtil.isNotBlank(device.getName())) {
                vo.setDeviceName(device.getName());
            }
            // 2. 根据productKey查询产品信息，获取categoryId
            ProductDO product = productMapper.getByProductKey(device.getProductKey());
            if (product == null || product.getCategoryId() == null) {
                return;
            }
            // 3. 根据categoryId查询分类名称
            CategoryDO category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                vo.setDeviceType(String.valueOf(category.getId()));
                vo.setDeviceTypeName(category.getName());
            }
        } catch (Exception e) {
            log.warn("[fillDeviceType] 获取设备类型失败，deviceId：{}", vo.getDeviceId(), e);
        }
    }

    /**
     * 查询导出用的告警记录列表
     */
    private List<AlertRecordDO> doQueryAlertRecordsForExport(String deviceType, String alarmType,
                                                             String status, Long propertyId,
                                                             String startTime, String endTime) {
        LambdaQueryWrapperX<AlertRecordDO> wrapper = new LambdaQueryWrapperX<AlertRecordDO>()
                .orderByDesc(AlertRecordDO::getAlertTime);

        // 报警类型映射
        if (StrUtil.isNotBlank(alarmType)) {
            String level = null;
            switch (alarmType) {
                case "fire":
                    level = "1";
                    break;
                case "fault":
                    level = "2";
                    break;
                case "risk":
                    level = "3";
                    break;
                case "offline":
                    level = "4";
                    break;
                case "false_alarm":
                    level = "5";
                    break;
            }
            if (level != null) {
                wrapper.eq(AlertRecordDO::getLevel, level);
            }
        }

        // 处置状态
        if (StrUtil.isNotBlank(status)) {
            if ("pending".equals(status)) {
                wrapper.eq(AlertRecordDO::getReadFlg, false);
            } else if ("handled".equals(status) || "closed".equals(status)) {
                wrapper.eq(AlertRecordDO::getReadFlg, true);
            }
        }

        // 时间范围
        if (StrUtil.isNotBlank(startTime)) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                long beginMs = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                wrapper.ge(AlertRecordDO::getAlertTime, beginMs);
            } catch (Exception e) {
                log.warn("[PDF导出] 解析开始时间失败: {}", startTime);
            }
        }
        if (StrUtil.isNotBlank(endTime)) {
            try {
                LocalDateTime end = LocalDateTime.parse(endTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                long endMs = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                wrapper.le(AlertRecordDO::getAlertTime, endMs);
            } catch (Exception e) {
                log.warn("[PDF导出] 解析结束时间失败: {}", endTime);
            }
        }

        // 限制最大导出数量
        wrapper.last("LIMIT 2000");
        return alertRecordMapper.selectList(wrapper);
    }

    /**
     * 将告警记录转换为显示行数据
     */
    private Map<String, String> convertRecordToDisplayRow(AlertRecordDO record) {
        Map<String, String> row = new LinkedHashMap<>();

        // 报警时间
        if (record.getAlertTime() != null) {
            LocalDateTime dt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(record.getAlertTime()), ZoneId.systemDefault());
            row.put("alarmTime", dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            row.put("alarmTime", "");
        }

        // 报警类型
        String alarmType = mapLevelToAlarmType(record.getLevel());
        row.put("alarmType", alarmType);
        row.put("alarmTypeName", getAlarmTypeName(alarmType));

        // 处置状态
        boolean isRead = Boolean.TRUE.equals(record.getReadFlg());
        row.put("status", isRead ? "handled" : "pending");
        row.put("statusName", isRead ? "已处置" : "待处理");

        // 解析 details JSON 扩展字段
        String details = record.getDetails();
        JSONObject json = null;
        if (StrUtil.isNotBlank(details)) {
            try {
                json = new JSONObject(details);
            } catch (Exception e) {
                // 非JSON，忽略
            }
        }

        row.put("propertyName", json != null ? json.getStr("propertyName", "") : "");
        row.put("deviceName", json != null ? json.getStr("deviceName", "") : (record.getName() != null ? record.getName() : ""));
        row.put("deviceTypeName", json != null ? json.getStr("deviceTypeName", "") : "");
        row.put("location", json != null ? json.getStr("location", "") : (record.getName() != null ? record.getName() : ""));
        row.put("handler", json != null ? json.getStr("handler", "") : "");

        String handleTimeStr = json != null ? json.getStr("handleTime", "") : "";
        row.put("handleTime", StrUtil.isNotBlank(handleTimeStr) ? handleTimeStr : "");

        Integer responseTime = json != null ? json.getInt("responseTime") : null;
        row.put("responseTime", responseTime != null ? responseTime + "s" : "");

        return row;
    }

    /**
     * 加载中文字体（支持 TTC/OTF/TTF 格式）
     */
    private PDType0Font loadChineseFont(PDDocument document) throws IOException {
        String[] fontPaths = {
                // Windows 常见中文字体（多为 TTC 格式）
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/msyhbd.ttc",
                // Linux 常见中文字体
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttf",
                // macOS 常见中文字体
                "/System/Library/Fonts/PingFang.ttc",
                "/System/Library/Fonts/STHeiti Light.ttc",
                "/System/Library/Fonts/STHeiti Medium.ttc",
        };

        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (!fontFile.exists() || !fontFile.isFile()) {
                continue;
            }

            try {
                if (fontPath.toLowerCase().endsWith(".ttc")) {
                    // TTC (TrueType Collection): 提取嵌入字体
                    return loadFontFromTtc(document, fontFile);
                } else {
                    // TTF/OTF: 直接通过流加载
                    try (InputStream is = new FileInputStream(fontFile)) {
                        return PDType0Font.load(document, is, true);
                    }
                }
            } catch (IOException e) {
                log.warn("[PDF导出] 字体加载失败: {}, 原因: {}", fontPath, e.getMessage());
            }
        }

        throw new IOException("未找到系统可用的中文字体文件，请安装中文字体（如宋体、微软雅黑等）后再试");
    }

    /**
     * 从 TTC (TrueType Collection) 文件中提取并加载字体
     */
    private PDType0Font loadFontFromTtc(PDDocument document, File ttcFile) throws IOException {
        TrueTypeCollection ttc = new TrueTypeCollection(ttcFile);
        try {
            final TrueTypeFont[] firstFont = new TrueTypeFont[1];
            ttc.processAllFonts(font -> {
                if (firstFont[0] == null) {
                    firstFont[0] = font;
                }
            });
            if (firstFont[0] == null) {
                throw new IOException("TTC文件中未找到字体: " + ttcFile.getAbsolutePath());
            }
            // 第二个参数 true 表示将字体嵌入PDF，确保跨平台可移植性
            return PDType0Font.load(document, firstFont[0], true);
        } finally {
            ttc.close();
        }
    }

    /**
     * 绘制PDF表格表头
     */
    private void drawPdfTableHeader(PDPageContentStream cs, PDType0Font font, String[] headers,
                                    float[] widths, float x, float y, float rowHeight) throws IOException {
        // 背景色
        cs.setNonStrokingColor(new Color(0.85f, 0.85f, 0.85f));
        float startX = x;
        for (int i = 0; i < widths.length; i++) {
            cs.addRect(startX, y - rowHeight, widths[i], rowHeight);
            cs.fill();
            startX += widths[i];
        }
        cs.setNonStrokingColor(Color.BLACK);

        // 表头文字
        cs.setFont(font, 7.5f);
        startX = x;
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(startX + 3, y - rowHeight + 4);
            cs.showText(headers[i]);
            cs.endText();
            startX += widths[i];
        }

        // 边框
        drawPdfTableBorders(cs, widths, x, y, rowHeight);
    }

    /**
     * 绘制PDF表格数据行
     */
    private void drawPdfTableRow(PDPageContentStream cs, PDType0Font font, String[] values,
                                 float[] widths, float x, float y, float rowHeight,
                                 boolean isEven) throws IOException {
        // 交替行背景
        if (isEven) {
            cs.setNonStrokingColor(new Color(0.95f, 0.95f, 0.97f));
            float startX = x;
            for (float w : widths) {
                cs.addRect(startX, y - rowHeight, w, rowHeight);
                cs.fill();
                startX += w;
            }
            cs.setNonStrokingColor(Color.BLACK);
        }

        // 数据文字
        cs.setFont(font, 7f);
        float startX = x;
        for (int i = 0; i < widths.length; i++) {
            String text = i < values.length ? values[i] : "";
            // 截断过长的文本
            String displayText = truncatePdfText(text, widths[i] - 6, font, 7f);
            cs.beginText();
            cs.newLineAtOffset(startX + 3, y - rowHeight + 3);
            cs.showText(displayText);
            cs.endText();
            startX += widths[i];
        }

        // 边框
        drawPdfTableBorders(cs, widths, x, y, rowHeight);
    }

    /**
     * 绘制PDF表格边框
     */
    private void drawPdfTableBorders(PDPageContentStream cs, float[] widths,
                                     float x, float y, float rowHeight) throws IOException {
        float totalW = 0;
        for (float w : widths) totalW += w;
        // 下边线
        cs.moveTo(x, y - rowHeight);
        cs.lineTo(x + totalW, y - rowHeight);
        cs.stroke();
    }

    /**
     * 截断PDF文本使其不超过指定宽度
     */
    private String truncatePdfText(String text, float maxWidth, PDType0Font font, float fontSize) throws IOException {
        if (StrUtil.isBlank(text)) return "";
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        if (textWidth <= maxWidth) return text;
        for (int i = text.length(); i > 0; i--) {
            String sub = text.substring(0, i);
            float w = font.getStringWidth(sub) / 1000f * fontSize;
            if (w <= maxWidth - 5) {
                return sub + "..";
            }
        }
        return "";
    }

}
