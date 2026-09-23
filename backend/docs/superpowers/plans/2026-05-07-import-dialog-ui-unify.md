# 导入弹窗父设备选择 UI 样式统一实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一导入弹窗中产品选择、父设备选择、文件上传三行的表单结构和输入框尺寸，使视觉一致。

**Architecture:** 扩展 `select-device` 组件增加 `size` prop，导入弹窗中传入 `size="default"` 统一高度；产品选择行补上 `el-form-item` 包裹；父设备选择行增加清除按钮和 `id` 绑定。

**Tech Stack:** Vue 3 + TypeScript, Element Plus

---

## 文件结构映射

| 文件 | 动作 | 职责 |
|------|------|------|
| `anwei-fire-remote-monitor-vue/src/components/EiotSelect/select-device.vue` | 修改 | 增加 `size` prop，模板 `el-input` 绑定 `:size` |
| `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/index.vue` | 修改 | 产品选择行加 `el-form-item`，父设备选择行调整尺寸、绑定 `id`、增加清除按钮 |

---

### Task 1: select-device.vue 增加 size prop

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/components/EiotSelect/select-device.vue`

- [ ] **Step 1: 增加 size prop**

在 `defineProps` 中增加：
```typescript
size: propTypes.string.def('small'),
```

- [ ] **Step 2: 模板中绑定 size**

将 `<el-input ... size="small" ...>` 改为：
```vue
<el-input class="cu-input" :placeholder="placeholder" :size="props.size" v-model.trim="name" @clear="onClear" clearable @click="handleSelect">
```

- [ ] **Step 3: 类型检查**

Run: `cd anwei-fire-remote-monitor-vue && npx vue-tsc --noEmit --skipLibCheck`
Expected: 无新增类型错误

---

### Task 2: index.vue 导入弹窗模板调整

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/index.vue`

- [ ] **Step 1: 产品选择行加 el-form-item**

将产品选择 `el-input` 用 `el-form-item label="产品"` 包裹：
```vue
<el-form-item label="产品">
  <el-input readonly :value="importProductObj?.name" :placeholder="$t('productKey')">
    <template #append>
      <el-button @click="selectImportProduct">{{ $t('选择产品') }}</el-button>
    </template>
  </el-input>
</el-form-item>
```

- [ ] **Step 2: 父设备选择行调整为统一样式**

替换现有的父设备选择 `el-form-item`：
```vue
<el-form-item label="父设备">
  <div style="display: flex; align-items: center; gap: 8px;">
    <select-device
      :id="importParentObj?.id?.toString() || ''"
      :node-type="0"
      size="default"
      placeholder="请选择父设备（可选）"
      @on-select="handleSelectParent"
      @update:id="(val) => { if (!val) clearImportParent() }"
      style="flex: 1;"
    />
    <el-button v-if="importParentObj" link type="danger" @click="clearImportParent">清除</el-button>
  </div>
</el-form-item>
```

- [ ] **Step 3: 类型检查**

Run: `cd anwei-fire-remote-monitor-vue && npx vue-tsc --noEmit --skipLibCheck`
Expected: 无新增类型错误

---

## 自检清单

**1. Spec coverage:**
- [x] select-device 增加 size prop → Task 1
- [x] 产品选择行加 el-form-item → Task 2 Step 1
- [x] 父设备选择行尺寸统一、增加清除按钮 → Task 2 Step 2
- [x] id 绑定和 clear 同步 → Task 2 Step 2

**2. Placeholder scan:**
- [x] 无 TBD/TODO
- [x] 所有步骤含完整代码

**3. Type consistency:**
- [x] `size` prop 类型为 string，Element Plus 兼容
- [x] `:id` 绑定为 string 类型
