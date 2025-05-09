package kr.co.mcplink.domain.mcpsecurity.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import kr.co.mcplink.domain.mcpsecurity.dto.McpScanResultDto;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class McpAnalysisService {

	private Path baseTempDir;
	private String osvScannerCommand;

	@Value("${app.analysis.temp-dir}")
	private String tempDirStr;
	@Value("${app.analysis.osv-scanner-cmd}")
	private String scannerCmd;

	@PostConstruct
	public void init() {
		baseTempDir = Paths.get(tempDirStr);
		try {
			Files.createDirectories(baseTempDir);
		} catch (IOException e) {
			log.error("임시 분석 디렉토리 생성 실패: {}", baseTempDir, e);
			throw new RuntimeException("임시 분석 디렉토리 생성 실패", e);
		}
	}

	public McpAnalysisService() {
		this.osvScannerCommand = "osv-scanner";
	}

	/**
	 * 모든 몽고DB에 있는 Git URL OSV 스캔, 현재는 노션 하드코딩
	 * @return McpServerScanResultDto 스캔 결과를 담은 DTO
	 */
	public McpScanResultDto scanSpecificServer() {
		String gitUrl = "https://github.com/makenotion/notion-mcp-server.git";
		String serverId = "notion-mcp-server-id"; // 식별을 위한 임의의 ID
		String serverName = "Notion MCP Server";    // 식별을 위한 임의의 이름

		Path cloneDir = baseTempDir.resolve("notion-mcp-server");
		Path reportOutputFile = baseTempDir.resolve("notion_report.json");

		log.info("지정된 URL 스캔 시작: {}, 클론 위치: {}, 리포트 파일: {}", gitUrl, cloneDir, reportOutputFile);
		McpScanResultDto result = performScanForUrl(gitUrl, serverId, serverName, cloneDir);

		if (result.scanSuccess() && result.osvOutputJson() != null) {
			try {
				Files.writeString(reportOutputFile, result.osvOutputJson(), StandardCharsets.UTF_8);
				log.info("OSV-Scanner JSON 출력을 파일에 저장했습니다: {}", reportOutputFile);
			} catch (IOException e) {
				log.error("OSV-Scanner JSON 출력을 파일에 저장 중 오류 발생 {}: {}", reportOutputFile, e.getMessage(), e);
			}
		}
		return result;
	}

	/**
	 * 주어진 Git URL에 대해 클론, OSV 스캔, 정리를 수행하는 내부 메소드.
	 * @param cloneDir 스캔을 위해 리포지토리를 클론할 대상 디렉토리
	 */
	private McpScanResultDto performScanForUrl(String gitUrl, String serverId, String serverName, Path cloneDir) {
		Path targetScanDir = cloneDir; // 이 URL은 루트 디렉토리 스캔

		String osvJsonOutput = null;
		boolean success = false;

		try {
			// 1. 클론 대상 디렉토리 생성
			Files.createDirectories(cloneDir);

			// 2. 클론
			log.info("'{}' 클론 시작...", gitUrl);
			if (!cloneRepository(gitUrl, cloneDir)) {
				log.error("Git 리포지토리 클론 실패: {}", gitUrl);

				return new McpScanResultDto(serverId, serverName, gitUrl, false, null);
			}

			// 3. OSV 스캔
			log.info("'{}' 디렉토리에 OSV-Scanner 실행...", targetScanDir);
			osvJsonOutput = runOsvScanner(targetScanDir);

			// osv-scanner 자체에서 오류 JSON을 반환했는지 확인
			if (osvJsonOutput != null && osvJsonOutput.trim().startsWith("{\"error\":")) {
				log.warn("OSV-Scanner 실행 중 오류 감지됨. 출력: {}", osvJsonOutput);
				// 실패 DTO 반환, osvOutputJson에는 scanner가 반환한 에러 JSON 포함
				return new McpScanResultDto(serverId, serverName, gitUrl, false, osvJsonOutput);
			} else if (osvJsonOutput == null) {
				log.error("OSV-Scanner 실행 후 null 출력을 받았습니다.");
				// 실패 DTO 반환 (osvOutputJson은 null)
				return new McpScanResultDto(serverId, serverName, gitUrl, false, null);
			}

			log.info("'{}' OSV-Scanner 실행 완료.", targetScanDir);
			success = true;
		} catch (IOException | InterruptedException e) {
			log.error("'{}' (URL: '{}') 스캔 중 오류 발생", serverName, gitUrl, e);
			Thread.currentThread().interrupt();
			// 예외 발생 시 실패 DTO 반환 (osvOutputJson은 null)
			return new McpScanResultDto(serverId, serverName, gitUrl, false, null);
		} finally {
			cleanup(cloneDir);
		}

		// 최종 결과 DTO 반환
		return new McpScanResultDto(serverId, serverName, gitUrl, success, osvJsonOutput);
	}

	private boolean cloneRepository(String gitUrl, Path targetDir) throws IOException, InterruptedException {
		// 터미널 명령어 실행, 커밋기록 안가져오는 clone (얕은 클론)
		// git clone --depth 1 https://github.com/makenotion/notion-mcp-server.git /app/analysis_temp/notion-mcp-server
		ProcessBuilder processBuilder = new ProcessBuilder("git", "clone", "--depth", "1", gitUrl,
			targetDir.toString());
		processBuilder.redirectErrorStream(true);

		log.info("Git 클론 실행: {}", String.join(" ", processBuilder.command()));
		Process process = processBuilder.start();

		StringBuilder gitOutput = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				gitOutput.append(line).append(System.lineSeparator());
				log.trace("GIT CLONE: {}", line);
			}
		}
		log.info("Git 클론 결과 ({}):\n{}", gitUrl, gitOutput);

		boolean finished = process.waitFor(5, TimeUnit.MINUTES); // 타임아웃 5분
		if (!finished) {
			process.destroyForcibly();
			log.error("Git 클론 타임아웃: {}", gitUrl);
			return false;
		}
		int exitCode = process.exitValue();
		log.info("Git 클론 ({}): 종료 코드 {}", gitUrl, exitCode);
		return exitCode == 0;
	}

	private String runOsvScanner(Path projectDir) throws IOException, InterruptedException {
		// 명령어 순서: osv-scanner scan source --format json -r .
		ProcessBuilder processBuilder = new ProcessBuilder(
			osvScannerCommand, "scan", "source", "--format", "json", "-r", "."
		);
		processBuilder.directory(projectDir.toFile()); // 작업 디렉토리 설정
		processBuilder.redirectErrorStream(true);
		log.info("OSV-Scanner 실행: {} (작업폴더: {})", String.join(" ", processBuilder.command()), projectDir);
		Process process = processBuilder.start();

		StringBuilder output = new StringBuilder();
		// 외부 프로세스 출력은 시스템 기본 인코딩 또는 UTF-8 사용 시도
		try (BufferedReader reader = new BufferedReader(
			new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append(System.lineSeparator());
			}
		}

		boolean finished = process.waitFor(10, TimeUnit.MINUTES); // 타임아웃 10분
		int exitCode = -1;
		if (!finished) {
			process.destroyForcibly();
			log.error("OSV-Scanner 타임아웃: {}", projectDir);
			// 타임아웃 시 에러 JSON 반환
			return String.format("{\"error\": \"OSV-Scanner 실행 시간 초과 (경로: %s)\"}",
				projectDir.toString().replace("\\", "\\\\"));
		}
		exitCode = process.exitValue();

		log.info("OSV-Scanner ({}): 종료 코드 {}. 출력 길이: {}", projectDir, exitCode, output.length());
		// 종료 코드가 0(성공) 또는 1(취약점 발견)이 아니면서 출력이 비어있다면 문제로 간주
		if (output.toString().trim().isEmpty() && exitCode != 0 && exitCode != 1) {
			log.warn("OSV-Scanner ({}) 종료 코드 {}와 함께 비어있는 출력을 반환했습니다.", projectDir, exitCode);
			// 비어있는 출력 대신 에러 JSON 반환
			return String.format("{\"error\": \"OSV-Scanner가 종료 코드 %d와 함께 비어있는 출력을 반환했습니다. (경로: %s)\"}", exitCode,
				projectDir.toString().replace("\\", "\\\\"));
		}
		// 정상적인 JSON 출력 또는 scanner 자체의 에러 JSON을 반환
		return output.toString();
	}

	private void cleanup(Path dir) {
		if (dir != null && Files.exists(dir)) {
			log.info("임시 디렉토리 삭제 시도: {}", dir);
			try {
				Files.walk(dir)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(file -> {
						if (!file.delete()) {
							log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
							// Windows에서는 파일 잠금으로 즉시 삭제되지 않을 수 있음
							// 필요하다면 deleteOnExit() 사용 고려 또는 재시도 로직 추가
							// file.deleteOnExit();
						}
					});
				if (Files.exists(dir) && !dir.toFile().delete()) {
					log.warn("최종 디렉토리 삭제 실패: {}. 수동 삭제가 필요할 수 있습니다.", dir);
				} else if (!Files.exists(dir)) {
					log.info("임시 디렉토리 삭제 완료: {}", dir);
				}
			} catch (IOException e) {
				log.error("임시 디렉토리 삭제 중 오류 발생 {}: {}", dir, e.getMessage(), e);
			}
		}
	}
}