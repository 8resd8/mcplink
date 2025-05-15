package kr.co.mcplink.domain.github.service;

import kr.co.mcplink.domain.github.dto.GithubReadmeDto;
import kr.co.mcplink.global.annotation.ExcludeResponseLog;
import kr.co.mcplink.global.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
public class FetchReadmeService {

    private final WebClient githubClient;

    public FetchReadmeService(
            @Qualifier("githubClient") WebClient githubClient
    ) {
        this.githubClient = githubClient;
    }

    @ExcludeResponseLog
    public String fetchReadme(String owner, String repo) {
        try {
            GithubReadmeDto readmeDto = githubClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(Constants.GITHUB_README_PATH)
                            .build(owner, repo))
                    .retrieve()
                    .bodyToMono(GithubReadmeDto.class)
                    .block();

            return (readmeDto != null) ? readmeDto.content() : null;

        } catch (WebClientResponseException.NotFound ex) {
            log.warn("README not found for {}/{}", owner, repo);
            return null;

        } catch (WebClientResponseException ex) {
            log.error("Failed to fetch README for {}/{}: {}", owner, repo, ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected error fetching README for {}/{}", owner, repo, ex);
            throw ex;
        }
    }
}
