package kr.co.mcplink.domain.schedule.kr.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.mcplink.domain.schedule.kr.service.ScheduleKrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule API kr", description = "⚠️ Do Not Use!! Only For OiPKL!! ⚠️")
public class ScheduleKrController {

    private final ScheduleKrService scheduleKrService;

    @PostMapping("/translate")
    public void translateData(int queryNum) {

        scheduleKrService.translateData(queryNum);
    }
}