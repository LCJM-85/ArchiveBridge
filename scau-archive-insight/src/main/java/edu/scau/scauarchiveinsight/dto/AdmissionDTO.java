package edu.scau.scauarchiveinsight.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AdmissionDTO {
    private Long id;
    private String studentNo;
    private String name;
    private String idCard;
    private String gender;
    private Integer degreeId;
    private String examNo;
    private Integer provinceId;
    private Integer majorId;
    private Integer admissionScore;
    private LocalDate admissionDate;
}
