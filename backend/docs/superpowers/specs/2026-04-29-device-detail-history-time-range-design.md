# 设备详情属性历史数据默认时间范围调整设计

## 背景

在设备详情页，用户点击属性列表中的"历史"按钮时，当前默认展示的时间范围为前24小时到后24小时（共48小时）。实际使用场景中，用户更关注最近的数据，因此将默认时间范围缩短为近两小时。

## 目标

- 每次点击属性"历史"按钮时，时间范围自动重置为近两小时（now - 2h ~ now）
- 初始 `historyTime` 状态值也同步调整为近两小时

## 修改范围

**文件**：`anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/detail.vue`

### 改动1：初始状态值

将 `historyTime` 的初始值从跨度48小时（前24h ~ 后24h）修改为近两小时（now - 2h ~ now）。

```js
// 修改前
historyTime: [
  new Date(new Date().getTime() - 24 * 3600 * 1000),
  new Date(new Date().getTime() + 24 * 3600 * 1000)
]

// 修改后
historyTime: [
  new Date(new Date().getTime() - 2 * 3600 * 1000),
  new Date()
]
```

### 改动2：showPropertyHistory 函数

每次触发"历史"查看时，先将 `historyTime` 重置为近两小时，再执行查询。

```js
// 修改前
const showPropertyHistory = (row) => {
  state.currHistoryProperty = row
  refreshPropertyHistory()
}

// 修改后
const showPropertyHistory = (row) => {
  state.currHistoryProperty = row
  state.historyTime = [
    new Date(new Date().getTime() - 2 * 3600 * 1000),
    new Date()
  ]
  refreshPropertyHistory()
}
```

## 不涉及修改的内容

- 快捷选项（最近1小时/6小时/1天/5天）保持不变
- 后端接口参数不变
- `timeRangeChange` 回调不变
