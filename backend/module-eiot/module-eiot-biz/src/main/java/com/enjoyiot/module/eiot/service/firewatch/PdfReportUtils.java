package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 报表渲染工具类（报警记录导出 / 智能消防月度报告共用）
 * <p>
 * 提供中文字体加载、表格绘制、文本绘制、柱状图与留白横线等静态方法。
 *
 * @author EnjoyIot
 */
@Slf4j
public final class PdfReportUtils {

    private PdfReportUtils() {
    }

    /** jar 内随包字体（Noto Sans SC，含完整拉丁+中文）：跟着 jar 走，容器/精简系统等没有系统字体的环境靠它兜底 */
    private static final String BUNDLED_FONT_RESOURCE = "/fonts/NotoSansSC-Regular.otf";

    /**
     * 加载中文字体（优先 jar 内随包字体，miss 再回退系统字体；系统字体支持 TTC/OTF/TTF 格式）
     */
    public static PDType0Font loadChineseFont(PDDocument document) throws IOException {
        // 随包字体优先：线上容器不自带中文字体，原来只查系统路径会让整个导出直接失败（本地 Windows 有 simsun 反而发现不了）；
        // 随包字体加载异常时继续往下走系统路径兜底
        try (InputStream bundled = PdfReportUtils.class.getResourceAsStream(BUNDLED_FONT_RESOURCE)) {
            if (bundled != null) {
                return PDType0Font.load(document, bundled, true);
            }
        } catch (IOException e) {
            log.warn("[PDF导出] 随包字体加载失败，回退系统字体: {}", e.getMessage());
        }

        String[] fontPaths = {
                // Windows 常见中文字体（多为 TTC 格式）
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/msyhbd.ttc",
                // Linux 常见中文字体
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttf",
                // macOS 常见中文字体
                "/System/Library/Fonts/PingFang.ttc",
                "/System/Library/Fonts/STHeiti Light.ttc",
                "/System/Library/Fonts/STHeiti Medium.ttc",
        };

        for (String fontPath : fontPaths) {
            File fontFile = new File(fontPath);
            if (!fontFile.exists() || !fontFile.isFile()) {
                continue;
            }

            try {
                if (fontPath.toLowerCase().endsWith(".ttc")) {
                    // TTC (TrueType Collection): 提取嵌入字体
                    return loadFontFromTtc(document, fontFile);
                } else {
                    // TTF/OTF: 直接通过流加载
                    try (InputStream is = new FileInputStream(fontFile)) {
                        return PDType0Font.load(document, is, true);
                    }
                }
            } catch (IOException e) {
                log.warn("[PDF导出] 字体加载失败: {}, 原因: {}", fontPath, e.getMessage());
            }
        }

        throw new IOException("未找到可用的中文字体（jar 内随包字体与系统字体均不可用），请检查打包资源是否完整");
    }

    /**
     * 从 TTC (TrueType Collection) 文件中提取并加载字体
     */
    private static PDType0Font loadFontFromTtc(PDDocument document, File ttcFile) throws IOException {
        TrueTypeCollection ttc = new TrueTypeCollection(ttcFile);
        try {
            final TrueTypeFont[] firstFont = new TrueTypeFont[1];
            ttc.processAllFonts(font -> {
                if (firstFont[0] == null) {
                    firstFont[0] = font;
                }
            });
            if (firstFont[0] == null) {
                throw new IOException("TTC文件中未找到字体: " + ttcFile.getAbsolutePath());
            }
            // 第二个参数 true 表示将字体嵌入PDF，确保跨平台可移植性
            return PDType0Font.load(document, firstFont[0], true);
        } finally {
            ttc.close();
        }
    }

    /**
     * 绘制PDF表格表头
     */
    public static void drawPdfTableHeader(PDPageContentStream cs, PDType0Font font, String[] headers,
                                          float[] widths, float x, float y, float rowHeight) throws IOException {
        // 背景色
        cs.setNonStrokingColor(new Color(0.85f, 0.85f, 0.85f));
        float startX = x;
        for (float width : widths) {
            cs.addRect(startX, y - rowHeight, width, rowHeight);
            cs.fill();
            startX += width;
        }
        cs.setNonStrokingColor(Color.BLACK);

        // 表头文字
        cs.setFont(font, 7.5f);
        startX = x;
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(startX + 3, y - rowHeight + 4);
            cs.showText(headers[i]);
            cs.endText();
            startX += widths[i];
        }

