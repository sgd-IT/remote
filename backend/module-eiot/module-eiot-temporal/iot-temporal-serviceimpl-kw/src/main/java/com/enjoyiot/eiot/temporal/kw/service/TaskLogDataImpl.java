package com.enjoyiot.eiot.temporal.kw.service;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enjoyiot.eiot.ITaskLogData;
import com.enjoyiot.eiot.temporal.kw.dao.KwTaskLogMapper;
import com.enjoyiot.eiot.temporal.kw.model.KwTaskLog;
import com.enjoyiot.framework.common.pojo.PageParam;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.common.util.object.BeanUtils;
import com.enjoyiot.module.eiot.api.task.dto.TaskLog;
import com.kaiwudb.util.KWTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TaskLogDataImpl implements ITaskLogData {

    @Autowired
    private KwTaskLogMapper taskLogMapper;

    @Override
    public void deleteByTaskId(Long taskId) {
        taskLogMapper.delete(KwTaskLog::getTaskId, taskId);
    }

    @Override
    public PageResult<TaskLog> findByTaskId(Long taskId, int page, int size) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(page);
        pageParam.setPageSize(size);
        PageResult<KwTaskLog> result = taskLogMapper.selectPage(pageParam,
                Wrappers.lambdaQuery(KwTaskLog.class).eq(KwTaskLog::getTaskId, taskId).orderByDesc(KwTaskLog::getTime)
        );
        return new PageResult<>(result.getList().stream().map(r ->
                        new TaskLog(r.getTime().toString(), taskId,
                                r.getContent(), r.getSuccess(), r.getTime().getTime()))
                .collect(Collectors.toList()), result.getTotal());
    }

    @Override
    public void add(TaskLog log) {
        KwTaskLog taskLog = BeanUtils.toBean(log, KwTaskLog.class);
        taskLog.setTime(new KWTimestamp(System.currentTimeMillis()));
        taskLogMapper.insert(taskLog);
    }

}
