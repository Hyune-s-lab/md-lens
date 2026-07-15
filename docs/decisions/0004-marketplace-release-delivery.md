# ADR-0004: Marketplace가 호환성 검증을 소유한다

- **상태:** 수락됨
- **날짜:** 2026-07-11

## 맥락

GitHub Actions와 JetBrains Marketplace는 모두 Plugin Verifier 검사를 실행할 수 있다.

모든 pull request와 GitHub Release에서 같은 다중 IDE 검증을 반복하면 Marketplace 제출의 품질을 높이지 못한 채 수 분만 추가된다.

첫 MdLens 버전은 Marketplace vendor와 플러그인 레코드를 만들기 위해 수동 업로드했다.

그 뒤 Marketplace는 더 넓은 호환성 검증을 수행하고 모든 새 플러그인 버전을 수동으로 승인한다.

## 결정

릴리스 파이프라인은 세 가지 책임으로 나눈다.

- Pull request CI는 빠른 테스트, 플러그인 빌드, 설정 및 구조 검증을 실행해야 한다.
- GitHub Draft Release는 태그가 가리키는 소스를 빌드하고 같은 빠른 패키지 검증을 실행한 뒤 결과 ZIP을 첨부해야 한다.
- Draft Release의 태그와 제목은 모두 태그 소스에 선언된 버전과 일치해야 한다.
- Draft Release 워크플로는 플러그인 XML ID와 `JETBRAINS_MARKETPLACE_TOKEN` repository secret을 사용해 바로 그 ZIP을 JetBrains Marketplace에 업로드해야 한다.
- Marketplace Overview는 오래 유지될 제품 기능만 설명하고, What's New는 현재 릴리스 요약과 GitHub Release 노트 링크만 제공해야 한다.

JetBrains Marketplace가 호환성 검증과 수동 승인을 소유해야 한다.

GitHub Release는 워크플로가 성공할 때까지 draft로 남아야 하며 Marketplace 승인은 자동화하지 않는다.

## 결과

- Pull request와 release draft는 중복된 다중 IDE Plugin Verifier 실행을 피한다.
- Marketplace는 GitHub Draft Release에 첨부된 것과 같은 ZIP을 받는다.
- 누락되거나 거부된 Marketplace 토큰은 draft를 공개하기 전에 Draft Release 워크플로를 실패시킨다.
- permanent Marketplace token은 GitHub repository secret에만 저장하며 커밋하거나 로그에 남기지 않는다.
- 첫 수동 Marketplace 업로드는 bootstrap 단계로 남고 이후 릴리스는 자동 업데이트 API를 사용할 수 있다.
