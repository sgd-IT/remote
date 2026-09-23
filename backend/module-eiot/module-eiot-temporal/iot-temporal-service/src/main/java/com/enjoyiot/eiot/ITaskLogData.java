package com.enjoyiot.eiot;


import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.task.dto.TaskLog;

public interface ITaskLogData {
    void deleteByTaskId(Long taskId);

    PageResult<TaskLog> findByTaskId(Long taskId, int page, int size);

    void add(TaskLog log);

}
