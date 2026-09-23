package com.enjoyiot.eiot.task.config;


import com.enjoyiot.eiot.task.ActionExecutorManager;
import com.enjoyiot.eiot.task.CommonJob;
import com.enjoyiot.eiot.task.TaskServiceImpl;
import com.enjoyiot.module.eiot.api.task.dto.TaskInfo;
import org.quartz.JobDetail;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.stereotype.Component;

@Component
public class JobFactory extends AdaptableJobFactory {
    @Autowired
    private AutowireCapableBeanFactory capableBeanFactory;
    @Autowired
    private ActionExecutorManager actionExecutorManager;
    @Autowired
    private TaskServiceImpl taskServiceImpl;

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        JobDetail jobDetail = bundle.getJobDetail();
        Object objTask = jobDetail.getJobDataMap().get("taskInfo");

        Class<?> clz = jobDetail.getJobClass();
        Object jobInstance;
        if (clz == CommonJob.class && objTask instanceof TaskInfo) {
            jobInstance = new CommonJob(actionExecutorManager, taskServiceImpl, (TaskInfo) objTask);
        } else {
            jobInstance = super.createJobInstance(bundle);
        }
        //进行注入
        capableBeanFactory.autowireBean(jobInstance);
        return jobInstance;
    }
}
