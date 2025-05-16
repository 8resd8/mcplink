package kr.co.mcplink.domain.schedule.v2.service;

import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.domain.github.dto.ParsedReadmeInfoDto;
import kr.co.mcplink.domain.mcpserver.v2.entity.McpServerV2;
import kr.co.mcplink.domain.mcpserver.v2.entity.McpTagV2;
import kr.co.mcplink.domain.mcpserver.v2.repository.McpServerV2Repository;
import kr.co.mcplink.domain.mcpserver.v2.repository.McpTagV2Repository;
import kr.co.mcplink.global.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataStoreService {

    private final McpServerV2Repository mcpServerV2Repository;
    private final McpTagV2Repository mcpTagV2Repository;

    public String saveMcpServer(GithubMetaDataDto metaData, ParsedReadmeInfoDto parsedReadmeInfo) {
        try {
            McpServerV2 mcpServer = toMcpServerV2(metaData, parsedReadmeInfo);

            if (mcpServer == null) {
                log.warn("Failed to create McpServer from metadata → invalid data");
                return null;
            }

            if (mcpServerV2Repository.existsByUrl(mcpServer.getUrl())) {
                log.warn("McpServer already exists with URL: {} → skip", mcpServer.getUrl());
                return null;
            }

            McpServerV2 savedMcpServer = mcpServerV2Repository.save(mcpServer);
            log.info("Successfully saved new McpServer with URL: {} → ID: {}", mcpServer.getUrl(), savedMcpServer.getId());
            return savedMcpServer.getId();
        } catch (Exception e) {
            log.error("Error saving McpServer for URL {}: {} → {}",
                    metaData.url() != null ? metaData.url() : "unknown",
                    e.getClass().getSimpleName(),
                    e.getMessage());
            return null;
        }
    }

    private McpServerV2 toMcpServerV2(GithubMetaDataDto m, ParsedReadmeInfoDto p) {
        if (m.url() == null || m.stars() == 0 || p.name() == null || p.command() == null || p.args() == null) {
            return null;
        }

        String rawUrl = m.url();
        String prepUrl = rawUrl;

        if (rawUrl.endsWith(".git")) {
            prepUrl = rawUrl.substring(0, rawUrl.length() - 4);
        }

        String pendingSummary = generatePendingSummary(prepUrl);

        return McpServerV2.builder()
                .url(prepUrl)
                .stars(m.stars())
                .official(m.official())
                .scanned(m.scanned())
                .securityRank(m.securityRank())
                .detail(
                        McpServerV2.McpServerDetail.builder()
                                .name(p.name())
                                .description(pendingSummary)
                                .command(p.command())
                                .args(p.args())
                                .env(p.env())
                                .build()
                )
                .build();
    }

    private String generatePendingSummary(String serverUrl) {

        return String.format(
                Constants.DESCRIPTION_NOT_YET_GENERATED,
                serverUrl
        );
    }

    public void updateSummary(String serverId, String summary, List<String> tags) {
        try {
            long updatedCount = mcpServerV2Repository.updateSummary(serverId, summary, tags);

            if (updatedCount > 0) {
                log.info("Updated summary and tags for server: {} → success", serverId);
            } else {
                log.warn("No server found with ID: {} for summary update → skip", serverId);
            }
        } catch (Exception e) {
            log.error("Error updating summary for server {} → {}: {}",
                    serverId,
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    public void saveMcpTag(String tag) {
        try {
            if (mcpTagV2Repository.existsByTag(tag)) {
                log.debug("Tag already exists: {} → skip", tag);
                return;
            }

            McpTagV2 mcpTag = McpTagV2.builder()
                    .tag(tag)
                    .build();

            mcpTagV2Repository.save(mcpTag);
            log.info("Saved new tag: {} → success", tag);
        } catch (DuplicateKeyException e) {
            log.debug("Tag already exists (concurrent insert): {} → skip", tag);
        } catch (Exception e) {
            log.error("Error saving tag {} → {}: {}",
                    tag,
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }
}