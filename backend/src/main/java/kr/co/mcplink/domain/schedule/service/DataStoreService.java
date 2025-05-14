package kr.co.mcplink.domain.schedule.service;

import kr.co.mcplink.domain.github.dto.GithubMetaDataDto;
import kr.co.mcplink.domain.github.model.ParsedReadmeInfo;
import kr.co.mcplink.domain.mcpserverv2.entity.McpServerV2;
import kr.co.mcplink.domain.mcpserverv2.repository.McpServerV2Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataStoreService {

    private final McpServerV2Repository mcpServerV2Repository;

    public String saveMcpServer(GithubMetaDataDto metaData, ParsedReadmeInfo parsedReadmeInfo) {
        McpServerV2 mcpServer = toMcpServerV2(metaData, parsedReadmeInfo);

        if (!mcpServerV2Repository.existsByUrl(mcpServer.getUrl())) {
            McpServerV2 savedMcpServer = mcpServerV2Repository.save(mcpServer);

            return savedMcpServer.getId();
        }
        return null;
    }

    private McpServerV2 toMcpServerV2(GithubMetaDataDto m, ParsedReadmeInfo p) {
        return McpServerV2.builder()
                .url(m.url())
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