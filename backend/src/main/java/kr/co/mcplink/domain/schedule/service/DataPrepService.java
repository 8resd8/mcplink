package kr.co.mcplink.domain.schedule.service;

import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.domain.github.dto.ParsedReadmeInfoDto;
import kr.co.mcplink.domain.github.service.FetchMetaDataService;
import kr.co.mcplink.domain.github.service.FetchReadmeService;
import kr.co.mcplink.domain.github.service.PrepReadmeService;
import kr.co.mcplink.domain.schedule.entity.GithubPendingQueue;
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
    private final PrepReadmeService prepReadmeService;
    private final EnQueueService enqueueService;
    private final GithubPendingQueueRepository githubRepository;

    public void prepGithub() {
        List<GithubPendingQueue> items = githubRepository.findTop10ByProcessedFalseOrderBySeqAsc();

        for (GithubPendingQueue item : items) {
            String pendingItemId = item.getId();
            githubRepository.updateProcessedById(pendingItemId, true);

            String owner = item.getOwner();
            String repo = item.getRepo();
            if (owner == null || repo == null) {
                log.warn("Missing owner/repo, skip item: {}", item.getId());
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
            }

            String savedServerId = dataStoreService.saveMcpServer(metaData, parsedReadmeInfo);

            if (savedServerId == null) {
                log.warn("McpServer not found for {}/{} <UNK> skip", owner, repo);
            }

            enqueueService.enqueueGemini(savedServerId, prepReadme);
        }
    }
}