package org.example.workload_service.Enum;

import lombok.Getter;

@Getter
public enum MonthEnum {
    JANUARY("Jan"),
    FEBRUARY("Feb"),
    MARCH("Mar"),
    APRIL("Apr"),
    MAY("May"),
    JUNE("Jun"),
    JULY("Jul"),
    AUGUST("Aug"),
    SEPTEMBER("Sep"),
    OCTOBER("Oct"),
    NOVEMBER("Nov"),
    DECEMBER("Dec");

    private final String displayName;

    MonthEnum(String displayName) {
        this.displayName = displayName;
    }

    public static MonthEnum fromInt(int month) {
        return MonthEnum.values()[month - 1];
    }
}