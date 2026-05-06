package edu.scau.scauarchiveinsight.dto;

import lombok.Data;

@Data
public class MetaDataDTO {
    private String fieldCode;
    private String fieldName;
    private String fieldType;
    private String sourceField;
    private String transformType;
    private String transformRule;
    private Boolean isRequired;
}
