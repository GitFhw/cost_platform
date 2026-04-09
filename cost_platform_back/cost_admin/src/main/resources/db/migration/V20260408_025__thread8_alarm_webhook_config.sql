INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '成本告警-WebHook通知开关', 'cost.alarm.webhook.enabled', 'false', 'N', 'admin', NOW(),
       'true 表示启用 Webhook 外部通知，false 表示仅保留平台内告警'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'cost.alarm.webhook.enabled'
);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '成本告警-WebHook地址', 'cost.alarm.webhook.url', '', 'N', 'admin', NOW(),
       '外部通知地址，建议接企业值守平台或消息网关'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'cost.alarm.webhook.url'
);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '成本告警-WebHook扩展头', 'cost.alarm.webhook.headers', '', 'N', 'admin', NOW(),
       'JSON 格式扩展请求头，例如 {\"X-App\":\"cost-platform\"}'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'cost.alarm.webhook.headers'
);

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '成本告警-WebHook签名密钥', 'cost.alarm.webhook.secret', '', 'N', 'admin', NOW(),
       '可选签名密钥，将以 X-Cost-Alarm-Secret 请求头透传'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_config WHERE config_key = 'cost.alarm.webhook.secret'
);
