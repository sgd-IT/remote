<script lang="ts" setup>
import { CHANGELOG, CHANGE_TYPE_LABEL, type ChangelogItemType } from './data'

defineOptions({ name: 'SystemChangelog' })

const latest = CHANGELOG[0]

// 标签颜色区分变更性质，让使用者扫一眼就能过滤掉不关心的条目
const tagTypeMap: Record<ChangelogItemType, 'primary' | 'success' | 'warning'> = {
  feature: 'primary',
  improve: 'success',
  fix: 'warning'
}
</script>

<template>
  <div class="changelog-page">
    <!-- 顶部最新更新概览 -->
    <div class="changelog-hero">
      <div class="hero-badge">最新更新</div>
      <div class="hero-version">{{ latest?.version }}</div>
      <div class="hero-meta">
        <span>{{ latest?.date }} 发布</span>
        <span class="hero-dot"></span>
        <span>{{ latest?.title }}</span>
      </div>
    </div>

    <!-- 版本时间线：越新的版本越靠上 -->
    <ContentWrap v-for="version in CHANGELOG" :key="version.version" class="version-card">
      <div class="version-header">
        <div class="version-heading">
          <span class="version-tag">{{ version.version }}</span>
          <span class="version-title">{{ version.title }}</span>
          <el-tag v-if="version.version === latest?.version" size="small" effect="dark" round>
            最新
          </el-tag>
        </div>
        <span class="version-date">{{ version.date }}</span>
      </div>
      <ul class="version-items">
        <li v-for="(item, index) in version.items" :key="index" class="version-item">
          <el-tag :type="tagTypeMap[item.type]" size="small" effect="plain">
            {{ CHANGE_TYPE_LABEL[item.type] }}
          </el-tag>
          <span class="item-text">{{ item.text }}</span>
        </li>
      </ul>
    </ContentWrap>
  </div>
</template>

<style lang="scss" scoped>
.changelog-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 960px;
  margin: 0 auto;
}

.changelog-hero {
  padding: 28px 32px;
  color: #fff;
  background: linear-gradient(135deg, #1e39b1 0%, #3b6cf0 60%, #5a8dff 100%);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgb(30 57 177 / 25%);
}

.hero-badge {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  background: rgb(255 255 255 / 18%);
  border: 1px solid rgb(255 255 255 / 30%);
  border-radius: 999px;
}

.hero-version {
  margin-top: 12px;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 1px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 13px;
  color: rgb(255 255 255 / 85%);
}

.hero-dot {
  width: 4px;
  height: 4px;
  background: rgb(255 255 255 / 60%);
  border-radius: 50%;
}

.version-card {
  margin-bottom: 0 !important;
}

.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.version-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.version-tag {
  padding: 2px 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 6px;
}

.version-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.version-date {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.version-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0;
  margin: 14px 0 0;
  list-style: none;
}

.version-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.item-text {
  font-size: 14px;
  line-height: 22px;
  color: var(--el-text-color-regular);
}
</style>
