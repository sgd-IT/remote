package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 巡检任务管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FirePatrolTaskService {

    /**
     * 创建任务
     *
     * @param createReqVO 创建信息
     * @return 任务ID
     */
    Long createTask(FirePatrolTaskCreateReqVO createReqVO);

    /**
     * 更新任务
     *
     * @param updateReqVO 更新信息
     */
    void updateTask(FirePatrolTaskUpdateReqVO updateReqVO);

    /**
     * 删除任务
     *
     * @param id 任务ID
     */
    void deleteTask(Long id);

    /**
     * 获取任务详情
     *
     * @param id 任务ID
     * @return 任务详情
     */
    FirePatrolTaskRespVO getTask(Long id);

    /**
     * 分页查询任务列表
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<FirePatrolTaskRespVO> pageTasks(FirePatrolTaskPageReqVO pageReqVO);

    /**
     * 开始任务
     *
     * @param id 任务ID
     */
    void startTask(Long id);

    /**
     * 完成任务
     *
     * @param id 任务ID
     * @param summary 巡检总结
     */
    void completeTask(Long id, String summary);

    /**
     * 取消任务
     *
     * @param id 任务ID
     * @param reason 取消原因
     */
    void cancelTask(Long id, String reason);

    /**
     * 获取任务的巡检点位列表
     *
     * @param taskId 任务ID
     * @return 点位列表
     */
    List<FirePatrolPointRespVO> listTaskPoints(Long taskId);

    /**
     * 检查巡检点位
     *
     * @param checkReqVO 检查信息
     */
    void checkPoint(FirePatrolPointCheckReqVO checkReqVO);

    /**
     * 批量添加巡检点位
     *
     * @param taskId 任务ID
     * @param deviceIds 设备ID列表
     * @return 添加数量
     */
    Integer addTaskPoints(Long taskId, List<Long> deviceIds);

    /**
     * 获取任务统计
     *
     * @param propertyId 物业ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计结果
     */
    Map<String, Object> getTaskStatistics(Long propertyId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取用户的待执行任务
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    List<FirePatrolTaskRespVO> getUserPendingTasks(Long userId);

    /**
     * 统计今日待执行任务数
     *
     * @return 任务数
     */
    Integer countTodayPendingTasks();

    /**
     * 检测并标记超期任务
     * 将计划日期已过且状态仍为"待执行"的任务标记为"已超期"
     *
     * @return 标记为超期的任务数
     */
    Integer checkOverdueTasks();

    /**
     * 导入巡检任务
     *
     * @param list 导入的巡检任务列表
     * @return 导入结果（成功数、失败数、失败详情）
     */
    Map<String, Object> importTasks(List<FirePatrolTaskImportVO> list);

}
