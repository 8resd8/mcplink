document.addEventListener('DOMContentLoaded', () => {
    const boardTbody = document.getElementById('board-tbody');
    const writePostButton = document.querySelector('a.cta-button[data-translate="boardWriteButton"]');

    console.log('writePostButton 선택됨:', writePostButton); // 버튼 선택 확인

    if (writePostButton) {
        writePostButton.addEventListener('click', async function(event) { // async 추가
            console.log("글쓰기 버튼 클릭됨"); // 핸들러 실행 확인
            event.preventDefault(); // 먼저 기본 링크 이동 방지

            let isLoggedIn = false;
            // globalUserName 직접 참조 대신 isLoggedIn 값 사용 준비
            console.log("ensureLoginStateKnown 호출 전."); 
            if (typeof window.ensureLoginStateKnown === 'function') {
                try {
                    isLoggedIn = await window.ensureLoginStateKnown();
                    console.log("ensureLoginStateKnown 호출 후, isLoggedIn:", isLoggedIn);
                } catch (error) {
                    console.error("ensureLoginStateKnown 실행 중 오류:", error);
                    alert('로그인 상태 확인 중 오류가 발생했습니다.');
                    return;
                }
            } else {
                console.error("window.ensureLoginStateKnown 함수를 찾을 수 없습니다.");
                // 폴백 시나리오도 globalUserName 직접 참조를 피해야 함
                // 이 경우, 로그인 상태를 알 수 없으므로 보수적으로 로그인 필요 알림
                alert('로그인 상태를 확인할 수 없습니다. 다시 시도해주세요.');
                return;
            }

            if (!isLoggedIn) { 
                alert('로그인이 필요한 서비스입니다.');
                console.log("로그인 필요 알림 후 종료");
            } else {
                console.log("페이지 이동 시도:", this.href);
                window.location.href = this.href; // 로그인 상태이면 페이지 이동
            }
        });
    } else {
        console.error("글쓰기 버튼을 찾을 수 없습니다.");
    }

    async function fetchPosts() {
        try {
            const response = await fetch(`${API_BASE_URL}/v1/posts`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const result = await response.json();

            // API 응답 구조에 맞게 실제 게시글 데이터 배열 가져오기
            const posts = result.data; 

            // 기존 예시 데이터 삭제
            boardTbody.innerHTML = ''; 

            if (posts && posts.length > 0) {
                posts.forEach(post => {
                    const row = boardTbody.insertRow();

                    const cellId = row.insertCell();
                    cellId.textContent = post.postId;

                    const cellTitle = row.insertCell();
                    const titleLink = document.createElement('a');
                    // 상세 페이지 URL로 변경
                    titleLink.href = `post-detail.html?id=${post.postId}`; 
                    titleLink.textContent = post.title;
                    cellTitle.appendChild(titleLink);

                    const cellCreator = row.insertCell();
                    // API 응답에 creator 필드가 있다고 가정
                    cellCreator.textContent = post.creator || 'N/A'; 

                    const cellDate = row.insertCell();
                    // 날짜 형식을 사용자의 현지 시간 기준으로 'YYYY-MM-DD'로 변경
                    if (post.createAt) {
                        let parsableDateString = post.createAt;
                        // 상세 페이지와 동일하게, 시간 문자열이 'T'를 포함하고 'Z'나 오프셋으로 끝나지 않으면 UTC로 가정하고 'Z' 추가
                        if (parsableDateString.includes('T') && !parsableDateString.endsWith('Z') && !parsableDateString.match(/[+-]\d{2}:\d{2}$/)) {
                            parsableDateString += 'Z';
                        }
                        const date = new Date(parsableDateString);
                        const year = date.getFullYear();
                        const month = String(date.getMonth() + 1).padStart(2, '0');
                        const day = String(date.getDate()).padStart(2, '0');
                        cellDate.textContent = `${year}-${month}-${day}`;
                    } else {
                        cellDate.textContent = 'N/A';
                    }
                });
            } else {
                const row = boardTbody.insertRow();
                const cell = row.insertCell();
                cell.colSpan = 4; // 테이블 헤더 컬럼 수에 맞게 조절
                cell.textContent = '게시글이 없습니다.';
                cell.style.textAlign = 'center';
            }
        } catch (error) {
            console.error('Error fetching posts:', error);
            boardTbody.innerHTML = ''; // 오류 발생 시 기존 내용 삭제
            const row = boardTbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 4; // 테이블 헤더 컬럼 수에 맞게 조절
            cell.textContent = '게시글을 불러오는 데 실패했습니다.';
            cell.style.textAlign = 'center';
        }
    }

    fetchPosts();
}); 