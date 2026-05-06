package edu.scau.scauarchiveinsight.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentVO {
    private Long id;
    private String studentNo;
    private String name;
    private String idCard;
    private String gender;
    private Integer provinceId;
    private String provinceName;
    private Integer majorId;
    private String majorName;
    private Integer classId;
    private String className;
    private Integer fileId;
    private String fileName;
    private LocalDate admissionDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
