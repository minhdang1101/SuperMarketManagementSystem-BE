package fu.se.smms.service;

import fu.se.smms.dto.DashboardSummaryDTO;


public interface DashboardService {

    DashboardSummaryDTO getSummary(String period);
}
