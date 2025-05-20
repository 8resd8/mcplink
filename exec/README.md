# 포팅 매뉴얼

## 1. 소스 클론 이후 참고 문서

[소스코드 클론 이후 참고 문서.pdf](깃랩 클론이후 가이드.pdf)


## 2. 외부 서비스 정보

### Gemini API

- GitHub API 수집하고 있는 README.md 파일 요약해서 description 필드 생성
- 수집한 mcp server name으로 관련있는 태그를 생성하는 데에 사용
  - e.g) "google-map-mcp" → "google map" "google" "map" "구글 맵" "구글 지도" "구글" "맵" "지도"

### GitHub API

- OWNER REPO 정보 수집
- README Base64 디코딩 
- 코드블럭들 중 mcp server 설치 JSON 형태인지 패턴 검사 
- 통과한 OWNER/REPO들의 설치 명령어 수집 (name, command, args, env)
- 통과한 OWNER/REPO들의 메타데이터 수집 (클론 URL, Stars 개수) 


### Social Login - SSAFY
- SSAFY Super App Application 등록 요청 후 사용
- 2025.05.20 기준 오프라인 신청만 가능
- 게시판 글 작성 하는 데 사용하기 위해 사용


## 3. DB 덤프 파일 최신본

[DB_dump.zip](mcp_servers.zip)

## 4. 시연 시나리오


### 시연자

- **김동욱**


