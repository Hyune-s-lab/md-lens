# ADR-0006: JCEF는 실제 번들 플러그인 ID로 의존한다

- **상태:** 수락됨
- **날짜:** 2026-07-12

## 맥락

IntelliJ Platform 2026.2는 JCEF 구현을 `Web Browser (JCEF)` 번들 플러그인으로 분리했다.

`JBCefApp`은 이 플러그인의 `intellij.platform.ui.jcef` 모듈에 들어 있다.

0.2.1은 `intellij.libraries.jcef`를 optional `<depends>` 값으로 선언했지만, 이것은 플러그인 내부 모듈 이름이다. classic 플러그인의 `<depends>`는 플러그인 ID를 기대하므로 IDE는 이 값을 미해결 플러그인으로 처리했고, MdLens 클래스 로더에 JCEF 모듈을 연결하지 않았다.

그 결과 Plugin Verifier는 호환으로 판정했지만, Marketplace의 2026.2 실제 IDE 실행 테스트에서는 `MdLensFileEditorProvider`가 `JBCefApp`을 로드하지 못했다.

## 결정

MdLens은 별도 descriptor를 갖는 optional `com.intellij.modules.jcef` 의존성을 선언해야 한다.

`com.intellij.modules.jcef`는 2026.2의 실제 번들 플러그인 ID다. classic 플러그인이 이 modular plugin에 의존하면 공개 JCEF 모듈의 클래스 로더가 MdLens 클래스 로더의 부모로 연결된다.

호환성 하한은 `HIDE_OTHER_EDITORS`가 안정 API인 IntelliJ Platform 2026.1로 정한다. 2026.1에서는 JCEF가 플랫폼 클래스패스에 있고, 2026.2에서는 optional 번들 플러그인을 통해 제공된다.

## 검증

- 수정 전 0.2.1을 IntelliJ IDEA 2026.2 RC에서 실행하고 Markdown 파일을 열어 Marketplace와 동일한 `NoClassDefFoundError: JBCefApp`을 재현했다.
- 의존성 ID만 수정한 뒤 같은 IDE와 파일에서 JCEF 에디터가 생성되고 예외가 사라지는 것을 확인했다.
- IntelliJ IDEA 2026.1.4에서 Plugin Verifier 호환 판정과 플러그인 로드를 확인했다.

## 결과

- 2026.2의 JCEF 모듈 경계가 실제 런타임 구조와 일치한다.
- Plugin Verifier 결과만으로 실제 에디터 생성 성공을 추정하지 않는다.
- JCEF 클래스 로더 변경은 대상 IDE에서 Markdown 파일을 실제로 여는 실행 검증을 포함해야 한다.
