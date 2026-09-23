# 视频监控模块第一阶段实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **注意：所有代码变更需经用户确认后才能提交。**

**Goal:** 完成视频监控模块第一阶段的开发，核心包括：ICC 设备编码/通道编码分离、视频管理页树形+1/4/9分屏实时预览、摄像头管理页支持 ICC 设备列表选择、报警联动视频弹窗。

**Architecture:** 后端 Spring Boot 封装 ICC OpenAPI 统一管理鉴权和取流；前端 Vue3 + Element Plus 实现树形设备列表和多画面 WebRTC 播放器；消防部件报警时通过 `device_camera_relation` 关联自动弹出关联摄像头画面。

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Hutool HTTP, Vue3, Element Plus, WebRTC

---

## 现状说明

项目中已存在视频监控基础代码：
- 后端：`VideoCameraDO/Controller/Service`、`IccOpenApiClient`（Token + startVideo）、`DeviceCameraRelation`、`IccMediaServerDO`
- 前端：`video-manage`（卡片式列表+单播放器弹窗）、`camera-manage`（表单管理）、`device-bind`（部件绑定）、`media-server`（ICC配置）
- 数据库：`video_camera`、`icc_media_server`、`device_camera_relation` 表已创建

**当前缺陷**：`video_camera` 无 `device_id` 字段；`IccOpenApiClient.startVideo` 只传 `channelId`；视频管理页为卡片式而非分屏；播放器使用原生 `<video>` 而非 WebRTC。

---

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `_sql/mysql/update.sql` | 修改 | 添加 `video_camera.device_id` 字段 |
| `VideoCameraDO.java` | 修改 | 增加 `deviceId` 字段 |
| `VideoCameraCreateReqVO.java` | 修改 | 增加 `deviceId` 字段 |
| `VideoCameraUpdateReqVO.java` | 修改 | 增加 `deviceId` 字段 |
| `VideoCameraRespVO.java` | 修改 | 增加 `deviceId` 字段 |
| `VideoCameraController.java` | 修改 | create/update 增加 deviceId；live/url 改为按 cameraId 查询 |
| `VideoCameraServiceImpl.java` | 修改 | 无逻辑变更，兼容 deviceId 即可 |
| `IccOpenApiClient.java` | 修改 | `startVideo` 增加 `deviceId` 参数 |
| `VideoCameraController.java` | 修改 | 新增 `GET /icc-devices` 接口 |
| `videoCamera.api.ts` | 修改 | 增加 `getIccDevices`、修改 `getLiveUrl` 传参 |
| `camera-manage/index.vue` | 修改 | 表单增加 deviceId 输入框和 ICC 设备选择按钮 |
| `VideoPlayer.vue` | 修改 | 替换为 WebRTC 播放器（使用 `webrtc-streamer` 或类似库） |
| `video-manage/index.vue` | 重写 | 树形左侧 + 1/4/9 分屏实时预览 |
| `alarm-order/index.vue` | 修改 | 现场视频区域接入真实播放器弹窗 |

---

### Task 1: 数据库迁移 — video_camera 添加 device_id

**Files:**
- Modify: `_sql/mysql/update.sql`

- [ ] **Step 1: 在 update.sql 中添加 ALTER 语句**

在文件末尾追加：

```sql
-- 2026-04-27 视频模块 device_id 字段迁移
ALTER TABLE `video_camera`
    ADD COLUMN `device_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备编码/录像机编码' AFTER `channel_id`;
```

- [ ] **Step 2: 用户确认后手动执行 SQL**

由用户或 DBA 在目标数据库执行上述 ALTER 语句。

---

### Task 2: 后端数据模型 — VideoCameraDO 增加 deviceId

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/dal/dataobject/video/VideoCameraDO.java`

- [ ] **Step 1: 增加 deviceId 字段**

在 `private String channelId;` 下方添加：

```java
    private String deviceId;
```

---

