package edu.scau.scauarchiveinsight.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DataPersistenceService {

    /**
     * 持久化提取的结构化数据
     * @param archiveType admission 或 graduation
     * @param data 提取的键值对
     */
    public void saveExtractedData(String archiveType, Map<String, String> data) {
        // 用户自行实现持久化逻辑
    }
}
