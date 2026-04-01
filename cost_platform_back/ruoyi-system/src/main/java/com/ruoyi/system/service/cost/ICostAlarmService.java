package com.ruoyi.system.service.cost;

import com.ruoyi.system.domain.cost.CostAlarmRecord;

import java.util.List;
import java.util.Map;

/**
 * 运行告警服务接口
 *
 * @author codex
 */
public interface ICostAlarmService
{
    void createAlarm(CostAlarmRecord alarmRecord);

    List<CostAlarmRecord> selectAlarmList(CostAlarmRecord query);

    Map<String, Object> selectAlarmStats(CostAlarmRecord query);

    int ackAlarm(Long alarmId);

    int resolveAlarm(Long alarmId);
}
