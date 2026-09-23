package com.enjoyiot.module.eiot.dal.dataobject.video;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

@TableName("video_camera")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCameraDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String deviceId;
    private String channelId;
    private String channelName;
    private Integer status;
    private Double longitude;
    private Double latitude;
    private String address;
    private Long deptId;
    private String remark;
    /** 码流类型：1-主码流，2-辅码流 */
    private String quality;
    /** 拉流类型：0-海康威视，1-大华ICC */
    private Integer pullType;
    /** 摄像头类型：-1录像机,0普通,1漏铝,2AI人员统计,3热成像,4其他 */
    private Integer type;
    /** 是否支持回放：0-不支持，1-支持 */
    private Integer supportReplay;
}
