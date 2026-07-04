package com.vidyapeet.performance;

import com.vidyapeet.performance.dto.PerformanceSummary;
import com.vidyapeet.performance.dto.StudentPerformanceRow;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/student/performance")
    @PreAuthorize("hasRole('STUDENT')")
    public PerformanceSummary myPerformance() {
        return performanceService.myPerformance();
    }

    @GetMapping("/admin/performance")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public List<StudentPerformanceRow> instituteOverview() {
        return performanceService.instituteOverview();
    }

    @GetMapping("/admin/students/{id}/performance")
    @PreAuthorize("hasRole('INSTITUTE_ADMIN')")
    public PerformanceSummary studentPerformance(@PathVariable Long id) {
        return performanceService.studentPerformance(id);
    }
}
