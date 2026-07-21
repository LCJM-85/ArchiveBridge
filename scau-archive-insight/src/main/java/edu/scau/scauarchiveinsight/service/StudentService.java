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

    public IPage<StudentVO> page(int current, int size, String keyword,
                                 String createTimeStart, String createTimeEnd,
                                 String updateTimeStart, String updateTimeEnd) {
        Page<StudentFact> page = new Page<>(current, size);
        LambdaQueryWrapper<StudentFact> wrapper = buildQueryWrapper(keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd);
        wrapper.orderByDesc(StudentFact::getAdmissionDate);

        IPage<StudentFact> result = studentFactMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public void add(StudentDTO dto) {
        StudentFact entity = toEntity(dto);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        studentFactMapper.insert(entity);
    }

    public void update(StudentDTO dto) {
        StudentFact entity = toEntity(dto);
        entity.setUpdateTime(LocalDateTime.now());
        studentFactMapper.updateById(entity);
    }

    public void delete(Long id) {
        studentFactMapper.deleteById(id);
    }

    public List<ProvinceDim> listProvinces() {
        return provinceDimMapper.selectList(null);
    }

    public List<MajorDim> listMajors() {
        return majorDimMapper.selectList(null);
    }

    public List<ClassDim> listClasses() {
        return classDimMapper.selectList(null);
    }

    private LambdaQueryWrapper<StudentFact> buildQueryWrapper(String keyword,
            String createTimeStart, String createTimeEnd, String updateTimeStart, String updateTimeEnd) {
        LambdaQueryWrapper<StudentFact> wrapper = new LambdaQueryWrapper<>();
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
        entity.setProvinceId(dto.getProvinceId());
        entity.setMajorId(dto.getMajorId());
        entity.setClassId(dto.getClassId());
        entity.setAdmissionDate(dto.getAdmissionDate());
        return entity;
    }
}
