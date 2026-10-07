package com.h8.ems.contracts.dto;

public record UpdateCapacityRequest(
        int edBedsFree,
        int icuBedsFree,
        int ventilatorsFree
) {
}
