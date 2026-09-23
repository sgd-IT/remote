package com.enjoyiot.module.eiot.controller.admin.channelconfig.vo;


import com.enjoyiot.module.eiot.api.TenantModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * author: EnjoyIot
 * date: 2023-05-11 16:30
 * description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel extends TenantModel {

    private Long id;

    private String code;

    private String title;

    private String icon;


}
