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
        alarmRecord.setCreateBy(firstNonBlank(alarmRecord.getCreateBy(), firstNonBlank(SecurityUtils.getUsername(), "system")));
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
    public int ackAlarm(Long alarmId)
    {
        requireAlarm(alarmId);
        return alarmRecordMapper.update(null, Wrappers.<CostAlarmRecord>lambdaUpdate()
                .eq(CostAlarmRecord::getAlarmId, alarmId)
                .set(CostAlarmRecord::getAlarmStatus, ALARM_STATUS_ACKED)
                .set(CostAlarmRecord::getAckBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostAlarmRecord::getAckTime, DateUtils.getNowDate())
                .set(CostAlarmRecord::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostAlarmRecord::getUpdateTime, DateUtils.getNowDate()));
    }

    @Override
    public int resolveAlarm(Long alarmId)
    {
        requireAlarm(alarmId);
        return alarmRecordMapper.update(null, Wrappers.<CostAlarmRecord>lambdaUpdate()
                .eq(CostAlarmRecord::getAlarmId, alarmId)
                .set(CostAlarmRecord::getAlarmStatus, ALARM_STATUS_RESOLVED)
                .set(CostAlarmRecord::getResolveBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
                .set(CostAlarmRecord::getResolveTime, DateUtils.getNowDate())
                .set(CostAlarmRecord::getUpdateBy, firstNonBlank(SecurityUtils.getUsername(), "system"))
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
}
