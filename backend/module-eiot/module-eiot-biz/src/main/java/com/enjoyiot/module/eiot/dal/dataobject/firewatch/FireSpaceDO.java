package com.enjoyiot.module.eiot.dal.dataobject.firewatch;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 空间管理 DO（楼栋/楼层/区域/详细位置）
 * <p>
 * 数据按租户隔离；唯一键 (tenant_id, building, floor, area, position) 防止同租户下重复录入。
 *
 * @author sgd
 */
@TableName("eiot_space")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireSpaceDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 楼栋（几号楼，如：1号楼/A栋）
     */
    private String building;

    /**
     * 楼层（几层，如：3层/-1层/3F）
     */
    private String floor;

    /**
     * 区域（如：东区/厨房/配电房）
     */
    private String area;

    /**
     * 详细位置（区域内具体点位，如：东侧走廊北端/301室；留空表示区域级空间）
     */
    private String position;

    /**
     * 备注
     */
    private String remark;
}
