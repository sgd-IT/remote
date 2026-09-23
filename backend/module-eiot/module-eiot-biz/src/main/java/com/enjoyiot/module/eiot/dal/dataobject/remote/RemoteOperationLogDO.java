package com.enjoyiot.module.eiot.dal.dataobject.remote;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("eiot_remote_operation_log")
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteOperationLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private String deviceName;
    private Long propertyId;
    private String propertyName;
    private String operationType;
    private String operationName;
    /**
     * 按钮名称（设备功能 buttonName）
     */
    private String buttonName;
    /**
     * 功能类型：BUS(总线)/MULTI(多线)/GENERAL(通用)
     */
    private String functionType;
    private Long operatorId;
    private String operatorName;
    private Long reviewerId;
    private String reviewerName;
    private Integer commandStatus;
    private String resultMsg;
    private String requestParams;
    private String ipAddress;
    private LocalDateTime operationTime;
}
