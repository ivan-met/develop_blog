package met.ivan.devblog.controller;

import met.ivan.devblog.dto.PlatformStatsResponse;
import met.ivan.devblog.service.AdminStatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    /**
     * GET /api/admin/stats
     * Returns aggregated platform statistics.
     */
    @GetMapping
    public PlatformStatsResponse getStats() {
        return adminStatsService.getStats();
    }
}
