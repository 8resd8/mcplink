FROM jenkins/jenkins:jdk17

USER root

# 패키지 목록 업데이트 및 빌드 필수 도구, Node.js 필수 도구, 타우리 빌드 필수 시스템 라이브러리 설치
RUN apt update && apt upgrade -y && \
    apt install -y --no-install-recommends \
    build-essential \
    curl wget pkg-config \
    libssl-dev \
    libgtk-3-dev \
    librsvg2-dev \
    libwebkit2gtk-4.1-dev \
    dirmngr gnupg apt-transport-https ca-certificates software-properties-common \
    && rm -rf /var/lib/apt/lists/*


# curl 명령은 root 권한으로 실행해야 /etc/apt/sources.list.d 에 파일을 추가할 수 있음
# 설치된 Node.js 및 npm은 시스템 전체에서 사용 가능하게 됨
RUN curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get install -y nodejs && \
    # pnpm 글로벌 설치
    npm install -g pnpm && \
    # npm 캐시 정리
    npm cache clean --force


# Rust 및 Cargo 설치 (rustup 사용)
# Dockerfile 빌드 중에는 root로 설치하는 것이 간편
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --profile minimal

# Cargo의 bin 디렉토리를 PATH 환경 변수에 추가 (root 사용자의 .cargo 디렉토리)
# 컨테이너 실행 시 jenkins 사용자가 이 경로를 찾을 수 있도록 설정
ENV PATH="/root/.cargo/bin:${PATH}"

RUN cargo install tauri-cli

USER jenkins