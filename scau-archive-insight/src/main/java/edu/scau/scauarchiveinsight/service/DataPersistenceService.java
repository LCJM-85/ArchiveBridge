package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.ClassDimMapper;
import edu.scau.scauarchiveinsight.mapper.DegreeDimMapper;
import edu.scau.scauarchiveinsight.mapper.DestinationDimMapper;
import edu.scau.scauarchiveinsight.mapper.GraduationFactMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import edu.scau.scauarchiveinsight.mapper.StudentFactMapper;
import edu.scau.scauarchiveinsight.pojo.AdmissionFact;
import edu.scau.scauarchiveinsight.pojo.ArchiveFileDim;
import edu.scau.scauarchiveinsight.pojo.ClassDim;
import edu.scau.scauarchiveinsight.pojo.DegreeDim;
import edu.scau.scauarchiveinsight.pojo.DestinationDim;
import edu.scau.scauarchiveinsight.pojo.GraduationFact;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import edu.scau.scauarchiveinsight.pojo.ProvinceDim;
import edu.scau.scauarchiveinsight.pojo.StudentFact;
import edu.scau.scauarchiveinsight.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    @Autowired
    private StudentFactMapper studentFactMapper;
    @Autowired
    private ClassDimMapper classDimMapper;
    @Autowired
    private GraduationFactMapper graduationFactMapper;
    @Autowired
    private DegreeDimMapper degreeDimMapper;
    @Autowired
    private DestinationDimMapper destinationDimMapper;

    /**
     * 持久化提取的结构化数据
     * @param archiveType admission 或 graduation
     * @param data 提取的键值对
     */
    public void saveExtractedData(String archiveType, Map<String, String> data, Integer fileId) {
        // 写入admission_fact
        if ("admission".equals(archiveType)) {
            AdmissionFact admissionFact = new AdmissionFact();

            // ========== 普通字符串字段：为空直接设null，不报错 ==========
            admissionFact.setStudentNo(data.get("student_no"));
            admissionFact.setExamNo(data.get("exam_no"));
            admissionFact.setName(data.get("name"));
            admissionFact.setIdCard(data.get("id_card"));
            admissionFact.setGender(data.get("gender"));

            // ========== 省份：为空就不查库，直接跳过 ==========
            String provinceName = data.get("province_name");
            if (provinceName != null && !provinceName.isBlank()) {
                ProvinceDim province = provinceDimMapper.selectOne(
                        Wrappers.<ProvinceDim>lambdaQuery().eq(ProvinceDim::getProvinceName, provinceName));
                if (province != null) {
                    admissionFact.setProvinceId(province.getProvinceId());
                }
            }

            // ========== 专业：为空就不查库，直接跳过 ==========
            String majorName = data.get("major_name");
            if (majorName != null && !majorName.isBlank()) {
                MajorDim major = majorDimMapper.selectOne(
                        Wrappers.<MajorDim>lambdaQuery().eq(MajorDim::getMajorName, majorName));
                if (major != null) {
                    admissionFact.setMajorId(major.getMajorId());
                }
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


            //同时写入 student_fact
            StudentFact studentFact = new StudentFact();
            studentFact.setStudentNo(data.get("student_no"));
            studentFact.setName(data.get("name"));
            studentFact.setIdCard(data.get("id_card"));
            studentFact.setGender(data.get("gender"));

            if (provinceName != null && !provinceName.isBlank()) {
                ProvinceDim province = provinceDimMapper.selectOne(
                        Wrappers.<ProvinceDim>lambdaQuery().eq(ProvinceDim::getProvinceName, provinceName));
                if (province != null) {
                    studentFact.setProvinceId(province.getProvinceId());
                }
            }

            if (majorName != null && !majorName.isBlank()) {
                MajorDim major = majorDimMapper.selectOne(
                        Wrappers.<MajorDim>lambdaQuery().eq(MajorDim::getMajorName, majorName));
                if (major != null) {
                    studentFact.setMajorId(major.getMajorId());
                }
            }

            String className = data.get("class_name");
            if (className != null && !className.isBlank()) {
                ClassDim classObj = classDimMapper.selectOne(
                        Wrappers.<ClassDim>lambdaQuery().eq(ClassDim::getClassName, className));
                if (classObj != null) {
                    studentFact.setClassId(classObj.getClassId());
                }
            }

            if (admissionDate != null) {
                studentFact.setAdmissionDate(admissionDate);
            }

            studentFactMapper.insert(studentFact);
        }
        else if ("graduation".equals(archiveType)) {
            GraduationFact graduationFact = new GraduationFact();
            graduationFact.setStudentNo(data.get("student_no"));
            graduationFact.setName(data.get("name"));
            graduationFact.setIdCard(data.get("id_card"));
            graduationFact.setGender(data.get("gender"));

            // 学历
            String degreeName = data.get("degree_name");
            if (degreeName != null && !degreeName.isBlank()) {
                DegreeDim degree = degreeDimMapper.selectOne(
                        Wrappers.<DegreeDim>lambdaQuery().eq(DegreeDim::getDegreeName, degreeName));
                if (degree != null) {
                    graduationFact.setDegreeId(degree.getDegreeId());
                }
            }

            // 去向
            String destName = data.get("dest_name");
            if (destName != null && !destName.isBlank()) {
                DestinationDim dest = destinationDimMapper.selectOne(
                        Wrappers.<DestinationDim>lambdaQuery().eq(DestinationDim::getDestName, destName));
                if (dest != null) {
                    graduationFact.setDestId(dest.getDestId());
                }
            }

            // 毕业日期
            String dateStr = data.get("graduation_date");
            LocalDate graduationDate = DateUtil.convertToLocalDate(dateStr);
            if (graduationDate != null) {
                graduationFact.setGraduationDate(graduationDate);
            }

            graduationFact.setFileId(fileId);

            graduationFactMapper.insert(graduationFact);
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
