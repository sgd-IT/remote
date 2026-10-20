package com.enjoyiot.module.eiot.api.device.dto;


import com.enjoyiot.module.eiot.api.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo  extends BaseModel {

    public static Integer ONLINE= 1;
    public static Integer OFFLINE=0;
    public static Integer STATE_NOT_ACTIVE=2;
    public static Integer STATE_LOCK=3;
    public static Integer NODE_TYPE_GATEWAY=0;
    public static Integer NODE_TYPE_DEVICE=1;
    public static Integer NODE_TYPE_SUB_DEVICE=2;
    public static Integer NODE_TYPE_VIDEO=2;

    private Long id;

    private String name;

    /**
     * 产品key
     */
    private String productKey;

    private String dn;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 品牌代码（四信API用）
     */
    private String brandCode;

    /**
     * API协议版本（四信API用）
     */
    private String apiVersion;

    /**
     * 设备密钥
     */
    private String secret;

    private Long parentId;

    /**
     * 所属平台用户ID
     */
    private String uid;

    /**
     * 关联子用户ID列表
     */
    private List<String> subUid = new ArrayList<>();

    /**
     * 设备定位对象
     */
//    private Locate locate = new Locate();

    /**
     * 设备在离线状态
     */
    private Integer state;

    /**
     * 设备属性
     */
    private Map<String, ?> property = new HashMap<>();

    /**
     * 设备标签
     */
    private Map<String, Tag> tag = new HashMap<>();

//    /**
//     * 设备所属分组
//     */
//    private Map<String, Group> group = new HashMap<>();


    public boolean isOnline() {
        return state != null && state==1;
    }

    public void setOnline(Boolean online){
        if (online){
            this.state = 1;
            return;
        }
        this.state = 0;

    }

    /**
     * 离线时间
     */
    private Long offlineTime;
    /**
     * 在线时间
     */
    private Long onlineTime;
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class Locate {
//
//        private String longitude;
//
//        private String latitude;
//
//    }
//
//
//
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tag {
        private String id;
        private String name;
        private String value;
    }
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class Group {
//        private String id;
//        private String name;
//    }



    private String addr;

    /**
     * 空间ID（关联 eiot_space.id，设备地址）
     */
    private Long spaceId;

    /**
     * 经纬度
     */
    private Double lat;
    /**
     * 经纬度
     */
    private Double lon;

    private String firmVersion;

    private Long tenantId;

    private String serialNo;

    private Boolean transparent;


    private Integer nodeType;

}
