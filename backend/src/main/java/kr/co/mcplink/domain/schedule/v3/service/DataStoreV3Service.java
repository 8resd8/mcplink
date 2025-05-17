package kr.co.mcplink.domain.schedule.v3.service;

import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.domain.github.dto.ParsedReadmeInfoDto;
import kr.co.mcplink.domain.mcpserver.v3.entity.McpServerV3;
import kr.co.mcplink.domain.mcpserver.v3.entity.McpTagV3;
import kr.co.mcplink.domain.mcpserver.v3.repository.McpServerV3Repository;
import kr.co.mcplink.domain.mcpserver.v3.repository.McpTagV3Repository;
import kr.co.mcplink.global.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataStoreV3Service {

    private final McpServerV3Repository mcpServerV3Repository;
    private final McpTagV3Repository mcpTagV3Repository;

    public String saveMcpServer(GithubMetaDataDto metaData, ParsedReadmeInfoDto parsedReadmeInfo) {
        try {
            McpServerV3 mcpServer = toMcpServerV3(metaData, parsedReadmeInfo);

            if (mcpServer == null) {
                log.warn("Failed to create McpServer from metadata → invalid data");
                return null;
            }

            if (mcpServerV3Repository.existsByUrl(mcpServer.getUrl())) {
                log.warn("McpServer already exists with URL: {} → skip", mcpServer.getUrl());
                return null;
            }

            McpServerV3 savedMcpServer = mcpServerV3Repository.save(mcpServer);
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

    public void updateSummary(String serverId, String summary, List<String> tags) {
        try {
            long updatedCount = mcpServerV3Repository.updateSummary(serverId, summary, tags);

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
            if (mcpTagV3Repository.existsByTag(tag)) {
                log.debug("Tag already exists: {} → skip", tag);
                return;
            }

            McpTagV3 mcpTag = McpTagV3.builder()
                    .tag(tag)
                    .build();

            mcpTagV3Repository.save(mcpTag);
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

    private McpServerV3 toMcpServerV3(GithubMetaDataDto m, ParsedReadmeInfoDto p) {
        if (m.url() == null || m.stars() == 0 || p.name() == null || p.command() == null || p.args() == null) {
            return null;
        }

        String rawUrl = m.url();
        String prepUrl = rawUrl;

        if (rawUrl.endsWith(".git")) {
            prepUrl = rawUrl.substring(0, rawUrl.length() - 4);
        }

        String pendingSummary = generatePendingSummary(prepUrl);

        return McpServerV3.builder()
                .url(prepUrl)
                .stars(m.stars())
                .official(m.official())
                .scanned(m.scanned())
                .securityRank(m.securityRank())
                .detail(
                        McpServerV3.McpServerDetail.builder()
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
}