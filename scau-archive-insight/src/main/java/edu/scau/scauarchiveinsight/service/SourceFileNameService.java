package edu.scau.scauarchiveinsight.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** 仅转换展示名，不修改物理文件名或文件关联。 */
@Service
public class SourceFileNameService {
    private final JdbcTemplate jdbc;
    public SourceFileNameService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<Integer, String> originalNames(Collection<Integer> fileIds) {
        List<Integer> ids = fileIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        var rows = jdbc.queryForList("SELECT file_id, original_file_name FROM archive_review_draft "
                + "WHERE file_id IN (" + placeholders + ") ORDER BY draft_id", ids.toArray());
        Map<Integer, String> names = new HashMap<>();
        for (var row : rows) {
            String name = (String) row.get("original_file_name");
            if (name != null && !name.isBlank()) names.put(((Number)row.get("file_id")).intValue(), name);
        }
        return names;
    }

    public String displayName(Integer fileId, String storedName, Map<Integer, String> originals) {
        if (fileId != null && originals.containsKey(fileId)) return originals.get(fileId);
        return storedName == null ? null : storedName.replaceFirst("^[0-9a-fA-F]{32}_(?=.+)", "");
    }
}
