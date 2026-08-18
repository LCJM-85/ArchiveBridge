package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.dto.StudentDTO;
import edu.scau.scauarchiveinsight.mapper.*;
import edu.scau.scauarchiveinsight.pojo.*;
import edu.scau.scauarchiveinsight.vo.StudentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class StudentService {

    @Autowired
    private StudentFactMapper studentFactMapper;

    @Autowired
    private ProvinceDimMapper provinceDimMapper;

    @Autowired
    private MajorDimMapper majorDimMapper;

    @Autowired
    private ClassDimMapper classDimMapper;

    @Autowired
    private ArchiveFileDimMapper archiveFileDimMapper;

    @Autowired
    private DegreeDimMapper degreeDimMapper;

    @Autowired
    private CollegeDimMapper collegeDimMapper;

    @Autowired
    private CacheService cacheService;

    public IPage<StudentVO> page(int current, int size, String keyword,
                                 String createTimeStart, String createTimeEnd,
                                 String updateTimeStart, String updateTimeEnd,
                                 Integer degreeId) {
        Page<StudentFact> page = new Page<>(current, size);
        LambdaQueryWrapper<StudentFact> wrapper = buildQueryWrapper(keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd, degreeId);
        wrapper.orderByDesc(StudentFact::getAdmissionDate);

        IPage<StudentFact> result = studentFactMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public void add(StudentDTO dto) {
        StudentFact entity = toEntity(dto);
        entity.setMajorId(resolveMajorId(dto.getMajorId(), dto.getMajorName()));
        entity.setClassId(resolveClassId(dto.getClassId(), dto.getClassName(), entity.getMajorId()));
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        studentFactMapper.insert(entity);
        cacheService.evictDashboard();
    }

    public void update(StudentDTO dto) {
        StudentFact entity = toEntity(dto);
        entity.setMajorId(resolveMajorId(dto.getMajorId(), dto.getMajorName()));
        entity.setClassId(resolveClassId(dto.getClassId(), dto.getClassName(), entity.getMajorId()));
        entity.setUpdateTime(LocalDateTime.now());
        studentFactMapper.updateById(entity);
        cacheService.evictDashboard();
    }

    private Integer resolveMajorId(Integer majorId, String majorName) {
        if (majorName != null && !majorName.isBlank()) {
            MajorDim dim = majorDimMapper.selectOne(
                    new LambdaQueryWrapper<MajorDim>().eq(MajorDim::getMajorName, majorName.trim()));
            if (dim == null) {
                dim = new MajorDim();
                dim.setMajorName(majorName.trim());
                dim.setCollegeId(ensureDefaultCollege());
                dim.setMajorCode("GEN" + System.currentTimeMillis());
                majorDimMapper.insert(dim);
                cacheService.evict(CacheService.MAJOR_KEY, CacheService.MAJOR_DROPDOWN_KEY,
                        CacheService.CLASS_KEY, CacheService.CLASS_DROPDOWN_KEY);
                cacheService.evictDashboard();
            }
            return dim.getMajorId();
        }
        return majorId;
    }

    private Integer ensureDefaultCollege() {
        CollegeDim college = collegeDimMapper.selectOne(
                new LambdaQueryWrapper<CollegeDim>().eq(CollegeDim::getCollegeName, "未知学院"));
        if (college == null) {
            college = new CollegeDim();
            college.setCollegeName("未知学院");
            collegeDimMapper.insert(college);
            cacheService.evict(CacheService.COLLEGE_KEY, CacheService.MAJOR_KEY, CacheService.MAJOR_DROPDOWN_KEY);
        }
        return college.getCollegeId();
    }

    private Integer resolveClassId(Integer classId, String className, Integer majorId) {
        if (className != null && !className.isBlank()) {
            ClassDim dim = classDimMapper.selectOne(
                    new LambdaQueryWrapper<ClassDim>().eq(ClassDim::getClassName, className.trim()));
            if (dim == null) {
                if (majorId == null) majorId = ensureDefaultMajor();
                dim = new ClassDim();
                dim.setClassName(className.trim());
                dim.setMajorId(majorId);
                dim.setGrade(String.valueOf(LocalDate.now().getYear()));
                dim.setStudyLength(4);
                classDimMapper.insert(dim);
                cacheService.evict(CacheService.CLASS_KEY, CacheService.CLASS_DROPDOWN_KEY);
            }
            return dim.getClassId();
        }
        return classId;
    }

    private Integer ensureDefaultMajor() {
        MajorDim major = majorDimMapper.selectOne(
                new LambdaQueryWrapper<MajorDim>().eq(MajorDim::getMajorName, "未定专业"));
        if (major == null) {
            major = new MajorDim();
            major.setMajorName("未定专业");
            major.setCollegeId(ensureDefaultCollege());
            major.setMajorCode("GEN" + System.currentTimeMillis());
            majorDimMapper.insert(major);
            cacheService.evict(CacheService.MAJOR_KEY, CacheService.MAJOR_DROPDOWN_KEY,
                    CacheService.CLASS_KEY, CacheService.CLASS_DROPDOWN_KEY);
            cacheService.evictDashboard();
        }
        return major.getMajorId();
    }

    public void delete(Long id) {
        studentFactMapper.deleteById(id);
        cacheService.evictDashboard();
    }

    public List<ProvinceDim> listProvinces() {
        List<ProvinceDim> cached = cacheService.get(CacheService.PROVINCE_KEY, new TypeReference<>() {});
        if (cached != null) return cached;
        List<ProvinceDim> result = provinceDimMapper.selectList(null);
        cacheService.put(CacheService.PROVINCE_KEY, result, CacheService.DIMENSION_TTL);
        return result;
    }

    public List<MajorDim> listMajors() {
        List<MajorDim> cached = cacheService.get(CacheService.MAJOR_DROPDOWN_KEY, new TypeReference<>() {});
        if (cached != null) return cached;
        List<MajorDim> result = majorDimMapper.selectList(null);
        cacheService.put(CacheService.MAJOR_DROPDOWN_KEY, result, CacheService.DIMENSION_TTL);
        return result;
    }

    public List<ClassDim> listClasses() {
        List<ClassDim> cached = cacheService.get(CacheService.CLASS_DROPDOWN_KEY, new TypeReference<>() {});
        if (cached != null) return cached;
        List<ClassDim> result = classDimMapper.selectList(null);
        cacheService.put(CacheService.CLASS_DROPDOWN_KEY, result, CacheService.DIMENSION_TTL);
        return result;
    }

    public List<DegreeDim> listDegrees() {
        // 招生/学籍只需层次（学士/硕士/博士），不展示具体学位
        return degreeDimMapper.selectList(
                new LambdaQueryWrapper<DegreeDim>().in(DegreeDim::getDegreeName, "学士", "硕士", "博士")
                        .orderByAsc(DegreeDim::getDegreeId));
    }

    private LambdaQueryWrapper<StudentFact> buildQueryWrapper(String keyword,
            String createTimeStart, String createTimeEnd, String updateTimeStart, String updateTimeEnd,
            Integer degreeId) {
        LambdaQueryWrapper<StudentFact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(degreeId != null, StudentFact::getDegreeId, degreeId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> {
                w.like(StudentFact::getStudentNo, keyword)
                 .or().like(StudentFact::getName, keyword)
                 .or().like(StudentFact::getIdCard, keyword)
                 .or().like(StudentFact::getGender, keyword);

                List<ProvinceDim> provinces = provinceDimMapper.selectList(
                        new LambdaQueryWrapper<ProvinceDim>().like(ProvinceDim::getProvinceName, keyword));
                if (!provinces.isEmpty()) {
                    w.or().in(StudentFact::getProvinceId,
                            provinces.stream().map(ProvinceDim::getProvinceId).collect(Collectors.toList()));
                }
                List<MajorDim> majors = majorDimMapper.selectList(
                        new LambdaQueryWrapper<MajorDim>().like(MajorDim::getMajorName, keyword));
                if (!majors.isEmpty()) {
                    w.or().in(StudentFact::getMajorId,
                            majors.stream().map(MajorDim::getMajorId).collect(Collectors.toList()));
                }
                List<ClassDim> classes = classDimMapper.selectList(
                        new LambdaQueryWrapper<ClassDim>().like(ClassDim::getClassName, keyword));
                if (!classes.isEmpty()) {
                    w.or().in(StudentFact::getClassId,
                            classes.stream().map(ClassDim::getClassId).collect(Collectors.toList()));
                }

                // 按来源文件名搜索
                List<Integer> matchedFiles = archiveFileDimMapper.selectList(
                        new LambdaQueryWrapper<ArchiveFileDim>().like(ArchiveFileDim::getFileName, keyword))
                        .stream().map(ArchiveFileDim::getFileId).collect(Collectors.toList());
                if (!matchedFiles.isEmpty()) {
                    w.or().in(StudentFact::getFileId, matchedFiles);
                }
            });
        }
        if (createTimeStart != null && !createTimeStart.isBlank()) {
            wrapper.ge(StudentFact::getCreateTime, LocalDate.parse(createTimeStart).atStartOfDay());
        }
        if (createTimeEnd != null && !createTimeEnd.isBlank()) {
            wrapper.le(StudentFact::getCreateTime, LocalDate.parse(createTimeEnd).atTime(LocalTime.MAX));
        }
        if (updateTimeStart != null && !updateTimeStart.isBlank()) {
            wrapper.ge(StudentFact::getUpdateTime, LocalDate.parse(updateTimeStart).atStartOfDay());
        }
        if (updateTimeEnd != null && !updateTimeEnd.isBlank()) {
            wrapper.le(StudentFact::getUpdateTime, LocalDate.parse(updateTimeEnd).atTime(LocalTime.MAX));
        }
        return wrapper;
    }

    private StudentVO toVO(StudentFact fact) {
        StudentVO vo = new StudentVO();
        vo.setId(fact.getId());
        vo.setStudentNo(fact.getStudentNo());
        vo.setName(fact.getName());
        vo.setIdCard(fact.getIdCard());
        vo.setGender(fact.getGender());
        if (fact.getDegreeId() != null) {
            vo.setDegreeId(fact.getDegreeId());
            DegreeDim d = degreeDimMapper.selectById(fact.getDegreeId());
            vo.setDegreeName(d != null ? d.getDegreeName() : null);
        }
        vo.setGraduated(fact.getGraduated());
        vo.setAdmissionDate(fact.getAdmissionDate());
        vo.setCreateTime(fact.getCreateTime());
        vo.setUpdateTime(fact.getUpdateTime());

        vo.setFileId(fact.getFileId());
        if (fact.getFileId() != null) {
            ArchiveFileDim file = archiveFileDimMapper.selectById(fact.getFileId());
            vo.setFileName(file != null ? file.getFileName() : null);
        }

        if (fact.getProvinceId() != null) {
            vo.setProvinceId(fact.getProvinceId());
            ProvinceDim p = provinceDimMapper.selectById(fact.getProvinceId());
            vo.setProvinceName(p != null ? p.getProvinceName() : null);
        }
        if (fact.getMajorId() != null) {
            vo.setMajorId(fact.getMajorId());
            MajorDim m = majorDimMapper.selectById(fact.getMajorId());
            vo.setMajorName(m != null ? m.getMajorName() : null);
        }
        if (fact.getClassId() != null) {
            vo.setClassId(fact.getClassId());
            ClassDim c = classDimMapper.selectById(fact.getClassId());
            vo.setClassName(c != null ? c.getClassName() : null);
        }
        return vo;
    }

    private StudentFact toEntity(StudentDTO dto) {
        StudentFact entity = new StudentFact();
        entity.setId(dto.getId());
        entity.setStudentNo(dto.getStudentNo());
        entity.setName(dto.getName());
        entity.setIdCard(dto.getIdCard());
        entity.setGender(dto.getGender());
        entity.setDegreeId(dto.getDegreeId());
        entity.setProvinceId(dto.getProvinceId());
        entity.setMajorId(dto.getMajorId());
        entity.setClassId(dto.getClassId());
        entity.setAdmissionDate(dto.getAdmissionDate());
        entity.setGraduated(dto.getGraduated());
        return entity;
    }
}
