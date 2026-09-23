package com.enjoyiot.eiot.temporal.rdb.config;

public interface Constants {

    /**
     * 根据产品key获取产品属性表名
     */
    static String getProductPropertyTableName(String productKey) {
        return String.format("product_property_%s", productKey.toLowerCase().replace("-", "_"));
    }

    /**
     * 时间字段名称
     */
    String TIME_FIELD = "time";

    /**
     * 设备ID字段名称
     */
    String DEVICE_ID_FIELD = "device_id";
}
