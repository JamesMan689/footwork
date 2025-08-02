package com.footwork.api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanDrillResponse {
    private Long drillId;
    private String drillName;
    private String drillDescription;
    private String drillType;
    private Integer duration;
    private String section;
    private Integer orderIndex;
    private String instructions;
    private String equipment;
} 