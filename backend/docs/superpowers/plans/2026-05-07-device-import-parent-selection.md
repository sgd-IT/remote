# 设备导入父设备选择功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在设备导入流程中增加可选的父设备选择，使导入的新设备能够在创建时直接绑定到指定的网关设备。

**Architecture:** 扩展现有 `importData` 接口增加可选 `parentId` 参数，在 `DeviceInfoServiceImpl.importDevice()` 中校验父设备（存在性 + nodeType=0）后直接设置 `saveReqVO.parentId`。前端在导入弹窗中复用 `select-device` 组件并增加 `nodeType` 过滤。

**Tech Stack:** Java 17, Spring Boot, Vue 3 + TypeScript, Element Plus

---

## 文件结构映射

| 文件 | 动作 | 职责 |
|------|------|------|
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/DeviceInfoController.java` | 修改 | Controller：增加 `parentId` 可选参数 |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoService.java` | 修改 | Service 接口：扩展 `importDevice` 签名 |
| `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoServiceImpl.java` | 修改 | Service 实现：增加父设备校验 + 设置 parentId |
| `anwei-fire-remote-monitor-vue/src/components/EiotSelect/select-device.vue` | 修改 | 通用设备选择器：增加 `nodeType` prop 过滤 |
| `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/index.vue` | 修改 | 设备列表页：导入弹窗增加父设备选择 UI |

---

### Task 1: 后端 Controller 增加 parentId 参数

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/device/DeviceInfoController.java`

- [ ] **Step 1: 修改 importDevice 方法签名和调用**

找到 `importDevice` 方法（约第 103-110 行），增加可选的 `parentId` 参数并透传给 Service：

```java
    @Operation(summary = "导入设备")
    @PreAuthorize("@ss.hasPermission('iot:device-info:create')")
    @PostMapping("/importData")
    public CommonResult<DeviceImportRespVO> importDevice(
            @RequestPart("file") MultipartFile file,
            @RequestParam("productId") Long productId,
            @RequestParam(value = "parentId", required = false) Long parentId) throws IOException {
        List<DeviceInfoImportVo> list = ExcelUtils.read(file, DeviceInfoImportVo.class);
        return CommonResult.success(deviceInfoService.importDevice(list, productId, parentId));
    }
```

- [ ] **Step 2: 编译检查**

Run: `cd anwei-fire-remote-monitor && mvn compile -pl module-eiot/module-eiot-biz -am -q`
Expected: 编译失败（DeviceInfoService.importDevice 方法签名不匹配，预期）

---

### Task 2: 后端 Service 接口扩展 importDevice 签名

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoService.java`

- [ ] **Step 1: 修改 importDevice 方法签名**

找到 `importDevice` 方法声明（约第 104 行）：

```java
    DeviceImportRespVO importDevice(List<DeviceInfoImportVo> list, Long productId, Long parentId);
```

- [ ] **Step 2: 编译检查**

Run: `cd anwei-fire-remote-monitor && mvn compile -pl module-eiot/module-eiot-biz -am -q`
Expected: 编译失败（DeviceInfoServiceImpl 未实现新方法签名，预期）

---

