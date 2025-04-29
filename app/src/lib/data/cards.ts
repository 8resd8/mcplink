// src/lib/data/cards.ts - 카드 더미 데이터

/**
 * 카드 데이터 타입
 */
export type Card = {
  id: number
  title: string
  content: string
}

/**
 * 더미 카드 데이터 목록
 */
export const cardData: Card[] = [
  {
    id: 1,
    title: "설치된 MCP 목록",
    content: "설치된 MCP 목록을 확인할 수 있습니다.",
  },
  {
    id: 2,
    title: "연결된 MCP 서버",
    content: "연결된 MCP 서버 목록을 확인할 수 있습니다.",
  },
  {
    id: 3,
    title: "서버 상태",
    content: "서버 상태를 확인할 수 있습니다.",
  },
  {
    id: 4,
    title: "테스트 자동화",
    content: "테스트 자동화 도구를 도입하여 CI/CD 파이프라인을 개선해야 합니다. 테스트 커버리지를 80% 이상으로 유지합니다.",
  },
  {
    id: 5,
    title: "사용자 피드백",
    content: "베타 테스터들로부터 받은 피드백을 분석하고 우선순위를 정해야 합니다. 다음 스프린트에 반영할 항목을 선별합니다.",
  },
  {
    id: 6,
    title: "배포 계획",
    content: "다음 릴리스 버전의 배포 계획을 수립해야 합니다. 롤백 전략과 모니터링 계획을 포함합니다.",
  },
  {
    id: 7,
    title: "성능 최적화",
    content: "애플리케이션의 성능 병목을 분석하고 최적화 작업을 진행해야 합니다. 로딩 시간을 30% 개선하는 것이 목표입니다.",
  },
  {
    id: 8,
    title: "보안 점검",
    content: "애플리케이션의 보안 취약점을 점검하고 필요한 패치를 적용해야 합니다. OWASP 가이드라인을 준수합니다.",
  },
  {
    id: 9,
    title: "문서화",
    content: "API 문서와 사용자 가이드를 업데이트해야 합니다. 최신 기능과 변경사항을 반영합니다.",
  },
  {
    id: 10,
    title: "팀 회의",
    content: "주간 팀 회의를 진행하고 스프린트 진행 상황을 공유해야 합니다. 이슈와 차단 요소를 논의합니다.",
  },
]

/**
 * 카드 데이터를 가져오는 함수 (실제 API 호출을 시뮬레이션)
 * @returns {Promise<Card[]>} 카드 데이터 배열을 반환하는 Promise
 */
export function getCards(): Promise<Card[]> {
  return new Promise((resolve) => {
    // API 호출 지연 시뮬레이션 (500ms)
    setTimeout(() => {
      resolve(cardData)
    }, 500)
  })
}

/**
 * ID로 특정 카드를 가져오는 함수
 * @param {number} id - 찾을 카드의 ID
 * @returns {Promise<Card|null>} 찾은 카드 또는 null을 반환하는 Promise
 */
export function getCardById(id: number): Promise<Card | null> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const card = cardData.find((card) => card.id === id) || null
      resolve(card)
    }, 300)
  })
}
