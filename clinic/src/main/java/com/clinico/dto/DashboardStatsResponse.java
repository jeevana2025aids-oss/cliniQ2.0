package com.clinico.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private long totalDoctors;
    private long totalPatients;
    private long totalSlots;
    private long availableSlots;
    private long bookedSlots;
    private long todayAppointments;
    private long totalAppointments;
}
