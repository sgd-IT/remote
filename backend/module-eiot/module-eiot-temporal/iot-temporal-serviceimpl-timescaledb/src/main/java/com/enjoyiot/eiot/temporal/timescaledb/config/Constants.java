package com.enjoyiot.eiot.temporal.timescaledb.config;

public interface Constants {

    /**
     * 根据产品key获取产品属性超级表名
     */
    static String getProductPropertySTableName(String productKey) {
        return String.format("product_property_%s", productKey.toLowerCase());
    }
}
