package kr.co.mcplink.domain.schedule.v2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.mcplink.domain.schedule.v2.service.DataPrepV2Service;
import kr.co.mcplink.domain.schedule.v2.service.EnQueueV2Service;
import kr.co.mcplink.domain.schedule.v2.service.ScheduleV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule API v2", description = "⚠️ Do Not Use!! Only For OiPKL!! ⚠️")
public class ScheduleV2Controller {

    private final EnQueueV2Service enqueueService;
    private final DataPrepV2Service dataPrepService;
    private final ScheduleV2Service scheduleService;

    @Deprecated
    @Operation(deprecated = true)
    @PostMapping("/enqueue/github")
    public void enqueueGithub(@RequestParam int queryNum1, @RequestParam int queryNum2) {

        enqueueService.enqueueGithub(queryNum1, queryNum2);
    }

    @Deprecated
    @Operation(deprecated = true)
    @PostMapping("/prep/github")
    public void prepGithub() {

        dataPrepService.prepGithub();
    }

    @Deprecated
    @Operation(deprecated = true)
    @PostMapping("/prep/gemini")
    public void prepGemini() {

        dataPrepService.prepGemini(null);
    }

    @Deprecated
    @Operation(deprecated = true)
    @PostMapping("/prep/data")
    public void schedule() {

        scheduleService.prepData();
    }

    @Deprecated
    @Operation(deprecated = true)
    @PostMapping("/prep/data/update")
    public void scheduleUpdate() {

        scheduleService.updateData();
    }
}