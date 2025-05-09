package kr.co.mcplink.domain.mcpsecurity.service;

import org.springframework.stereotype.Service;

import kr.co.mcplink.domain.mcpsecurity.dto.McpServerScanResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpScanService {

	private final McpAnalysisService analysisService;

	public void triggerNotionScan() {
		// 스캔할 서버 가져옴
		McpServerScanResultDto result = analysisService.scanSpecificServer();

		if (result.scanSuccess()) {
			log.info("Notion 서버 스캔 성공. JSON 결과 길이: {}", result.osvOutputJson() != null ? result.osvOutputJson().length() : 0);
			// 여기서 result.osvOutputJson()을 파싱하여 추가 작업 수행
			// 예: JSON 파싱 후 DB 저장, 알림 발송 등
			// String json = result.osvOutputJson();
			// ... 파싱 로직 ...
		} else {
			log.error("Notion 서버 스캔 실패.");
			if (result.osvOutputJson() != null && result.osvOutputJson().contains("error")) {
				log.error("실패 원인 (JSON): {}", result.osvOutputJson());
			}
			// 실패 처리 로직
		}
	}
}
