package kr.co.mcplink.domain.schedule.v3.service;

import kr.co.mcplink.domain.github.dto.GithubSearchResultDto;
import kr.co.mcplink.domain.github.service.FetchSearchResultService;
import kr.co.mcplink.domain.schedule.v3.entity.GeminiPendingQueueV3;
import kr.co.mcplink.domain.schedule.v3.entity.GithubPendingQueueV3;
import kr.co.mcplink.domain.schedule.v3.repository.GeminiPendingQueueV3Repository;
import kr.co.mcplink.domain.schedule.v3.repository.GithubPendingQueueV3Repository;
import kr.co.mcplink.global.annotation.ExcludeParamLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnQueueV3Service {

    private final FetchSearchResultService fetchSearchResultService;
    private final GithubPendingQueueV3Repository githubRepository;
    private final GeminiPendingQueueV3Repository geminiRepository;

    public void enqueueGithub(int queryNum1, int queryNum2) {
        List<GithubSearchResultDto> results = fetchSearchResultService.fetchSearchResult(queryNum1, queryNum2);
        for (GithubSearchResultDto dto : results) {
            String owner = dto.owner();
            String repo  = dto.repo();
            String name  = owner + "|" + repo;

            if (!githubRepository.existsByName(name)) {
                GithubPendingQueueV3 entity = GithubPendingQueueV3.builder()
                        .name(name)
                        .owner(owner)
                        .repo(repo)
                        .build();

                githubRepository.save(entity);
            }
        }
    }

    @ExcludeParamLog
    public void enqueueGemini(String serverId, String serverName, String prepReadme) {
        if (!geminiRepository.existsByServerId(serverId)) {
            GeminiPendingQueueV3 entity = GeminiPendingQueueV3.builder()
                    .serverId(serverId)
                    .serverName(serverName)
                    .prepReadme(prepReadme)
                    .build();

            geminiRepository.save(entity);
        }
    }
}