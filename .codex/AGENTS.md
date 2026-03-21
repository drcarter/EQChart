# AGENTS.md for .codex

## Scope
- 이 디렉터리의 문서는 Codex용 프로젝트 규칙 저장소다.
- 프로젝트 전반의 모듈 경계와 코드 배치 판단은 `.codex/rules/architecture.rules`를 우선 참조해 주십시오.
- 프로젝트 전반의 테스트 작성 기준은 `.codex/rules/testing.rules`를 우선 참조해 주십시오.
- 프로젝트 전반의 subagent 역할 분리와 병렬 작업 규칙은 `.codex/SUBAGENTS.md`를 우선 참조해 주십시오.
- 아래 문서화 규칙은 `.codex` 디렉터리만이 아니라 저장소 전체에서 생성하거나 수정하는 Kotlin 파일(`.kt`, `.kts`)과 공개 API 설계에 적용해 주십시오.

## Architecture Rule
- 저장소 전체의 아키텍처 가드레일은 `.codex/rules/architecture.rules`에 정의되어 있습니다.
- 새로운 코드의 위치를 정하거나 모듈 간 참조를 추가할 때는 해당 규칙을 먼저 확인해 주십시오.
- 이 문서의 KDoc 규칙은 아키텍처 규칙과 별개이며, 함께 적용됩니다.

## Testing Rule
- 저장소 전체의 테스트 가드레일은 `.codex/rules/testing.rules`에 정의되어 있습니다.
- 구현을 추가하거나 수정할 때는 관련 unit test를 함께 추가하거나 갱신하는 것을 기본 원칙으로 삼아 주십시오.
- 테스트 예외 여부와 모듈별 테스트 위치 판단은 해당 규칙을 먼저 확인해 주십시오.

## Subagent Rule
- 저장소 작업을 병렬화할 때는 `.codex/SUBAGENTS.md`의 기본 역할과 write scope를 따르십시오.
- cross-module 변경이 필요한 경우에도 하나의 subagent가 여러 모듈을 동시에 직접 수정하지 말고, 계약 변경과 후속 구현을 분리해 주십시오.
- shared file, 문서, 루트 설정 파일은 충돌 위험이 높으므로 명시적 소유권 없이 병렬 수정하지 말아 주십시오.
- 테스트 책임, handoff 형식, shared file lock 규칙도 `.codex/SUBAGENTS.md` 기준을 따르십시오.

## KDoc Rules
- `public class`, `interface`, `object`, `enum class`, `data class`에는 KDoc을 작성해 주십시오.
- 외부에서 사용되는 `public constructor`, `public function`, `public property`에는 KDoc을 작성해 주십시오.
- `internal` 선언도 다른 파일이나 모듈에서 재사용될 가능성이 높다면 KDoc을 작성해 주십시오.
- `private` 선언은 모두 의무는 아니지만, 의도가 바로 드러나지 않는 로직, 제약 조건이 있는 로직, 상태 변경이 있는 로직에는 KDoc 또는 짧은 설명 주석을 남겨 주십시오.
- 새로 추가하는 `public API`는 KDoc 없이 마무리하지 말아 주십시오.
- 기존 파일을 실질적으로 수정할 때, 그 파일 안의 관련 `public API`에 KDoc이 빠져 있으면 가능한 같은 변경에서 함께 보강해 주십시오.
- 수학 연산, 렌더링 파이프라인, 좌표 변환, 상태 전이, lifecycle 제약, 성능 최적화처럼 호출자나 다음 작성자가 즉시 이해하기 어려운 로직에는 짧은 설명 주석을 남겨 주십시오.
- 반대로 코드만 읽어도 자명한 대입, 단순 위임, 이름 그대로의 동작에는 불필요한 주석을 추가하지 말아 주십시오.
- 주석과 KDoc은 "필요한 곳에는 항상 남기되, 의미 없는 설명은 쓰지 않는다"는 원칙으로 유지해 주십시오.

## KDoc Content
- KDoc에는 구현 세부사항을 반복하기보다, 먼저 "무엇을 위한 코드인지"를 설명해 주십시오.
- 파라미터 의미가 이름만으로 충분히 드러나지 않으면 `@param`을 작성해 주십시오.
- 반환 의미가 모호하거나 결과 형식에 제약이 있으면 `@return`을 작성해 주십시오.
- 예외, 부작용, 상태 변경, 스레드 제약, 단위(`px`, `dp`, `ms` 등), 값 범위 제한이 있으면 반드시 문서에 포함해 주십시오.
- 기본값, nullable 처리, 호출 순서 제약이 있으면 KDoc에 명시해 주십시오.
- Android lifecycle 연동, OpenGL/Canvas 렌더링 순서, touch/gesture 해석, range normalization, clipping 같은 맥락 의존 로직은 호출자가 헷갈리지 않도록 목적과 제약을 적어 주십시오.

## Style
- 한 줄 요약으로 시작하시고, 필요할 때만 추가 설명을 덧붙여 주십시오.
- "값을 설정합니다", "리스트를 반환합니다"처럼 의미가 약한 설명은 피해 주십시오.
- 코드와 이름만 읽어도 자명한 내용은 반복하지 말아 주십시오.
- 문서가 다소 길어지더라도, 호출자가 알아야 하는 제약과 의도를 우선해서 적어 주십시오.

## Maintenance
- 기존 KDoc이 현재 구현과 어긋나면 코드 수정 시 함께 갱신해 주십시오.
- 새로운 `public API`를 추가하실 때는 KDoc 없이 마무리하지 말아 주십시오.
- 작업 완료 전에는 "이번 변경으로 추가되거나 실질적으로 바뀐 공개 API에 필요한 KDoc이 모두 들어갔는지"를 확인해 주십시오.
- review나 commit 전에는 "복잡한 internal/private 로직에 다음 작성자를 위한 짧은 설명이 필요한지"를 한 번 더 확인해 주십시오.
