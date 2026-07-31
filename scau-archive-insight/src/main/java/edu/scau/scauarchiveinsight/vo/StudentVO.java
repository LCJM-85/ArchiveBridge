package edu.scau.scauarchiveinsight.vo;

import edu.scau.scauarchiveinsight.util.DesensitizeUtil;
import edu.scau.scauarchiveinsight.util.Sensitive;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentVO {
    private Long id;
    private String studentNo;
    @Sensitive(DesensitizeUtil.SensitiveType.NAME)
    private String name;
    @Sensitive(DesensitizeUtil.SensitiveType.ID_CARD)
    private String idCard;
    private String gender;
    private Integer degreeId;
    private String degreeName;
    private Integer provinceId;
    private String provinceName;
    private Integer majorId;
    private String majorName;
    private Integer classId;
    private String className;
    private Boolean graduated;
    private Integer fileId;
    private String fileName;
    private LocalDate admissionDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
