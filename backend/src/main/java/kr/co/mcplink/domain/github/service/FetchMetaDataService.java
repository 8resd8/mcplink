package kr.co.mcplink.domain.github.service;

import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.global.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
public class FetchMetaDataService {

    private final WebClient githubClient;

    public FetchMetaDataService(
            @Qualifier("githubClient") WebClient githubClient
    ) {
        this.githubClient = githubClient;
    }

    public GithubMetaDataDto fetchMetaData(String owner, String repo) {
        try {
            return githubClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(Constants.GITHUB_REPO_PATH)
                            .build(owner, repo))
                    .retrieve()
                    .bodyToMono(GithubMetaDataDto.class)
                    .block();

        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Repository not found for {}/{}", owner, repo);
            return null;

        } catch (WebClientResponseException ex) {
            log.error("Failed to fetch metadata for {}/{}: {}", owner, repo, ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected error fetching metadata for {}/{}", owner, repo, ex);
            throw ex;
        }
    }
}