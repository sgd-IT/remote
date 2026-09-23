package com.enjoyiot.eiot.virtualdevice;

import com.enjoyiot.module.eiot.api.device.dto.DeviceInfo;
import com.enjoyiot.module.eiot.api.virtualdevice.dto.VirtualDevice;
import com.enjoyiot.module.eiot.api.virtualdevice.dto.VirtualDeviceLog;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class VirtualExecutor implements Job {

    @Override
    public void execute(JobExecutionContext context) {
        Map<String, Object> data = context.getMergedJobDataMap();
        VirtualManager virtualManager = (VirtualManager) data.get("virtualManager");
        VirtualDevice virtualDevice = (VirtualDevice) data.get("virtualDevice");
        List<DeviceInfo> devices = (List<DeviceInfo>) data.get("devices");
        devices = devices == null ? new ArrayList<>() : devices;
        JobDetail jobDetail = context.getJobDetail();
        String jobKey = jobDetail.getKey().toString();

        VirtualDeviceLog virtualDeviceLog = VirtualDeviceLog.builder()
                .virtualDeviceId(virtualDevice.getId())
                .virtualDeviceName(virtualDevice.getName())
                .deviceTotal(devices.size())
                .result("success")
                .build();

        try {
            for (DeviceInfo device : devices) {
                if (device == null) {
                    continue;
                }
                log.info("invoke virtual device report,jobKey:{},deviceId:{}", jobKey, device.getId());
                virtualManager.invokeReport(device, virtualDevice.getId());
            }
        } catch (Throwable e) {
            virtualDeviceLog.setResult(e.getMessage());
            log.error("execute job error", e);
        }
        virtualManager.saveLog(virtualDeviceLog);
    }
}
