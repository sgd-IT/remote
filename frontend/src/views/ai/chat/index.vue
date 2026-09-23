<template>
  <div class="ai-chat-container">
    <!-- 头部 -->
    <div class="chat-header">
      <div class="header-left">
        <el-icon class="header-icon"><Monitor /></el-icon>
        <span class="header-title">AI 智能分析</span>
        <el-tag size="small" type="success" effect="dark" round>{{ modelName }}</el-tag>
      </div>
      <div class="header-right">
        <el-button text @click="handleClearChat">
          <el-icon><Delete /></el-icon>清空对话
        </el-button>
        <el-button text @click="goBack">
          <el-icon><Back /></el-icon>返回
        </el-button>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesRef">
      <!-- 欢迎提示 -->
      <div v-if="messages.length === 0" class="welcome-section">
        <div class="welcome-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#409eff" stroke-width="1.5">
            <path d="M12 2a7 7 0 0 1 7 7c0 3-2 5.5-4 7.5V20a2 2 0 0 1-2 2h-2a2 2 0 0 1-2-2v-3.5C7 14.5 5 12 5 9a7 7 0 0 1 7-7z"/>
            <path d="M10 22h4"/>
            <path d="M9 14h6"/>
          </svg>
        </div>
        <h3>AI 智能分析助手</h3>
        <p>我可以帮您分析消防设备状态、报警数据、隐患排查等，请输入您的问题</p>
        <div class="quick-questions">
          <div
            v-for="q in quickQuestions"
            :key="q"
            class="quick-question-item"
            @click="handleQuickQuestion(q)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ q }}</span>
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['message-row', msg.role]"
      >
        <div :class="['message-avatar', msg.role]">
          <el-icon v-if="msg.role === 'user'" :size="18"><User /></el-icon>
          <el-icon v-else :size="18"><Monitor /></el-icon>
        </div>
        <div :class="['message-bubble', msg.role]">
          <!-- 工具调用状态提示 -->
          <div v-if="msg.toolStatus" class="tool-call-status">
            <el-icon class="tool-icon is-loading"><Loading /></el-icon>
            <span>{{ msg.toolStatus }}</span>
          </div>
          <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
          <div v-if="msg.loading" class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-wrapper">
        <el-input
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入您的问题，按 Enter 发送，Shift+Enter 换行..."
          :disabled="sending"
          @keydown.enter.exact.prevent="handleSend"
          resize="none"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          circle
          :loading="sending"
          :disabled="!inputText.trim() || sending"
          @click="handleSend"
          class="send-btn"
        />
      </div>
      <div class="input-hint">
        基于 {{ modelName }} 大模型 · 回答仅供参考
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Monitor, Delete, Back, User, ChatDotRound, Promotion, Loading } from '@element-plus/icons-vue'
import { chatCompletions, type ChatMessage } from '@/api/ai/chat'

const router = useRouter()

const modelName = ref('Qwen3.5-35B')
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref<HTMLDivElement>()

interface DisplayMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  loading?: boolean
  toolStatus?: string
}

const messages = ref<DisplayMessage[]>([])

const quickQuestions = [
  '当前系统有多少设备处于离线状态？',
  '请分析最近24小时的报警趋势',
  '如何排查火灾自动报警系统的常见故障？',
  '请帮我制定巡检计划'
]

/** 系统提示词 */
const SYSTEM_PROMPT = `你是安为智慧消防远程值守系统的AI分析助手。你具备以下能力：
1. 分析消防设备运行状态、报警数据和趋势
2. 提供火灾自动报警系统的故障排查建议
3. 解读消防法规和标准要求
4. 辅助制定巡检计划和隐患整改方案
5. 分析消防值守数据并提供决策支持

请用中文回答，回答要专业、准确、有条理。如果用户的问题超出你的知识范围，请如实说明。`

/** 渲染简易 Markdown */
function renderMarkdown(text: string): string {
  if (!text) return ''
  return text
    // 代码块
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="lang-$1">$2</code></pre>')
    // 行内代码
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // 粗体
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    // 标题
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    // 无序列表
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    // 有序列表
    .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
    // 换行
    .replace(/\n/g, '<br/>')
}

/** 滚动到底部 */
function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

/** 快捷问题 */
function handleQuickQuestion(question: string) {
  inputText.value = question
  handleSend()
}

