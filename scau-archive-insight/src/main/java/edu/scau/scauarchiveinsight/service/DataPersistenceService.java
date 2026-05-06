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
import edu.scau.scauarchiveinsight.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(DataPersistenceService.class);

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
        if ("admission".equals(archiveType)) {
            saveAdmissionData(data, fileId);
        } else if ("graduation".equals(archiveType)) {
            saveGraduationData(data, fileId);
        }
    }

    // ===================== 招生档案：admission_fact + student_fact 去重持久化 =====================

    private void saveAdmissionData(Map<String, String> data, Integer fileId) {
        // 维度字段提前查出，student_fact 和 admission_fact 共用
        String provinceName = data.get("province_name");
        ProvinceDim province = fuzzyLookupProvince(provinceName);

        String majorName = data.get("major_name");
        MajorDim major = fuzzyLookupMajor(majorName);

        String className = data.get("class_name");
        ClassDim classObj = fuzzyLookupClass(className);

        String dateStr = data.get("admission_date");
        LocalDate admissionDate = DateUtil.convertToLocalDate(dateStr);

        LocalDateTime now = LocalDateTime.now();

        // ========== student_fact：去重匹配 student_no → id_card ==========
        StudentFact studentFact = buildStudentFact(data, province, major, classObj, admissionDate, fileId);
        StudentFact existingStudent = null;

        String studentNo = data.get("student_no");
        if (studentNo != null && !studentNo.isBlank()) {
            existingStudent = studentFactMapper.selectOne(
                    Wrappers.<StudentFact>lambdaQuery().eq(StudentFact::getStudentNo, studentNo));
        }
        if (existingStudent == null) {
            String idCard = data.get("id_card");
            if (idCard != null && !idCard.isBlank()) {
                existingStudent = studentFactMapper.selectOne(
                        Wrappers.<StudentFact>lambdaQuery().eq(StudentFact::getIdCard, idCard));
            }
        }

        if (existingStudent != null) {
            // 更新已有记录
            studentFact.setId(existingStudent.getId());
            studentFact.setCreateTime(existingStudent.getCreateTime());
            studentFact.setUpdateTime(now);
            studentFactMapper.updateById(studentFact);
        } else {
            studentFact.setCreateTime(now);
            studentFact.setUpdateTime(now);
            studentFactMapper.insert(studentFact);
        }

        // ========== admission_fact：去重匹配 student_no → id_card → exam_no ==========
        AdmissionFact admissionFact = buildAdmissionFact(data, province, major, admissionDate, fileId);

        AdmissionFact existingAdmission = null;
        if (studentNo != null && !studentNo.isBlank()) {
            existingAdmission = admissionFactMapper.selectOne(
                    Wrappers.<AdmissionFact>lambdaQuery().eq(AdmissionFact::getStudentNo, studentNo));
        }
        if (existingAdmission == null) {
            String idCard = data.get("id_card");
            if (idCard != null && !idCard.isBlank()) {
                existingAdmission = admissionFactMapper.selectOne(
                        Wrappers.<AdmissionFact>lambdaQuery().eq(AdmissionFact::getIdCard, idCard));
            }
        }
        if (existingAdmission == null) {
            String examNo = data.get("exam_no");
            if (examNo != null && !examNo.isBlank()) {
                existingAdmission = admissionFactMapper.selectOne(
                        Wrappers.<AdmissionFact>lambdaQuery().eq(AdmissionFact::getExamNo, examNo));
            }
        }

        if (existingAdmission != null) {
            admissionFact.setId(existingAdmission.getId());
            admissionFact.setCreateTime(existingAdmission.getCreateTime());
            admissionFact.setUpdateTime(now);
            admissionFactMapper.updateById(admissionFact);
        } else {
            admissionFact.setCreateTime(now);
            admissionFact.setUpdateTime(now);
            admissionFactMapper.insert(admissionFact);
        }
    }

    // ===================== 毕业档案：graduation_fact 去重持久化 =====================

    private void saveGraduationData(Map<String, String> data, Integer fileId) {
        String degreeName = data.get("degree_name");
        DegreeDim degree = fuzzyLookupDegree(degreeName);

        String destName = data.get("dest_name");
        DestinationDim dest = fuzzyLookupDestination(destName);

        String dateStr = data.get("graduation_date");
        LocalDate graduationDate = DateUtil.convertToLocalDate(dateStr);

        LocalDateTime now = LocalDateTime.now();

        GraduationFact graduationFact = new GraduationFact();
        graduationFact.setStudentNo(data.get("student_no"));
        graduationFact.setName(data.get("name"));
        graduationFact.setIdCard(data.get("id_card"));
        graduationFact.setGender(data.get("gender"));
        if (degree != null) {
            graduationFact.setDegreeId(degree.getDegreeId());
        }
        if (dest != null) {
            graduationFact.setDestId(dest.getDestId());
        }
        if (graduationDate != null) {
            graduationFact.setGraduationDate(graduationDate);
        }
        graduationFact.setFileId(fileId);

        // 去重匹配：student_no → id_card
        GraduationFact existing = null;
        String studentNo = data.get("student_no");
        if (studentNo != null && !studentNo.isBlank()) {
            existing = graduationFactMapper.selectOne(
                    Wrappers.<GraduationFact>lambdaQuery().eq(GraduationFact::getStudentNo, studentNo));
        }
        if (existing == null) {
            String idCard = data.get("id_card");
            if (idCard != null && !idCard.isBlank()) {
                existing = graduationFactMapper.selectOne(
                        Wrappers.<GraduationFact>lambdaQuery().eq(GraduationFact::getIdCard, idCard));
            }
        }

        if (existing != null) {
            graduationFact.setId(existing.getId());
            graduationFact.setCreateTime(existing.getCreateTime());
            graduationFact.setUpdateTime(now);
            graduationFactMapper.updateById(graduationFact);
        } else {
            graduationFact.setCreateTime(now);
            graduationFact.setUpdateTime(now);
            graduationFactMapper.insert(graduationFact);
        }

        // 同步标记 student_fact 为已毕业
        markStudentGraduated(studentNo, data.get("id_card"), now);
    }

    /**
     * 毕业入库时，同步将 student_fact 对应记录标记为已毕业
     */
    private void markStudentGraduated(String studentNo, String idCard, LocalDateTime now) {
        StudentFact target = null;
        if (studentNo != null && !studentNo.isBlank()) {
            target = studentFactMapper.selectOne(
                    Wrappers.<StudentFact>lambdaQuery().eq(StudentFact::getStudentNo, studentNo));
        }
        if (target == null && idCard != null && !idCard.isBlank()) {
            target = studentFactMapper.selectOne(
                    Wrappers.<StudentFact>lambdaQuery().eq(StudentFact::getIdCard, idCard));
        }
        if (target != null && !Boolean.TRUE.equals(target.getGraduated())) {
            target.setGraduated(true);
            target.setUpdateTime(now);
            studentFactMapper.updateById(target);
            log.info("学生已毕业标记: student_no={}, id_card={}", studentNo, idCard);
        }
    }

    // ===================== 实体构建方法 =====================

    private StudentFact buildStudentFact(Map<String, String> data, ProvinceDim province,
                                          MajorDim major, ClassDim classObj, LocalDate admissionDate,
                                          Integer fileId) {
        StudentFact fact = new StudentFact();
        fact.setStudentNo(data.get("student_no"));
        fact.setName(data.get("name"));
        fact.setIdCard(data.get("id_card"));
        fact.setGender(data.get("gender"));
        if (province != null) fact.setProvinceId(province.getProvinceId());
        if (major != null) fact.setMajorId(major.getMajorId());
        if (classObj != null) fact.setClassId(classObj.getClassId());
        if (admissionDate != null) fact.setAdmissionDate(admissionDate);
        fact.setFileId(fileId);
        return fact;
    }

    private AdmissionFact buildAdmissionFact(Map<String, String> data, ProvinceDim province,
                                              MajorDim major, LocalDate admissionDate, Integer fileId) {
        AdmissionFact fact = new AdmissionFact();
        fact.setStudentNo(data.get("student_no"));
        fact.setExamNo(data.get("exam_no"));
        fact.setName(data.get("name"));
        fact.setIdCard(data.get("id_card"));
        fact.setGender(data.get("gender"));
        if (province != null) fact.setProvinceId(province.getProvinceId());
        if (major != null) fact.setMajorId(major.getMajorId());
        if (admissionDate != null) fact.setAdmissionDate(admissionDate);
        fact.setFileId(fileId);
        return fact;
    }

    // ===================== 维度表模糊查询（提取为方法，避免重复代码） =====================

    private ProvinceDim fuzzyLookupProvince(String provinceName) {
        if (provinceName == null || provinceName.isBlank()) return null;
        ProvinceDim dim = provinceDimMapper.selectOne(
                Wrappers.<ProvinceDim>lambdaQuery().eq(ProvinceDim::getProvinceName, provinceName.trim()));
        if (dim == null) {
            List<String> allNames = provinceDimMapper.selectList(null).stream()
                    .map(ProvinceDim::getProvinceName).collect(Collectors.toList());
            String corrected = fuzzyResolve(provinceName, allNames, "省份");
            if (corrected != null) {
                dim = provinceDimMapper.selectOne(
                        Wrappers.<ProvinceDim>lambdaQuery().eq(ProvinceDim::getProvinceName, corrected));
            }
        }
        return dim;
    }

    private MajorDim fuzzyLookupMajor(String majorName) {
        if (majorName == null || majorName.isBlank()) return null;
        MajorDim dim = majorDimMapper.selectOne(
                Wrappers.<MajorDim>lambdaQuery().eq(MajorDim::getMajorName, majorName.trim()));
        if (dim == null) {
            List<String> allNames = majorDimMapper.selectList(null).stream()
                    .map(MajorDim::getMajorName).collect(Collectors.toList());
            String corrected = fuzzyResolve(majorName, allNames, "专业");
            if (corrected != null) {
                dim = majorDimMapper.selectOne(
                        Wrappers.<MajorDim>lambdaQuery().eq(MajorDim::getMajorName, corrected));
            }
        }
        return dim;
    }

    private ClassDim fuzzyLookupClass(String className) {
        if (className == null || className.isBlank()) return null;
        ClassDim dim = classDimMapper.selectOne(
                Wrappers.<ClassDim>lambdaQuery().eq(ClassDim::getClassName, className.trim()));
        if (dim == null) {
            List<String> allNames = classDimMapper.selectList(null).stream()
                    .map(ClassDim::getClassName).collect(Collectors.toList());
            String corrected = fuzzyResolve(className, allNames, "班级");
            if (corrected != null) {
                dim = classDimMapper.selectOne(
                        Wrappers.<ClassDim>lambdaQuery().eq(ClassDim::getClassName, corrected));
            }
        }
        return dim;
    }

    private DegreeDim fuzzyLookupDegree(String degreeName) {
        if (degreeName == null || degreeName.isBlank()) return null;
        DegreeDim dim = degreeDimMapper.selectOne(
                Wrappers.<DegreeDim>lambdaQuery().eq(DegreeDim::getDegreeName, degreeName.trim()));
        if (dim == null) {
            List<String> allNames = degreeDimMapper.selectList(null).stream()
                    .map(DegreeDim::getDegreeName).collect(Collectors.toList());
            String corrected = fuzzyResolve(degreeName, allNames, "学历");
            if (corrected != null) {
                dim = degreeDimMapper.selectOne(
                        Wrappers.<DegreeDim>lambdaQuery().eq(DegreeDim::getDegreeName, corrected));
            }
        }
        return dim;
    }

    private DestinationDim fuzzyLookupDestination(String destName) {
        if (destName == null || destName.isBlank()) return null;
        DestinationDim dim = destinationDimMapper.selectOne(
                Wrappers.<DestinationDim>lambdaQuery().eq(DestinationDim::getDestName, destName.trim()));
        if (dim == null) {
            List<String> allNames = destinationDimMapper.selectList(null).stream()
                    .map(DestinationDim::getDestName).collect(Collectors.toList());
            String corrected = fuzzyResolve(destName, allNames, "去向");
            if (corrected != null) {
                dim = destinationDimMapper.selectOne(
                        Wrappers.<DestinationDim>lambdaQuery().eq(DestinationDim::getDestName, corrected));
            }
        }
        return dim;
    }

    /**
     * 维度值模糊匹配：精确匹配失败后，用编辑距离纠正常见 OCR 偏差
     * 例如 "广冬" → "广东"，"福洲" → "福州"
     *
     * @param input     OCR 识别出的原始值
     * @param standards 维度表中所有标准值列表
     * @param label     维度名称（仅用于日志）
     * @return 纠正后的标准值，无合适匹配返回 null
     */
    private String fuzzyResolve(String input, List<String> standards, String label) {
        if (input == null || input.isBlank() || standards == null || standards.isEmpty()) {
            return null;
        }
        String corrected = TextUtil.fuzzyMatch(input, standards, TextUtil.autoThreshold(input));
        if (corrected != null) {
            log.warn("维度值模糊纠正 [{}]: \"{}\" → \"{}\"", label, input, corrected);
        }
        return corrected;
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
