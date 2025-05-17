package kr.co.mcplink.domain.schedule.v3.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.mcplink.domain.schedule.v3.service.ScheduleV3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule API v3", description = "⚠️ Do Not Use!! Only For OiPKL!! ⚠️")
public class ScheduleV3Controller {

    private final ScheduleV3Service scheduleService;

    @PostMapping("/init")
    public void initData() {

        scheduleService.initData();
    }

    @PostMapping("/prep")
    public void prepData() {

        scheduleService.prepData();
    }

    @PostMapping("/update")
    public void updateData() {

        scheduleService.updateData();
    }
}
