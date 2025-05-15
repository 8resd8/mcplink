package kr.co.mcplink.domain.schedule.service;

import kr.co.mcplink.domain.gemini.dto.ReadmeSummaryDto;
import kr.co.mcplink.domain.gemini.service.FetchSummaryService;
import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.domain.github.dto.ParsedReadmeInfoDto;
import kr.co.mcplink.domain.github.service.FetchMetaDataService;
import kr.co.mcplink.domain.github.service.FetchReadmeService;
import kr.co.mcplink.domain.github.service.PrepReadmeService;
import kr.co.mcplink.domain.schedule.entity.GeminiPendingQueue;
import kr.co.mcplink.domain.schedule.entity.GithubPendingQueue;
import kr.co.mcplink.domain.schedule.repository.GeminiPendingQueueRepository;
import kr.co.mcplink.domain.schedule.repository.GithubPendingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataPrepService {

    private final DataStoreService dataStoreService;
    private final FetchReadmeService fetchReadmeService;
    private final FetchMetaDataService fetchMetaDataService;
    private final FetchSummaryService fetchSummaryService;
    private final PrepReadmeService prepReadmeService;
    private final EnQueueService enqueueService;
    private final GithubPendingQueueRepository githubRepository;
    private final GeminiPendingQueueRepository geminiRepository;

    public void prepGithub() {
        List<GithubPendingQueue> items = githubRepository.findTop10ByProcessedFalseOrderBySeqAsc();
        if (items == null) {
            log.info("No pending items in Github queue");
            return;
        }

        for (GithubPendingQueue item : items) {
            String pendingItemId = item.getId();
            githubRepository.updateProcessedById(pendingItemId, true);

            try {
                String owner = item.getOwner();
                String repo = item.getRepo();
                if (owner == null || repo == null) {
                    log.warn("Invalid Github pending item → {}", pendingItemId);
                    continue;
                }

                String rawReadme = fetchReadmeService.fetchReadme(owner, repo);
                if (rawReadme == null) {
                    log.warn("No README fetched for {}/{} → skip", owner, repo);
                    continue;
                }

                String prepReadme = prepReadmeService.decodeReadme(rawReadme);
                if (prepReadme == null) {
                    log.warn("Failed to decode README for {}/{} → skip", owner, repo);
                    continue;
                }

                ParsedReadmeInfoDto parsedReadmeInfo = prepReadmeService.parseReadme(prepReadme);
                if (parsedReadmeInfo == null) {
                    log.warn("README parsing failed for {}/{} → skip", owner, repo);
                    continue;
                }

                log.info("Parsed README info for {}/{} → {}", owner, repo, parsedReadmeInfo);

                GithubMetaDataDto metaData = fetchMetaDataService.fetchMetaData(owner, repo);
                if (metaData == null) {
                    log.warn("Metadata not found for {}/{} → skip", owner, repo);
                    continue;
                }

                String savedServerId = dataStoreService.saveMcpServer(metaData, parsedReadmeInfo);
                if (savedServerId == null) {
                    log.warn("McpServer not found for {}/{} → skip", owner, repo);
                    continue;
                }

                enqueueService.enqueueGemini(savedServerId, prepReadme);
                log.info("Successfully processed and enqueued Gemini task for {}/{}", owner, repo);
            } catch (Exception e) {
                log.error("Error processing Github item {} → {}", pendingItemId, e.getMessage(), e);
            }
        }
    }

    public void prepGemini() {
        GeminiPendingQueue item = geminiRepository.findTop1ByProcessedFalseOrderBySeqAsc();
        if (item == null) {
            log.info("No pending items in Gemini queue");
            return;
        }

        String pendingItemId = item.getId();
//        geminiRepository.updateProcessedById(pendingItemId, true);

        String serverId = item.getServerId();
        String prepReadme = item.getPrepReadme();

        if (serverId == null || prepReadme == null || prepReadme.isEmpty()) {
            log.warn("Invalid Gemini pending item → {}", pendingItemId);
            return;
        }

        try {
            ReadmeSummaryDto readmeSummary = fetchSummaryService.fetchSummary(prepReadme);
            if (readmeSummary == null) {
                log.warn("Failed to generate summary for serverId → {}", serverId);
                return;
            }

            if (readmeSummary.summary() == null || readmeSummary.summary().isEmpty()) {
                log.warn("Empty summary generated for serverId → {}", serverId);
                return;
            }

            log.info("Generated summary for serverId: {} → Summary: '{}', Tags: {}",
                    serverId, readmeSummary.summary(), readmeSummary.tags());

            // data save

            log.info("Successfully processed for {}", serverId);
        } catch (Exception e) {
            log.error("Error processing Gemini item {} → {}", pendingItemId, e.getMessage(), e);
        }
    }
}