### Task 3: 后端 ServiceImpl 实现父设备校验和 parentId 设置

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/device/DeviceInfoServiceImpl.java`

- [ ] **Step 1: 修改 importDevice 方法实现**

找到 `importDevice` 方法（约第 253-293 行），修改为：

```java
    @Override
    public DeviceImportRespVO importDevice(List<DeviceInfoImportVo> list, Long productId, Long parentId) {
        // 1.1 参数校验
        if (CollUtil.isEmpty(list)) {
            throw new ServiceException(400, "导入数据不许为空");
        }
        Product product = ProductConvert.INSTANCE.convert(productMapper.selectById(productId));
        if (Objects.isNull(product)) {
            throw new ServiceException(400, "产品不存在");
        }

        // 1.2 父设备校验（如果传了 parentId）
        if (ObjectUtil.isNotNull(parentId)) {
            DeviceInfo parentDevice = getDeviceInfo(parentId);
            if (Objects.isNull(parentDevice)) {
                throw new ServiceException(400, "父设备不存在");
            }
            if (!Integer.valueOf(0).equals(parentDevice.getNodeType())) {
                throw new ServiceException(400, "父设备必须是网关设备");
            }
        }

        // 2. 遍历，逐个创建 or 更新
        DeviceImportRespVO respVO = DeviceImportRespVO.builder().createKey(new ArrayList<>())
                .updateKey(new ArrayList<>()).failureKeyMap(new LinkedHashMap<>()).build();

        list.forEach(o -> {
            DeviceInfoSaveReqVO saveReqVO = BeanUtils.toBean(o, DeviceInfoSaveReqVO.class);
            saveReqVO.setProductKey(product.getProductKey());
            // 设置父设备（可选）
            if (ObjectUtil.isNotNull(parentId)) {
                saveReqVO.setParentId(parentId);
            }

            try {
                ValidationUtils.validate(saveReqVO);
            } catch (ConstraintViolationException ex) {
                respVO.getFailureKeyMap().put(saveReqVO.getName(), ex.getMessage());
                return;
            }

            try {
                validateDeviceForCreateOrUpdate(saveReqVO);
            } catch (ConstraintViolationException ex) {
                respVO.getFailureKeyMap().put(saveReqVO.getName(), ex.getMessage());
                return;
            }
            Long deviceId = createDeviceInfo(saveReqVO);
            if (ObjectUtil.isNotNull(deviceId)) {
                respVO.getCreateKey().add(saveReqVO.getName());
            }

        });

        return respVO;
    }
```

- [ ] **Step 2: 编译检查**

Run: `cd anwei-fire-remote-monitor && mvn compile -pl module-eiot/module-eiot-biz -am -q`
Expected: 编译成功（BUILD SUCCESS）

- [ ] **Step 3: 后端单元测试（可选，若项目有现有测试模式则执行）**

若 `DeviceInfoServiceImplTest` 存在，运行相关测试。
Run: `cd anwei-fire-remote-monitor && mvn test -pl module-eiot/module-eiot-biz -Dtest=DeviceInfoServiceImplTest -q`
Expected: 测试通过（若无此测试类则跳过）

---

### Task 4: 前端 select-device 组件增加 nodeType 过滤

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/components/EiotSelect/select-device.vue`

- [ ] **Step 1: 增加 nodeType prop**

在 `defineProps` 中增加 `nodeType` prop：

```typescript
const props = defineProps({
  id: propTypes.string.def(''),
  dn: propTypes.string.def(''),
  productPk: propTypes.string.def(''),
  multiple: propTypes.bool.def(false),
  placeholder: propTypes.string.def('请选择设备'),
  // 新增：按设备类型过滤（如 0=网关设备）
  nodeType: propTypes.number.def(undefined),
})
```

- [ ] **Step 2: 在 getData 中传入 nodeType**

修改 `getData` 方法（约第 248-259 行）：

```typescript
const getData = () => {
  state.loading = true
  DeviceInfoApi.getDeviceInfoPage({
    ...state.query,
    productKey: props.productPk || state.query['productKey'],
    nodeType: props.nodeType,
    ...state.page,
  }).then((res) => {
    data.value = res.list || []
    state.total = res.total
  })
  state.loading = false
}
```

- [ ] **Step 3: 类型检查**

Run: `cd anwei-fire-remote-monitor-vue && npx vue-tsc --noEmit --skipLibCheck`
Expected: 无类型错误（或仅有原有错误）

---

### Task 5: 前端导入弹窗增加父设备选择

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/deviceinfo/index.vue`

- [ ] **Step 1: 引入 select-device 组件**

在 `<script>` 的 import 区域（约第 293-313 行附近）增加：

```typescript
import SelectDevice from '@/components/EiotSelect/select-device.vue'
```

- [ ] **Step 2: 增加父设备选择相关状态**

在 `<script setup>` 中增加响应式状态（可在 `importProductObj` 附近，约第 393 行）：

```typescript
const importParentObj = ref<any>()

const handleSelectParent = (row: any) => {
  importParentObj.value = row
}

