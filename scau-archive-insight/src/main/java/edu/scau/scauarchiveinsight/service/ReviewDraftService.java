package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.util.DateUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.nio.file.*;
import java.util.*;

/**
 * 解析出口只写草稿；正式入库由确认接口触发。
 * JSON 保留原始解析结果，版本号防止旧页面覆盖，行锁串行化确认/放弃。
 */
@Service
public class ReviewDraftService {
    public static class VersionConflictException extends IllegalStateException {
        public VersionConflictException(String message) { super(message); }
    }
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    private final DataPersistenceService persistence;
    private final QualityScoreService quality;
    private final MetaDataService metadata;
    private final StorageService storage;
    private final OCRTaskManager tasks;

    public ReviewDraftService(JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager manager,
            DataPersistenceService persistence, QualityScoreService quality, MetaDataService metadata,
            StorageService storage, OCRTaskManager tasks) {
        this.jdbc = jdbc; this.json = json; this.tx = new TransactionTemplate(manager);
        this.persistence = persistence; this.quality = quality; this.metadata = metadata;
        this.storage = storage; this.tasks = tasks;
    }

    public void stage(String name, String type, String archiveType,
                      List<Map<String, String>> records, List<?> issues) {
        if (records == null || records.isEmpty()) throw new IllegalArgumentException("未解析到记录");
        if (!Set.of("admission", "graduation").contains(archiveType))
            throw new IllegalArgumentException("不支持的档案类型");
        tasks.beginPersistence();
        Integer logId = tasks.getCurrentTaskId();
        if (logId == null) throw new IllegalStateException("解析任务缺少日志关联");
        String source = storage.reviewSourcePath(name);
        String originalName = tasks.getCurrentOriginalName();
        List<Map<String, Object>> fields = fields(archiveType);
        var edited = normalize(records, fields);
        tx.executeWithoutResult(status -> {
            jdbc.update("""
                INSERT INTO archive_review_draft
                (log_id,original_file_name,stored_file_name,source_path,file_type,archive_type,
                 original_records,edited_records,schema_snapshot,original_issues)
                VALUES (?,?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb),CAST(? AS jsonb))
                """, logId, originalName == null ? name : originalName, name, source, type, archiveType,
                encode(records), encode(edited), encode(fields), encode(issues == null ? List.of() : issues));
            int changed = jdbc.update("""
                UPDATE ocr_log_dim SET recognize_status='pending_review',
                message='解析完成，等待审查确认', error_message=NULL, updated_at=CURRENT_TIMESTAMP
                WHERE log_id=? AND recognize_status='processing'
                """, logId);
            if (changed != 1) throw new IllegalStateException("任务已结束，不能创建审查草稿");
        });
        storage.markProcessingFinished(name);
    }

    // 仅展示入库实际支持的字段，额外原始列仍保留在 original_records。
    private List<Map<String, Object>> fields(String type) {
        List<String> codes = "admission".equals(type)
            ? List.of("student_no","exam_no","name","id_card","gender","province_name",
                      "major_name","class_name","degree_name","admission_date","admission_score")
            : List.of("student_no","name","id_card","gender","degree_name","dest_name","graduation_date");
        Map<String,String> labels = Map.ofEntries(
            Map.entry("student_no","学号"),Map.entry("exam_no","考生号"),Map.entry("name","姓名"),
            Map.entry("id_card","身份证号"),Map.entry("gender","性别"),Map.entry("province_name","生源省份"),
            Map.entry("major_name","专业"),Map.entry("class_name","班级"),Map.entry("degree_name","学历"),
            Map.entry("admission_date","录取日期"),Map.entry("admission_score","录取分数"),
            Map.entry("dest_name","毕业去向"),Map.entry("graduation_date","毕业日期"));
        var rules = metadata.list();
        List<Map<String,Object>> result = new ArrayList<>();
        for (String code : codes) {
            String label = labels.get(code); boolean required = false;
            for (var rule : rules) if (code.equals(rule.getFieldCode())) {
                if (rule.getFieldName() != null && !rule.getFieldName().isBlank()) label = rule.getFieldName();
                required = Boolean.TRUE.equals(rule.getIsRequired());
            }
            result.add(Map.of("code", code, "label", label, "required", required));
        }
        return result;
    }

    static List<Map<String,String>> normalize(List<Map<String,String>> records,
                                               List<Map<String,Object>> fields) {
        if (records == null) throw new IllegalArgumentException("记录列表不能为空");
        List<Map<String,String>> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (var row : records) {
            if (row == null) throw new IllegalArgumentException("记录不能为 null");
            Map<String,String> copy = new LinkedHashMap<>();
            String id = row.get("_row_id");
            if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
            if (!ids.add(id)) throw new IllegalArgumentException("记录行标识重复");
            copy.put("_row_id", id);
            for (var field : fields) {
                String code = (String) field.get("code");
                String value = row.get(code);
                copy.put(code, value == null ? "" : value.trim());
            }
            result.add(copy);
        }
        return result;
    }

