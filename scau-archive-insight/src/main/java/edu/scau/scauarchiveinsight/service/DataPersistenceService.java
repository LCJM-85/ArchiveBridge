package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.pojo.ArchiveFileDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DataPersistenceService {

    @Autowired
    private ArchiveFileDimMapper archiveFileDimMapper;
    @Autowired
    private ProvinceDimMapper provinceDimMapper;
    @Autowired
    private MajorDimMapper majorDimMapper;
    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    /**
     * 持久化提取的结构化数据
     * @param archiveType admission 或 graduation
     * @param data 提取的键值对
     */
    public void saveExtractedData(String archiveType, Map<String, String> data, Integer fileId) {
        // 1. 防止 archiveType 为null 导致空指针
        if ("admission".equals(archiveType)) {
            AdmissionFact admissionFact = new AdmissionFact();

            // ========== 普通字符串字段：为空直接设null，不报错 ==========
            admissionFact.setStudentNo(data.get("student_no"));
            admissionFact.setExamNo(data.get("exam_no"));
            admissionFact.setName(data.get("name"));
            admissionFact.setIdCard(data.get("id_card"));

            // ========== 省份：为空就不查库，直接跳过 ==========
            String provinceName = data.get("province_name");
            if (provinceName != null && !provinceName.isBlank()) {
                String provinceId = provinceDimMapper.selectIdByName(provinceName);
                admissionFact.setProvinceId(provinceId);
            }

            // ========== 专业：为空就不查库，直接跳过 ==========
            String majorName = data.get("major_name");
            if (majorName != null && !majorName.isBlank()) {
                String majorId = majorDimMapper.selectIdByName(majorName);
                admissionFact.setMajorId(majorId);
            }

            String dateStr = data.get("admission_date");
            LocalDate admissionDate = DateUtil.convertToLocalDate(dateStr);
            //日期转换成功就塞入
            if (admissionDate != null) {
                admissionFact.setAdmissionDate(admissionDate);
            }

            // 文件ID
            admissionFact.setFileId(fileId);

            // 插入
            admissionFactMapper.insert(admissionFact);
        }
        else if archiveType.equals("graduation") {

        }

    }

    public int saveArchiveFileDimData(String fileName, String fileType) {
        ArchiveFileDim archiveFileDim = new ArchiveFileDim();
        archiveFileDim.setFileName(fileName);
        archiveFileDim.setFileType(fileType);
        archiveFileDim.setUploadTime(LocalDateTime.now());
        archiveFileDimMapper.insert(archiveFileDim);
        int id = archiveFileDim.getFileId();
        return id;
    }
}
