<template>
  <div class="ai-analysis-page">
    <header class="page-header">
      <div class="header-left">
        <span class="header-title">AI 智能分析</span>
        <el-tag size="small" type="success" effect="dark" round>{{ modelName }}</el-tag>
      </div>
      <div class="header-right">
        <el-button text class="header-action-btn" @click="handleClearChat">
          <el-icon><Delete /></el-icon>清空对话
        </el-button>
        <el-tooltip content="退出" placement="bottom">
          <div class="header-close-btn" @click="goBack">
            <el-icon :size="18"><Close /></el-icon>
          </div>
        </el-tooltip>
      </div>
    </header>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesRef">
      <!-- 欢迎提示 -->
      <div v-if="messages.length === 0" class="welcome-section">
        <div class="welcome-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="rgba(79,195,247,0.8)" stroke-width="1.5">
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
import { Monitor, Delete, Close, User, ChatDotRound, Promotion, Loading } from '@element-plus/icons-vue'
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

const SYSTEM_PROMPT = `你是安为智慧消防远程值守系统的AI分析助手。你具备以下能力：
1. 分析消防设备运行状态、报警数据和趋势
2. 提供火灾自动报警系统的故障排查建议
3. 解读消防法规和标准要求
4. 辅助制定巡检计划和隐患整改方案
5. 分析消防值守数据并提供决策支持

请用中文回答，回答要专业、准确、有条理。如果用户的问题超出你的知识范围，请如实说明。`

function renderMarkdown(text: string): string {
  if (!text) return ''
  return text
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="lang-$1">$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br/>')
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function handleQuickQuestion(question: string) {
  inputText.value = question
  handleSend()
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  scrollToBottom()

  const aiMsg: DisplayMessage = { role: 'assistant', content: '', loading: true, toolStatus: '正在分析...' }
  messages.value.push(aiMsg)
  scrollToBottom()

  sending.value = true

  try {
    const chatMessages: ChatMessage[] = [
      { role: 'system', content: SYSTEM_PROMPT },
      ...messages.value
        .filter(m => m.role !== 'system' && !m.loading)
        .map(m => ({ role: m.role as 'user' | 'assistant', content: m.content }))
    ]

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
      let finalContent = res.content
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

function handleClearChat() {
  messages.value = []
}

// 退出：优先返回进入来源页面，无历史记录时兜底跳转值守大屏
const goBack = () => {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push('/duty-screen')
  }
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style lang="scss" scoped>
$bg1: #0a1628;
$bg2: #0d1f3c;
$bg3: #0f2747;
$bd: #1e3a5f;
$accent: rgba(79,195,247,0.9);
$accent-dim: rgba(79,195,247,0.5);
$slate: #8b9dc5;
$blue: #1890ff;
$red: #ff4d4f;
$green: #52c41a;

.ai-analysis-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $bg1;
  font-family: 'Microsoft YaHei', sans-serif;
  overflow: hidden;
  color: #e8f0fe;
}

/* 头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 52px;
  flex-shrink: 0;
  background: linear-gradient(170deg, rgba(14,44,78,0.70) 0%, rgba(10,36,66,0.65) 35%, rgba(8,30,56,0.70) 100%);
  border-bottom: 1px solid rgba(79,195,247,0.1);
  box-shadow: 0 2px 10px rgba(0,0,0,0.18), inset 0 1px 0 rgba(79,195,247,0.04);

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .header-title {
    font-size: 18px;
    font-weight: bold;
    background: linear-gradient(90deg, #fff, #7ec8f8, #54a0ff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: 2px;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;

    .header-action-btn {
      color: $slate;
      &:hover {
        color: #fff;
        background: rgba(79,195,247,0.08);
      }
    }
  }
}

.header-close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  color: $slate;
  cursor: pointer;
  border: 1px solid rgba(79,195,247,0.12);
  transition: all 0.3s;
  &:hover {
    background: rgba(255, 77, 79, 0.15);
    border-color: rgba(255, 77, 79, 0.4);
    color: #ff4d4f;
  }
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: $bd; border-radius: 2px; }
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
    color: #fff;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: $slate;
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
    background: rgba(14,44,78,0.6);
    border: 1px solid rgba(79,195,247,0.12);
    border-radius: 10px;
    cursor: pointer;
    font-size: 13px;
    color: $slate;
    transition: all 0.2s;

    .el-icon {
      color: $accent;
      flex-shrink: 0;
    }

    &:hover {
      border-color: rgba(79,195,247,0.35);
      background: rgba(24,144,255,0.08);
      color: #fff;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(79,195,247,0.1);
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
    background: $blue;
    color: #fff;
  }

  &.assistant {
    background: linear-gradient(135deg, #0d47a1, #1565c0);
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
    background: $blue;
    color: #fff;
    border-top-right-radius: 4px;
  }

  &.assistant {
    background: rgba(14,44,78,0.8);
    color: #e8f0fe;
    border-top-left-radius: 4px;
    border: 1px solid rgba(79,195,247,0.08);
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);

    :deep(h2), :deep(h3), :deep(h4) {
      margin: 8px 0 4px;
      color: $accent;
    }

    :deep(code) {
      background: rgba(0,0,0,0.3);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
      color: #ff7875;
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

    :deep(strong) {
      color: #fff;
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
    background: $accent;
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
  background: rgba(79,195,247,0.08);
  border: 1px solid rgba(79,195,247,0.15);
  border-radius: 8px;
  font-size: 13px;
  color: $accent;

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
  background: rgba(13,31,60,0.6);
  border-top: 1px solid rgba(79,195,247,0.08);
  flex-shrink: 0;

  .input-wrapper {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    max-width: 800px;
    margin: 0 auto;
    background: rgba(10,22,40,0.8);
    border-radius: 12px;
    padding: 8px 12px;
    border: 1px solid rgba(79,195,247,0.1);
    transition: border-color 0.3s;

    &:focus-within {
      border-color: rgba(79,195,247,0.35);
      background: rgba(10,22,40,1);
    }

    :deep(.el-textarea__inner) {
      background: transparent !important;
      box-shadow: none !important;
      border: none !important;
      padding: 4px 0;
      font-size: 14px;
      line-height: 1.6;
      color: #e8f0fe;

      &::placeholder {
        color: rgba(139,157,197,0.5);
      }
    }
  }

  .send-btn {
    flex-shrink: 0;
  }

  .input-hint {
    text-align: center;
    font-size: 12px;
    color: rgba(139,157,197,0.5);
    margin-top: 8px;
  }
}
</style>
