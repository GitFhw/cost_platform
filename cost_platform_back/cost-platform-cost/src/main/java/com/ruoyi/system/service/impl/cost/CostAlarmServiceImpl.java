package com.ruoyi.system.service.impl.cost;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.cost.CostAlarmRecord;
import com.ruoyi.system.domain.cost.CostScene;
import com.ruoyi.system.mapper.cost.CostAlarmRecordMapper;
import com.ruoyi.system.mapper.cost.CostSceneMapper;
import com.ruoyi.system.service.cost.ICostAlarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运行告警服务实现
 *
 * @author codex
 */
@Service
public class CostAlarmServiceImpl implements ICostAlarmService
{
    private static final String ALARM_STATUS_OPEN = "OPEN";
    private static final String ALARM_STATUS_ACKED = "ACKED";
    private static final String ALARM_STATUS_RESOLVED = "RESOLVED";

    @Autowired
    private CostAlarmRecordMapper alarmRecordMapper;

    @Autowired
    private CostSceneMapper sceneMapper;

    @Override
    public void createAlarm(CostAlarmRecord alarmRecord)
    {
        alarmRecord.setAlarmStatus(firstNonBlank(alarmRecord.getAlarmStatus(), ALARM_STATUS_OPEN));
        alarmRecord.setAlarmLevel(firstNonBlank(alarmRecord.getAlarmLevel(), "WARN"));
        alarmRecord.setTriggerTime(alarmRecord.getTriggerTime() == null ? DateUtils.getNowDate() : alarmRecord.getTriggerTime());
        alarmRecord.setCreateBy(firstNonBlank(alarmRecord.getCreateBy(), resolveOperator()));
        alarmRecord.setCreateTime(alarmRecord.getCreateTime() == null ? DateUtils.getNowDate() : alarmRecord.getCreateTime());
        alarmRecord.setUpdateBy(firstNonBlank(alarmRecord.getUpdateBy(), alarmRecord.getCreateBy()));
        alarmRecord.setUpdateTime(alarmRecord.getUpdateTime() == null ? DateUtils.getNowDate() : alarmRecord.getUpdateTime());
        alarmRecordMapper.insert(alarmRecord);
    }

    @Override
    public List<CostAlarmRecord> selectAlarmList(CostAlarmRecord query)
    {
        List<CostAlarmRecord> alarms = alarmRecordMapper.selectList(Wrappers.<CostAlarmRecord>lambdaQuery()
                .eq(query.getSceneId() != null, CostAlarmRecord::getSceneId, query.getSceneId())
                .eq(query.getTaskId() != null, CostAlarmRecord::getTaskId, query.getTaskId())
                .eq(StringUtils.isNotEmpty(query.getBillMonth()), CostAlarmRecord::getBillMonth, query.getBillMonth())
                .eq(StringUtils.isNotEmpty(query.getAlarmType()), CostAlarmRecord::getAlarmType, query.getAlarmType())
                .eq(StringUtils.isNotEmpty(query.getAlarmLevel()), CostAlarmRecord::getAlarmLevel, query.getAlarmLevel())
                .eq(StringUtils.isNotEmpty(query.getAlarmStatus()), CostAlarmRecord::getAlarmStatus, query.getAlarmStatus())
                .like(StringUtils.isNotEmpty(query.getAlarmTitle()), CostAlarmRecord::getAlarmTitle, query.getAlarmTitle())
                .orderByDesc(CostAlarmRecord::getTriggerTime)
                .orderByDesc(CostAlarmRecord::getAlarmId));
        enrichSceneInfo(alarms);
        return alarms;
    }

    @Override
    public Map<String, Object> selectAlarmStats(CostAlarmRecord query)
    {
        List<CostAlarmRecord> alarms = selectAlarmList(query);
        LinkedHashMap<String, Object> stats = new LinkedHashMap<>();
        stats.put("alarmCount", alarms.size());
        stats.put("openCount", alarms.stream().filter(item -> ALARM_STATUS_OPEN.equals(item.getAlarmStatus())).count());
        stats.put("ackedCount", alarms.stream().filter(item -> ALARM_STATUS_ACKED.equals(item.getAlarmStatus())).count());
        stats.put("resolvedCount", alarms.stream().filter(item -> ALARM_STATUS_RESOLVED.equals(item.getAlarmStatus())).count());
        return stats;
    }

    @Override
    public Map<String, Object> selectAlarmOverview(CostAlarmRecord query)
    {
        List<CostAlarmRecord> alarms = selectAlarmList(query);
        LinkedHashMap<String, Object> overview = new LinkedHashMap<>();
        overview.put("recentTrend", buildRecentTrend(alarms, 7));
        overview.put("topAlarmTypes", buildTopAlarmTypes(alarms, 5));
        overview.put("topTasks", buildTopTasks(alarms, 5));
        overview.put("levelDistribution", buildLevelDistribution(alarms));
        return overview;
    }

    @Override
    public int ackAlarm(Long alarmId)
    {
        requireAlarm(alarmId);
        return alarmRecordMapper.update(null, Wrappers.<CostAlarmRecord>lambdaUpdate()
                .eq(CostAlarmRecord::getAlarmId, alarmId)
                .set(CostAlarmRecord::getAlarmStatus, ALARM_STATUS_ACKED)
                .set(CostAlarmRecord::getAckBy, resolveOperator())
                .set(CostAlarmRecord::getAckTime, DateUtils.getNowDate())
                .set(CostAlarmRecord::getUpdateBy, resolveOperator())
                .set(CostAlarmRecord::getUpdateTime, DateUtils.getNowDate()));
    }

