package kr.co.mcplink.domain.schedule.service;

import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.domain.github.dto.ParsedReadmeInfoDto;
import kr.co.mcplink.domain.mcpserverv2.entity.McpServerV2;
import kr.co.mcplink.domain.mcpserverv2.repository.McpServerV2Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataStoreService {

    private final McpServerV2Repository mcpServerV2Repository;

    public String saveMcpServer(GithubMetaDataDto metaData, ParsedReadmeInfoDto parsedReadmeInfo) {
        McpServerV2 mcpServer = toMcpServerV2(metaData, parsedReadmeInfo);

        if (mcpServer == null) {
            return null;
        }

        if (!mcpServerV2Repository.existsByUrl(mcpServer.getUrl())) {
            McpServerV2 savedMcpServer = mcpServerV2Repository.save(mcpServer);

            return savedMcpServer.getId();
        }
        return null;
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

        return McpServerV2.builder()
                .url(prepUrl)
                .stars(m.stars())
                .official(m.official())
                .scanned(m.scanned())
                .securityRank(m.securityRank())
                .detail(
                        McpServerV2.McpServerDetail.builder()
                                .name(p.name())
                                .command(p.command())
                                .args(p.args())
                                .env(p.env())
                                .build()
                )
                .build();
    }
}