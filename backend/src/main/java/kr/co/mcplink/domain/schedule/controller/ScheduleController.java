package kr.co.mcplink.domain.schedule.controller;

import kr.co.mcplink.domain.schedule.service.DataPrepService;
import kr.co.mcplink.domain.schedule.service.EnQueueService;
import kr.co.mcplink.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final EnQueueService enqueueService;
    private final DataPrepService dataPrepService;
    private final ScheduleService scheduleService;

    @PostMapping("/enqueue/github")
    public void enqueueGithub(@RequestParam int queryNum) {

        enqueueService.enqueueGithub(queryNum);
    }

    @PostMapping("/prep/github")
    public void prepGithub() {

        dataPrepService.prepGithub();
    }

    @PostMapping("/prep/gemini")
    public void prepGemini() {

        dataPrepService.prepGemini();
    }

    @PostMapping("/prep/data")
    public void schedule() {

        scheduleService.prepData();
    }
}