    static List<Map<String,Object>> validate(List<Map<String,String>> rows,
                                             List<Map<String,Object>> fields) {
        List<Map<String,Object>> issues = new ArrayList<>();
        if (rows.isEmpty()) issues.add(Map.of("level","error","message","至少需要一条记录"));
        Map<String,Set<String>> seen = new HashMap<>();
        for (int index=0; index<rows.size(); index++) {
            var row = rows.get(index);
            boolean populated = false;
            for (var field : fields) {
                String code = (String) field.get("code");
                String value = row.getOrDefault(code, "");
                populated |= !value.isBlank();
                String error = null;
                if (!value.isBlank()) {
                    if (code.equals("id_card") && !value.matches("[0-9]{17}[0-9Xx]"))
                        error = "身份证号必须为18位，末位允许X";
                    if (code.equals("gender") && !Set.of("男","女").contains(value)) error = "性别仅支持男或女";
                    if (code.endsWith("_date") && DateUtil.convertToLocalDate(value) == null) error = "日期无法转换";
                    if (code.equals("admission_score")) try {
                        if (Integer.parseInt(value)<0) error = "分数不能为负数";
                    } catch (NumberFormatException ex) { error = "分数必须是有效整数"; }
                } else if (Boolean.TRUE.equals(field.get("required"))) {
                    issues.add(issue("warning", index, row, code, field.get("label")+"未填写"));
                }
                if (error != null) issues.add(issue("error", index, row, code, error));
                if (Set.of("student_no","exam_no","id_card").contains(code) && !value.isBlank()
                        && !seen.computeIfAbsent(code, k -> new HashSet<>()).add(value.toUpperCase(Locale.ROOT)))
                    issues.add(issue("warning",index,row,code,"文件内标识重复，入库可能合并已有学生"));
            }
            if (!populated) issues.add(issue("error",index,row,"","空记录请删除或填写"));
        }
        return issues;
    }

    private static Map<String,Object> issue(String level, int index, Map<String,String> row,
                                            String field, String message) {
        return Map.of("level",level,"row",index+1,"rowId",row.get("_row_id"),"field",field,"message",message);
    }

    public Map<String,Object> list(String status, String type, String keyword, Integer logId, int page, int size) {
        page=Math.max(1,page); size=Math.max(1,Math.min(100,size));
        String where = " WHERE (?='' OR status=?) AND (?='' OR archive_type=?)"
            + " AND original_file_name ILIKE ? AND (?::integer IS NULL OR log_id=?)";
        String state = status == null ? "" : status, archive = type == null ? "" : type;
        String search = "%"+(keyword == null ? "" : keyword)+"%";
        Object[] filter = {state,state,archive,archive,search,logId,logId};
        Long total = jdbc.queryForObject("SELECT count(*) FROM archive_review_draft"+where,Long.class,filter);
        var args = new ArrayList<>(Arrays.asList(filter)); args.add(size); args.add((page-1)*size);
        var records = jdbc.queryForList("""
            SELECT draft_id,log_id,original_file_name,archive_type,status,version,file_id,
            jsonb_array_length(edited_records) AS row_count,created_at,updated_at,last_error
            FROM archive_review_draft
            """+where+" ORDER BY created_at DESC,draft_id DESC LIMIT ? OFFSET ?",args.toArray());
        return Map.of("records",records,"total",total,"current",page,"size",size);
    }

    private Map<String,Object> raw(long id, boolean lock) {
        var found = jdbc.queryForList("SELECT *, original_records::text AS original_json,"
            +"edited_records::text AS edited_json,schema_snapshot::text AS schema_json,"
            +"original_issues::text AS issues_json FROM archive_review_draft WHERE draft_id=?"
            +(lock ? " FOR UPDATE" : ""), id);
        if (found.isEmpty()) throw new IllegalArgumentException("审查草稿不存在");
        return found.get(0);
    }

    public Map<String,Object> detail(long id) {
        var result = raw(id,false);
        var rows = decodeRows((String) result.remove("edited_json"));
        var fields = decodeObjects((String) result.remove("schema_json"));
        result.put("records", rows); result.put("fields", fields);
        result.put("originalRecords",decodeRows((String) result.remove("original_json")));
        result.put("originalIssues",decodeList((String)result.remove("issues_json")));
        result.put("issues",validate(rows,fields));
        result.put("score",quality.evaluate((String)result.get("archive_type"),rows));
        for (String key : List.of("original_records","edited_records","schema_snapshot","original_issues","source_path"))
            result.remove(key);
        return result;
    }

    public Map<String,Object> save(long id, int version, List<Map<String,String>> records) {
        saveEdits(id,version,records,false);
        return detail(id);
    }

    private int saveEdits(long id, int version, List<Map<String,String>> records, boolean confirming) {
        return tx.execute(transaction -> {
            var draft = raw(id,true);
            // 在同一行锁内判断重复确认，避免先读取 pending 后被另一个请求完成的竞态。
            if (confirming && "imported".equals(draft.get("status")))
                return ((Number)draft.get("version")).intValue();
            requirePending(draft,version);
            var rows = normalize(records,decodeObjects((String)draft.get("schema_json")));
            jdbc.update("""
                UPDATE archive_review_draft SET edited_records=CAST(? AS jsonb),version=version+1,
                last_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE draft_id=?
                """,encode(rows),id);
            return version+1;
        });
    }

