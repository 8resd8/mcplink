package kr.co.mcplink.domain.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import kr.co.mcplink.domain.github.dto.GithubSearchResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FetchSearchResultService {

    private static final String SEARCH_PATH = "/search/repositories";
    private static final String[] LANGUAGES = { "typescript", "javascript", "python" };
    private static final String[] LICENSES  = { "mit", "apache-2.0", "gpl-3.0" };

    private final WebClient githubClient;

    public FetchSearchResultService(
            @Qualifier("GithubClient") WebClient githubClient
    ) {
        this.githubClient = githubClient;
    }

    public List<GithubSearchResultDto> fetchSearchResult(int queryNum) {
        List<GithubSearchResultDto> results = new ArrayList<>();
        String query = buildQuery(queryNum);

        for (int page = 1; page <= 10; page++) {
            final int currentPage = page;

            JsonNode root = githubClient.get()
                    .uri(uriBuilder -> {
                        URI uri = uriBuilder
                                .path(SEARCH_PATH)
                                .queryParam("q",        query)
                                .queryParam("sort",     "stars")
                                .queryParam("order",    "desc")
                                .queryParam("per_page", 100)
                                .queryParam("page",     currentPage)
                                .build();

                        log.info("fetchSearchResult queryNum={} page={} → 요청 URL={}",
                                queryNum, currentPage, uri);
                        return uri;
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (root == null || !root.has("items")) {
                break;
            }
            JsonNode items = root.get("items");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {
                String owner = item.get("owner").get("login").asText();
                String repo  = item.get("name").asText();
                results.add(new GithubSearchResultDto(owner, repo));
            }
        }

        return results;
    }

    private String buildQuery(int queryNum) {
        int idx       = queryNum - 1;
        String lang   = LANGUAGES[idx / 3];
        String lic    = LICENSES[idx % 3];
        return String.format(
                "mcp server language:%s license:%s stars:>5",
                lang, lic
        );
    }
}