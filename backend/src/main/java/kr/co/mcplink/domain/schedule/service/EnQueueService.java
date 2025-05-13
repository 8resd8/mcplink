package kr.co.mcplink.domain.schedule.service;

import kr.co.mcplink.domain.github.dto.GithubSearchResultDto;
import kr.co.mcplink.domain.github.service.FetchSearchResultService;
import kr.co.mcplink.domain.schedule.entity.GithubPendingQueue;
import kr.co.mcplink.domain.schedule.repository.GithubPendingQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnQueueService {

    private final FetchSearchResultService fetchSearchResultService;
    private final GithubPendingQueueRepository queueRepository;

    public void enqueue(int queryNum) {
        List<GithubSearchResultDto> results = fetchSearchResultService.fetchSearchResult(queryNum);
        for (GithubSearchResultDto dto : results) {
            String owner = dto.owner();
            String repo  = dto.repo();
            String name  = owner + "|" + repo;

            if (!queueRepository.existsByName(name)) {
                GithubPendingQueue entity = GithubPendingQueue.builder()
                        .name(name)
                        .owner(owner)
                        .repo(repo)
                        .build();

                queueRepository.save(entity);
            }
        }
    }
}
