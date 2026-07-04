package com.vidyapeet.attempt;

import com.vidyapeet.attempt.dto.ResultResponse;
import com.vidyapeet.attempt.dto.StartedTestResponse;
import com.vidyapeet.attempt.dto.StudentTestSummary;
import com.vidyapeet.attempt.dto.SubmitAttemptRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentTestController {

    private final TakeTestService takeTestService;

    public StudentTestController(TakeTestService takeTestService) {
        this.takeTestService = takeTestService;
    }

    @GetMapping("/tests")
    public List<StudentTestSummary> myTests() {
        return takeTestService.listStudentTests();
    }

    @PostMapping("/tests/{testId}/start")
    public StartedTestResponse start(@PathVariable Long testId) {
        return takeTestService.start(testId);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResultResponse submit(
            @PathVariable Long attemptId, @Valid @RequestBody SubmitAttemptRequest request) {
        return takeTestService.submit(attemptId, request);
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ResultResponse resultByAttempt(@PathVariable Long attemptId) {
        return takeTestService.getResultByAttempt(attemptId);
    }

    @GetMapping("/tests/{testId}/result")
    public ResultResponse resultForTest(@PathVariable Long testId) {
        return takeTestService.getResultForTest(testId);
    }
}
