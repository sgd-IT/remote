package com.enjoyiot.eiot.task;


import cn.hutool.core.util.ObjectUtil;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.api.task.dto.RuleAction;
import com.enjoyiot.module.eiot.api.task.dto.TaskInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.List;

@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonJob implements Job {

    private ActionExecutorManager actionExecutorManager;

    private TaskServiceImpl taskServiceImpl;

    private TaskInfo taskInfo;

    @Override
    public void execute(JobExecutionContext context) {

        Long tenantId = taskInfo.getTenantId();
        if(ObjectUtil.isNotNull(tenantId)){
            TenantUtils.execute(tenantId, ()->{doJob();});

        }else{
            doJob();
        }



    }
    private void doJob(){
        String result = "";
        boolean success = true;
        try {
            List<RuleAction> actions = taskInfo.getActions();
            log.info("start execute job,task:{}", taskInfo.getId());
            for (RuleAction action : actions) {
                actionExecutorManager.execute(action.getType(), action.getConfig());
            }
        } catch (Throwable e) {
            log.error("execute job failed", e);
            result = e.getMessage();
            success = false;
        }
        if (TaskInfo.TYPE_DELAY.equals(taskInfo.getType())) {
            taskServiceImpl.finishTask(taskInfo.getId());
        }
        taskServiceImpl.addLog(taskInfo.getId(), success, result);
    }

}