        // 边框
        drawPdfTableBorders(cs, widths, x, y, rowHeight);
    }

    /**
     * 绘制PDF表格数据行
     */
    public static void drawPdfTableRow(PDPageContentStream cs, PDType0Font font, String[] values,
                                       float[] widths, float x, float y, float rowHeight,
                                       boolean isEven) throws IOException {
        // 交替行背景
        if (isEven) {
            cs.setNonStrokingColor(new Color(0.95f, 0.95f, 0.97f));
            float startX = x;
            for (float w : widths) {
                cs.addRect(startX, y - rowHeight, w, rowHeight);
                cs.fill();
                startX += w;
            }
            cs.setNonStrokingColor(Color.BLACK);
        }

        // 数据文字
        cs.setFont(font, 7f);
        float startX = x;
        for (int i = 0; i < widths.length; i++) {
            String text = i < values.length ? values[i] : "";
            // 截断过长的文本
            String displayText = truncatePdfText(text, widths[i] - 6, font, 7f);
            cs.beginText();
            cs.newLineAtOffset(startX + 3, y - rowHeight + 3);
            cs.showText(displayText);
            cs.endText();
            startX += widths[i];
        }

        // 边框
        drawPdfTableBorders(cs, widths, x, y, rowHeight);
    }

    /**
     * 绘制PDF表格边框
     */
    private static void drawPdfTableBorders(PDPageContentStream cs, float[] widths,
                                            float x, float y, float rowHeight) throws IOException {
        float totalW = 0;
        for (float w : widths) totalW += w;
        // 下边线
        cs.moveTo(x, y - rowHeight);
        cs.lineTo(x + totalW, y - rowHeight);
        cs.stroke();
    }

    /**
     * 截断PDF文本使其不超过指定宽度
     */
    public static String truncatePdfText(String text, float maxWidth, PDType0Font font, float fontSize) throws IOException {
        if (StrUtil.isBlank(text)) return "";
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        if (textWidth <= maxWidth) return text;
        for (int i = text.length(); i > 0; i--) {
            String sub = text.substring(0, i);
            float w = font.getStringWidth(sub) / 1000f * fontSize;
            if (w <= maxWidth - 5) {
                return sub + "..";
            }
        }
        return "";
    }

    /**
     * 在指定坐标绘制一行文本
     */
    public static void showText(PDPageContentStream cs, PDType0Font font, float fontSize,
                                String text, float x, float y) throws IOException {
        if (StrUtil.isBlank(text)) {
            return;
        }
        cs.setFont(font, fontSize);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /**
     * 计算文本渲染宽度
     */
    public static float textWidth(PDType0Font font, String text, float fontSize) throws IOException {
        if (StrUtil.isBlank(text)) {
            return 0f;
        }
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    /**
     * 绘制小节标题（带下划线装饰）
     */
    public static void drawSectionTitle(PDPageContentStream cs, PDType0Font font,
                                        String title, float x, float y) throws IOException {
        showText(cs, font, 11, title, x, y);
        cs.setStrokingColor(new Color(0.2f, 0.45f, 0.85f));
        cs.setLineWidth(1.2f);
        cs.moveTo(x, y - 4);
        cs.lineTo(x + textWidth(font, title, 11) + 4, y - 4);
        cs.stroke();
        cs.setLineWidth(1f);
        cs.setStrokingColor(Color.BLACK);
    }

    /**
     * 绘制留白横线区（用于人工填写建议/计划）
     */
    public static void drawBlankLines(PDPageContentStream cs, float x, float startY,
                                      float width, int lineCount, float lineGap) throws IOException {
        cs.setStrokingColor(new Color(0.75f, 0.75f, 0.75f));
        for (int i = 0; i < lineCount; i++) {
            float lineY = startY - i * lineGap;
            cs.moveTo(x, lineY);
            cs.lineTo(x + width, lineY);
            cs.stroke();
        }
        cs.setStrokingColor(Color.BLACK);
    }

    /**
     * 绘制网格单元格边框与可选底色（全网格表格式版面使用）
     */
    public static void drawGridCellRect(PDPageContentStream cs, float x, float y,
                                        float width, float height, Color bg) throws IOException {
        if (bg != null) {
            cs.setNonStrokingColor(bg);
            cs.addRect(x, y - height, width, height);
            cs.fill();
            cs.setNonStrokingColor(Color.BLACK);
        }
        cs.addRect(x, y - height, width, height);
        cs.stroke();
    }

    /**
     * 绘制网格单元格（全边框 + 可选底色 + 单行文本，垂直居中；center=true 时水平居中）
     */
    public static void drawGridCell(PDPageContentStream cs, PDType0Font font, String text,
                                    float x, float y, float width, float height,
                                    Color bg, float fontSize, boolean center) throws IOException {
        drawGridCellRect(cs, x, y, width, height, bg);
        if (StrUtil.isBlank(text)) {
            return;
        }
        String displayText = truncatePdfText(text, width - 8, font, fontSize);
        float textW = font.getStringWidth(displayText) / 1000f * fontSize;
        float textX = center ? x + (width - textW) / 2 : x + 4;
        float textY = y - height / 2 - fontSize * 0.35f;
        showText(cs, font, fontSize, displayText, textX, textY);
    }

    /**
     * 将文本按渲染宽度折行（按换行符分段后逐字测量宽度，支持中英文混排）
     *
     * @return 折行后的行列表（空行已过滤）
     */
    public static List<String> wrapText(PDType0Font font, String text, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return lines;
        }
        for (String paragraph : text.split("\n")) {
            String content = paragraph.trim();
            if (content.isEmpty()) {
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (current.length() > 0
                        && font.getStringWidth(current.toString() + c) / 1000f * fontSize > maxWidth) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                current.append(c);
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    /**
     * 绘制双色柱状图（按天对比告警数与已处置数）
     *
     * @param labels      每天一个标签（如日期）
     * @param alarmValues 每天的告警数
     * @param handleValues 每天的已处置数
     * @param baselineY   柱状图基准线 Y 坐标
     * @param maxBarHeight 最高柱的高度
     */
    public static void drawBarChart(PDPageContentStream cs, PDType0Font font,
                                    String[] labels, int[] alarmValues, int[] handleValues,
                                    float x, float baselineY, float totalWidth, float maxBarHeight) throws IOException {
        int n = labels.length;
        if (n == 0) {
            return;
        }
        int max = 1;
        for (int i = 0; i < n; i++) {
            max = Math.max(max, Math.max(alarmValues[i], handleValues[i]));
        }
        Color alarmColor = new Color(0.86f, 0.30f, 0.24f);
        Color handleColor = new Color(0.18f, 0.65f, 0.35f);
        float slot = totalWidth / n;
        float barWidth = Math.max(1.5f, Math.min(5f, slot / 3.2f));

        // 基准线
        cs.setStrokingColor(new Color(0.6f, 0.6f, 0.6f));
        cs.moveTo(x, baselineY);
        cs.lineTo(x + totalWidth, baselineY);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);

        for (int i = 0; i < n; i++) {
            float slotX = x + i * slot;
            float barH1 = alarmValues[i] * 1f / max * maxBarHeight;
            float barH2 = handleValues[i] * 1f / max * maxBarHeight;
            float baseX = slotX + slot / 2 - barWidth - 0.5f;
            if (barH1 > 0.5f) {
                cs.setNonStrokingColor(alarmColor);
                cs.addRect(baseX, baselineY, barWidth, barH1);
                cs.fill();
            }
            if (barH2 > 0.5f) {
                cs.setNonStrokingColor(handleColor);
                cs.addRect(baseX + barWidth + 1f, baselineY, barWidth, barH2);
                cs.fill();
            }
            cs.setNonStrokingColor(Color.BLACK);

            // 数值标注
            int value = Math.max(alarmValues[i], handleValues[i]);
            if (value > 0) {
                String valueText = String.valueOf(value);
                float vw = textWidth(font, valueText, 5.5f);
                showText(cs, font, 5.5f, valueText, slotX + slot / 2 - vw / 2,
                        baselineY + Math.max(barH1, barH2) + 2.5f);
            }
            // 日期标签（每 3 天标一个，避免重叠）
            if (i % 3 == 0 || i == n - 1) {
                String label = labels[i];
                float lw = textWidth(font, label, 5.5f);
                showText(cs, font, 5.5f, label, slotX + slot / 2 - lw / 2, baselineY - 9);
            }
        }

        // 图例（右上角）
        float legendY = baselineY + maxBarHeight + 12;
        cs.setNonStrokingColor(alarmColor);
        cs.addRect(x + totalWidth - 118, legendY, 7, 7);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
        showText(cs, font, 7, "告警数", x + totalWidth - 108, legendY);
        cs.setNonStrokingColor(handleColor);
        cs.addRect(x + totalWidth - 62, legendY, 7, 7);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
        showText(cs, font, 7, "已处置数", x + totalWidth - 52, legendY);
    }
}
