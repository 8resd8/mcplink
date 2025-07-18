package kr.co.mcplink.domain.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import kr.co.mcplink.domain.github.dto.GithubSearchResultDto;
import kr.co.mcplink.global.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class FetchSearchResultService {

    private final WebClient githubClient;

    public FetchSearchResultService(
            @Qualifier("githubClient") WebClient githubClient
    ) {
        this.githubClient = githubClient;
    }

    public List<GithubSearchResultDto> fetchSearchResult(int queryNum1, Integer queryNum2) {
        int subNum = (queryNum2 == null) ? 1 : queryNum2;
        String query = buildQuery(queryNum1, subNum);

        return IntStream.rangeClosed(1, 10)
                .mapToObj(page -> {
                    String logUrl = Constants.GITHUB_SEARCH_PATH + "?q=" + query + "&sort=stars&order=desc&per_page=100&page=" + page;
                    log.info("fetchSearchResult queryNum1={} queryNum2={} page={} → 요청 URL={}",
                            queryNum1, subNum, page, logUrl);

                    return githubClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path(Constants.GITHUB_SEARCH_PATH)
                                    .queryParam("q", query)
                                    .queryParam("sort", "stars")
                                    .queryParam("order", "desc")
                                    .queryParam("per_page", 100)
                                    .queryParam("page", page)
                                    .build())
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();
                })

                .takeWhile(root -> root != null
                        && root.has("items")
                        && root.get("items").isArray()
                        && !root.get("items").isEmpty())
                .flatMap(root -> StreamSupport.stream(root.get("items").spliterator(), false))
                .map(item -> new GithubSearchResultDto(
                        item.get("owner").get("login").asText(),
                        item.get("name").asText()
                ))
                .collect(Collectors.toList());
    }

    private String buildQuery(int queryNum, int subNum) {
        int idx       = queryNum - 1;
        String lang   = Constants.GITHUB_LANGUAGES[idx / 3];
        String lic    = Constants.GITHUB_LICENSES[idx % 3];

        String starsCondition = switch (subNum) {
            case 1 -> "stars:>5";
            case 2 -> "stars:1..5";
            case 3 -> "stars:0";
            default -> {
                log.warn("Invalid subNum provided: {}. Defaulting to stars:>5", subNum);
                yield "stars:>5";
            }
        };

        return String.format(
                "mcp server language:%s license:%s %s",
                lang, lic, starsCondition
        );
    }
}