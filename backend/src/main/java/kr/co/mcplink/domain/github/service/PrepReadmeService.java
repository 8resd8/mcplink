package kr.co.mcplink.domain.github.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.mcplink.domain.github.dto.ParsedReadmeInfoDto;
import kr.co.mcplink.global.annotation.ExcludeResponseLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrepReadmeService {

    @ExcludeResponseLog
    public String decodeReadme(String rawReadme) {
        if (rawReadme == null) {
            log.warn("Raw README is null, skip decoding");
            return null;
        }

        try {
            String normalized = rawReadme.replaceAll("\\r?\\n", "");
            byte[] decodedBytes = Base64.getDecoder().decode(normalized);

            return new String(decodedBytes, StandardCharsets.UTF_8);

        } catch (IllegalArgumentException ex) {
            log.error("Failed to Base64-decode README", ex);
            return null;

        } catch (Exception ex) {
            log.error("Unexpected error during README decoding", ex);
            return null;
        }
    }

    public ParsedReadmeInfoDto parseReadme(String prepReadme) {
        // 1. 마크다운/HTML 코드 블록 추출
        Pattern codeBlockPat = Pattern.compile(
                "(?:```(?:json)?\\s*([\\s\\S]*?)```)|(?:<pre><code[^>]*>([\\s\\S]*?)</code></pre>)",
                Pattern.DOTALL
        );
        Matcher codeBlockM = codeBlockPat.matcher(prepReadme);

        // 2. JSON 파서 설정
        ObjectMapper mapper = createConfiguredMapper();

        // 3. 각 코드 블록 처리
        while (codeBlockM.find()) {
            String codeBlockContent = codeBlockM.group(1) != null
                    ? codeBlockM.group(1)
                    : codeBlockM.group(2);

            try {
                // 4. JSON 노드 파싱 시도
                JsonNode rootNode = parseJsonContent(codeBlockContent, mapper);
                if (rootNode == null) continue;

                // 5. 표준 MCP 구성 처리 (mcpServers 구조)
                ParsedReadmeInfoDto mcpConfig = processMcpServersStructure(rootNode);
                if (mcpConfig != null) {
                    return mcpConfig;
                }

                // 6. 단순 구성 처리 (직접 command/args가 있는 경우)
                ParsedReadmeInfoDto directConfig = processDirectConfig(rootNode);
                if (directConfig != null) {
                    return directConfig;
                }
            } catch (Exception e) {
                log.warn("parseReadme: Exception during parsing: {}", e.getMessage());
            }
        }

        // 7. 파싱 실패 처리
        log.warn("parseReadme: No valid configuration found in README");
        return null;
    }

    /**
     * Jackson ObjectMapper를 설정하여 생성
     */
    private ObjectMapper createConfiguredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        return mapper;
    }

    /**
     * 코드 블록 내용에서 JsonNode 파싱 시도
     */
    private JsonNode parseJsonContent(String codeBlockContent, ObjectMapper mapper) {
        try {
            return mapper.readTree(codeBlockContent);
        } catch (Exception e) {
            Pattern jsonObjectPat = Pattern.compile("\\{[\\s\\S]*?\\}(?=[,;]?\\s*$)", Pattern.DOTALL);
            Matcher jsonObjectM = jsonObjectPat.matcher(codeBlockContent);

            if (jsonObjectM.find()) {
                String jsonPart = jsonObjectM.group(0);
                try {
                    return mapper.readTree(jsonPart);
                } catch (Exception ex) {
                    log.warn("parseReadme: Failed to extract JSON object from JavaScript: {}", ex.getMessage());
                    return null;
                }
            } else {
                log.warn("parseReadme: No JSON object found in code block");
                return null;
            }
        }
    }

    /**
     * 표준 MCP 구성 처리 (mcpServers 구조)
     */
    private ParsedReadmeInfoDto processMcpServersStructure(JsonNode rootNode) {
        if (!rootNode.has("mcpServers") || !rootNode.get("mcpServers").isObject()) {
            return null;
        }

        JsonNode mcpServers = rootNode.get("mcpServers");
        Iterator<Map.Entry<String, JsonNode>> serverEntries = mcpServers.fields();

        while (serverEntries.hasNext()) {
            Map.Entry<String, JsonNode> entry = serverEntries.next();
            String serverName = entry.getKey();
            JsonNode serverConfig = entry.getValue();

            // "mcpServers"는 직접 name으로 사용하지 않음
            if ("mcpServers".equals(serverName)) {
                continue;
            }

            // command 필드 확인
            if (!isValidCommand(serverConfig)) {
                continue;
            }

            // args 배열 확인
            List<String> args = extractArgs(serverConfig);
            if (args == null) {
                log.warn("parseReadme: Invalid args for server '{}'", serverName);
                continue;
            }

            // env 객체 처리
            Map<String, String> env = extractEnv(serverConfig);

            // 유효한 구성 생성 및 반환
            ParsedReadmeInfoDto info = buildInfoObject(serverName,
                    serverConfig.get("command").asText(),
                    args,
                    env);
            log.info("parseReadme: successfully parsed → {}", info);
            return info;
        }

        return null;
    }

    /**
     * 단순 구성 처리 (직접 command/args가 있는 경우)
     */
    private ParsedReadmeInfoDto processDirectConfig(JsonNode rootNode) {
        if (!isValidCommand(rootNode)) {
            return null;
        }

        // args 배열 확인
        List<String> args = extractArgs(rootNode);
        if (args == null) {
            log.warn("parseReadme: Invalid args for direct config");
            return null;
        }

        // env 객체 처리
        Map<String, String> env = extractEnv(rootNode);

        // 유효한 구성 생성 및 반환
        ParsedReadmeInfoDto info = buildInfoObject("default",
                rootNode.get("command").asText(),
                args,
                env);
        log.info("parseReadme: successfully parsed (direct config) → {}", info);
        return info;
    }

    /**
     * command 필드가 유효한지 확인
     */
    private boolean isValidCommand(JsonNode node) {
        if (!node.has("command") || !node.get("command").isTextual()) {
            return false;
        }

        String command = node.get("command").asText();
        if (!("npx".equals(command) || "uvx".equals(command))) {
            log.warn("parseReadme: 'command' value is not 'npx' or 'uvx': {}", command);
            return false;
        }

        return true;
    }

    /**
     * args 배열 추출
     */
    private List<String> extractArgs(JsonNode node) {
        if (!node.has("args") || !node.get("args").isArray()) {
            log.warn("parseReadme: 'args' array not found or invalid");
            return null;
        }

        JsonNode argsNode = node.get("args");
        List<String> args = new ArrayList<>();

        for (JsonNode argNode : argsNode) {
            if (argNode.isTextual()) {
                args.add(argNode.asText());
            } else {
                log.warn("parseReadme: Non-text argument found in 'args' array");
            }
        }

        if (args.isEmpty()) {
            log.warn("parseReadme: 'args' array was empty");
            return null;
        }

        return args;
    }

    /**
     * env 객체 추출
     */
    private Map<String, String> extractEnv(JsonNode node) {
        if (!node.has("env") || !node.get("env").isObject()) {
            return null;
        }

        Map<String, String> env = new HashMap<String, String>() {
            @Override
            public String toString() {
                if (isEmpty()) return "{}";

                StringBuilder sb = new StringBuilder("{");
                boolean first = true;

                for (Map.Entry<String, String> entry : entrySet()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;

                    sb.append("\"").append(entry.getKey()).append("\": \"")
                            .append(entry.getValue().replace("\"", "\\\"")).append("\"");
                }

                sb.append("}");
                return sb.toString();
            }
        };

        JsonNode envNode = node.get("env");
        Iterator<Map.Entry<String, JsonNode>> envEntries = envNode.fields();

        while (envEntries.hasNext()) {
            Map.Entry<String, JsonNode> entry = envEntries.next();
            String key = entry.getKey();
            JsonNode valueNode = entry.getValue();

            if (valueNode.isTextual()) {
                env.put(key, valueNode.asText());
            }
        }

        return env;
    }

    /**
     * ParsedReadmeInfo 객체 생성
     */
    private ParsedReadmeInfoDto buildInfoObject(String name, String command, List<String> args, Map<String, String> env) {
        return ParsedReadmeInfoDto.builder()
                .name(name)
                .command(command)
                .args(args)
                .env(env)
                .build();
    }
}