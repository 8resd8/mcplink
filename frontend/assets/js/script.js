// script.js

document.addEventListener('DOMContentLoaded', function () {
    // 번역 데이터는 이제 JSON 파일에서 직접 로드됩니다.
    window.currentTranslations = {}; // 로드된 번역 데이터를 저장할 변수 (전역으로 변경)
    let globalUserName = null; // 사용자 이름을 저장할 변수 (또는 localStorage 사용)
    let userInfoPromise = null; // Promise를 저장할 변수

    function updateLoginState(isLoggedIn, userName = null) {
        const loginButton = document.getElementById('login-button');
        if (!loginButton) {
            console.error("updateLoginState: 로그인 버튼을 찾을 수 없습니다.");
            return;
        }

        // 이전 이벤트 리스너 제거
        if (loginButton.__handler) {
            loginButton.removeEventListener('click', loginButton.__handler);
        }

        if (isLoggedIn) {
            globalUserName = userName; 
            console.log("로그인 상태로 UI 업데이트. 사용자명:", userName);

            // 아이콘 제거, navLogout 키로 번역된 텍스트 사용
            loginButton.innerHTML = `
                ${window.translate('navLogout', '로그아웃')} 
                ${userName ? `(${userName})` : ''}
            `;
            loginButton.__handler = handleLogout;
            loginButton.addEventListener('click', handleLogout);
        } else {
            globalUserName = null; 
            console.log("로그아웃 상태로 UI 업데이트.");

            // 아이콘 제거, navLogin 키로 번역된 텍스트 사용
            loginButton.innerHTML = window.translate('navLogin', '로그인');
            const loginHandler = function() {
                console.log("로그인 버튼 클릭됨. SSAFY 인증 시작...");
                const clientId = '64da588a-f264-4a90-924c-7cb326bf9d3a';
                const redirectUri = encodeURIComponent(`${API_BASE_URL}/v1/auth/ssafy/callback`);
                const responseType = 'code';
                const scope = encodeURIComponent('email name'); 
                const oauthUrl = `https://project.ssafy.com/oauth/sso-check?response_type=${responseType}&client_id=${clientId}&redirect_uri=${redirectUri}&scope=${scope}`;
                window.location.href = oauthUrl;
            };
            loginButton.__handler = loginHandler;
            loginButton.addEventListener('click', loginHandler);
        }
    }

    async function actualFetchUserInfo() { 
        console.log("사용자 정보 API 호출 시도...");
        try {
            const response = await fetch(`${API_BASE_URL}/v1/users/info`, { 
                method: 'GET',
                credentials: 'include', 
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (response.ok) {
                const userInfo = await response.json();
                console.log("사용자 정보 수신 성공:", userInfo);
                
                let userName = '사용자'; 
                if (userInfo && userInfo.data) { 
                    if (userInfo.data.nickname) { 
                        userName = userInfo.data.nickname;
                    } else if (userInfo.data.name) { 
                        userName = userInfo.data.name;
                    }
                } else if (userInfo && userInfo.nickname) { 
                    userName = userInfo.nickname;
                } else if (userInfo && userInfo.name) { 
                    userName = userInfo.name;
                }
                updateLoginState(true, userName);
                return true; // 로그인 성공
            } else {
                console.log(`사용자 정보 API 응답 상태: ${response.status} ${response.statusText}`);
                updateLoginState(false); 
                return false; // 로그인 실패
            }
        } catch (error) {
            console.error("사용자 정보 API 호출 중 오류:", error);
            updateLoginState(false); 
            return false; // 로그인 실패
        }
    }

    function fetchUserInfoAndUpdateLoginState() {
        // userInfoPromise가 아직 생성되지 않았거나, 이미 완료된 Promise라면 새로 생성하지 않고 기존 Promise를 반환하거나, 상태에 따라 새로 호출
        // 간단하게는, 페이지 로드 시 한 번만 호출되도록 하거나, 필요시 상태를 보고 다시 호출하도록 할 수 있습니다.
        // 여기서는 한 번 생성된 Promise를 계속 사용하도록 합니다.
        if (!userInfoPromise) {
            userInfoPromise = actualFetchUserInfo();
        }
        return userInfoPromise;
    }
    
    // 전역으로 Promise를 제공 (다른 스크립트에서 사용하기 위함)
    window.ensureLoginStateKnown = fetchUserInfoAndUpdateLoginState;

    function handleLogout() {
        console.log("로그아웃 처리 시작");

        // API_BASE_URL이 정의되어 있는지 확인
        if (typeof API_BASE_URL === 'undefined') {
            console.error('API_BASE_URL is not defined. Cannot perform server logout.');
            // API_BASE_URL이 없으면 최소한 프론트엔드 로그아웃이라도 처리
            globalUserName = null;
            updateLoginState(false);
            alert('로그아웃 설정에 오류가 있어 현재는 브라우저에만 로그아웃됩니다.'); // 사용자 알림
            return;
        }

        fetch(`${API_BASE_URL}/v1/auth/logout`, {
            method: 'POST',
            credentials: 'include' // 쿠키를 포함하여 요청
        })
        .then(response => {
            if (response.ok) {
                console.log("서버 로그아웃 성공");
            } else {
                // 서버에서 응답은 왔지만, 성공적이지 않은 경우 (예: 400, 500 에러)
                console.error("서버 로그아웃 실패:", response.status, response.statusText);
                // 실패하더라도 프론트엔드는 로그아웃 처리를 시도할 수 있습니다.
            }
            return response.json().catch(() => ({})); // 응답 본문이 없거나 JSON이 아닐 수 있으므로 오류 처리
        })
        .then(data => {
            console.log("서버 응답 데이터 (있다면):", data);
        })
        .catch(error => {
            // 네트워크 오류 등 fetch 자체가 실패한 경우
            console.error("서버 로그아웃 요청 중 오류:", error);
            alert('로그아웃 중 서버와 통신에 실패했습니다. 잠시 후 다시 시도해주세요.');
        })
        .finally(() => {
            // 성공/실패 여부와 관계없이 프론트엔드 로그아웃 처리
            globalUserName = null;
            // localStorage.removeItem('userName'); // 필요하다면 주석 해제
            console.log("프론트엔드에서 로그아웃 처리 완료. UI 업데이트.");
            updateLoginState(false);
            // 필요에 따라 로그인 페이지로 리디렉션하거나 페이지를 새로고침 할 수 있습니다.
            // window.location.href = 'index.html';
            // window.location.reload();
        });
    }

    // --- 헤더 로드 함수 ---
    const loadHeader = async () => {
        try {
            const response = await fetch('partials/header.html');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const headerHtml = await response.text();
            const headerPlaceholder = document.getElementById('header-placeholder');
            if (headerPlaceholder) {
                headerPlaceholder.innerHTML = headerHtml;
                const logoImg = headerPlaceholder.querySelector('.logo img');
                if (logoImg && logoImg.getAttribute('src') === 'assets/images/logo.png') { 
                    logoImg.setAttribute('src', 'assets/images/logo.png'); 
                }
                initializeHeaderScripts(); 
            }
        } catch (error) {
            console.error('Error loading header:', error);
        }
    };

    // --- 푸터 로드 함수 ---
    const loadFooter = async () => {
        try {
            const response = await fetch('partials/footer.html');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const footerHtml = await response.text();
            const footerPlaceholder = document.getElementById('footer-placeholder');
            if (footerPlaceholder) {
                footerPlaceholder.innerHTML = footerHtml;
            }
        } catch (error) {
            console.error('Error loading footer:', error);
        }
    };

    // --- 번역 적용 함수 ---
    const applyTranslations = (translationsData) => {
        window.currentTranslations = translationsData; // 현재 번역 (전역 업데이트)
        const elements = document.querySelectorAll('[data-translate]');
        elements.forEach(el => {
            const key = el.getAttribute('data-translate');
            if (currentTranslations && currentTranslations[key]) {
                if (el.tagName !== 'TITLE') { // TITLE 태그는 아래에서 별도 처리
                    if (el.tagName === 'OL' || el.tagName === 'UL') {
                        el.innerHTML = currentTranslations[key];
                    } else {
                        // 특정 키에 대해서는 innerHTML을 사용하도록 유지 (기존 로직)
                        if (key === 'reqClaudeApp' || key === 'reqMcplinkApp' || key === 'troubleshootingDescStart' || key === 'troubleshootingDescEnd' || key === 'step1List' || key === 'step2List' || key === 'step3List' || key === 'step4List' || key.startsWith('qnaA')) { // qna 답변들도 innerHTML 사용 가능하도록 추가
                            el.innerHTML = currentTranslations[key];
                        } else {
                            el.innerText = currentTranslations[key];
                        }
                    }
                }
            }
        });
        // HTML lang 속성 변경
        document.documentElement.lang = localStorage.getItem('preferredLanguage') || 'ko';

        // 페이지 제목을 mcplink로 고정
        document.title = "mcplink";

        // 페이지 설명 번역 (일반화)
        let pageKeyPart = window.location.pathname.split('/').pop().split('.')[0];
        if (pageKeyPart === '' || pageKeyPart === 'index') {
            pageKeyPart = 'main'; // 루트 또는 index.html인 경우 'mainPageTitle', 'mainPageDescription' 사용
        } else if (pageKeyPart === 'qna') {
            pageKeyPart = 'qna'; // qna.html인 경우 'qnaPageTitle', 'qnaPageDescription' 사용
        }
        // 다른 페이지들도 필요한 경우 여기에 추가 (예: download)
        else if (pageKeyPart === 'download') {
            pageKeyPart = 'download';
        }
        else if (pageKeyPart === 'contact') {
            pageKeyPart = 'contact';
        }
        else if (pageKeyPart === 'board') { // 게시판 페이지 처리 추가
            pageKeyPart = 'board';
        }

        const descriptionKey = `${pageKeyPart}PageDescription`;
        const metaDescriptionTag = document.querySelector('meta[name="description"]');
        if (metaDescriptionTag && currentTranslations[descriptionKey]) {
            metaDescriptionTag.setAttribute('content', currentTranslations[descriptionKey]);
        }
    };

    // --- 언어 설정 및 번역 로드 함수 ---
    const setLanguage = async (lang) => {
        try {
            const response = await fetch(`lang/${lang}.json`);
            if (!response.ok) {
                console.error(`Error loading translation file: lang/${lang}.json`);
                if (lang !== 'ko') {
                    await setLanguage('ko'); // Fallback to Korean
                }
                return;
            }
            const translationsData = await response.json();
            localStorage.setItem('preferredLanguage', lang);
            applyTranslations(translationsData);

            // 로그인 상태에 따라 버튼 텍스트 업데이트 (번역 적용 후)
            const loginButton = document.getElementById('login-button');
            if (loginButton) {
                const isLoggedIn = globalUserName !== null;
                updateLoginState(isLoggedIn, globalUserName); // updateLoginState가 내부적으로 번역된 텍스트를 사용하도록 수정 필요
            }

            // 페이지 로드 시 네비게이션 링크 활성화 (메인 페이지만)
            if (window.location.pathname.endsWith('/') || window.location.pathname.endsWith('/index.html') || window.location.pathname === '/') {
                activateNavLink();
            }

            // 언어 변경 이벤트 발생
            const event = new CustomEvent('languageChanged', { detail: { language: lang, translations: translationsData } });
            document.dispatchEvent(event);

        } catch (error) {
            console.error('Error processing translation data:', error);
        }
    };

    // 전역 translate 함수 정의
    window.translate = function(key, fallback = null) {
        if (window.currentTranslations && typeof window.currentTranslations[key] === 'string') {
            return window.currentTranslations[key];
        }
        // 개발 중 키가 누락된 경우를 위해, 또는 기본값을 명시적으로 보여주기 위해
        // console.warn(`Translation key "${key}" not found. Using fallback or key.`);
        const readableFallback = key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
        return fallback !== null ? fallback : readableFallback; 
    };

    // --- 헤더 관련 스크립트 초기화 함수 ---
    const initializeHeaderScripts = () => {
        console.log("initializeHeaderScripts 시작. 현재 URL:", window.location.href);

        // 모바일 메뉴 토글 버튼 동작
        const mobileMenuToggle = document.querySelector('.mobile-menu-toggle');
        const navContainer = document.querySelector('.nav-container');
        
        if (mobileMenuToggle && navContainer) {
            // 페이지 로드 시 메뉴가 열려있지 않도록 강제로 닫기
            navContainer.classList.remove('show');
            mobileMenuToggle.classList.remove('active');
            document.querySelector('.mobile-menu-toggle i').className = 'bi bi-list';
            
            mobileMenuToggle.addEventListener('click', function() {
                this.classList.toggle('active');
                navContainer.classList.toggle('show');
                
                // 아이콘 변경 (햄버거 <-> X)
                const icon = this.querySelector('i');
                if (navContainer.classList.contains('show')) {
                    icon.className = 'bi bi-x-lg';
                } else {
                    icon.className = 'bi bi-list';
                }
            });
            
            // 메뉴 항목 클릭 시 메뉴 닫기
            const menuItems = document.querySelectorAll('.nav-container a:not(.nav-dropdown-toggle)');
            menuItems.forEach(item => {
                item.addEventListener('click', function() {
                    if (window.innerWidth <= 768) {
                        navContainer.classList.remove('show');
                        mobileMenuToggle.classList.remove('active');
                        document.querySelector('.mobile-menu-toggle i').className = 'bi bi-list';
                    }
                });
            });
            
            // 모바일 네비게이션 드롭다운 토글
            const navDropdownToggles = document.querySelectorAll('.nav-dropdown-toggle');
            navDropdownToggles.forEach(toggle => {
                toggle.addEventListener('click', function(e) {
                    if (window.innerWidth <= 768) {
                        e.preventDefault();
                        const parentDropdown = this.closest('.nav-dropdown');
                        
                        // 다른 열린 드롭다운 닫기
                        document.querySelectorAll('.nav-dropdown').forEach(item => {
                            if (item !== parentDropdown) {
                                item.classList.remove('open');
                            }
                        });
                        
                        // 현재 드롭다운 토글
                        parentDropdown.classList.toggle('open');
                    }
                });
            });
            
            // 메뉴 외부 클릭 시 메뉴 닫기
            document.addEventListener('click', function(event) {
                const isClickInsideNav = navContainer.contains(event.target);
                const isClickOnToggle = mobileMenuToggle.contains(event.target);
                
                if (!isClickInsideNav && !isClickOnToggle && navContainer.classList.contains('show')) {
                    navContainer.classList.remove('show');
                    mobileMenuToggle.classList.remove('active');
                    document.querySelector('.mobile-menu-toggle i').className = 'bi bi-list';
                }
            });
            
            // 화면 크기가 변경될 때 메뉴 상태 조정
            window.addEventListener('resize', function() {
                if (window.innerWidth > 768) {
                    navContainer.classList.remove('show');
                    mobileMenuToggle.classList.remove('active');
                    document.querySelector('.mobile-menu-toggle i').className = 'bi bi-list';
                    
                    // 모바일 드롭다운 초기화
                    document.querySelectorAll('.nav-dropdown').forEach(dropdown => {
                        dropdown.classList.remove('open');
                    });
                }
            });
        }

        const languageSelector = document.querySelector('.language-selector');
        if (!languageSelector) return; // 헤더에 언어 선택기가 없으면 중단

        const dropdownToggle = languageSelector.querySelector('.dropdown-toggle');
        const dropdownMenu = languageSelector.querySelector('.dropdown-menu');
        const dropdownItems = languageSelector.querySelectorAll('.dropdown-item');

        // 언어 선택기에 현재 언어 표시하기
        const currentLang = localStorage.getItem('preferredLanguage') || 'ko';
        
        // 모든 언어 옵션에서 체크 아이콘 초기 숨김
        document.querySelectorAll('.language-selector .dropdown-item').forEach(item => {
            const icon = item.querySelector('.check-icon');
            if (icon) {
                icon.style.display = 'none';
            }
            
            // 현재 선택된 언어에만 체크 아이콘 표시
            if (item.getAttribute('data-lang') === currentLang) {
                if (icon) {
                    icon.style.display = 'inline-block';
                }
                item.classList.add('active');
                
                // 드롭다운 버튼에 현재 언어 표시
                const dropdownToggle = document.querySelector('.language-selector .dropdown-toggle span');
                if (dropdownToggle) {
                    dropdownToggle.textContent = item.textContent.trim();
                }
            }
        });

        // 언어 선택 드롭다운 아이템 이벤트 리스너
        dropdownItems.forEach(item => {
            item.addEventListener('click', async function(e) {
                e.preventDefault();
                e.stopPropagation(); // 이벤트 버블링 방지
                const selectedLang = this.getAttribute('data-lang');

                // 활성 상태 변경 및 체크 아이콘 표시/숨김
                dropdownItems.forEach(i => {
                    i.classList.remove('active');
                    const icon = i.querySelector('.check-icon');
                    if (icon) {
                        icon.style.display = 'none';
                    }
                });
                this.classList.add('active');
                const checkIcon = this.querySelector('.check-icon');
                if (checkIcon) {
                    checkIcon.style.display = 'inline-block';
                }

                // 토글 버튼 텍스트 변경
                if(dropdownToggle && dropdownToggle.querySelector('span')) {
                    dropdownToggle.querySelector('span').textContent = this.textContent.trim();
                }

                // 언어 변경 적용
                await setLanguage(selectedLang);

                // 드롭다운 닫기
                if (languageSelector) languageSelector.classList.remove('show');
            });
        });

        // 드롭다운 토글 기능
        if (dropdownToggle) {
            dropdownToggle.addEventListener('click', function(e) {
                e.stopPropagation(); // 이벤트 버블링 방지
                if (languageSelector) languageSelector.classList.toggle('show');
            });
        }

        // 드롭다운 외부 클릭 시 닫기
        document.addEventListener('click', function(e) {
            if (languageSelector && !languageSelector.contains(e.target)) {
                languageSelector.classList.remove('show');
            }
        });


        // --- 테마 전환 로직 ---
        const themeToggleButton = document.getElementById('theme-toggle-button');
        const currentTheme = localStorage.getItem('theme') ? localStorage.getItem('theme') : null;

        if (currentTheme === 'dark') {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }

        if (themeToggleButton) {
            console.log("테마 토글 버튼 이벤트 리스너 추가");
            themeToggleButton.addEventListener('click', function() {
                console.log("테마 토글 버튼 클릭됨");
                document.body.classList.toggle('dark-mode');
                const isDarkMode = document.body.classList.contains('dark-mode');
                localStorage.setItem('theme', isDarkMode ? 'dark' : 'light');
            });
        } else {
            console.error("테마 토글 버튼을 찾을 수 없습니다:", themeToggleButton);
        }

        // --- 페이지별 링크 활성화 로직 --- 제거 ---

        // index.html에서만 스크롤 기반 활성화 초기 호출
        const currentPath = window.location.pathname;
        if (currentPath.endsWith('/') || currentPath.endsWith('/index.html') || currentPath === '/') {
             activateNavLink();
        }

        // 언어 선택기 초기화
        initializeLanguageSelector();

        // 로그인 버튼 이벤트 핸들러
        const loginButton = document.getElementById('login-button');
        if (loginButton) {
            console.log("로그인 버튼(#login-button) 확인됨. 초기 UI는 fetchUserInfoAndUpdateLoginState 결과에 따라 설정됩니다.");
            // 초기 로그인/로그아웃 버튼 UI 설정은 updateLoginState에서 담당하므로, 여기서는 기본 상태만 둡니다.
        } else {
            console.error("헤더 초기화: 로그인 버튼(#login-button)을 찾을 수 없습니다.");
        }

        // 페이지 로드 시 사용자 정보 조회 및 로그인 상태에 따른 UI 업데이트 실행
        fetchUserInfoAndUpdateLoginState(); 
    };

    // --- 부드러운 스크롤 ---
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            if (this.closest('.language-selector')) return; 

            const targetId = this.getAttribute('href');
            // href가 실제로 #으로 시작하는 내부 앵커인지 확인
            if (targetId && targetId.startsWith('#') && targetId !== '#') {
                e.preventDefault();
                const targetElement = document.querySelector(targetId);
                if (targetElement) {
                    calculateHeaderHeight(); 
                    const offsetPosition = targetElement.offsetTop - headerHeight;
                    window.scrollTo({
                        top: offsetPosition,
                        behavior: 'smooth'
                    });
                }
            } else if (targetId === '#') {
                e.preventDefault(); // href="#"인 경우 기본 동작(페이지 상단으로 이동) 방지
            }
            // 그 외의 경우 (예: 외부 URL, 다운로드 링크)는 기본 동작을 따르도록 preventDefault를 호출하지 않음
        });
    });

    // --- 스크롤 애니메이션 ---
    const animatedElements = document.querySelectorAll('.section, .card, .step');
    const observerOptions = { root: null, rootMargin: '0px', threshold: 0.1 };
    const observerCallback = (entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                // observer.unobserve(entry.target); // 한 번만 실행하려면 주석 해제
            } else {
                entry.target.classList.remove('visible'); // 화면 밖으로 나가면 다시 숨김
            }
        });
    };
    const scrollObserver = new IntersectionObserver(observerCallback, observerOptions);
    animatedElements.forEach(el => {
        el.classList.add('fade-in'); // 초기 클래스 추가
        scrollObserver.observe(el);
    });

    // --- 활성 링크 강조 ---
    let headerHeight = 0;
    const calculateHeaderHeight = () => {
        const headerElement = document.querySelector('#header-placeholder header');
        headerHeight = headerElement ? headerElement.offsetHeight : 0;
    };

    const activateNavLink = () => {
        const currentPath = window.location.pathname;
        // index.html 또는 루트 경로가 아니면 스크롤 기반 활성화 중지
        if (!(currentPath.endsWith('/') || currentPath.endsWith('/index.html') || currentPath === '/')) {
            return;
        }

        calculateHeaderHeight();
        let currentSectionId = '';
        const scrollPosition = window.pageYOffset;
        const sections = document.querySelectorAll('section[id]');
        const navLinks = document.querySelectorAll('#header-placeholder .nav-links a');

        sections.forEach(section => {
            const sectionTop = section.offsetTop - headerHeight - 5; // 약간의 오차범위 추가
            const sectionBottom = sectionTop + section.offsetHeight;
            if (scrollPosition >= sectionTop && scrollPosition < sectionBottom) {
                currentSectionId = section.getAttribute('id');
            }
        });
        
        // 페이지 상단에 있을 때 첫 번째 섹션 활성화 (또는 "소개" 링크)
        if (sections.length > 0 && scrollPosition < sections[0].offsetTop - headerHeight - 5) {
             currentSectionId = sections[0].getAttribute('id'); // 혹은 특정 ID (예: 'introduction')
        }

        // 페이지 하단에 도달했을 때 마지막 섹션 활성화
        if ((window.innerHeight + window.pageYOffset) >= document.body.offsetHeight - 2 && sections.length > 0) {
            currentSectionId = sections[sections.length - 1].getAttribute('id');
        }

        navLinks.forEach(link => {
            link.classList.remove('active');
            const linkHref = link.getAttribute('href');
            // href가 '/#sectionId' 또는 '#sectionId' 형태와 일치하는지 확인
            if (linkHref && linkHref.includes('#')) {
                const linkSectionId = linkHref.substring(linkHref.lastIndexOf('#') + 1);
                if (linkSectionId === currentSectionId) {
                    link.classList.add('active');
                }
            }
        });
    };

    // 스크롤 이벤트 리스너 추가 (index.html 또는 루트 경로일 때만)
    const currentPath = window.location.pathname;
    if (currentPath.endsWith('/') || currentPath.endsWith('/index.html') || currentPath === '/') {
        let scrollTimeout;
        window.addEventListener('scroll', () => {
            clearTimeout(scrollTimeout);
            scrollTimeout = setTimeout(activateNavLink, 50); // 디바운싱
        });
    }

    // 다운로드 버튼들에 대한 이벤트 리스너 추가
    const setupDownloadButtonNavigation = () => {
        const downloadButtons = [
            // index.html 내의 다운로드 버튼
            document.querySelector('.cta-button[data-translate="downloadButton"]'),
            // footer 내의 다운로드 링크
            document.querySelector('a[data-translate="footerLinkDownload"]')
        ];

        downloadButtons.forEach(button => {
            if (button) {
                button.addEventListener('click', function(e) {
                    e.preventDefault();
                    // download.html 페이지로 이동
                    window.location.href = 'download.html';
                });
            }
        });

        // download.html 페이지 내의 다운로드 버튼들 (실제 파일 다운로드 링크)
        // 이 부분은 download.html 페이지가 로드될 때 해당 페이지의 스크립트에서 처리하거나,
        // 여기에 추가한다면, 해당 요소들이 존재하는지 확인 후 이벤트 리스너를 추가해야 합니다.
        // 예시:
        // if (window.location.pathname.includes('download.html')) {
        //     document.querySelectorAll('.download-options .download-button').forEach(btn => {
        //         btn.addEventListener('click', function(e) {
        //             // 실제 다운로드 로직 또는 기본 동작 유지
        //             // e.preventDefault(); // 필요에 따라
        //             console.log('Downloading:', this.href);
        //         });
        //     });
        // }
    };


    // --- 페이지 로드 시 초기 설정 ---
    const initializePage = async () => {
        await loadHeader(); // 헤더 로드 및 관련 스크립트 초기화 대기
        await loadFooter(); // 푸터 로드
        
        const preferredLanguage = localStorage.getItem('preferredLanguage') || 'ko';
        await setLanguage(preferredLanguage); // 초기 언어 설정 (번역 적용 포함)
        setupDownloadButtonNavigation(); // 다운로드 버튼 네비게이션 설정
        
        // 페이지 로드 시 현재 경로에 따라 네비게이션 링크 활성화
            const navLinks = document.querySelectorAll('#header-placeholder .nav-links a');
        const currentPath = window.location.pathname;

            navLinks.forEach(link => {
            link.classList.remove('active'); // 모든 링크 비활성화 초기화
                const linkPath = link.getAttribute('href');

            // 현재 경로와 링크 경로가 일치하는지 확인 (상대 경로 및 절대 경로 모두 고려)
            if (currentPath.endsWith(linkPath) || currentPath.endsWith('/' + linkPath)) {
                    link.classList.add('active');
                
                // 드롭다운 메뉴의 부모 링크도 활성화
                const parentLi = link.closest('.nav-dropdown');
                if (parentLi) {
                    const parentLink = parentLi.querySelector('.nav-dropdown-toggle');
                    if (parentLink) {
                        parentLink.classList.add('active');
                    }
                }
                }
            });
        
        // 드롭다운 메뉴 항목도 활성화 확인
        const dropdownLinks = document.querySelectorAll('#header-placeholder .nav-dropdown-menu a');
        dropdownLinks.forEach(link => {
            link.classList.remove('active'); // 모든 드롭다운 링크 초기화
            const linkPath = link.getAttribute('href');
            
            // 현재 경로와 링크 경로가 일치하는지 확인
            if (currentPath.endsWith(linkPath) || currentPath.endsWith('/' + linkPath)) {
                link.classList.add('active');
                
                // 드롭다운 부모 링크도 활성화
                const parentLi = link.closest('.nav-dropdown');
                if (parentLi) {
                    const parentLink = parentLi.querySelector('.nav-dropdown-toggle');
                    if (parentLink) {
                        parentLink.classList.add('active');
                    }
                }
            }
        });

        // index.html 또는 루트 경로인 경우 스크롤 기반 활성화
        if (currentPath.endsWith('/') || currentPath.endsWith('/index.html') || currentPath === '/') {
            activateNavLink();
        }
    };

    initializePage(); // 페이지 초기화 함수 호출

    // 언어 선택기 초기화
    function initializeLanguageSelector() {
        const languageItems = document.querySelectorAll('.language-selector .dropdown-item');
        const currentLanguage = document.querySelector('.current-language');
        
        // 현재 선택된 언어 확인
        const currentLang = localStorage.getItem('preferredLanguage') || 'ko';
        
        // 초기 체크 표시 설정
        languageItems.forEach(item => {
            const lang = item.getAttribute('data-lang');
            const checkIcon = item.querySelector('.language-check');
            
            if (lang === currentLang) {
                // 현재 언어 텍스트 설정
                if (currentLanguage) {
                    currentLanguage.textContent = lang === 'ko' ? '한국어' : 'English';
                }
                
                // 체크 아이콘 표시 (visibility 사용)
                if (checkIcon) {
                    checkIcon.style.visibility = 'visible';
                }
            } else {
                // 다른 언어는 체크 아이콘 숨김 (visibility 사용)
                if (checkIcon) {
                    checkIcon.style.visibility = 'hidden';
                }
            }
        });
        
        // 언어 선택 이벤트
        languageItems.forEach(item => {
            item.addEventListener('click', function(e) {
                e.preventDefault();
                const lang = this.getAttribute('data-lang');
                
                // 선택된 언어 저장
                localStorage.setItem('preferredLanguage', lang);
                
                // 체크 아이콘 업데이트 (visibility 사용)
                languageItems.forEach(item => {
                    const itemLang = item.getAttribute('data-lang');
                    const checkIcon = item.querySelector('.language-check');
                    
                    if (itemLang === lang) {
                        if (checkIcon) {
                            checkIcon.style.visibility = 'visible';
                        }
                        // 현재 언어 텍스트 업데이트
                        if (currentLanguage) {
                            currentLanguage.textContent = lang === 'ko' ? '한국어' : 'English';
                        }
                    } else {
                        if (checkIcon) {
                            checkIcon.style.visibility = 'hidden';
                        }
                    }
                });
                
                // 언어에 맞게 번역 적용
                setLanguage(lang);
            });
        });
    }
}); // DOMContentLoaded 이벤트 리스너 닫기


