package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.dto.AdmissionDTO;
import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.DegreeDimMapper;
import edu.scau.scauarchiveinsight.mapper.MajorDimMapper;
import edu.scau.scauarchiveinsight.mapper.ProvinceDimMapper;
import edu.scau.scauarchiveinsight.pojo.AdmissionFact;
import edu.scau.scauarchiveinsight.pojo.ArchiveFileDim;
import edu.scau.scauarchiveinsight.pojo.DegreeDim;
import edu.scau.scauarchiveinsight.pojo.MajorDim;
import edu.scau.scauarchiveinsight.pojo.ProvinceDim;
import edu.scau.scauarchiveinsight.vo.AdmissionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdmissionService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    @Autowired
    private ProvinceDimMapper provinceDimMapper;

    @Autowired
    private MajorDimMapper majorDimMapper;

    @Autowired
    private ArchiveFileDimMapper archiveFileDimMapper;

    @Autowired
    private DegreeDimMapper degreeDimMapper;

    public IPage<AdmissionVO> page(int current, int size, String keyword,
                                   String createTimeStart, String createTimeEnd,
                                   String updateTimeStart, String updateTimeEnd,
                                   Integer degreeId) {
        Page<AdmissionFact> page = new Page<>(current, size);
        LambdaQueryWrapper<AdmissionFact> wrapper = buildQueryWrapper(keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd, degreeId);
        wrapper.orderByDesc(AdmissionFact::getAdmissionDate);

        IPage<AdmissionFact> result = admissionFactMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public void add(AdmissionDTO dto) {
        AdmissionFact entity = toEntity(dto);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        admissionFactMapper.insert(entity);
    }

    public void update(AdmissionDTO dto) {
        AdmissionFact entity = toEntity(dto);
        entity.setUpdateTime(LocalDateTime.now());
        admissionFactMapper.updateById(entity);
    }

    public void delete(Long id) {
        admissionFactMapper.deleteById(id);
    }

    public List<ProvinceDim> listProvinces() {
        return provinceDimMapper.selectList(null);
    }

    public List<MajorDim> listMajors() {
        return majorDimMapper.selectList(null);
    }

    public List<DegreeDim> listDegrees() {
        // 招生/学籍只需层次（学士/硕士/博士），不展示具体学位
        return degreeDimMapper.selectList(
                new LambdaQueryWrapper<DegreeDim>().in(DegreeDim::getDegreeName, "学士", "硕士", "博士")
                        .orderByAsc(DegreeDim::getDegreeId));
    }

    private LambdaQueryWrapper<AdmissionFact> buildQueryWrapper(String keyword,
            String createTimeStart, String createTimeEnd, String updateTimeStart, String updateTimeEnd,
            Integer degreeId) {
        LambdaQueryWrapper<AdmissionFact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(degreeId != null, AdmissionFact::getDegreeId, degreeId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> {
                w.like(AdmissionFact::getStudentNo, keyword)
                 .or().like(AdmissionFact::getName, keyword)
                 .or().like(AdmissionFact::getIdCard, keyword)
                 .or().like(AdmissionFact::getExamNo, keyword)
                 .or().like(AdmissionFact::getGender, keyword);

                List<ProvinceDim> provinces = provinceDimMapper.selectList(
                        new LambdaQueryWrapper<ProvinceDim>().like(ProvinceDim::getProvinceName, keyword));
                if (!provinces.isEmpty()) {
                    w.or().in(AdmissionFact::getProvinceId,
                            provinces.stream().map(ProvinceDim::getProvinceId).collect(Collectors.toList()));
                }
                List<MajorDim> majors = majorDimMapper.selectList(
                        new LambdaQueryWrapper<MajorDim>().like(MajorDim::getMajorName, keyword));
                if (!majors.isEmpty()) {
                    w.or().in(AdmissionFact::getMajorId,
                            majors.stream().map(MajorDim::getMajorId).collect(Collectors.toList()));
                }

                // 按来源文件名搜索
                List<Integer> matchedFiles = archiveFileDimMapper.selectList(
                        new LambdaQueryWrapper<ArchiveFileDim>().like(ArchiveFileDim::getFileName, keyword))
                        .stream().map(ArchiveFileDim::getFileId).collect(Collectors.toList());
                if (!matchedFiles.isEmpty()) {
                    w.or().in(AdmissionFact::getFileId, matchedFiles);
                }
            });
        }
        if (createTimeStart != null && !createTimeStart.isBlank()) {
            wrapper.ge(AdmissionFact::getCreateTime, LocalDate.parse(createTimeStart).atStartOfDay());
        }
        if (createTimeEnd != null && !createTimeEnd.isBlank()) {
            wrapper.le(AdmissionFact::getCreateTime, LocalDate.parse(createTimeEnd).atTime(LocalTime.MAX));
        }
        if (updateTimeStart != null && !updateTimeStart.isBlank()) {
            wrapper.ge(AdmissionFact::getUpdateTime, LocalDate.parse(updateTimeStart).atStartOfDay());
        }
        if (updateTimeEnd != null && !updateTimeEnd.isBlank()) {
            wrapper.le(AdmissionFact::getUpdateTime, LocalDate.parse(updateTimeEnd).atTime(LocalTime.MAX));
        }
        return wrapper;
    }

    private AdmissionVO toVO(AdmissionFact fact) {
        AdmissionVO vo = new AdmissionVO();
        vo.setId(fact.getId());
        vo.setStudentNo(fact.getStudentNo());
        vo.setExamNo(fact.getExamNo());
        vo.setName(fact.getName());
        vo.setIdCard(fact.getIdCard());
        vo.setGender(fact.getGender());
        if (fact.getDegreeId() != null) {
            vo.setDegreeId(fact.getDegreeId());
            DegreeDim d = degreeDimMapper.selectById(fact.getDegreeId());
            vo.setDegreeName(d != null ? d.getDegreeName() : null);
        }
        vo.setAdmissionScore(fact.getAdmissionScore());
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
        return vo;
    }

    private AdmissionFact toEntity(AdmissionDTO dto) {
        AdmissionFact entity = new AdmissionFact();
        entity.setId(dto.getId());
        entity.setStudentNo(dto.getStudentNo());
        entity.setName(dto.getName());
        entity.setIdCard(dto.getIdCard());
        entity.setGender(dto.getGender());
        entity.setDegreeId(dto.getDegreeId());
        entity.setExamNo(dto.getExamNo());
        entity.setProvinceId(dto.getProvinceId());
        entity.setMajorId(dto.getMajorId());
        entity.setAdmissionScore(dto.getAdmissionScore());
        entity.setAdmissionDate(dto.getAdmissionDate());
        return entity;
    }
}
