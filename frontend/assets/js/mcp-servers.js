document.addEventListener('DOMContentLoaded', () => {
    const serverListTbody = document.getElementById('mcp-server-tbody');
    const paginationControls = document.getElementById('pagination-controls');
    
    let currentPage = 1;
    const itemsPerPage = 10; // 페이지 당 보여줄 아이템 수, API의 size 파라미터와 일치

    // 언어 변경 감지 및 목록 새로고침
    document.addEventListener('languageChanged', () => {
        console.log('Language changed event detected in mcp-servers.js. Reloading list.');
        // 테이블 헤더 등 data-translate 속성을 가진 요소는 script.js에서 이미 처리됨.
        // 여기서는 JavaScript로 동적으로 생성하는 텍스트를 위해 목록을 다시 로드하여
        // fetchMcpServers 내부의 translate 함수가 새 번역을 사용하도록 함.
        if (typeof API_BASE_URL !== 'undefined') {
             fetchMcpServers(currentPage, itemsPerPage);
        } else {
            // API_BASE_URL이 없으면 오류 메시지만 번역된 텍스트로 업데이트 시도
            serverListTbody.innerHTML = `<tr><td colspan="7" class="text-center">${window.translate('apiConfigError')}</td></tr>`;
        }
    });

    async function fetchMcpServers(page, size) {
        try {
            // 이전 버튼/다음 버튼 상태 업데이트를 위해 로딩 시작 시 버튼 비활성화
            if (paginationControls) {
                const prevButton = paginationControls.querySelector('.prev-page');
                const nextButton = paginationControls.querySelector('.next-page');
                if (prevButton) prevButton.disabled = true;
                if (nextButton) nextButton.disabled = true;
            }
            serverListTbody.innerHTML = `<tr><td colspan="7" class="text-center">${window.translate('loadingServers')}</td></tr>`;

            const response = await fetch(`${API_BASE_URL}/v3/mcp/servers/web?page=${page}&size=${size}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const result = await response.json();
            
            renderMcpServers(result.data?.mcpServers || [], page);
            if (result.data && result.data.pageInfo) {
                renderPagination(result.data.pageInfo, page, size);
                currentPage = page; // 현재 페이지 업데이트
            } else {
                if(paginationControls) paginationControls.innerHTML = ''; // 페이지 정보 없으면 페이지네이션 숨김
            }

        } catch (error) {
            console.error('Error fetching MCP servers:', error);
            serverListTbody.innerHTML = '';
            const row = serverListTbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 7;
            cell.textContent = window.translate('errorLoadingServers');
            cell.style.textAlign = 'center';
            if(paginationControls) paginationControls.innerHTML = ''; // 오류 시 페이지네이션 숨김
        }
    }

    function renderMcpServers(servers, requestedPage) {
        serverListTbody.innerHTML = ''; // 기존 내용 삭제

        if (servers && servers.length > 0) {
            servers.forEach((server, index) => {
                const row = serverListTbody.insertRow();

                // 1. 페이지 기준 순차 번호 (클릭 가능하게)
                const cellNo = row.insertCell();
                const itemNumber = (requestedPage - 1) * itemsPerPage + index + 1;
                const noLink = document.createElement('a');
                noLink.href = `server-detail.html?id=${server.id}`;
                noLink.textContent = itemNumber;
                cellNo.appendChild(noLink);
                // cellNo.classList.add('text-center'); // 번호는 보통 중앙 정렬, 부모 td의 text-align으로 처리될 수 있음

                const cellName = row.insertCell();
                const serverLink = document.createElement('a');
                serverLink.href = `server-detail.html?id=${server.id}`;
                serverLink.textContent = server.mcpServer?.name || 'N/A';
                cellName.appendChild(serverLink);

                // 3. 설명 (클릭 가능하게)
                const cellDesc = row.insertCell();
                const descLink = document.createElement('a');
                descLink.href = `server-detail.html?id=${server.id}`;
                descLink.textContent = server.mcpServer?.description || 'N/A';
                cellDesc.appendChild(descLink);

                const cellStars = row.insertCell();
                let starsDisplay = 'N/A';
                if (server.stars !== undefined) {
                    if (server.stars >= 1000000) {
                        starsDisplay = (server.stars / 1000000).toFixed(1).replace(/\.0$/, '') + 'M';
                    } else if (server.stars >= 1000) {
                        starsDisplay = (server.stars / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
                    } else {
                        starsDisplay = server.stars.toString();
                    }
                }
                cellStars.textContent = starsDisplay;
                cellStars.classList.add('text-center');

                const cellScanned = row.insertCell();
                let scannedTitle = '';
                if (server.scanned === true) {
                    scannedTitle = window.translate('scanCompleted', '검사 완료');
                    cellScanned.innerHTML = `<i class="bi bi-check-lg text-success" title="${scannedTitle}"></i>`;
                } else if (server.scanned === false) {
                    scannedTitle = window.translate('scanNotPerformed', '검사 안됨');
                    cellScanned.innerHTML = `<i class="bi bi-x-lg text-danger" title="${scannedTitle}"></i>`;
                } else {
                    scannedTitle = window.translate('scanUnknown', '알 수 없음');
                    cellScanned.innerHTML = `<i class="bi bi-question-lg text-muted" title="${scannedTitle}"></i>`;
                }
                cellScanned.classList.add('text-center');

                const cellOfficial = row.insertCell();
                let officialTitle = '';
                if (server.official === true) {
                    officialTitle = window.translate('officialServer', '공식');
                    cellOfficial.innerHTML = `<i class="bi bi-check-lg text-success" title="${officialTitle}"></i>`;
                } else {
                    officialTitle = window.translate('unofficialServer', '비공식');
                    cellOfficial.innerHTML = `<i class="bi bi-x-lg text-danger" title="${officialTitle}"></i>`;
                }
                cellOfficial.classList.add('text-center');

                const cellSecurityRank = row.insertCell();
                let rankIconHtml = '';
                const rankValue = server.securityRank?.toUpperCase();
                let rankTitle = server.securityRank ? server.securityRank.charAt(0).toUpperCase() + server.securityRank.slice(1).toLowerCase() : 'Unrated';

                switch (rankValue) {
                    case 'UNRATED': rankIconHtml = `<i class="bi bi-question-circle text-muted" title="${window.translate('securityUnrated', rankTitle)}"></i>`; break;
                    case 'LOW': rankIconHtml = `<i class="bi bi-shield-fill-check text-success" title="${window.translate('securityLow', rankTitle)}"></i>`; break;
                    case 'MODERATE': rankIconHtml = `<i class="bi bi-shield-fill-exclamation text-warning" title="${window.translate('securityModerate', rankTitle)}"></i>`; break;
                    case 'HIGH': rankIconHtml = `<i class="bi bi-shield-fill-x text-danger" title="${window.translate('securityHigh', rankTitle)}"></i>`; break;
                    case 'CRITICAL': 
                        rankIconHtml = `<i class="bi bi-exclamation-triangle-fill text-danger" title="${window.translate('securityCritical', rankTitle)}"></i>`;
                        break;
                    default: rankIconHtml = `<i class="bi bi-question-circle text-muted" title="${window.translate('securityUnrated', 'Unrated')}"></i>`; break;
                }
                cellSecurityRank.innerHTML = rankIconHtml;
                cellSecurityRank.classList.add('text-center');
            });
        } else {
            const row = serverListTbody.insertRow();
            const cell = row.insertCell();
            cell.colSpan = 7;
            cell.textContent = window.translate('noServersAvailable');
            cell.style.textAlign = 'center';
        }
    }

    function renderPagination(pageInfo, requestedPage, requestedSize) {
        if (!paginationControls) return;
        paginationControls.innerHTML = ''; 

        const { totalItems, hasNextPage } = pageInfo;
        const totalPages = Math.ceil(totalItems / requestedSize);

        if (totalPages <= 1 && !hasNextPage && requestedPage === 1 && totalItems <= requestedSize) {
            return; // 아이템이 없거나 한 페이지만 있고, 다음 페이지도 없으면 페이지네이션 X
        }

        const ul = document.createElement('ul');
        ul.className = 'pagination justify-content-center';

        // 이전 페이지 버튼
        const prevLi = document.createElement('li');
        prevLi.className = 'page-item';
        const prevLink = document.createElement('a');
        prevLink.className = 'page-link prev-page';
        prevLink.href = '#';
        prevLink.setAttribute('aria-label', window.translate('previousPage', 'Previous'));
        prevLink.innerHTML = '<i class="bi bi-chevron-double-left"></i>';
        if (requestedPage === 1) {
            prevLi.classList.add('disabled');
        } else {
            prevLink.addEventListener('click', (e) => {
                e.preventDefault();
                fetchMcpServers(requestedPage - 1, requestedSize);
            });
        }
        prevLi.appendChild(prevLink);
        ul.appendChild(prevLi);

        // 페이지 번호 버튼 생성
        const maxPageButtonsToShow = 5; // 한 번에 보여줄 최대 페이지 번호 버튼 수
        let startPage, endPage;

        if (totalPages <= maxPageButtonsToShow) {
            // 전체 페이지 수가 최대 표시 버튼 수보다 작거나 같으면 모든 페이지 번호 표시
            startPage = 1;
            endPage = totalPages;
        } else {
            // 현재 페이지를 중심으로 페이지 번호 표시
            const maxPagesBeforeCurrent = Math.floor(maxPageButtonsToShow / 2);
            const maxPagesAfterCurrent = Math.ceil(maxPageButtonsToShow / 2) - 1;

            if (requestedPage <= maxPagesBeforeCurrent) {
                startPage = 1;
                endPage = maxPageButtonsToShow;
            } else if (requestedPage + maxPagesAfterCurrent >= totalPages) {
                startPage = totalPages - maxPageButtonsToShow + 1;
                endPage = totalPages;
            } else {
                startPage = requestedPage - maxPagesBeforeCurrent;
                endPage = requestedPage + maxPagesAfterCurrent;
            }
        }
        
        // 시작 페이지가 1보다 클 경우 "처음" 버튼 또는 "..." 추가 (선택적)
        if (startPage > 1) {
            const firstPageLi = document.createElement('li');
            firstPageLi.className = 'page-item';
            const firstPageLink = document.createElement('a');
            firstPageLink.className = 'page-link';
            firstPageLink.href = '#';
            firstPageLink.textContent = '1';
            firstPageLink.setAttribute('aria-label', window.translate('gotoPage', 'Go to page') + ' 1');
            firstPageLink.addEventListener('click', (e) => {
                e.preventDefault();
                fetchMcpServers(1, requestedSize);
            });
            firstPageLi.appendChild(firstPageLink);
            ul.appendChild(firstPageLi);
            if (startPage > 2) { // "..." 추가
                const ellipsisLi = document.createElement('li');
                ellipsisLi.className = 'page-item disabled';
                ellipsisLi.innerHTML = '<span class="page-link">...</span>';
                ul.appendChild(ellipsisLi);
            }
        }


        for (let i = startPage; i <= endPage; i++) {
            const pageLi = document.createElement('li');
            pageLi.className = 'page-item';
            const pageLink = document.createElement('a');
            pageLink.className = 'page-link';
            pageLink.href = '#';
            pageLink.textContent = i;
            pageLink.setAttribute('aria-label', window.translate('gotoPage', 'Go to page') + ` ${i}`);

            if (i === requestedPage) {
                pageLi.classList.add('active');
                pageLink.setAttribute('aria-current', 'page');
            } else {
                pageLink.addEventListener('click', (e) => {
                    e.preventDefault();
                    fetchMcpServers(i, requestedSize);
                });
            }
            pageLi.appendChild(pageLink);
            ul.appendChild(pageLi);
        }

        // 마지막 페이지가 endPage보다 클 경우 "..." 또는 "마지막" 버튼 추가 (선택적)
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) { // "..." 추가
                 const ellipsisLi = document.createElement('li');
                 ellipsisLi.className = 'page-item disabled';
                 ellipsisLi.innerHTML = '<span class="page-link">...</span>';
                 ul.appendChild(ellipsisLi);
            }
            const lastPageLi = document.createElement('li');
            lastPageLi.className = 'page-item';
            const lastPageLink = document.createElement('a');
            lastPageLink.className = 'page-link';
            lastPageLink.href = '#';
            lastPageLink.textContent = totalPages;
            lastPageLink.setAttribute('aria-label', window.translate('gotoPage', 'Go to page') + ` ${totalPages}`);
            lastPageLink.addEventListener('click', (e) => {
                e.preventDefault();
                fetchMcpServers(totalPages, requestedSize);
            });
            lastPageLi.appendChild(lastPageLink);
            ul.appendChild(lastPageLi);
        }


        // 다음 페이지 버튼
        const nextLi = document.createElement('li');
        nextLi.className = 'page-item';
        const nextLink = document.createElement('a');
        nextLink.className = 'page-link next-page';
        nextLink.href = '#';
        nextLink.setAttribute('aria-label', window.translate('nextPage', 'Next'));
        nextLink.innerHTML = '<i class="bi bi-chevron-double-right"></i>';
        if (!hasNextPage && requestedPage >= totalPages) { // requestedPage >= totalPages 조건 추가
            nextLi.classList.add('disabled');
        } else {
            nextLink.addEventListener('click', (e) => {
                e.preventDefault();
                fetchMcpServers(requestedPage + 1, requestedSize);
            });
        }
        nextLi.appendChild(nextLink);
        ul.appendChild(nextLi);

        paginationControls.appendChild(ul);
    }

    // 초기 로드: 첫 페이지 데이터 가져오기
    if (typeof API_BASE_URL !== 'undefined') {
        fetchMcpServers(currentPage, itemsPerPage);
    } else {
        console.error("API_BASE_URL is not defined. Cannot fetch MCP servers.");
        serverListTbody.innerHTML = `<tr><td colspan="7" class="text-center">${window.translate('apiConfigError')}</td></tr>`;
    }
}); 