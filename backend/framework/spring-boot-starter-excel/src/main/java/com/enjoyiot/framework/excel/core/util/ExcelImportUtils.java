package com.enjoyiot.framework.excel.core.util;

import com.enjoyiot.framework.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Excel 导入通用工具类
 *
 * @author sgd
 */
@Slf4j
public class ExcelImportUtils {

    /**
     * 导入结果封装
     */
    public static class ImportResult {
        private int successCount;
        private int failCount;
        private final List<Map<String, Object>> errors = new ArrayList<>();

        public void addError(int rowNum, String reason) {
            failCount++;
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("row", rowNum);
            error.put("reason", reason);
            errors.add(error);
        }

        public void addError(int rowNum, String name, String reason) {
            failCount++;
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("row", rowNum);
            error.put("name", name != null ? name : "");
            error.put("reason", reason);
            errors.add(error);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            if (!errors.isEmpty()) {
                result.put("failDetails", errors);
            }
            return result;
        }
    }

    /**
     * 行处理函数式接口
     *
     * @param <T> Excel 数据模型类型
     */
    @FunctionalInterface
    public interface RowProcessor<T> {
        /**
         * 处理单行数据
         *
         * @param data    当前行数据
         * @param rowNum  行号（从 2 开始，第 1 行是表头）
         * @param result  导入结果收集器
         */
        void process(T data, int rowNum, ImportResult result);
    }

    /**
     * 从 Excel 文件读取并逐行处理
     *
     * @param file      Excel 文件
     * @param clazz     数据模型 Class
     * @param processor 行处理器
     * @param logTag    日志标签（用于区分不同业务）
     * @return 导入结果
     */
    public static <T> Map<String, Object> processImport(
            MultipartFile file, Class<T> clazz, RowProcessor<T> processor, String logTag) {
        List<T> dataList;
        try {
            dataList = ExcelUtils.read(file, clazz);
        } catch (IOException e) {
            throw new ServiceException(500, "读取Excel文件失败，请检查文件格式");
        }
        return processImport(dataList, processor, logTag);
    }

    /**
     * 对已解析的数据列表逐行处理
     *
     * @param dataList  已解析的数据列表
     * @param processor 行处理器
     * @param logTag    日志标签
     * @return 导入结果
     */
    public static <T> Map<String, Object> processImport(
            List<T> dataList, RowProcessor<T> processor, String logTag) {
        if (dataList == null || dataList.isEmpty()) {
            throw new ServiceException(400, "导入数据为空");
        }

        ImportResult result = new ImportResult();

        for (int i = 0; i < dataList.size(); i++) {
            int rowNum = i + 2; // Excel 第1行是表头，数据从第2行开始
            T data = dataList.get(i);
            try {
                processor.process(data, rowNum, result);
                result.successCount++;
            } catch (Exception e) {
                result.addError(rowNum, e.getMessage());
                log.warn("[{}] 第{}行导入失败: {}", logTag, rowNum, e.getMessage());
            }
        }

        log.info("[{}] 导入完成，成功{}条，失败{}条", logTag, result.successCount, result.failCount);
        return result.toMap();
    }
}
