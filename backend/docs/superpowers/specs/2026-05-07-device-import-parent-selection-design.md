# 设备导入父设备选择功能设计

## 1. 概述

### 1.1 背景

现有设备导入功能（`POST /eiot/device/importData`）支持通过 Excel 批量创建设备，但导入时无法直接绑定父设备。导入完成后，用户需要手动进入设备列表，选择子设备并调用绑定接口，操作繁琐。

### 1.2 设计目标

在设备导入流程中增加**可选的父设备选择**，使导入的新设备能够**在创建时直接绑定到指定的父设备**，减少用户操作步骤。

### 1.3 约束条件

- 父设备必须是**网关设备**（`nodeType = 0`）
- 父设备选择为**可选项**，允许不选
- 暂不支持在 Excel 模板中指定父设备，仅通过页面选择统一绑定

---

## 2. 架构设计

采用**最小改动方案**：扩展现有导入接口和导入弹窗，在设备创建时直接注入 `parentId`。

```
前端：设备导入弹窗
├── 产品选择（必填，已有）
├── 父设备选择（可选，新增）
│   └── 只读输入框 + "选择父设备"按钮
│   └── 弹窗仅展示 nodeType=0 的网关设备
└── 文件上传（已有）

后端：DeviceInfoController.importDevice()
├── 接收 file + productId + parentId（可选）
├── 解析 Excel
├── 校验 parentId（若传了）：存在性 + nodeType=0
└── 遍历创建设备，saveReqVO.setParentId(parentId)
```

---

## 3. 后端设计

### 3.1 接口变更

**接口**：`POST /eiot/device/importData`

**请求参数变更**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel 文件（已有） |
| productId | Long | 是 | 产品 ID（已有） |
| parentId | Long | **否** | **父设备 ID（新增，可选）** |

### 3.2 Service 层变更

**接口方法**：`DeviceInfoService.importDevice(List<DeviceInfoImportVo> list, Long productId, Long parentId)`

**实现逻辑**（`DeviceInfoServiceImpl.importDevice`）：

1. 若 `parentId` 不为空：
   - 查询父设备 `deviceInfoService.getDeviceInfo(parentId)`
   - 校验父设备存在，不存在则抛出 `ServiceException`
   - 校验父设备 `nodeType == 0`，不是网关设备则抛出 `ServiceException`
2. 遍历 Excel 设备列表：
   - `saveReqVO.setParentId(parentId)`（仅在 parentId 不为空时设置）
   - 调用 `createDeviceInfo(saveReqVO)` 创建设备

### 3.3 校验规则

| 校验项 | 失败时行为 |
|--------|-----------|
| parentId 存在性 | `ServiceException(400, "父设备不存在")` |
| parentId 类型 | `ServiceException(400, "父设备必须是网关设备")` |
| Excel 行校验失败 | 保持现有行为：记录到 `failureKeyMap`，继续处理其他行 |

---

## 4. 前端设计

### 4.1 导入弹窗改造

在 `index.vue` 的导入对话框（`fileUploadDialog`）中，产品选择下方增加父设备选择行：

```vue
<el-input readonly :value="importParentObj?.name" placeholder="请选择父设备（可选）">
  <template #append>
    <el-button @click="selectImportParent">选择父设备</el-button>
    <el-button v-if="importParentObj" @click="clearImportParent">清除</el-button>
  </template>
</el-input>
```

### 4.2 设备选择弹窗

复用或参考 `product-list.vue` 的模式，新建 `device-list.vue` 组件（或复用已有的子设备/设备选择组件）：

- 弹窗标题："选择父设备"
- 过滤条件：固定 `nodeType = 0`（仅展示网关设备）
- 支持分页和搜索
- 单选模式

### 4.3 上传 URL 变更

```typescript
const uploadUrl = computed(() => {
  let url = importDeviceUrl.value + '?productId=' + importProductObj.value?.id
  if (importParentObj.value?.id) {
    url += '&parentId=' + importParentObj.value.id
  }
  return url
})
```

---

## 5. 数据流

```
用户点击"导入设备"
  └── 打开导入弹窗
      ├── 选择产品（必填）
      ├── 选择父设备（可选）→ 弹窗过滤 nodeType=0 设备
      └── 上传 Excel 文件
          └── POST /eiot/device/importData?productId=xxx&parentId=yyy
              ├── 解析 Excel
              ├── 校验 parentId（存在性 + nodeType=0）
              ├── 遍历设备列表
              │   └── saveReqVO.setParentId(yyy)
              │   └── createDeviceInfo(saveReqVO)
              └── 返回 DeviceImportRespVO
                  ├── createKey: 成功创建的设备名称列表
                  ├── updateKey: 更新成功的设备名称列表
                  └── failureKeyMap: 失败的设备及原因
```

---

## 6. 边界处理

| 场景 | 处理策略 |
|------|---------|
| 未选择父设备 | `parentId` 为空，保持现有导入逻辑不变 |
| 父设备校验失败 | 整个导入请求返回错误，不创建任何设备（与现有"产品不存在"行为一致） |
| Excel 中部分行校验失败 | 失败的行记录到 `failureKeyMap`，成功的行正常创建并绑定父设备 |
| 父设备被删除 | 导入时实时查询校验，已删除则返回错误 |

---

## 7. 影响范围

| 文件 | 改动 |
|------|------|
| `DeviceInfoController.java` | `importDevice` 增加 `parentId` 可选参数 |
| `DeviceInfoService.java` | `importDevice` 方法签名增加 `Long parentId` |
| `DeviceInfoServiceImpl.java` | `importDevice` 增加父设备校验和 `setParentId` 逻辑 |
| `index.vue` | 导入对话框增加父设备选择 UI |
| `device-list.vue`（新建） | 设备选择弹窗组件，过滤网关设备 |

---

## 8. 测试策略

| 测试项 | 预期结果 |
|--------|---------|
| 导入时不选父设备 | 设备正常创建，`parentId` 为 null |
| 导入时选择有效父设备 | 所有新建设备 `parentId` 指向该设备 |
| 导入时选择非网关设备 | 返回 400，提示"父设备必须是网关设备" |
| 导入时选择不存在的父设备 | 返回 400，提示"父设备不存在" |
| Excel 部分行失败 | 成功行绑定父设备，失败行记录在 `failureKeyMap` |
