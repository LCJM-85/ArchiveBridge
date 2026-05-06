package edu.scau.scauarchiveinsight.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GraduationVO {
    private Long id;
    private String studentNo;
    private String name;
    private String idCard;
    private String gender;
    private Integer degreeId;
    private String degreeName;
    private Integer destId;
    private String destName;
    private Integer fileId;
    private String fileName;
    private LocalDate graduationDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