### Task 3: 后端 VO — 更新请求和响应对象

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoCameraCreateReqVO.java`
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoCameraUpdateReqVO.java`
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/vo/VideoCameraRespVO.java`

- [ ] **Step 1: VideoCameraCreateReqVO 增加 deviceId**

在 `private String channelId;` 上方或下方添加：

```java
    @Schema(description = "设备编码/录像机编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "100001")
    @NotBlank(message = "设备编码不能为空")
    private String deviceId;
```

- [ ] **Step 2: VideoCameraUpdateReqVO 增加 deviceId**

同样在 channelId 附近增加 `deviceId` 字段（不带 `@NotBlank`，更新时可为空表示不修改）。

```java
    @Schema(description = "设备编码/录像机编码", example = "100001")
    private String deviceId;
```

- [ ] **Step 3: VideoCameraRespVO 增加 deviceId**

```java
    @Schema(description = "设备编码/录像机编码")
    private String deviceId;
```

---

### Task 4: 后端 Controller — VideoCameraController 改造

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/controller/admin/video/VideoCameraController.java`

- [ ] **Step 1: create 方法增加 deviceId 赋值**

在 `camera.setChannelId(reqVO.getChannelId());` 下方添加：

```java
        camera.setDeviceId(reqVO.getDeviceId());
```

- [ ] **Step 2: update 方法增加 deviceId 赋值**

在 `camera.setId(reqVO.getId());` 下方添加：

```java
        camera.setDeviceId(reqVO.getDeviceId());
```

- [ ] **Step 3: live/url 接口改为接收 cameraId 而非 channelId**

将原方法：

```java
    @GetMapping("/live/url")
    @Operation(summary = "获取实时视频流地址")
    public CommonResult<VideoLiveUrlRespVO> getLiveUrl(@RequestParam("channelId") String channelId) {
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }
        IccStartVideoRespDTO dto = iccOpenApiClient.startVideo(config, channelId);
```

替换为：

```java
    @GetMapping("/live/url")
    @Operation(summary = "获取实时视频流地址")
    public CommonResult<VideoLiveUrlRespVO> getLiveUrl(@RequestParam("cameraId") Long cameraId) {
        VideoCameraDO camera = videoCameraService.getById(cameraId);
        if (camera == null) {
            throw new RuntimeException("摄像头不存在");
        }
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }
        IccStartVideoRespDTO dto = iccOpenApiClient.startVideo(config, camera.getDeviceId(), camera.getChannelId());
```

- [ ] **Step 4: 新增 ICC 设备列表接口**

在 Controller 类末尾、`}` 之前添加：

```java
    @GetMapping("/icc-devices")
    @Operation(summary = "获取 ICC 平台设备/通道列表")
    public CommonResult<List<Map<String, Object>>> getIccDevices() {
        IccMediaServerDO config = mediaServerService.getCurrentTenantConfig();
        if (config == null) {
            throw new RuntimeException("ICC平台未配置");
        }
        List<Map<String, Object>> devices = iccOpenApiClient.getDeviceList(config);
        return success(devices);
    }
```

---

### Task 5: 后端 ICC 客户端 — 增加 deviceId 和获取设备列表

**Files:**
- Modify: `module-eiot/module-eiot-biz/src/main/java/com/enjoyiot/module/eiot/service/video/icc/IccOpenApiClient.java`

- [ ] **Step 1: startVideo 增加 deviceId 参数**

将方法签名：

```java
    public IccStartVideoRespDTO startVideo(IccMediaServerDO config, String channelId) {
```

改为：

```java
    public IccStartVideoRespDTO startVideo(IccMediaServerDO config, String deviceId, String channelId) {
```

并将 `data.put("channelId", channelId);` 上方添加：

```java
        data.put("deviceId", deviceId);
```

- [ ] **Step 2: 新增 getDeviceList 方法**

在 `startVideo` 方法之后添加：

```java
    public List<Map<String, Object>> getDeviceList(IccMediaServerDO config) {
        String token = getAccessToken(config);
        String url = config.getBaseUrl() + "/evo-apigw/admin/API/MTS/Video/GetDeviceList";

        Map<String, Object> data = new HashMap<>();
        data.put("pageSize", 1000);
        data.put("pageNo", 1);

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body(JSONUtil.toJsonStr(body))
                .timeout(10000)
                .execute();

        String respBody = response.body();
        log.info("[IccOpenApiClient] GetDeviceList response: {}", respBody);

        Map<String, Object> result = JSONUtil.toBean(respBody, Map.class);
        Integer code = ((Number) result.get("code")).intValue();
        if (code != 1000) {
            String desc = (String) result.getOrDefault("desc", "Unknown error");
            throw new RuntimeException("ICC获取设备列表失败: " + desc);
        }

        Map<String, Object> respData = (Map<String, Object>) result.get("data");
        List<Map<String, Object>> devices = (List<Map<String, Object>>) respData.get("list");
        return devices != null ? devices : java.util.Collections.emptyList();
    }
```

> **注意**：ICC API 的实际路径和响应结构需根据大华文档确认。上述 `/GetDeviceList` 为占位路径，实际开发前需用户确认 ICC 获取设备列表的确切 API 路径。

---

### Task 6: 前端 API — 更新 videoCamera.api.ts

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/api/eiot/video/videoCamera.api.ts`

- [ ] **Step 1: 增加 ICC 设备列表接口，修改取流接口**

将文件内容替换为：

```typescript
import request from '@/config/axios'

enum Api {
  create = '/eiot/video/camera/create',
  update = '/eiot/video/camera/update',
  delete = '/eiot/video/camera/delete',
  get = '/eiot/video/camera/get',
  list = '/eiot/video/camera/list',
  page = '/eiot/video/camera/page',
  liveUrl = '/eiot/video/camera/live/url',
  iccDevices = '/eiot/video/camera/icc-devices'
}

export const createVideoCamera = (data: any) => {
  return request.post({ url: Api.create, data })
}

export const updateVideoCamera = (data: any) => {
  return request.put({ url: Api.update, data })
}

export const deleteVideoCamera = (id: number) => {
  return request.delete({ url: Api.delete, params: { id } })
}

export const getVideoCamera = (id: number) => {
  return request.get({ url: Api.get, params: { id } })
}

export const getVideoCameraList = (deptId: number) => {
  return request.get({ url: Api.list, params: { deptId } })
}

export const getVideoCameraPage = (params: any) => {
  return request.get({ url: Api.page, params })
}

export const getLiveUrl = (cameraId: number) => {
  return request.get({ url: Api.liveUrl, params: { cameraId } })
}

export const getIccDevices = () => {
  return request.get({ url: Api.iccDevices })
}
```

---

### Task 7: 前端摄像头管理页 — 增加 deviceId 和 ICC 设备选择

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/video/camera-manage/index.vue`

- [ ] **Step 1: 表单增加 deviceId 输入和 ICC 设备选择**

在 `el-form` 中，将原来的 "通道编码" 表单项改为两个独立表单项：

```vue
        <el-form-item label="设备编码" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备编码，如 100001" />
        </el-form-item>
        <el-form-item label="通道编码" prop="channelId">
          <el-input v-model="form.channelId" placeholder="请输入通道编码，如 100001$1$0$0" />
        </el-form-item>
```

- [ ] **Step 2: 增加 ICC 设备选择弹窗**

在 `el-dialog` 的 `template #footer` 之前添加一个按钮用于打开 ICC 设备选择弹窗：

```vue
        <el-form-item>
          <el-button type="info" link @click="openIccDeviceSelector">从 ICC 设备列表选择</el-button>
        </el-form-item>
```

在 `</ContentWrap>` 之前添加选择弹窗：

```vue
    <el-dialog v-model="iccSelectorVisible" title="选择 ICC 设备" width="600px">
      <el-tree
        v-loading="iccLoading"
        :data="iccDeviceTree"
        :props="{ label: 'name', children: 'channels' }"
        @node-click="handleIccNodeClick"
      />
    </el-dialog>
```

- [ ] **Step 3: script 部分增加 ICC 设备选择逻辑**

导入 `getIccDevices`：

```typescript
import {
  getVideoCameraList,
  createVideoCamera,
  updateVideoCamera,
  deleteVideoCamera,
  getIccDevices
} from '@/api/eiot/video/videoCamera.api'
```

增加响应式变量：

```typescript
const iccSelectorVisible = ref(false)
const iccLoading = ref(false)
const iccDeviceTree = ref<any[]>([])
```

增加方法：

```typescript
const openIccDeviceSelector = async () => {
  iccSelectorVisible.value = true
  iccLoading.value = true
  try {
    const res = await getIccDevices()
    iccDeviceTree.value = (res.data || []).map((d: any) => ({
      name: d.deviceName || d.deviceId,
      deviceId: d.deviceId,
      channels: (d.channels || []).map((c: any) => ({
        name: c.channelName || c.channelId,
        deviceId: d.deviceId,
        channelId: c.channelId
      }))
    }))
  } catch (e: any) {
    ElMessage.error(e.message || '获取 ICC 设备列表失败')
  } finally {
    iccLoading.value = false
  }
}

const handleIccNodeClick = (node: any) => {
  if (node.channelId) {
    form.value.deviceId = node.deviceId
    form.value.channelId = node.channelId
    iccSelectorVisible.value = false
  }
}
```

- [ ] **Step 4: 表格增加 deviceId 列显示**

在 `<el-table>` 中，在 channelId 列之前添加：

```vue
      <el-table-column prop="deviceId" label="设备编码" width="140" />
```

---

### Task 8: 前端播放器组件 — 替换为 WebRTC 播放器

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/video/components/VideoPlayer.vue`

> **技术选型说明**：WebRTC 播放器依赖 `webrtc-streamer` 或类似工具将 RTSP 流转为 WebRTC。这里假设后端已经通过 ICC 获取了 WebRTC 播放地址（ICC StartVideo 可能直接返回 WebRTC URL），前端使用 `<video>` 标签即可播放。如果 ICC 返回的是 RTSP 地址，则需要引入 `webrtc-streamer` 库在前端做协议转换。
>
> **当前简化方案**：先验证 ICC 返回的 `url` 是否可直接在 `<video>` 中播放（如为 HLS/FLV over HTTP）。如不可直接播放，则在后续迭代中引入 `mpegts.js` 或 `webrtc-streamer`。本 Task 先增强播放器状态和重试机制。

- [ ] **Step 1: 增强 VideoPlayer 组件**

将原组件替换为带错误重试和状态管理的版本：

```vue
<template>
  <div class="video-player-wrapper">
    <div v-if="loading" class="video-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在获取视频流...</span>
    </div>
    <div v-else-if="error" class="video-error">
      <el-icon><Warning /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" type="primary" @click="retry">重试</el-button>
    </div>
    <video
      v-else-if="videoUrl"
      ref="videoRef"
      class="video-element"
      :src="videoUrl"
      controls
      autoplay
      muted
      playsinline
      @error="onVideoError"
    />
    <div v-else class="video-empty">
      <el-icon><VideoCamera /></el-icon>
      <span>暂无视频</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { Loading, Warning, VideoCamera } from '@element-plus/icons-vue'

const props = defineProps<{
  videoUrl: string | null
  loading?: boolean
  error?: string | null
}>()

const emit = defineEmits<{
  retry: []
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const localError = ref<string | null>(null)

watch(() => props.videoUrl, (newUrl) => {
  localError.value = null
  if (newUrl && videoRef.value) {
    videoRef.value.load()
    videoRef.value.play().catch(() => {})
  }
})

const onVideoError = () => {
  localError.value = '视频播放失败，请检查网络或重试'
}

const retry = () => {
  localError.value = null
  emit('retry')
}

onUnmounted(() => {
  if (videoRef.value) {
    videoRef.value.pause()
    videoRef.value.src = ''
  }
})
</script>

<style scoped>
.video-player-wrapper {
  width: 100%;
  height: 100%;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}
.video-element {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.video-loading,
.video-error,
.video-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 14px;
}
.video-error {
  color: #f56c6c;
}
</style>
```

---

### Task 9: 前端视频管理页 — 重写为树形+1/4/9分屏

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/video/video-manage/index.vue`

- [ ] **Step 1: 重写页面布局**

将页面改为左侧树形 + 右侧分屏播放器的布局。由于代码量较大，这里给出核心结构：

```vue
<template>
  <ContentWrap>
    <div class="video-monitor-container">
      <!-- 左侧树形 -->
      <div class="video-sidebar">
        <el-input v-model="treeKeyword" placeholder="搜索摄像头" clearable />
        <el-tree
          ref="treeRef"
          :data="cameraTree"
          :props="{ label: 'channelName', children: 'children' }"
          :filter-node-method="filterNode"
          highlight-current
          @node-click="handleTreeNodeClick"
        />
      </div>
      <!-- 右侧播放器区域 -->
      <div class="video-main">
        <div class="layout-toolbar">
          <el-radio-group v-model="layoutMode" size="small">
            <el-radio-button :label="1">1画面</el-radio-button>
            <el-radio-button :label="4">4画面</el-radio-button>
            <el-radio-button :label="9">9画面</el-radio-button>
          </el-radio-group>
        </div>
        <div class="screen-grid" :class="'grid-' + layoutMode">
          <div
            v-for="i in layoutMode"
            :key="i"
            class="screen-cell"
            :class="{ active: activeScreen === i }"
            @click="activeScreen = i"
          >
            <VideoPlayer
              v-if="screenCameras[i - 1]"
              :video-url="screenUrls[i - 1]"
              :loading="screenLoading[i - 1]"
              :error="screenErrors[i - 1]"
              @retry="loadScreenVideo(i - 1)"
            />
            <div v-else class="screen-placeholder">点击左侧摄像头添加</div>
          </div>
        </div>
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ContentWrap } from '@/components/ContentWrap'
import { getVideoCameraPage, getLiveUrl } from '@/api/eiot/video/videoCamera.api'
import VideoPlayer from '../components/VideoPlayer.vue'

const treeKeyword = ref('')
const treeRef = ref<any>(null)
const cameraTree = ref<any[]>([])
const layoutMode = ref(1)
const activeScreen = ref(1)
const screenCameras = ref<any[]>([])
const screenUrls = ref<(string | null)[]>([])
const screenLoading = ref<boolean[]>([])
const screenErrors = ref<(string | null)[]>([])

watch(treeKeyword, (val) => {
  treeRef.value?.filter(val)
})

const filterNode = (value: string, data: any) => {
  if (!value) return true
  return data.channelName?.includes(value) || data.channelId?.includes(value)
}

const loadCameras = async () => {
  const res = await getVideoCameraPage({ pageNo: 1, pageSize: 1000 })
  const list = res.data?.list || []
  // 按 deptId 分组构建树，简化实现：先扁平展示
  cameraTree.value = list.map((c: any) => ({
    ...c,
    channelName: c.channelName || c.channelId
  }))
}

const handleTreeNodeClick = (node: any) => {
  if (!node.id) return
  const idx = activeScreen.value - 1
  screenCameras.value[idx] = node
  loadScreenVideo(idx)
}

const loadScreenVideo = async (idx: number) => {
  const camera = screenCameras.value[idx]
  if (!camera) return
  screenLoading.value[idx] = true
  screenErrors.value[idx] = null
  screenUrls.value[idx] = null
  try {
    const res = await getLiveUrl(camera.id)
    const url = res.data?.url
    if (url) {
      screenUrls.value[idx] = url
    } else {
      screenErrors.value[idx] = '未获取到视频流地址'
    }
  } catch (e: any) {
    screenErrors.value[idx] = e.message || '获取视频流失败'
  } finally {
    screenLoading.value[idx] = false
  }
}

onMounted(() => {
  loadCameras()
  // 初始化 9 个屏幕的数组
  for (let i = 0; i < 9; i++) {
    screenCameras.value.push(null)
    screenUrls.value.push(null)
    screenLoading.value.push(false)
    screenErrors.value.push(null)
  }
})
</script>

<style scoped>
.video-monitor-container {
  display: flex;
  height: calc(100vh - 160px);
  gap: 16px;
}
.video-sidebar {
  width: 260px;
  border-right: 1px solid #e4e7ed;
  padding-right: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.video-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.layout-toolbar {
  margin-bottom: 12px;
}
.screen-grid {
  flex: 1;
  display: grid;
  gap: 8px;
}
.grid-1 {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}
.grid-4 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
}
.grid-9 {
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr;
}
.screen-cell {
  background: #000;
  border: 2px solid transparent;
  cursor: pointer;
  position: relative;
}
.screen-cell.active {
  border-color: var(--el-color-primary);
}
.screen-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 14px;
}
</style>
```

---

### Task 10: 前端报警联动 — 火警处置页接入视频弹窗

**Files:**
- Modify: `anwei-fire-remote-monitor-vue/src/views/eiot/firewatch/alarm-order/index.vue`

- [ ] **Step 1: 导入依赖并增加联动逻辑**

在 `<script setup>` 顶部导入：

```typescript
import { getCamerasByDevice } from '@/api/eiot/video/deviceCameraRelation.api'
import { getLiveUrl } from '@/api/eiot/video/videoCamera.api'
import VideoPlayer from '@/views/eiot/video/components/VideoPlayer.vue'
```

- [ ] **Step 2: 替换"现场视频"占位区域**

找到右侧面板中 `video-placeholder` 区域，改为：

```vue
          <div class="video-container">
            <div v-if="linkedCameras.length === 0" class="video-placeholder">
              <el-icon :size="48"><VideoCamera /></el-icon>
              <p>该报警部件未绑定摄像头</p>
            </div>
            <div v-else-if="!currentVideoUrl" class="video-placeholder" @click="loadAlarmVideo">
              <el-icon :size="48"><VideoPlay /></el-icon>
              <p>点击播放关联摄像头</p>
            </div>
            <VideoPlayer
              v-else
              :video-url="currentVideoUrl"
              :loading="videoLoading"
              :error="videoError"
              @retry="loadAlarmVideo"
            />
          </div>
```

- [ ] **Step 3: 增加联动数据和方法**

在 script 中增加：

```typescript
const linkedCameras = ref<any[]>([])
const currentVideoUrl = ref<string | null>(null)
const videoLoading = ref(false)
const videoError = ref<string | null>(null)

// 在选择报警工单时加载关联摄像头
const onSelectAlarm = async (row: any) => {
  selectedAlarm.value = row
  currentVideoUrl.value = null
  videoError.value = null
  if (row.deviceId) {
    try {
      const res = await getCamerasByDevice(row.deviceId)
      linkedCameras.value = res.data || []
    } catch (e) {
      linkedCameras.value = []
    }
  } else {
    linkedCameras.value = []
  }
}

const loadAlarmVideo = async () => {
  if (linkedCameras.value.length === 0) return
  videoLoading.value = true
  videoError.value = null
  try {
    const res = await getLiveUrl(linkedCameras.value[0].id)
    currentVideoUrl.value = res.data?.url || null
    if (!currentVideoUrl.value) {
      videoError.value = '未获取到视频流地址'
    }
  } catch (e: any) {
    videoError.value = e.message || '获取视频流失败'
  } finally {
    videoLoading.value = false
  }
}
```

---

## 计划自检

### Spec 覆盖检查

| 设计文档要求 | 对应 Task |
|-------------|----------|
| `video_camera` 表增加 `device_id` | Task 1 |
| `VideoCameraDO` 增加 `deviceId` | Task 2 |
| 新增/修改 API 增加 `deviceId` | Task 3, 4 |
| `IccOpenApiClient.startVideo` 传入 `deviceId` | Task 5 |
| 新增 `GET /icc-devices` | Task 4, 5 |
| 前端 channel_id 中文输入提示 | Task 7 |
| 前端摄像头管理页 ICC 设备选择 | Task 7 |
| 视频管理页树形+1/4/9分屏 | Task 9 |
| 报警联动视频弹窗 | Task 10 |

### Placeholder 扫描

- `GetDeviceList` API 路径需用户确认 ICC 实际接口（已在 Task 5 标注）。

### 类型一致性

- `deviceId` 在后端为 `String`，在前端表单为 `string`。
- `cameraId` 在后端 `live/url` 接口为 `Long`，前端传 `number`。

---

## 执行选项

**Plan saved to `docs/superpowers/plans/2026-04-27-video-surveillance-phase1.md`.**

两个执行选项：

**1. Subagent-Driven（推荐）** — 每个 Task 派发独立子代理执行，我在每个 Task 完成后审查代码质量，迭代快

**2. Inline Execution** — 在当前会话中按顺序执行所有 Task，批量处理，中间有检查点供你审查

**选择哪种方式执行？**
