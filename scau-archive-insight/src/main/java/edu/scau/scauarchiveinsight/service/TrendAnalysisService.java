package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class TrendAnalysisService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    public List<Map<String, Object>> yearlyTrend(Integer startYear, Integer endYear, String degreeName) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.yearlyTrend(startDate, endDate, degreeName);
    }

    public List<Map<String, Object>> majorTrend(Integer startYear, Integer endYear, String degreeName) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.majorTrend(startDate, endDate, degreeName);
    }

    public List<Map<String, Object>> provinceTrend(Integer startYear, Integer endYear, String degreeName) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.provinceTrend(startDate, endDate, degreeName);
    }

    public List<Map<String, Object>> scoreTrend(Integer startYear, Integer endYear, String degreeName) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.scoreTrend(startDate, endDate, degreeName);
    }

    public List<Map<String, Object>> genderTrend(Integer startYear, Integer endYear, String degreeName) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.genderTrend(startDate, endDate, degreeName);
    }
}
