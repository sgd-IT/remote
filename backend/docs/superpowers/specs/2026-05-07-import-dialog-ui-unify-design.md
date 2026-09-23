# 导入弹窗父设备选择 UI 样式统一设计

## 目标

调整设备导入弹窗中父设备选择区域的 UI，使三行（产品、父设备、文件上传）的表单结构、输入框高度、边距完全一致，整体视觉和谐。

## 当前问题

1. 产品选择行缺少 `el-form-item` 包裹，无 label，与下面两行边距不一致
2. `select-device` 组件内部 `el-input` 固定 `size="small"`（32px），产品选择行为默认尺寸（40px），高度不对齐
3. 父设备选择后缺少清除按钮，用户选错后无法取消

## 方案

采用"统一表单结构 + select-device 增加 size 控制"方案。

### 改动一：`select-device.vue` 增加 `size` prop

- 新增 `size: propTypes.string.def('small')`
- 模板中 `el-input` 的 `size` 绑定为 `:size="props.size"`
- 现有使用方不传 `size` 时行为不变

### 改动二：`index.vue` 导入弹窗模板

**产品选择行**：补上 `el-form-item label="产品"` 包裹

**父设备选择行**：
- `select-device` 传入 `size="default"`，使输入框高度与产品选择一致（40px）
- 绑定 `:id` prop，使组件能正确显示已选设备名称
- 监听 `@update:id`，同步处理组件内部 clear 事件
- 在组件右侧增加"清除"按钮（`el-button link type="danger"`），只在已选设备时显示

**文件上传行**：已有 `el-form-item`，无需改动。

## 边界处理

- 不选父设备时，显示 placeholder，清除按钮隐藏
- 点击组件内部 clear 图标或外部清除按钮，同步清空 `importParentObj`
- `select-device` 组件弹窗内部的设备列表不受 `size` prop 影响
