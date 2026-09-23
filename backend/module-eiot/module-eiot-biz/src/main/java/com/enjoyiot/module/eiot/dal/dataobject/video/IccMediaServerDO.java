package com.enjoyiot.module.eiot.dal.dataobject.video;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

@TableName("icc_media_server")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IccMediaServerDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serverName;
    private String baseUrl;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
    private String publicKey;
    private Boolean enabled;
}
