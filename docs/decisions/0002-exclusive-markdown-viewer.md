# ADR-0002: MdLens이 Markdown 편집기 선택을 소유한다

- **상태:** 수락됨
- **날짜:** 2026-07-11

## 맥락

JetBrains IDE는 하나의 Markdown 파일에 여러 `FileEditorProvider` 구현체를 로드할 수 있다.

MdLens이 기본 편집기 뒤에 배치되면, 내장 Markdown 미리 보기가 선택된 채 별도의 MdLens 탭이 아래에 나타났다.

이때 보이는 페이지는 다른 렌더러의 결과이므로 MdLens 설정이 적용되지 않는 것처럼 보였다.

프로젝트 복원에도 제약이 있다. 인덱싱 중 사용할 수 없는 provider는 저장된 편집기를 복원할 때 생략될 수 있다.

MdLens을 사용할 수 있게 된 뒤에도 내장 Markdown 편집기가 계속 활성 상태로 남을 수 있다.

## 결정

플러그인이 활성화된 동안 MdLens은 `.md`와 `.markdown` 파일의 유일한 일반 편집기여야 한다.

- provider는 `HIDE_OTHER_EDITORS`를 사용해야 한다.
- 인덱싱과 프로젝트 복원에서도 같은 편집기가 선택되도록 provider는 `DumbAware`여야 한다.
- 모양 설정은 지원하는 Markdown 파일에서 실제로 보이는 렌더러에 항상 적용되어야 한다.

## 결과

- 사용자는 어떤 Markdown 미리 보기가 활성 상태인지 구분할 필요가 없다.
- 시작과 인덱싱 중에도 MdLens 동작이 안정적이다.
- MdLens이 활성화된 동안 다른 Markdown 편집기는 일반 편집기 조합에 나타나지 않는다.
- MdLens 설치는 지원 파일의 편집보다 읽기 전용 보기를 의도적으로 우선한다.
- 다른 플러그인도 독점 편집기 소유권을 요구하면 IntelliJ가 두 provider를 모두 유지할 수 있다. 이 경우 사용자는 독점 뷰어를 하나만 활성화해야 한다.
- `HIDE_OTHER_EDITORS`는 이전 플랫폼에서 실험적이었지만, MdLens의 지원 하한인 2026.1부터는 안정 API다.

MdLens을 보조 편집기로 유지하는 안은 렌더러 소유권을 모호하게 만들므로 기각했다.

`HIDE_DEFAULT_EDITOR`도 플랫폼 기본 편집기만 제거할 뿐 다른 Markdown 플러그인 사이에서 MdLens을 유일한 provider로 만들지 못하므로 충분하지 않았다.