    @Override
    public int resolveAlarm(Long alarmId)
    {
        requireAlarm(alarmId);
        return alarmRecordMapper.update(null, Wrappers.<CostAlarmRecord>lambdaUpdate()
                .eq(CostAlarmRecord::getAlarmId, alarmId)
                .set(CostAlarmRecord::getAlarmStatus, ALARM_STATUS_RESOLVED)
                .set(CostAlarmRecord::getResolveBy, resolveOperator())
                .set(CostAlarmRecord::getResolveTime, DateUtils.getNowDate())
                .set(CostAlarmRecord::getUpdateBy, resolveOperator())
                .set(CostAlarmRecord::getUpdateTime, DateUtils.getNowDate()));
    }

    private CostAlarmRecord requireAlarm(Long alarmId)
    {
        CostAlarmRecord alarm = alarmRecordMapper.selectById(alarmId);
        if (alarm == null)
        {
            throw new ServiceException("告警记录不存在，请刷新后重试");
        }
        return alarm;
    }

    private void enrichSceneInfo(List<CostAlarmRecord> alarms)
    {
        Set<Long> sceneIds = alarms.stream().map(CostAlarmRecord::getSceneId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (sceneIds.isEmpty())
        {
            return;
        }
        Map<Long, CostScene> sceneMap = sceneMapper.selectBatchIds(sceneIds).stream()
                .collect(Collectors.toMap(CostScene::getSceneId, item -> item));
        alarms.forEach(item -> {
            CostScene scene = sceneMap.get(item.getSceneId());
            if (scene != null)
            {
                item.setSceneCode(scene.getSceneCode());
                item.setSceneName(scene.getSceneName());
            }
        });
    }

    private String firstNonBlank(String first, String second)
    {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    private String resolveOperator()
    {
        try
        {
            return firstNonBlank(SecurityUtils.getUsername(), "system");
        }
        catch (Exception ignored)
        {
            return "system";
        }
    }

    /**
     * 构建最近 N 天告警趋势。
     */
    private List<Map<String, Object>> buildRecentTrend(List<CostAlarmRecord> alarms, int recentDays)
    {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<LocalDate, List<CostAlarmRecord>> dayGroups = alarms.stream()
                .filter(item -> item.getTriggerTime() != null)
                .collect(Collectors.groupingBy(item -> item.getTriggerTime().toInstant().atZone(zoneId).toLocalDate()));
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate end = LocalDate.now(zoneId);
        LocalDate start = end.minusDays(Math.max(recentDays - 1L, 0L));
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1))
        {
            List<CostAlarmRecord> dayAlarms = dayGroups.getOrDefault(date, List.of());
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.format(formatter));
            row.put("count", dayAlarms.size());
            row.put("openCount", dayAlarms.stream().filter(item -> ALARM_STATUS_OPEN.equals(item.getAlarmStatus())).count());
            row.put("errorCount", dayAlarms.stream().filter(item -> "ERROR".equals(item.getAlarmLevel())).count());
            trend.add(row);
        }
        return trend;
    }

    /**
     * 构建高频告警类型排行。
     */
    private List<Map<String, Object>> buildTopAlarmTypes(List<CostAlarmRecord> alarms, int limit)
    {
        return alarms.stream()
                .collect(Collectors.groupingBy(item -> firstNonBlank(item.getAlarmType(), "UNKNOWN"), Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<CostAlarmRecord> grouped = entry.getValue();
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("alarmType", entry.getKey());
                    row.put("count", grouped.size());
                    row.put("openCount", grouped.stream().filter(item -> ALARM_STATUS_OPEN.equals(item.getAlarmStatus())).count());
                    row.put("latestTime", grouped.stream().map(CostAlarmRecord::getTriggerTime).filter(Objects::nonNull).max(Date::compareTo).orElse(null));
                    return row;
                })
                .sorted(Comparator.<Map<String, Object>, Integer>comparing(item -> ((Number) item.get("count")).intValue()).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 构建任务热点排行，便于定位异常集中任务。
     */
    private List<Map<String, Object>> buildTopTasks(List<CostAlarmRecord> alarms, int limit)
    {
        return alarms.stream()
                .filter(item -> item.getTaskId() != null)
                .collect(Collectors.groupingBy(CostAlarmRecord::getTaskId, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<CostAlarmRecord> grouped = entry.getValue();
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("taskId", entry.getKey());
                    row.put("sceneName", grouped.stream().map(CostAlarmRecord::getSceneName).filter(StringUtils::isNotEmpty).findFirst().orElse("-"));
                    row.put("billMonth", grouped.stream().map(CostAlarmRecord::getBillMonth).filter(StringUtils::isNotEmpty).findFirst().orElse("-"));
                    row.put("count", grouped.size());
                    row.put("openCount", grouped.stream().filter(item -> ALARM_STATUS_OPEN.equals(item.getAlarmStatus())).count());
                    row.put("latestTitle", grouped.stream().map(CostAlarmRecord::getAlarmTitle).filter(StringUtils::isNotEmpty).findFirst().orElse("-"));
                    return row;
                })
                .sorted(Comparator.<Map<String, Object>, Integer>comparing(item -> ((Number) item.get("count")).intValue()).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 构建告警等级分布。
     */
    private List<Map<String, Object>> buildLevelDistribution(List<CostAlarmRecord> alarms)
    {
        return alarms.stream()
                .collect(Collectors.groupingBy(item -> firstNonBlank(item.getAlarmLevel(), "WARN"), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("alarmLevel", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
