-- eiot_alert_record 增加"报警类型（新语义）"与"确认档位"字段
-- 依据：《消防告警字段语义定稿》第 1 步（加列留痕，纯加法、零行为变化）
-- alarm_kind：1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息；NULL=过渡期旧数据（按 level 旧语义解释）
-- tier：1=Ⅰ档 2=Ⅱ档 3=Ⅲ档 4=Ⅳ档（仅一级事件）；NULL=不适用或过渡期旧数据
-- 注意：两列必须允许 NULL——NULL 是"新旧语义"的单一判据，禁止设 DEFAULT 或回填 0
ALTER TABLE eiot_alert_record
  ADD COLUMN alarm_kind tinyint NULL COMMENT '报警类型（新语义）：1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息；NULL=过渡期旧数据，按 level 旧语义解释' AFTER level,
  ADD COLUMN tier tinyint NULL COMMENT '确认档位（新语义，仅一级）：1=Ⅰ档 2=Ⅱ档 3=Ⅲ档 4=Ⅳ档；NULL=不适用或过渡期旧数据' AFTER alarm_kind;
