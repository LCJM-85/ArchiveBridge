package edu.scau.scauarchiveinsight.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentDTO {
    private Long id;
    private String studentNo;
    private String name;
    private String idCard;
    private String gender;
    private Integer degreeId;
    private Integer provinceId;
    private Integer majorId;
    private Integer classId;
    private LocalDate admissionDate;
    private Boolean graduated;
    private String majorName;
    private String className;
}
