package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GeographicService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    public List<Map<String, Object>> provinceStats() {
        return admissionFactMapper.provinceStats();
    }

    public Map<String, Object> provinceMapGeoJson() {
        Map<String, Object> result = admissionFactMapper.provinceMapGeoJson();
        String geojsonStr = (String) result.get("geojson");
        if (geojsonStr == null) {
            return Map.of("geojson", "{\"type\":\"FeatureCollection\",\"features\":[]}");
        }
        return Map.of("geojson", geojsonStr);
    }
}