/** 发送消息 */
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  // 添加用户消息
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  // 添加 AI 占位消息
  const aiMsg: DisplayMessage = { role: 'assistant', content: '', loading: true, toolStatus: '正在分析...' }
  messages.value.push(aiMsg)
  scrollToBottom()

  sending.value = true

  try {
    // 构建消息历史
    const chatMessages: ChatMessage[] = [
      { role: 'system', content: SYSTEM_PROMPT },
      ...messages.value
        .filter(m => m.role !== 'system' && !m.loading)
        .map(m => ({ role: m.role as 'user' | 'assistant', content: m.content }))
    ]

    // 调用非流式接口（后端会自动执行工具调用循环）
    aiMsg.toolStatus = '正在查询系统数据...'
    scrollToBottom()

    const res = await chatCompletions({
      messages: chatMessages,
      stream: false,
      temperature: 0.7,
      maxTokens: 4096
    }) as any

    aiMsg.loading = false
    aiMsg.toolStatus = undefined

    if (res && res.content) {
      // 过滤掉工具调用标记，只保留最终文本
      let finalContent = res.content
      // 移除 [TOOL_CALL]...[/TOOL_CALL] 标记
      finalContent = finalContent.replace(/\[TOOL_CALL\][\s\S]*?\[\/TOOL_CALL\]/g, '').trim()
      aiMsg.content = finalContent || '已获取数据，但AI未生成有效回答。'
    } else {
      aiMsg.content = '抱歉，AI 服务暂时无法响应，请稍后重试。'
    }
  } catch (e: any) {
    aiMsg.loading = false
    aiMsg.toolStatus = undefined
    aiMsg.content = `请求失败: ${e.message || '未知错误'}。请检查网络连接后重试。`
    console.error('[AI Chat] error:', e)
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

/** 清空对话 */
function handleClearChat() {
  messages.value = []
}

/** 返回上一页 */
function goBack() {
  router.back()
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style lang="scss" scoped>
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
  font-family: 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* 头部 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: linear-gradient(135deg, #1a237e 0%, #283593 50%, #1565c0 100%);
  color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 10;

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .header-icon {
    font-size: 24px;
    color: #64b5f6;
  }

  .header-title {
    font-size: 18px;
    font-weight: 600;
    letter-spacing: 1px;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 4px;

    .el-button {
      color: rgba(255, 255, 255, 0.85);
      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.1);
      }
    }
  }
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

/* 欢迎区域 */
.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;

  .welcome-icon {
    margin-bottom: 20px;
    animation: float 3s ease-in-out infinite;
  }

  h3 {
    font-size: 22px;
    color: #1a237e;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
    margin-bottom: 32px;
  }

  .quick-questions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    max-width: 640px;
    width: 100%;
  }

  .quick-question-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 14px 18px;
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 12px;
    cursor: pointer;
    font-size: 13px;
    color: #606266;
    transition: all 0.2s;

    .el-icon {
      color: #409eff;
      flex-shrink: 0;
    }

    &:hover {
      border-color: #409eff;
      background: #ecf5ff;
      color: #409eff;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
    }
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

/* 消息行 */
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;

  &.user {
    flex-direction: row-reverse;
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.user {
    background: #409eff;
    color: #fff;
  }

  &.assistant {
    background: linear-gradient(135deg, #1a237e, #1565c0);
    color: #fff;
  }
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  max-width: 75%;
  word-break: break-word;

  &.user {
    background: #409eff;
    color: #fff;
    border-top-right-radius: 4px;
  }

  &.assistant {
    background: #fff;
    color: #303133;
    border-top-left-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    :deep(h2), :deep(h3), :deep(h4) {
      margin: 8px 0 4px;
      color: #1a237e;
    }

    :deep(code) {
      background: #f5f7fa;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
      color: #e74c3c;
    }

    :deep(pre) {
      background: #1e1e1e;
      color: #d4d4d4;
      padding: 12px 16px;
      border-radius: 8px;
      overflow-x: auto;
      margin: 8px 0;

      code {
        background: none;
        color: inherit;
        padding: 0;
      }
    }

    :deep(li) {
      margin-left: 20px;
      margin-bottom: 4px;
    }

    :deep(details) {
      margin-bottom: 8px;

      summary {
        cursor: pointer;
        color: #909399;
        font-size: 12px;
      }
    }
  }
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  span {
    width: 8px;
    height: 8px;
    background: #409eff;
    border-radius: 50%;
    animation: typing 1.4s infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
  30% { opacity: 1; transform: translateY(-4px); }
}

/* 工具调用状态 */
.tool-call-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: linear-gradient(90deg, #ecf5ff, #f0f9eb);
  border-radius: 8px;
  font-size: 13px;
  color: #409eff;

  .tool-icon {
    font-size: 16px;
    animation: rotating 2s linear infinite;
  }
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 输入区域 */
.chat-input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #ebeef5;

  .input-wrapper {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    max-width: 800px;
    margin: 0 auto;
    background: #f5f7fa;
    border-radius: 12px;
    padding: 8px 12px;
    border: 2px solid transparent;
    transition: border-color 0.3s;

    &:focus-within {
      border-color: #409eff;
      background: #fff;
    }

    :deep(.el-textarea__inner) {
      background: transparent !important;
      box-shadow: none !important;
      border: none !important;
      padding: 4px 0;
      font-size: 14px;
      line-height: 1.6;
    }
  }

  .send-btn {
    flex-shrink: 0;
  }

  .input-hint {
    text-align: center;
    font-size: 12px;
    color: #c0c4cc;
    margin-top: 8px;
  }
}
</style>
