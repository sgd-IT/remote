import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 转义用户可见文案里的 HTML 特殊字符，防止 Excel 内容或异常信息带崩弹窗结构
 */
const escapeHtml = (value: any) =>
  String(value ?? '').replace(
    /[&<>"']/g,
    (char) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char] as string
  )

/**
 * 统一展示 Excel 导入结果：
 * - 全部成功：只弹一条轻提示
 * - 有失败行：弹窗按"第几行 + 原因"逐行列出，用户可对照 Excel 改完再导入
 *
 * 兼容两种响应来源：axios 封装返回的 { code, data } 完整报文，以及 el-upload 直传的原始响应体
 */
export const showImportResult = (res: any) => {
  const data = res?.data ?? res
  const successCount = data?.successCount ?? 0
  const failCount = data?.failCount ?? 0

  if (failCount <= 0) {
    ElMessage.success(`导入完成，成功 ${successCount} 条`)
    return
  }

  const details: any[] = Array.isArray(data?.failDetails) ? data.failDetails : []
  const rows = details
    .map((item) => {
      const name = item?.name ? `（${escapeHtml(item.name)}）` : ''
      return `<div>第 ${escapeHtml(item?.row ?? '?')} 行${name}：${escapeHtml(item?.reason || '未知原因')}</div>`
    })
    .join('')
  const allFailed = successCount <= 0
  const summary = `成功 <b>${successCount}</b> 条，失败 <b style="color: ${allFailed ? '#f56c6c' : '#e6a23c'}">${failCount}</b> 条，失败明细如下：`
  // 弹窗点 X / ESC 关闭时 ElMessageBox 会 reject，这里吞掉避免控制台出现未处理的 Promise
  ElMessageBox.alert(
    `<div style="max-height: 300px; overflow-y: auto; text-align: left; line-height: 1.9; font-size: 13px;"><div>${summary}</div>${rows}</div>`,
    '导入结果',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '知道了',
      type: allFailed ? 'error' : 'warning'
    }
  ).catch(() => {})
}