    private void requirePending(Map<String,Object> draft, int version) {
        if (!"pending_review".equals(draft.get("status"))) throw new IllegalStateException("该草稿已结束，不能修改");
        if (((Number)draft.get("version")).intValue()!=version)
            throw new VersionConflictException("草稿已被修改，请重新加载后再操作");
    }

    public Map<String,Object> confirm(long id, int version, List<Map<String,String>> records) {
        // 先持久化本次编辑：后续入库失败时，用户修正仍然保留。
        final int expected = saveEdits(id,version,records,true);
        try {
            tx.executeWithoutResult(transaction -> {
                var draft = raw(id,true);
                if ("imported".equals(draft.get("status"))) return; // 重试不重复入库
                requirePending(draft,expected);
                var rows = decodeRows((String)draft.get("edited_json"));
                var problems = validate(rows,decodeObjects((String)draft.get("schema_json")));
                if (problems.stream().anyMatch(p -> "error".equals(p.get("level"))))
                    throw new IllegalArgumentException("存在无法入库的字段或空行，请修正后再确认");
                // 原文件不存在时阻止入库，不能生成无来源档案。
                storage.resolveReviewSource((String)draft.get("source_path"));
                List<Map<String,String>> business = new ArrayList<>();
                for (var row : rows) {
                    var copy = new LinkedHashMap<>(row);
                    copy.remove("_row_id");
                    // 草稿以空字符串编辑；数据库以缺失/null表示未填写，避免触发长度约束。
                    copy.entrySet().removeIf(entry -> entry.getValue()==null || entry.getValue().isBlank());
                    business.add(copy);
                }
                Integer fileId = persistence.saveFileData((String)draft.get("stored_file_name"),
                    (String)draft.get("file_type"),(String)draft.get("archive_type"),business);
                quality.scoreFile(fileId,(String)draft.get("archive_type"),business,0);
                jdbc.update("""
                    UPDATE archive_review_draft SET status='imported',file_id=?,version=version+1,
                    confirmed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP,last_error=NULL WHERE draft_id=?
                    """,fileId,id);
                jdbc.update("""
                    UPDATE ocr_log_dim SET file_id=?,recognize_status='success',message='审查确认，已入库',
                    error_message=NULL,updated_at=CURRENT_TIMESTAMP WHERE log_id=?
                    """,fileId,draft.get("log_id"));
            });
        } catch (RuntimeException ex) {
            try {
                jdbc.update("UPDATE archive_review_draft SET last_error=? WHERE draft_id=? AND status='pending_review'",
                    ex.getMessage(),id);
            } catch (RuntimeException diagnosticFailure) { ex.addSuppressed(diagnosticFailure); }
            throw ex;
        }
        // 文件移动不属于数据库事务；失败后仍保持已入库，重试只补归档、不重复写业务数据。
        finalizeArchive(id);
        return detail(id);
    }

    private void finalizeArchive(long id) {
        var draft = raw(id,false);
        try {
            Path source = storage.resolveReviewSource((String)draft.get("source_path"));
            if (storage.isReviewTemporary(source)) storage.moveArchiveFile((String)draft.get("stored_file_name"));
            jdbc.update("UPDATE archive_review_draft SET last_error=NULL WHERE draft_id=?",id);
        } catch (Exception ex) {
            jdbc.update("UPDATE archive_review_draft SET last_error=? WHERE draft_id=?",
                "数据已入库，原文件归档失败，可再次确认重试："+ex.getMessage(),id);
        }
    }

    public Map<String,Object> discard(long id, int version) {
        tx.executeWithoutResult(transaction -> {
            var draft=raw(id,true); requirePending(draft,version);
            jdbc.update("""
                UPDATE archive_review_draft SET status='discarded',version=version+1,
                updated_at=CURRENT_TIMESTAMP WHERE draft_id=?
                """,id);
            jdbc.update("""
                UPDATE ocr_log_dim SET recognize_status='discarded',message='已放弃入库',
                updated_at=CURRENT_TIMESTAMP WHERE log_id=?
                """,draft.get("log_id"));
        });
        // 保留原文件，避免文件系统移动失败导致状态和文件位置不一致。
        return detail(id);
    }

    public Path source(long id) {
        return storage.resolveReviewSource((String)raw(id,false).get("source_path"));
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalArgumentException("草稿数据无法序列化",ex); }
    }
    private List<Map<String,String>> decodeRows(String value) {
        try { return json.readValue(value,new TypeReference<>() {}); }
        catch (Exception ex) { throw new IllegalStateException("草稿记录无法读取",ex); }
    }
    private List<Map<String,Object>> decodeObjects(String value) {
        try { return json.readValue(value,new TypeReference<>() {}); }
        catch (Exception ex) { throw new IllegalStateException("草稿字段无法读取",ex); }
    }
    private List<?> decodeList(String value) {
        try { return json.readValue(value,List.class); }
        catch (Exception ex) { throw new IllegalStateException("解析提示无法读取",ex); }
    }
}
