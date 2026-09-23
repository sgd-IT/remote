package com.enjoyiot.module.eiot.controller.admin.alert.vo;


import com.enjoyiot.framework.common.pojo.PageParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: EnjoyIot
 * @Date: 2023/5/13 15:22
 * @Description:
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessagePageReq extends PageParam {

    private Long id;

    private String messageType;

    private Boolean status;

}
