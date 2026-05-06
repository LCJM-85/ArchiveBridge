package edu.scau.scauarchiveinsight.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class GraduationDTO {
    private Long id;
    private String studentNo;
    private String name;
    private String idCard;
    private String gender;
    private Integer degreeId;
    private Integer destId;
    private LocalDate graduationDate;
}
