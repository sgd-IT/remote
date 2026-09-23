package com.enjoyiot.framework.common.util.string;

public class SubDeviceCodeUtil {

    /**
     * 拼接消防设备编号
     * @param machineNo 主机号
     * @param loopNo 回路号
     * @param addressNo 设备地址号
     * @return 7位设备编号字符串
     */
    public static String generateDeviceCode(Integer machineNo, Integer loopNo, Integer addressNo) {
        // %02d：数字不足2位前置补0；%03d：不足3位前置补0
        return String.format("%02d%02d%03d", machineNo, loopNo, addressNo);
    }

    public static void main(String[] args) {
        // 测试案例
        System.out.println(generateDeviceCode(1, 2, 5));      // 0102005
        System.out.println(generateDeviceCode(10, 8, 36));    // 1008036
        System.out.println(generateDeviceCode(99, 99, 999)); // 9999999
        System.out.println(generateDeviceCode(5, 15, 120));   // 0515120
    }
}