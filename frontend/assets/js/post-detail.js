document.addEventListener('DOMContentLoaded', () => {
    const postTitleEl = document.getElementById('post-title');
    const postCreatorEl = document.getElementById('post-creator');
    const postCreateAtEl = document.getElementById('post-createAt');
    const postUpdatedAtEl = document.getElementById('post-updatedAt');
    const postContentEl = document.getElementById('post-content');
    const postLikeCountEl = document.getElementById('post-likeCount');

    // HTML 문서의 <title> 요소
    const pageTitleEl = document.querySelector('title');

    async function fetchPostDetail() {
        const params = new URLSearchParams(window.location.search);
        const postId = params.get('id');

        if (!postId) {
            postTitleEl.textContent = '잘못된 접근입니다.';
            postContentEl.innerHTML = '<p>게시글 ID가 제공되지 않았습니다. 목록으로 돌아가 올바른 게시글을 선택해주세요.</p>';
            if(pageTitleEl) pageTitleEl.textContent = '오류 - mcplink';
            // 다른 메타 정보들은 숨기거나 초기화
            if (postCreatorEl) postCreatorEl.style.display = 'none';
            if (postCreateAtEl) postCreateAtEl.style.display = 'none';
            if (postUpdatedAtEl) postUpdatedAtEl.style.display = 'none';
            return;
        }

        try {
            const response = await fetch(`${API_BASE_URL}/v1/posts/${postId}`);
            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error('Post not found');
                } else {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
            }
            const result = await response.json();
            // console.log('API 응답 전체:', JSON.stringify(result, null, 2)); // 임시 로그 제거
            const post = result.data; // API 응답 구조 변경에 따라 수정

            if (post && typeof post.postId !== 'undefined') { // postId 존재 여부로 객체 유효성 한번 더 체크
                postTitleEl.textContent = post.title;
                postCreatorEl.textContent = `작성자: ${post.creator || 'N/A'}`;

                // 날짜 및 시간 형식 함수
                const formatDateTime = (dateString) => {
                    if (!dateString) return 'N/A';

                    let parsableDateString = dateString;
                    // 문자열이 'T'를 포함하고, 'Z'나 시간대 오프셋(+HH:MM 또는 -HH:MM)으로 끝나지 않으면 UTC로 가정하고 'Z'를 추가
                    if (parsableDateString.includes('T') && !parsableDateString.endsWith('Z') && !parsableDateString.match(/[+-]\d{2}:\d{2}$/)) {
                        parsableDateString += 'Z';
                    }

                    const date = new Date(parsableDateString);
                    
                    return `${date.getFullYear()}년 ${String(date.getMonth() + 1).padStart(2, '0')}월 ${String(date.getDate()).padStart(2, '0')}일 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
                };

                postCreateAtEl.textContent = `작성: ${formatDateTime(post.createAt)}`;
                
                if (post.updateAt && post.createAt !== post.updateAt) {
                    postUpdatedAtEl.textContent = ` | 수정: ${formatDateTime(post.updateAt)}`;
                    postUpdatedAtEl.style.display = 'inline'; // 숨겨진 수정일 표시
                } else {
                    postUpdatedAtEl.style.display = 'none'; // 수정일이 없거나 생성일과 같으면 숨김
                }

                if (postLikeCountEl && typeof post.likeCount === 'number') {
                    postLikeCountEl.innerHTML = ` | <i class="bi bi-heart-fill"></i> ${post.likeCount}`;
                } else if (postLikeCountEl) {
                    postLikeCountEl.innerHTML = ` | <i class="bi bi-heart-fill"></i> 0`; // 기본값 또는 N/A 처리
                }
                
                // 게시글 내용이 HTML일 수 있으므로 innerHTML 사용 (주의: XSS 방어 필요 시 DOMPurify 등 라이브러리 사용 고려)
                postContentEl.innerHTML = post.content || '<p>내용이 없습니다.</p>'; 

                // 필요하다면 여기에 '좋아요 수' 등 다른 정보도 표시할 수 있습니다.
                // 예: const postLikeCountEl = document.getElementById('post-likeCount');
                // if(postLikeCountEl && post.likeCount !== undefined) postLikeCountEl.textContent = `좋아요: ${post.likeCount}`;

            } else {
                throw new Error('Post data is not available');
            }

        } catch (error) {
            console.error('Error fetching post detail:', error);
            if(pageTitleEl) pageTitleEl.textContent = '게시글 로드 실패 - mcplink';
            postTitleEl.textContent = '게시글을 불러올 수 없습니다.';
            if (error.message === 'Post not found') {
                postContentEl.innerHTML = '<p>요청하신 게시글을 찾을 수 없습니다. 삭제되었거나 잘못된 ID일 수 있습니다.</p>';
            } else {
                postContentEl.innerHTML = '<p>게시글을 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</p>';
            }
            // 다른 메타 정보들은 숨기거나 초기화
            if (postCreatorEl) postCreatorEl.style.display = 'none';
            if (postCreateAtEl) postCreateAtEl.style.display = 'none';
            if (postUpdatedAtEl) postUpdatedAtEl.style.display = 'none';
        }
    }

    fetchPostDetail();
}); 