const clearImportParent = () => {
  importParentObj.value = undefined
}
```

- [ ] **Step 3: 修改导入弹窗模板**

在导入弹窗的 `el-form` 中（约第 256-272 行），在产品选择下方增加父设备选择：

```vue
      <el-form ref="ossFormRef" v-model="fileUploadDialog.visible" label-width="80px">
        <el-form-item label="产品">
          <el-input readonly :value="importProductObj?.name" :placeholder="$t('productKey')">
            <template #append>
              <el-button @click="selectImportProduct">{{ $t('选择产品') }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <!-- 新增：父设备选择（可选） -->
        <el-form-item label="父设备">
          <select-device
            v-model:id="importParentObj?.id"
            :node-type="0"
            placeholder="请选择父设备（可选）"
            @on-select="handleSelectParent"
          />
        </el-form-item>
        <el-form-item label="文件名">
          <fileUpload
            :fileSize="10"
            :fileType="['xlsx', 'xls']"
            :limit="1"
            uploadType="url"
            :uploadUrl="uploadUrl"
            @upload-success="handleUploadSuccess"
          />
        </el-form-item>
      </el-form>
```

注意：如果 `select-device` 组件的 `v-model:id` 绑定方式不适用（因为 `importParentObj` 初始为 undefined），可以改为：

```vue
        <el-form-item label="父设备">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input readonly :value="importParentObj?.name" placeholder="请选择父设备（可选）" style="flex: 1;">
              <template #append>
                <el-button @click="selectParentDialogVisible = true">选择</el-button>
              </template>
            </el-input>
            <el-button v-if="importParentObj" type="danger" link @click="clearImportParent">清除</el-button>
          </div>
        </el-form-item>
```

然后增加一个弹窗：
```vue
    <el-dialog title="选择父设备" v-model="selectParentDialogVisible" width="1200px" append-to-body destroy-on-close>
      <select-device
        :node-type="0"
        placeholder="请选择父设备"
        @on-select="(row) => { handleSelectParent(row); selectParentDialogVisible = false; }"
      />
    </el-dialog>
```

考虑到 `select-device` 组件内部已经包含弹窗逻辑，更简洁的用法是直接使用组件本身（它会自带弹窗）：

```vue
        <el-form-item label="父设备">
          <select-device
            :id="importParentObj?.id"
            :node-type="0"
            placeholder="请选择父设备（可选）"
            @on-select="handleSelectParent"
          />
        </el-form-item>
```

但需要注意 `select-device` 组件中 `name` computed 属性使用的是 `dialogState.data?.name || props.id || props.dn || ''`。由于我们不使用 `v-model:id`，需要确保组件能正确显示名称。

更可靠的方式是参照产品选择的模式（只读输入框 + 按钮 + 自定义弹窗），因为 `select-device` 的行为可能不完全匹配导入弹窗的样式需求。

**最终方案**：复用 `select-device` 组件但放在表单项内，或者直接用 `el-input` + `select-device` 的对话框模式。

考虑到简洁性和可控性，建议这样写：

```vue
        <el-form-item label="父设备">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input
              readonly
              :value="importParentObj?.name || importParentObj?.dn"
              placeholder="请选择父设备（可选）"
              style="flex: 1;"
            >
              <template #append>
                <el-button @click="openSelectParent">选择</el-button>
              </template>
            </el-input>
            <el-button v-if="importParentObj" link type="danger" @click="clearImportParent">清除</el-button>
          </div>
        </el-form-item>
```

然后使用 `select-device` 的实例方法或直接操作其 dialog。但 `select-device` 没有暴露 `open` 方法。

**最简单可靠的方案**：直接在导入弹窗模板中内联使用 `select-device` 组件，利用它自带的输入框和弹窗：

```vue
        <el-form-item label="父设备">
          <select-device
            :node-type="0"
            placeholder="请选择父设备（可选）"
            @on-select="handleSelectParent"
          />
        </el-form-item>
```

但这会在选择后只显示 id 或 dn，不显示 name。

**最佳方案**：修改 `select-device` 组件，支持 `v-model:data` 或确保 `@on-select` 能带回完整数据，然后在导入弹窗中管理状态。

由于 `select-device` 的 `@on-select` 事件已经传回了完整的 `row` 对象，我们可以在 `handleSelectParent` 中接收并存储。

但显示问题：`select-device` 组件内部的 `name` computed 依赖 `dialogState.data?.name || props.id || props.dn || ''`。如果不传 `id` 或 `dn` prop，它会显示空字符串，直到选择后才显示。

这是可以接受的，因为初始状态就是未选择。

所以最简洁的模板是：

```vue
        <el-form-item label="父设备">
          <select-device
            :node-type="0"
            placeholder="请选择父设备（可选）"
            @on-select="handleSelectParent"
          />
        </el-form-item>
```

然后在 `handleSelectParent` 中存储选择的设备对象。

但我还需要处理 `uploadUrl`。当前 `uploadUrl` 是硬编码的字符串，需要改为 computed 属性或方法。

当前代码（约第 268 行）：
```vue
:uploadUrl="importDeviceUrl + '?productId=' + importProductObj?.id"
```

需要改为：
```vue
:uploadUrl="computedUploadUrl"
```

然后在 script 中定义：
```typescript
const computedUploadUrl = computed(() => {
  let url = importDeviceUrl.value + '?productId=' + (importProductObj.value?.id || '')
  if (importParentObj.value?.id) {
    url += '&parentId=' + importParentObj.value.id
  }
  return url
})
```

- [ ] **Step 4: 修改 uploadUrl 为计算属性**

将模板中的 `:uploadUrl` 绑定改为计算属性，并定义该计算属性：

```typescript
const computedUploadUrl = computed(() => {
  let url = importDeviceUrl.value + '?productId=' + (importProductObj.value?.id || '')
  if (importParentObj.value?.id) {
    url += '&parentId=' + importParentObj.value.id
  }
  return url
})
```

模板中改为：
```vue
:uploadUrl="computedUploadUrl"
```

- [ ] **Step 5: 处理导入成功后的状态重置**

在 `handleUploadSuccess` 方法中（如果存在），确保导入成功后清除 `importParentObj`，以便下次导入时从空状态开始。若该方法只是关闭弹窗和刷新列表，则无需额外处理。

- [ ] **Step 6: 类型检查**

Run: `cd anwei-fire-remote-monitor-vue && npx vue-tsc --noEmit --skipLibCheck`
Expected: 无新增类型错误

---

### Task 6: 前端功能验证

**Files:**
- N/A（验证任务）

- [ ] **Step 1: 启动前端开发服务器**

Run: `cd anwei-fire-remote-monitor-vue && npm run dev`
Expected: 服务器启动成功，显示本地访问地址

- [ ] **Step 2: 功能验证清单**

1. 打开设备管理页面，点击"导入设备"
2. 选择产品（必填）
3. 点击"选择父设备"按钮，确认弹窗中只显示网关设备（nodeType=0）
4. 选择一个网关设备，确认输入框显示设备名称
5. 上传有效的设备 Excel 文件
6. 导入成功后，检查数据库中新建设备的 `parent_id` 字段是否已填充
7. 重复测试：不选父设备导入，确认 `parent_id` 为 null
8. 重复测试：选择非网关设备（如果后端校验正确，应返回错误）

- [ ] **Step 3: 后端接口测试（可选，使用 Postman 或 curl）**

```bash
curl -X POST "http://localhost:48080/eiot/device/importData?productId=1&parentId=999" \
  -H "Authorization: Bearer {token}" \
  -F "file=@test.xlsx"
```

Expected: 若 parentId=999 不存在，返回 `{"code":400,"msg":"父设备不存在"}`

---

## 自检清单

**1. Spec coverage:**
- [x] 后端接口增加 parentId 参数 → Task 1
- [x] Service 接口扩展 → Task 2
- [x] ServiceImpl 校验和设置 parentId → Task 3
- [x] 前端设备选择器增加 nodeType 过滤 → Task 4
- [x] 前端导入弹窗增加父设备选择 UI → Task 5
- [x] 上传 URL 附加 parentId → Task 5 Step 4
- [x] 父设备类型限制（nodeType=0）→ Task 3 Step 1
- [x] 父设备可选 → Task 5

**2. Placeholder scan:**
- [x] 无 TBD/TODO/"implement later"
- [x] 无模糊描述，所有步骤含完整代码

**3. Type consistency:**
- [x] `parentId` 在 Controller/Service/Impl 中类型一致（Long）
- [x] `nodeType` 在 Vue 组件中为 number 类型
- [x] 方法名一致：`importDevice` 签名匹配
