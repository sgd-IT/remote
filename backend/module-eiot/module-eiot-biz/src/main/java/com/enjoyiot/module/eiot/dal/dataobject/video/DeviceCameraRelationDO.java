package com.enjoyiot.module.eiot.dal.dataobject.video;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.time.LocalDateTime;

@TableName("device_camera_relation")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCameraRelationDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long deviceId;
    private Long cameraId;
    private String relationType;
    private LocalDateTime bindTime;
}
