package com.enjoyiot.eiot.temporal.es.service;


import com.enjoyiot.eiot.ITaskLogData;
import com.enjoyiot.eiot.temporal.es.dao.TaskLogRepository;
import com.enjoyiot.eiot.temporal.es.convert.EsTaskLogConvert;
import com.enjoyiot.eiot.temporal.es.document.DocTaskLog;
import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.task.dto.TaskLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TaskLogDataImpl implements ITaskLogData {

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Override
    public void deleteByTaskId(Long taskId) {
        taskLogRepository.deleteByTaskId(taskId);
    }

    @Override
    public PageResult<TaskLog> findByTaskId(Long taskId, int page, int size) {
        Page<DocTaskLog> paged = taskLogRepository.findByTaskIdOrderByLogAtDesc(taskId, Pageable.ofSize(size).withPage(page - 1));
        return new PageResult<>(
                paged.getContent().stream().map(o -> EsTaskLogConvert.INSTANCE.convert(o))
                        .collect(Collectors.toList()), paged.getTotalElements());
    }

    @Override
    public void add(TaskLog log) {
        taskLogRepository.save(EsTaskLogConvert.INSTANCE.convertDoc(log));
    }
}
