package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.dto.GraduationDTO;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.*;
import edu.scau.scauarchiveinsight.pojo.*;
import edu.scau.scauarchiveinsight.vo.GraduationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GraduationService {

    @Autowired
    private GraduationFactMapper graduationFactMapper;

    @Autowired
    private DegreeDimMapper degreeDimMapper;

    @Autowired
    private DestinationDimMapper destinationDimMapper;

    @Autowired
    private ArchiveFileDimMapper archiveFileDimMapper;

    public IPage<GraduationVO> page(int current, int size, String keyword,
                                    String createTimeStart, String createTimeEnd,
                                    String updateTimeStart, String updateTimeEnd) {
        Page<GraduationFact> page = new Page<>(current, size);
        LambdaQueryWrapper<GraduationFact> wrapper = buildQueryWrapper(keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd);
        wrapper.orderByDesc(GraduationFact::getGraduationDate);

        IPage<GraduationFact> result = graduationFactMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    public void add(GraduationDTO dto) {
        GraduationFact entity = toEntity(dto);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        graduationFactMapper.insert(entity);
    }

    public void update(GraduationDTO dto) {
        GraduationFact entity = toEntity(dto);
        entity.setUpdateTime(LocalDateTime.now());
        graduationFactMapper.updateById(entity);
    }

    public void delete(Long id) {
        graduationFactMapper.deleteById(id);
    }

    public List<DegreeDim> listDegrees() {
        return degreeDimMapper.selectList(null);
    }

    public List<DestinationDim> listDestinations() {
        return destinationDimMapper.selectList(null);
    }

    private LambdaQueryWrapper<GraduationFact> buildQueryWrapper(String keyword,
            String createTimeStart, String createTimeEnd, String updateTimeStart, String updateTimeEnd) {
        LambdaQueryWrapper<GraduationFact> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> {
                w.like(GraduationFact::getStudentNo, keyword)
                 .or().like(GraduationFact::getName, keyword)
                 .or().like(GraduationFact::getIdCard, keyword)
                 .or().like(GraduationFact::getGender, keyword);

                List<DegreeDim> degrees = degreeDimMapper.selectList(
                        new LambdaQueryWrapper<DegreeDim>().like(DegreeDim::getDegreeName, keyword));
                if (!degrees.isEmpty()) {
                    w.or().in(GraduationFact::getDegreeId,
                            degrees.stream().map(DegreeDim::getDegreeId).collect(Collectors.toList()));
                }
                List<DestinationDim> dests = destinationDimMapper.selectList(
                        new LambdaQueryWrapper<DestinationDim>().like(DestinationDim::getDestName, keyword));
                if (!dests.isEmpty()) {
                    w.or().in(GraduationFact::getDestId,
                            dests.stream().map(DestinationDim::getDestId).collect(Collectors.toList()));
                }

                // 按来源文件名搜索
                List<Integer> matchedFiles = archiveFileDimMapper.selectList(
                        new LambdaQueryWrapper<ArchiveFileDim>().like(ArchiveFileDim::getFileName, keyword))
                        .stream().map(ArchiveFileDim::getFileId).collect(Collectors.toList());
                if (!matchedFiles.isEmpty()) {
                    w.or().in(GraduationFact::getFileId, matchedFiles);
                }
            });
        }
        if (createTimeStart != null && !createTimeStart.isBlank()) {
            wrapper.ge(GraduationFact::getCreateTime, LocalDate.parse(createTimeStart).atStartOfDay());
        }
        if (createTimeEnd != null && !createTimeEnd.isBlank()) {
            wrapper.le(GraduationFact::getCreateTime, LocalDate.parse(createTimeEnd).atTime(LocalTime.MAX));
        }
        if (updateTimeStart != null && !updateTimeStart.isBlank()) {
            wrapper.ge(GraduationFact::getUpdateTime, LocalDate.parse(updateTimeStart).atStartOfDay());
        }
        if (updateTimeEnd != null && !updateTimeEnd.isBlank()) {
            wrapper.le(GraduationFact::getUpdateTime, LocalDate.parse(updateTimeEnd).atTime(LocalTime.MAX));
        }
        return wrapper;
    }

    private GraduationVO toVO(GraduationFact fact) {
        GraduationVO vo = new GraduationVO();
        vo.setId(fact.getId());
        vo.setStudentNo(fact.getStudentNo());
        vo.setName(fact.getName());
        vo.setIdCard(fact.getIdCard());
        vo.setGender(fact.getGender());
        vo.setGraduationDate(fact.getGraduationDate());
        vo.setCreateTime(fact.getCreateTime());
        vo.setUpdateTime(fact.getUpdateTime());

        vo.setFileId(fact.getFileId());
        if (fact.getFileId() != null) {
            ArchiveFileDim file = archiveFileDimMapper.selectById(fact.getFileId());
            vo.setFileName(file != null ? file.getFileName() : null);
        }

        if (fact.getDegreeId() != null) {
            vo.setDegreeId(fact.getDegreeId());
            DegreeDim d = degreeDimMapper.selectById(fact.getDegreeId());
            vo.setDegreeName(d != null ? d.getDegreeName() : null);
        }
        if (fact.getDestId() != null) {
            vo.setDestId(fact.getDestId());
            DestinationDim d = destinationDimMapper.selectById(fact.getDestId());
            vo.setDestName(d != null ? d.getDestName() : null);
        }
        return vo;
    }

    private GraduationFact toEntity(GraduationDTO dto) {
        GraduationFact entity = new GraduationFact();
        entity.setId(dto.getId());
        entity.setStudentNo(dto.getStudentNo());
        entity.setName(dto.getName());
        entity.setIdCard(dto.getIdCard());
        entity.setGender(dto.getGender());
        entity.setDegreeId(dto.getDegreeId());
        entity.setDestId(dto.getDestId());
        entity.setGraduationDate(dto.getGraduationDate());
        return entity;
    }
}
