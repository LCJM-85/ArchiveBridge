package edu.scau.scauarchiveinsight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.scauarchiveinsight.pojo.AdmissionFact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdmissionFactMapper extends BaseMapper<AdmissionFact> {

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS year, COUNT(*)::int AS count " +
            "FROM admission_fact " +
            "<where>" +
            "<if test='degreeName != null'>AND EXISTS (SELECT 1 FROM degree_dim deg WHERE admission_fact.degree_id = deg.degree_id AND deg.degree_name LIKE CONCAT('%', #{degreeName}, '%'))</if> " +
            "<if test='startDate != null'>AND COALESCE(admission_date, create_time::date) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND COALESCE(admission_date, create_time::date) &lt;= #{endDate}</if> " +
            "</where> " +
            "GROUP BY year ORDER BY year" +
            "</script>")
    List<Map<String, Object>> yearlyTrend(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("degreeName") String degreeName);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM COALESCE(f.admission_date, f.create_time::date))::int AS year, " +
            "COALESCE(m.major_name, '未知') AS majorName, COUNT(*)::int AS count " +
            "FROM admission_fact f " +
            "LEFT JOIN major_dim m ON f.major_id = m.major_id " +
            "<where>" +
            "AND f.major_id IS NOT NULL " +
            "<if test='degreeName != null'>AND EXISTS (SELECT 1 FROM degree_dim deg WHERE f.degree_id = deg.degree_id AND deg.degree_name LIKE CONCAT('%', #{degreeName}, '%'))</if> " +
            "<if test='startDate != null'>AND COALESCE(f.admission_date, f.create_time::date) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND COALESCE(f.admission_date, f.create_time::date) &lt;= #{endDate}</if> " +
            "</where> " +
            "GROUP BY year, m.major_name ORDER BY year, count DESC" +
            "</script>")
    List<Map<String, Object>> majorTrend(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("degreeName") String degreeName);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM COALESCE(f.admission_date, f.create_time::date))::int AS year, " +
            "COALESCE(p.province_name, '未知') AS provinceName, COUNT(*)::int AS count " +
            "FROM admission_fact f " +
            "LEFT JOIN province_dim p ON f.province_id = p.province_id " +
            "<where>" +
            "<if test='degreeName != null'>AND EXISTS (SELECT 1 FROM degree_dim deg WHERE f.degree_id = deg.degree_id AND deg.degree_name LIKE CONCAT('%', #{degreeName}, '%'))</if> " +
            "<if test='startDate != null'>AND COALESCE(f.admission_date, f.create_time::date) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND COALESCE(f.admission_date, f.create_time::date) &lt;= #{endDate}</if> " +
            "</where> " +
            "GROUP BY year, p.province_name ORDER BY year, count DESC" +
            "</script>")
    List<Map<String, Object>> provinceTrend(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("degreeName") String degreeName);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS year, " +
            "AVG(admission_score)::int AS avgScore, " +
            "MAX(admission_score)::int AS maxScore, " +
            "MIN(admission_score)::int AS minScore " +
            "FROM admission_fact " +
            "WHERE admission_score IS NOT NULL " +
            "<if test='degreeName != null'>AND EXISTS (SELECT 1 FROM degree_dim deg WHERE admission_fact.degree_id = deg.degree_id AND deg.degree_name LIKE CONCAT('%', #{degreeName}, '%'))</if> " +
            "<if test='startDate != null'>AND COALESCE(admission_date, create_time::date) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND COALESCE(admission_date, create_time::date) &lt;= #{endDate}</if> " +
            "GROUP BY year ORDER BY year" +
            "</script>")
    List<Map<String, Object>> scoreTrend(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("degreeName") String degreeName);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS year, " +
            "COALESCE(gender, '未知') AS gender, COUNT(*)::int AS count " +
            "FROM admission_fact " +
            "<where>" +
            "<if test='degreeName != null'>AND EXISTS (SELECT 1 FROM degree_dim deg WHERE admission_fact.degree_id = deg.degree_id AND deg.degree_name LIKE CONCAT('%', #{degreeName}, '%'))</if> " +
            "<if test='startDate != null'>AND COALESCE(admission_date, create_time::date) &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND COALESCE(admission_date, create_time::date) &lt;= #{endDate}</if> " +
            "</where> " +
            "GROUP BY year, gender ORDER BY year, gender" +
            "</script>")
    List<Map<String, Object>> genderTrend(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("degreeName") String degreeName);

    @Select("SELECT COALESCE(p.province_name, '未知') AS provinceName, COUNT(*)::int AS count " +
            "FROM admission_fact f " +
            "LEFT JOIN province_dim p ON f.province_id = p.province_id " +
            "GROUP BY p.province_name ORDER BY count DESC")
    List<Map<String, Object>> provinceStats();

    @Select("SELECT json_build_object(" +
            "'type','FeatureCollection'," +
            "'features',json_agg(json_build_object(" +
            "'type','Feature'," +
            "'properties',json_build_object('name',province_name,'id',province_id)," +
            "'geometry',ST_AsGeoJSON(ST_Simplify(geom,0.05),4)::json" +
            ")))::text AS geojson FROM province_dim WHERE geom IS NOT NULL")
    Map<String, Object> provinceMapGeoJson();

    @Select("SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS year, " +
            "COUNT(*)::int AS count " +
            "FROM admission_fact " +
            "GROUP BY year ORDER BY year")
    List<Map<String, Object>> yearlyAdmissionCounts();

    @Select("SELECT EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int AS year, " +
            "COUNT(*)::int AS count " +
            "FROM admission_fact " +
            "WHERE EXISTS (SELECT 1 FROM degree_dim deg WHERE admission_fact.degree_id = deg.degree_id AND deg.degree_name LIKE CONCAT('%', #{degreeName}, '%')) " +
            "GROUP BY year ORDER BY year")
    List<Map<String, Object>> yearlyAdmissionCountsByDegreeName(@Param("degreeName") String degreeName);

    @Select("SELECT m.major_name AS source, deg.degree_name AS target, COUNT(*)::int AS value " +
            "FROM admission_fact a " +
            "JOIN graduation_fact g ON a.student_no = g.student_no " +
            "JOIN major_dim m ON a.major_id = m.major_id " +
            "JOIN degree_dim deg ON g.degree_id = deg.degree_id " +
            "GROUP BY m.major_name, deg.degree_name " +
            "ORDER BY m.major_name")
    List<Map<String, Object>> sankeyMajorDegree();

    @Select("SELECT deg.degree_name AS source, d.dest_name AS target, COUNT(*)::int AS value " +
            "FROM graduation_fact g " +
            "JOIN degree_dim deg ON g.degree_id = deg.degree_id " +
            "JOIN destination_dim d ON g.dest_id = d.dest_id " +
            "GROUP BY deg.degree_name, d.dest_name " +
            "ORDER BY deg.degree_name")
    List<Map<String, Object>> sankeyDegreeDest();

    @Select("SELECT COUNT(*)::int AS total, COUNT(DISTINCT province_id)::int AS provinceCount " +
            "FROM admission_fact WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = #{year}")
    Map<String, Object> reportOverview(@Param("year") int year);

    @Select("SELECT m.major_name AS name, COUNT(*)::int AS count " +
            "FROM admission_fact f JOIN major_dim m ON f.major_id = m.major_id " +
            "WHERE EXTRACT(YEAR FROM COALESCE(f.admission_date, f.create_time::date))::int = #{year} " +
            "GROUP BY m.major_name ORDER BY count DESC")
    List<Map<String, Object>> reportMajorDist(@Param("year") int year);

    @Select("SELECT COALESCE(p.province_name, '未知') AS name, COUNT(*)::int AS count " +
            "FROM admission_fact f LEFT JOIN province_dim p ON f.province_id = p.province_id " +
            "WHERE EXTRACT(YEAR FROM COALESCE(f.admission_date, f.create_time::date))::int = #{year} " +
            "GROUP BY p.province_name ORDER BY count DESC")
    List<Map<String, Object>> reportProvinceDist(@Param("year") int year);

    @Select("SELECT AVG(admission_score)::int AS avgScore, MAX(admission_score)::int AS maxScore, " +
            "MIN(admission_score)::int AS minScore " +
            "FROM admission_fact " +
            "WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = #{year} " +
            "AND admission_score IS NOT NULL " +
            "AND EXISTS (SELECT 1 FROM degree_dim deg WHERE admission_fact.degree_id = deg.degree_id AND deg.degree_name LIKE '%学士%')")
    Map<String, Object> reportScore(@Param("year") int year);

    @Select("SELECT gender, COUNT(*)::int AS count " +
            "FROM admission_fact " +
            "WHERE EXTRACT(YEAR FROM COALESCE(admission_date, create_time::date))::int = #{year} " +
            "GROUP BY gender")
    List<Map<String, Object>> reportGender(@Param("year") int year);

    @Select("SELECT d.dest_name AS name, COUNT(*)::int AS count " +
            "FROM graduation_fact g JOIN destination_dim d ON g.dest_id = d.dest_id " +
            "WHERE EXTRACT(YEAR FROM g.graduation_date) = #{year} " +
            "GROUP BY d.dest_name ORDER BY count DESC")
    List<Map<String, Object>> reportDestination(@Param("year") int year);

    @Select("SELECT COUNT(*)::int AS totalAdmissions FROM admission_fact")
    Integer dashboardTotalAdmissions();

    @Select("SELECT COUNT(*)::int AS totalGraduates FROM graduation_fact")
    Integer dashboardTotalGraduates();

    @Select("SELECT COUNT(DISTINCT major_id)::int FROM admission_fact WHERE major_id IS NOT NULL")
    Integer dashboardMajorCount();

    @Select("SELECT ROUND(AVG(admission_score))::int FROM admission_fact " +
            "WHERE admission_score IS NOT NULL " +
            "AND EXISTS (SELECT 1 FROM degree_dim deg WHERE admission_fact.degree_id = deg.degree_id AND deg.degree_name LIKE '%学士%')")
    Integer dashboardAvgScore();

    @Select("SELECT COALESCE(deg.degree_name, '未知') AS name, COUNT(*)::int AS count " +
            "FROM admission_fact f LEFT JOIN degree_dim deg ON f.degree_id = deg.degree_id " +
            "GROUP BY deg.degree_name ORDER BY count DESC")
    List<Map<String, Object>> degreeDistribution();
}
