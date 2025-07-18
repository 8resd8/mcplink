package kr.co.mcplink.domain.schedule.v3.service;

import kr.co.mcplink.domain.mcpserver.v3.repository.McpServerV3Repository;
import kr.co.mcplink.domain.schedule.v3.repository.GeminiPendingQueueV3Repository;
import kr.co.mcplink.domain.schedule.v3.repository.GithubPendingQueueV3Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleV3Service {

    private final EnQueueV3Service enQueueService;
    private final DataPrepV3Service dataPrepService;
    private final McpServerV3Repository mcpServerV3Repository;
    private final GithubPendingQueueV3Repository githubRepository;
    private final GeminiPendingQueueV3Repository geminiRepository;

    public void initData() {

        try {
            for (int i = 1; i <= 9; i++) {
                for (int j = 1; j <= 3; j++) {

                    enQueueService.enqueueGithub(i, j);

                    String currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    log.info("✅✅✅✅✅ {} || query1: {}, query2: {} ✅✅✅✅✅",
                            currentTime, i, j);

                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            String finishTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("☑️☑️☑️☑️☑️ {} || FINISH ☑️☑️☑️☑️☑️", finishTime);
        } catch (Exception e) {

            String errorTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.error("❌❌❌❌❌ {} || ERROR: {} ❌❌❌❌❌",
                    errorTime, e.getMessage(), e);
        }
    }

    public void prepData() {

        int githubBatchCount = 0;
        int geminiBatchCount = 0;
        long pendingGithubTasksCount = 0;
        long pendingGeminiTasksCount = 0;

        try {
            github:
            for (int i = 1; i <= 50; i++) {
                for (int j = 1; j <= 10; j++) {

                    pendingGithubTasksCount = githubRepository.countByProcessedFalse();

                    if (pendingGithubTasksCount == 0)
                        break github;

                    dataPrepService.prepGithub();
                    githubBatchCount++;
                }

                String currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                log.info("✅✅✅✅✅ {} || batch: {}, pending: {} ✅✅✅✅✅",
                        currentTime, githubBatchCount, pendingGithubTasksCount);
            }

            String finishTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("☑️☑️☑️☑️☑️ {} || total: {} ☑️☑️☑️☑️☑️",
                    finishTime, githubBatchCount);
        } catch (Exception e) {

            String errorTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.error("❌❌❌❌❌ {} || ERROR: {} ❌❌❌❌❌",
                    errorTime, e.getMessage(), e);
        }

        try {
            long[] executionTimes = new long[5];
            int timeIndex = 0;
            int executionsInLastMinute = 0;

            gemini:
            for (int i = 1; i <= 500; i++) {
                pendingGeminiTasksCount = geminiRepository.countByProcessedFalse();

                if (pendingGeminiTasksCount == 0)
                    break gemini;

                long currentTime = System.currentTimeMillis();

                executionsInLastMinute = 0;
                for (int t = 0; t < 5; t++) {
                    if (executionTimes[t] > 0 && currentTime - executionTimes[t] < 60000) {
                        executionsInLastMinute++;
                    }
                }

                if (executionsInLastMinute >= 5) {
                    long oldestExecution = Long.MAX_VALUE;
                    for (int t = 0; t < 5; t++) {
                        if (executionTimes[t] > 0 && executionTimes[t] < oldestExecution) {
                            oldestExecution = executionTimes[t];
                        }
                    }

                    long waitTime = (oldestExecution + 60000) - currentTime;
                    if (waitTime > 0) {
                        try {
                            String sleepTimeStr = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            log.info("⏰⏰⏰⏰⏰ {} || Gemini sleep - batch: {} ⏰⏰⏰⏰⏰",
                                    sleepTimeStr, geminiBatchCount);

                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break gemini;
                        }
                    }
                }

                dataPrepService.prepGemini(null);
                geminiBatchCount++;

                executionTimes[timeIndex] = System.currentTimeMillis();
                timeIndex = (timeIndex + 1) % 5;

                if (geminiBatchCount % 10 == 0) {
                    String currentTimeStr = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    log.info("✅✅✅✅✅ {} || Gemini batch: {}, pending: {} ✅✅✅✅✅",
                            currentTimeStr, geminiBatchCount, pendingGeminiTasksCount);
                }
            }

            String finishTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("☑️☑️☑️☑️☑️ {} || Gemini total: {} ☑️☑️☑️☑️☑️",
                    finishTime, geminiBatchCount);
        } catch (Exception e) {

            String errorTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.error("❌❌❌❌❌ {} || Gemini ERROR: {} ❌❌❌❌❌",
                    errorTime, e.getMessage(), e);
        }
    }

    public void updateData() {

        int geminiBatchCount = 0;

        List<String> updateIds = mcpServerV3Repository.findIdsByDetailDescriptionContaining("We apologize for the inconvenience");
        String updateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        if (!updateIds.isEmpty()) {
            log.info("⭐⭐⭐⭐⭐ {} || Update List: {} ⭐⭐⭐⭐⭐",
                    updateTime, updateIds);
        } else {
            log.info("⭐⭐⭐⭐⭐ {} || No List !! ⭐⭐⭐⭐⭐",
                    updateTime);
        }

        try {
            long[] executionTimes = new long[5];
            int timeIndex = 0;
            int executionsInLastMinute = 0;

            gemini:
            for (String updateId : updateIds) {
                long currentTime = System.currentTimeMillis();
                executionsInLastMinute = 0;

                for (int t = 0; t < 5; t++) {
                    if (executionTimes[t] > 0 && currentTime - executionTimes[t] < 60000) {
                        executionsInLastMinute++;
                    }
                }

                if (executionsInLastMinute >= 5) {
                    long oldestExecution = Long.MAX_VALUE;
                    for (int t = 0; t < 5; t++) {
                        if (executionTimes[t] > 0 && executionTimes[t] < oldestExecution) {
                            oldestExecution = executionTimes[t];
                        }
                    }

                    long waitTime = (oldestExecution + 60000) - currentTime;
                    if (waitTime > 0) {
                        try {
                            String sleepTimeStr = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            log.info("⏰⏰⏰⏰⏰ {} || Gemini sleep - batch: {} ⏰⏰⏰⏰⏰",
                                    sleepTimeStr, geminiBatchCount);

                            Thread.sleep(waitTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break gemini;
                        }
                    }
                }

                dataPrepService.prepGemini(updateId);
                geminiBatchCount++;

                executionTimes[timeIndex] = System.currentTimeMillis();
                timeIndex = (timeIndex + 1) % 5;

                String currentTimeStr = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                log.info("✅✅✅✅✅ {} || Gemini batch: {}, updateId: {} ✅✅✅✅✅",
                        currentTimeStr, geminiBatchCount, updateId);
            }

            String finishTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("☑️☑️☑️☑️☑️ {} || Gemini total: {} ☑️☑️☑️☑️☑️",
                    finishTime, geminiBatchCount);
        } catch (Exception e) {

            String errorTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.error("❌❌❌❌❌ {} || Gemini ERROR: {} ❌❌❌❌❌",
                    errorTime, e.getMessage(), e);
        }
    }
}