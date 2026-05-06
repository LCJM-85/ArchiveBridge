package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.QualityScoreDimMapper;
import edu.scau.scauarchiveinsight.pojo.MetaDataStandard;
import edu.scau.scauarchiveinsight.pojo.QualityScoreDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据质量评分：每个文件归档后自动评分
 * 完整性 40% + 准确性 30% + 一致性 20% + 时效性 10%
 */
@Service
public class QualityScoreService {

    @Autowired
    private QualityScoreDimMapper qualityScoreDimMapper;

    @Autowired
    private MetaDataService metaDataService;

    /**
     * 对已处理的文件进行质量评分并入库
     *
     * @param fileId      归档文件 ID
     * @param archiveType admission / graduation
     * @param records     该文件解析出的所有数据行（用于计算完整性）
     * @param errorCount  校验错误/警告数量（用于计算准确性）
     */
    public void scoreFile(Integer fileId, String archiveType,
                          List<Map<String, String>> records, int errorCount) {
        // 以 metadata_standard 总规则数为完整字段总数
        List<MetaDataStandard> rules = metaDataService.list();
        int totalFieldCount = rules.size();

        // 完整性 40%
        int completeness = calcCompleteness(records, totalFieldCount);

        // 准确性 30%
        int accuracy = calcAccuracy(records != null ? records.size() : 0, errorCount);

        // 一致性 20%：错误越多，一致性越差
        int consistency = calcConsistency(errorCount);

        // 时效性 10%
        int timeliness = 100;

        int total = completeness * 40 / 100
                  + accuracy     * 30 / 100
                  + consistency  * 20 / 100
                  + timeliness   * 10 / 100;

        QualityScoreDim score = new QualityScoreDim();
        score.setFileId(fileId);
        score.setCompleteness(completeness);
        score.setConsistency(consistency);
        score.setAccuracy(accuracy);
        score.setTimeliness(timeliness);
        score.setTotalScore(total);
        score.setCheckTime(LocalDateTime.now());

        qualityScoreDimMapper.insert(score);
    }

    /**
     * 完整性：非空字段数 / 总字段数，全文件平均
     */
    private int calcCompleteness(List<Map<String, String>> records, int totalFieldCount) {
        if (records == null || records.isEmpty() || totalFieldCount == 0) return 0;
        double nonEmpty = 0;
        double totalCells = (double) records.size() * totalFieldCount;
        for (Map<String, String> rec : records) {
            for (String val : rec.values()) {
                if (val != null && !val.isBlank()) nonEmpty++;
            }
        }
        return (int) Math.round(nonEmpty / totalCells * 100);
    }

    /**
     * 准确性：基于错误/警告记录占比
     * 无错=100，错误数越接近记录数越低
     */
    private int calcAccuracy(int recordCount, int errorCount) {
        if (recordCount == 0) return 0;
        if (errorCount >= recordCount) return 0;
        return (int) Math.round((1.0 - (double) errorCount / recordCount) * 100);
    }

    /**
     * 一致性：按错误数量分级扣分
     * 0=100, 1-2=80, 3-5=60, 6+=40
     */
    private int calcConsistency(int errorCount) {
        if (errorCount <= 0) return 100;
        if (errorCount <= 2) return 80;
        if (errorCount <= 5) return 60;
        return 40;
    }
}
