package kr.co.mcplink.domain.schedule.kr.service;

import kr.co.mcplink.domain.gemini.service.FetchSynonymService;
import kr.co.mcplink.domain.mcpserver.v3.repository.McpTagV3Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleKrService {

    private final FetchSynonymService fetchSynonymService;
    private final DataStoreKrService dataStoreKrService;
    private final McpTagV3Repository mcpTagV3Repository;

    public void translateData(int queryNum) {
        List<String> tags = mcpTagV3Repository.findTagsByPage(queryNum);

        if (tags == null || tags.isEmpty()) {
            log.info("No tags found for page {}", queryNum);
            return;
        }

        List<String> errorNameList = new ArrayList<>();
        int forbiddenCnt = 3;
        int processedCount = 0;

        long[] executionTimes = new long[10];
        int timeIndex = 0;
        int executionsInLastMinute = 0;

        for (String tag : tags) {
            long currentTime = System.currentTimeMillis();

            executionsInLastMinute = 0;
            for (int t = 0; t < 10; t++) {
                if (executionTimes[t] > 0 && currentTime - executionTimes[t] < 60000) {
                    executionsInLastMinute++;
                }
            }

            if (executionsInLastMinute >= 10) {
                long oldestExecution = Long.MAX_VALUE;
                for (int t = 0; t < 10; t++) {
                    if (executionTimes[t] > 0 && executionTimes[t] < oldestExecution) {
                        oldestExecution = executionTimes[t];
                    }
                }

                long waitTime = (oldestExecution + 60000) - currentTime;
                if (waitTime > 0) {
                    try {
                        String sleepTimeStr = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        log.info("⏰⏰⏰⏰⏰ {} || Synonym sleep - batch: {} ⏰⏰⏰⏰⏰",
                                sleepTimeStr, processedCount);

                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            List<String> synonyms;
            try {
                synonyms = fetchSynonymService.fetchSynonyms(tag);

                executionTimes[timeIndex] = System.currentTimeMillis();
                timeIndex = (timeIndex + 1) % 10;
                processedCount++;
            } catch (WebClientResponseException.Forbidden e) {
                log.error("☢️☢️☢️☢️☢️ 403 Forbidden error for {} ☢️☢️☢️☢️☢️", tag);
                forbiddenCnt--;
                if (forbiddenCnt == 0) {
                    break;
                }
                continue;
            }

            if (synonyms == null) {
                log.warn("☢️☢️☢️☢️☢️ Failed to translate for {} ☢️☢️☢️☢️☢️", tag);
                errorNameList.add(tag);
                continue;
            }

            dataStoreKrService.saveSynonymMapping(synonyms);
            log.info("Successfully translated for {}", tag);

            if (processedCount % 10 == 0) {
                String currentTimeStr = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                log.info("✅✅✅✅✅ {} || Synonym batch: {}, remaining: {} ✅✅✅✅✅",
                        currentTimeStr, processedCount, tags.size() - processedCount);
            }
        }

        if (!errorNameList.isEmpty()) {
            log.warn("⚠️⚠️⚠️⚠️⚠️ Processed with errors for following items: {} ⚠️⚠️⚠️⚠️⚠️", errorNameList);
        }

        String finishTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("☑️☑️☑️☑️☑️ {} || Synonym total processed: {} ☑️☑️☑️☑️☑️",
                finishTime, processedCount);
    }
}