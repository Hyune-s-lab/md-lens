# MdLens 에이전트 가이드라인

규칙 앞의 키워드는 RFC 2119를 따른다: **MUST**(반드시), **MUST NOT**(금지), **SHOULD**(강력 권장, 예외는 근거 필요), **MAY**(허용).

## 제품

- **MUST** MdLens는 JetBrains IDE용 가볍고 읽기 전용인 Markdown 뷰어로 유지한다.
- **MUST NOT** 편집, 자동완성, 인스펙션, 리팩터링, WYSIWYG, 백그라운드 인덱싱을 추가하지 않는다.
- **MUST** 핵심 렌더링은 오프라인에서 동작하고 런타임 CDN에 의존하지 않는다.
- **SHOULD** 사람이 직접 쓰기에도 직관적이어야 한다.

## 아키텍처

- **MUST** 렌더링 코어는 호스트에 독립적인 TypeScript로 유지한다. 브라우저 익스텐션 등 다른 호스트로 포팅할 수 있어야 한다.
- **MUST** Kotlin은 JetBrains IDE 통합을 위한 얇은 계층으로만 유지하고, 호스트와 렌더러 사이의 계약은 작게 유지한다.
- **MUST** 문서 보기는 부작용이 없어야 한다 — 프로젝트에 쓰지 않고, 입력은 신뢰할 수 없는 콘텐츠로 취급해 소독하며, 무거운 리소스는 필요할 때만 로드한다.

## 문서

- **MUST** 공개 콘텐츠(문서, UI 텍스트, Git 히스토리, 이슈, PR, 릴리스 노트)는 영어로 쓴다. 결정 기록과 이 문서(AGENTS.md)는 한국어로 쓰고, 오너와의 비공개 논의는 한국어로 해도 된다.
- **SHOULD** 코드 식별자와 정착된 기술 용어는 원문 그대로 둔다.
- **MUST** `README.md`는 간결한 제품 진입점으로 유지하고 핵심 기능만 설명한다.
- **MUST** `docs/roadmap.md`가 릴리스 범위와 향후 작업을 소유한다. 릴리스된 작업은 마이너 버전당 한 섹션(`0.4.x`)에 불릿 몇 개로 뭉뚱그리고, 버전별 상세는 GitHub Release notes에 맡긴다. 이전 플러그인(MarkdownNeat) 시절은 Earlier Releases에 한 줄씩 남긴다.
- **MUST** `docs/architecture.md`는 현재 아키텍처만 기술하고, 승인된 결정은 `docs/decisions/`에 남긴다.
- **MUST** Marketplace Overview는 오래가는 제품 역량만 담고, 링크는 GitHub 저장소 루트만 허용한다. What's New는 현재 릴리스만 변경 유형별로 요약하고 링크를 넣지 않는다.
- **MUST NOT** 같은 상태나 결정을 여러 문서에 중복 기재하지 않는다.

## 검증

- **SHOULD** 변경은 작고 실행 가능하게, 가장 가까운 유효 레벨에서 테스트한다. 렌더러 테스트는 정상 입력, 잘못된 다이어그램, 안전하지 않은 콘텐츠를 커버한다.
- **MUST** PR CI는 빠른 테스트와 패키지 검증을 돌린다. 호환성 검증은 JetBrains Marketplace에 맡기고, 가벼움에 영향을 주는 변경은 크기·시작 시간·메모리 영향을 측정한다.
- **MUST** 보고에는 실제 수행한 검증만 나열하고 남은 리스크를 밝힌다. 수동 스모크 테스트는 자동화가 못 다루는 동작만 다룬다.

## Git과 릴리스

- **MUST** 작업은 목적이 분명한 브랜치에서 하고, 리뷰 가능한 PR(드래프트 금지, `Hyune-c` 지정)로 `main`에 반영한다.
- **MUST NOT** 관련 없는 변경을 한 커밋에 섞거나, 생성 산출물·IDE 캐시·로컬 환경 파일을 커밋하지 않는다.
- **MUST** 릴리스 노트와 버전 히스토리는 GitHub Releases가 소유한다. 제목은 `v<version>` 형식, 본문은 Spring Boot 스타일 이모지 헤딩(`:star: New Features`, `:lady_beetle: Bug Fixes`, `:notebook_with_decorative_cover: Documentation`, `:hammer: Dependency Upgrades`)으로 변경 유형별 한 섹션에 모은다.
- **MUST** 릴리스는 하나의 흐름으로 진행한다: 드래프트 생성 → 릴리스 워크플로 디스패치(태그 빌드, 패키지 검증, ZIP 첨부, Marketplace 업로드) → 워크플로 성공 후에만 드래프트 발행과 보고.
- **MAY** 에이전트는 PR 머지, GitHub Release 생성, 빌드 아티팩트 첨부를 자율적으로 수행한다.
- **MUST NOT** Marketplace 업로드는 오너의 명시적 요청 없이 하지 않는다.
