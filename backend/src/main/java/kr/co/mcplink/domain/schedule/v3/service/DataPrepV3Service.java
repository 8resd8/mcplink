package kr.co.mcplink.domain.schedule.v3.service;

import kr.co.mcplink.domain.gemini.service.FetchSummaryService;
import kr.co.mcplink.domain.gemini.service.FetchTagService;
import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.domain.github.dto.ParsedReadmeInfoDto;
import kr.co.mcplink.domain.github.service.FetchMetaDataService;
import kr.co.mcplink.domain.github.service.FetchReadmeService;
import kr.co.mcplink.domain.github.service.PrepReadmeService;
import kr.co.mcplink.domain.schedule.v3.entity.GeminiPendingQueueV3;
import kr.co.mcplink.domain.schedule.v3.entity.GithubPendingQueueV3;
import kr.co.mcplink.domain.schedule.v3.repository.GeminiPendingQueueV3Repository;
import kr.co.mcplink.domain.schedule.v3.repository.GithubPendingQueueV3Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataPrepV3Service {

    private final DataStoreV3Service dataStoreService;
    private final EnQueueV3Service enqueueService;
    private final FetchMetaDataService fetchMetaDataService;
    private final FetchReadmeService fetchReadmeService;
    private final FetchSummaryService fetchSummaryService;
    private final FetchTagService fetchTagService;
    private final PrepReadmeService prepReadmeService;
    private final GithubPendingQueueV3Repository githubRepository;
    private final GeminiPendingQueueV3Repository geminiRepository;

    public void prepGithub() {
        List<GithubPendingQueueV3> items = githubRepository.findTop10ByProcessedFalseOrderBySeqAsc();
        if (items == null) {
            log.info("No pending items in Github queue");
            return;
        }

        for (GithubPendingQueueV3 item : items) {
            String pendingItemId = item.getId();

            try {
                String owner = item.getOwner();
                String repo = item.getRepo();
                if (owner == null || repo == null) {
                    log.warn("Invalid Github pending item → {}", pendingItemId);
                    githubRepository.updateProcessedById(pendingItemId, true);
                    continue;
                }

                String rawReadme = fetchReadmeService.fetchReadme(owner, repo);
                if (rawReadme == null) {
                    log.warn("No README fetched for {}/{} → skip", owner, repo);
                    githubRepository.updateProcessedById(pendingItemId, true);
                    continue;
                }

                String prepReadme = prepReadmeService.decodeReadme(rawReadme);
                if (prepReadme == null) {
                    log.warn("Failed to decode README for {}/{} → skip", owner, repo);
                    githubRepository.updateProcessedById(pendingItemId, true);
                    continue;
                }

                ParsedReadmeInfoDto parsedReadmeInfo = prepReadmeService.parseReadme(prepReadme);
                if (parsedReadmeInfo == null) {
                    log.warn("README parsing failed for {}/{} → skip", owner, repo);
                    githubRepository.updateProcessedById(pendingItemId, true);
                    continue;
                }

                log.info("Parsed README info for {}/{} → {}", owner, repo, parsedReadmeInfo);

                GithubMetaDataDto metaData = fetchMetaDataService.fetchMetaData(owner, repo);
                if (metaData == null) {
                    log.warn("Metadata not found for {}/{} → skip", owner, repo);
                    githubRepository.updateProcessedById(pendingItemId, true);
                    continue;
                }

                String savedServerId = dataStoreService.saveMcpServer(metaData, parsedReadmeInfo);
                if (savedServerId == null) {
                    log.warn("McpServer not found for {}/{} → skip", owner, repo);
                    continue;
                }

                String savedServerName = parsedReadmeInfo.name();
                githubRepository.updateProcessedById(pendingItemId, true);
                enqueueService.enqueueGemini(savedServerId, savedServerName, prepReadme);
                log.info("Successfully processed and enqueued Gemini task for {}/{}", owner, repo);
            } catch (Exception e) {
                log.error("Error processing Github item {} → {}", pendingItemId, e.getMessage(), e);
            }
        }
    }

    public void prepGemini(String updateId) {
        Optional<GeminiPendingQueueV3> itemOpt;

        if (updateId != null && updateId.startsWith("{\"_id\":")) {
            updateId = updateId.replaceAll("\\{\"_id\":\\s*\"([^\"]+)\"\\}", "$1");
        }

        if (updateId == null) {
            itemOpt = geminiRepository.findTop1ByProcessedFalseOrderBySeqAsc();
        } else {
            itemOpt = geminiRepository.findByServerId(updateId);
        }

        if (itemOpt.isEmpty()) {
            log.info("No pending items in Gemini queue");
            return;
        }

        GeminiPendingQueueV3 item = itemOpt.get();
        String pendingItemId = item.getId();
        String serverId = item.getServerId();
        String serverName = item.getServerName();
        String prepReadme = item.getPrepReadme();

        if (serverId == null || prepReadme == null || prepReadme.isEmpty()) {
            log.warn("Invalid Gemini pending item → {}", pendingItemId);
            geminiRepository.updateProcessedById(pendingItemId, true);
            return;
        }

        try {
            String readmeSummary = fetchSummaryService.fetchSummary(prepReadme, serverId);
            if (readmeSummary == null) {
                log.warn("Failed to generate summary for serverId → {}", serverId);
                return;
            }

            log.info("Generated summary for serverId: {} → Summary: '{}'", serverId, readmeSummary);

            List<String> generatedTags = fetchTagService.fetchTags(serverName);
            if (generatedTags == null || generatedTags.isEmpty()) {
                log.warn("Failed to generate tags for serverId → {}", serverId);
                return;
            }

            dataStoreService.updateSummary(serverId, readmeSummary, generatedTags);
            for (String generatedTag : generatedTags) {
                dataStoreService.saveMcpTag(generatedTag);
            }
            geminiRepository.updateProcessedById(pendingItemId, true);
            log.info("Successfully processed for {}", serverId);
        } catch (Exception e) {
            log.error("Error processing Gemini item {} → {}", pendingItemId, e.getMessage(), e);
        }
    }
}