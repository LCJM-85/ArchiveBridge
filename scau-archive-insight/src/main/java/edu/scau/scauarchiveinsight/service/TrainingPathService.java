package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TrainingPathService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    public Map<String, Object> sankeyData() {
        List<Map<String, Object>> majorDegree = admissionFactMapper.sankeyMajorDegree();
        List<Map<String, Object>> degreeDest = admissionFactMapper.sankeyDegreeDest();

        // 收集唯一节点
        Set<String> nodeSet = new LinkedHashSet<>();
        for (Map<String, Object> link : majorDegree) {
            nodeSet.add((String) link.get("source"));
            nodeSet.add((String) link.get("target"));
        }
        for (Map<String, Object> link : degreeDest) {
            nodeSet.add((String) link.get("source"));
            nodeSet.add((String) link.get("target"));
        }

        List<Map<String, String>> nodes = new ArrayList<>();
        for (String name : nodeSet) {
            Map<String, String> node = new HashMap<>();
            node.put("name", name);
            nodes.add(node);
        }

        List<Map<String, Object>> links = new ArrayList<>();
        links.addAll(majorDegree);
        links.addAll(degreeDest);

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        return result;
    }
}
