---
name: generate-sap-api
description: SAP RAP OData V4 API YAML 명세 또는 OData 응답 JSON 샘플과 필드 설명을 분석해 Web/OData record DTO를 생성한다. API 신규 생성, 수정, 일괄 생성에서는 프로젝트 규칙에 맞춰 Controller, ApiDoc, Service, Repository, MapStruct Converter, 테스트도 생성한다.
---

# SAP RAP OData V4 API 자동 생성 Skill

## 1. 목적과 실행 방식

사용자가 지정한 `api-specs/**/*.yaml` 파일·디렉터리 또는 OData 응답 JSON 샘플과 필드 설명을 읽고, 기존 Spring Boot 프로젝트 구조에 맞춰 요청한 API 코드나 DTO를 생성한다.

- 프로젝트 루트의 `AGENTS.md`를 최우선 개발 규칙으로 따른다.
- 이 Skill은 생성 작업의 절차를 정의한다. 실제 Java 패키지, 공통 메서드, 응답 타입은 프로젝트 소스가 진실의 원천이다.
- 기본 동작은 **분석 및 생성 계획 제시**다. 사용자가 같은 요청에서 명시적으로 즉시 생성을 지시한 경우에만 승인 대기 없이 생성할 수 있다.
- 사용자가 계획 승인을 요구했거나, 공통 모듈 수정·기존 코드 대규모 변경·불명확한 계약이 있으면 반드시 승인을 기다린다.
- YAML이 여러 개면 전부 먼저 분석하고 충돌을 검증한 후 작업한다.

## 2. 입력

### 생략 가능한 출력 설정

- `webResponse`, `swagger`, `converter`는 선택 사항이다. 자동 결정할 수 있는 출력 설정을 사용자에게 요구하거나 YAML에 다시 추가하지 않는다. 기존 명시적 설정은 override로 우선 적용한다.
- Web 응답 DTO 이름은 기존 동일 기능의 DTO·Controller 명명 규칙을 우선 재사용하고, 없으면 `{Feature}Response`로 결정한다. OData DTO 이름이 `ODataEmployeeResponse`이면 접두사를 제거한 `EmployeeResponse`를 후보로 사용할 수 있다. 기능명과 DTO명이 다르면 기존 소스와 명세의 맥락으로 일관된 이름을 정한다.
- Web 응답 형태는 명시된 API 동작·페이징 설정과 기존 공통 응답 규격에서 결정한다. 목록 + pagination.enabled=true이면 PAGE, 페이징 없는 목록이면 LIST, 단건이면 OBJECT, 응답 없는 작업이면 EMPTY로 처리한다. 샘플 배열 하나만으로 PAGE를 가정하지 않는다.
- Swagger는 프로젝트에서 사용 중이면 기본 활성화한다. 기존 동일 기능의 Tag와 ApiDoc을 재사용하고, 없으면 기능명에 맞는 Tag와 `{Feature}ApiDoc`을 생성한다. 별도 tag/apiDocName 지정이나 `enabled: false`가 필요한 경우에만 swagger 블록을 사용한다.
- 기존 기능의 이름과 신규 feature 이름이 다른 경우 신규 경로의 명명 규칙을 우선 확인한다. 서로 다른 기능에 기존 Employee 이름을 무조건 재사용하지 않는다.

사용자의 요청에서 다음을 파악한다.

- 단일 명세: `api-specs/employee/search.yaml`
- 기능 단위: `api-specs/employee/`
- 전체 명세: `api-specs/`
- 모드: `analyze`(기본), `generate`(명시적 생성), `validate`(검증만)

YAML과 응답 샘플 모두 없으면 어떤 입력을 사용할지 질문한다. 응답 샘플이 있으면 YAML 경로를 필수로 요구하지 않는다. 지정 경로 밖의 명세를 임의로 포함하지 않는다.

### 2.1 응답 샘플 기반 DTO 생성

사용자가 OData 응답 JSON 샘플과 `필드명 설명[...]` 목록을 제공하면 **api-specs의 sample·description 읽기 → 필드·타입·설명 추론 → DTO 생성** 순서로 처리한다. 추론 결과를 YAML의 fields에 중복 저장하지 않는다. DTO만 요청했다면 Controller·Service·Repository를 생성하거나 Endpoint 등의 API 계약을 임의로 채우지 않는다.

#### api-specs 저장 및 재생성

- 사용자는 `sapResponse` 아래에 `sample`(응답 샘플)과 선택 사항인 `description`(필드 설명 목록)만 제공하면 된다. `sapResponse.fields`와 `webResponse.fields`는 필수가 아니며, 샘플로 추론할 수 있으면 생성하거나 다시 저장하지 않는다.
- `sample`은 YAML 객체 또는 JSON 형태의 여러 줄 문자열로, `description`은 `|` 블록의 `필드명 설명[...]` 목록으로 받을 수 있다. 문서에서 사용한 `sapResposne`은 오타로 보고 정식 키 `sapResponse`를 사용한다.
- 샘플 배열 래퍼는 `value`뿐 아니라 사용자가 제공한 `data`도 허용한다. 내부 객체를 항목 DTO로 만들고 실제 래퍼 이름을 `collectionProperty`에 기록한다. `data`를 표준 OData V4 이름이라고 가정하거나 실제 응답의 이름을 임의로 `value`로 바꾸지 않는다.
- 설명 필드명은 정확히 일치하는 키를 우선한다. 없으면 대소문자를 무시했을 때 유일한 키만 매칭한다. 예: 설명의 `EmployeeID` → 샘플의 `EmployeeId`. 여러 키가 매칭되면 추측하지 않고 충돌을 보고한다. JSON 키의 원래 표기는 보존한다.
- `{data:[{EmployeeId:"222221c"}]}`처럼 키 따옴표가 없는 예시는 입력 표기로 허용하되, 실제 JSON 파일에 저장할 때는 키에 따옴표를 붙여 유효한 JSON으로 정규화한다. 문자열 내용과 타입은 바꾸지 않는다.
- 기존 명세의 명시적 fields는 샘플에서 추론할 수 없는 타입·이름 변경·필드 선택 등을 지정하는 선택적 override로만 사용한다. 명시적 계약과 샘플이 충돌하면 차이를 보고한다. 요청 필드 정의는 이 응답 fields 생략 규칙과 별도로 유지한다.
- 새 기능이면 `api-specs/{feature}/`에 명세를 작성한다. DTO만 요청되고 API 계약이 없으면 DTO 정의만 담은 부분 명세로 저장하고 부분 명세임을 명시한다. HTTP 경로·Service Path·키·Action을 추측해서 완전한 API 명세로 만들지 않는다.
- `sap`, `sapResponse` 등의 기존 YAML 키는 유지하며, `dtoName`은 `ODataEmployeeResponse` 같은 OData 명명 규칙을 따른다.
- YAML에는 입력 sample과 description 목록을 보관한다. 설명의 `[...]`는 생성 시 제거하여 OData 필드 주석과 Web `@Schema(description = "...")`에 반영한다. 재생성 시에도 동일하게 적용한다.
- 원본 응답은 제공된 샘플을 `api-specs/{feature}/samples/`의 JSON 파일로 보관하고 해당 명세의 YAML 주석에 상대 경로와 샘플 기반 추론 여부를 남긴다. 불필요한 실제 개인정보·비밀값은 그대로 복제하지 말고 타입과 형식을 보존한 예시 값으로 치환한다.
- 이미 확정된 타입·설명과 새 샘플이 충돌하면 기존 계약을 임의로 덮어쓰지 않고 차이를 보고한다. 분석 모드에서는 저장할 내용을 제시하고, 생성 모드에서는 명세와 코드를 함께 반영한다.

#### 필드와 설명 처리

- 기본 응답 입력은 `{"value":[{"name":"홍길동","age":23}]}`처럼 `value` 배열로 감싼 OData V4 목록이다. `value` 자체를 사원 등의 업무 DTO 필드로 만들지 않고, 배열 안의 객체를 업무 DTO의 기준으로 삼는다. 여러 객체가 있으면 모든 행을 확인한다.
- 설명 목록은 `name 이름[...]`, `age 나이[...]`처럼 별도 텍스트로 제공된다. 각 줄의 첫 필드명을 `value` 내부 객체의 필드명과 연결하고, 나머지 텍스트에서 대괄호 구간을 제거한 값을 description으로 사용한다.
- JSON의 실제 필드명으로 설명 목록을 연결한다. 중첩 객체에서 같은 이름이 반복되면 객체 경로로 구분한다.
- `필드명` 뒤의 텍스트를 description으로 사용한다. description 안의 `[...]`는 대괄호와 내부 내용을 함께 제거하고 앞뒤 공백을 정리한다. JSON 키나 샘플 값에 있는 대괄호는 제거하지 않는다.
- OData DTO의 Java 필드는 lowerCamelCase로 만들고 기존 Naming Strategy 규칙을 적용한다. Web DTO도 lowerCamelCase를 사용한다.
- 설명이 제공된 Web Request/Response DTO의 record component에 `@Schema(description = "정리한 설명")`을 추가한다. Java 문자열의 따옴표·역슬래시·줄바꿈은 올바르게 이스케이프한다.
- OData Response DTO에는 `@Schema`를 붙이지 않는다. description이 있으면 각 record component 선언의 맨 뒤에 `// 설명` 형태의 주석을 추가한다. 쉼표가 있는 필드는 쉼표 뒤에 주석을 두며, 마지막 필드의 닫는 괄호는 다음 줄에 둔다. 설명의 줄바꿈은 공백으로 정리하여 한 줄 주석으로 작성한다.
- description은 선택 사항이다. 설명이 없거나 대괄호 제거 후 빈 설명이 되면 해당 필드의 OData 주석과 Web `@Schema(description = ...)`을 생략한다. 설명을 임의로 만들거나 설명 누락을 오류로 처리하지 않는다. 타입을 확인할 수 있으면 설명 없이도 필드를 생성한다. 샘플에 없는 설명 필드는 타입 추론 불가로 보고한다.
- `value`는 목록 envelope로, `@odata.*`는 OData 메타데이터로 구분한다. Web DTO에 OData 메타데이터를 업무 필드로 노출하지 않는다.
- 목록의 envelope DTO와 배열 내부 항목 DTO는 분리한다. collection과 collectionProperty는 샘플에서 추론할 수 있으며 명시된 값이 있으면 일치 여부를 확인한다. 항목 필드는 메모리에서 추론하고 YAML fields에 중복 저장하지 않는다. Web 페이지 응답의 페이징 계약은 샘플만으로 추측하지 않는다.
- 샘플이 `name`, `age`처럼 lowerCamelCase라면 OData JSON도 그 이름을 유지한다. UpperCamelCase Naming Strategy는 `EmployeeName` 등 실제 필드가 UpperCamelCase인 경우에 적용한다. 클래스 단위 Naming Strategy를 사용하되 샘플의 JSON 키를 임의로 대문자로 바꾸지 않는다.

다음 입력의 처리 예:

```json
{"value":[{"name":"홍길동","age":23}]}
```

```text
name 이름[...]
age 나이[...]
```

항목 필드는 `name: String, description: 이름`, `age: Integer, description: 나이`로 추론한다. Web 항목 DTO는 다음과 같이 생성하며, OData 항목 DTO의 JSON 키도 `name`, `age`로 유지한다.

```java
public record PersonResponse(
        @Schema(description = "이름") String name,
        @Schema(description = "나이") Integer age) {
}
```

OData 항목 DTO에는 `@Schema` 대신 필드 뒤 주석을 사용한다. 아래 예제는 name에만 description이 제공된 경우다.

```java
public record ODataPersonResponse(
        String name, // 이름
        Integer age
) {
}
```

#### 타입 추론

- 사용자가 명시한 타입과 실제 OData 메타데이터를 우선한다. 샘플과 충돌하면 임의로 덮어쓰지 않고 차이를 보고한다.
- 메타데이터가 없으면 모든 샘플 행의 non-null 값을 함께 확인하여 타입을 추론한다. 샘플에서 추론한 타입과 실제 계약 검증 여부를 보고한다.
- JSON 문자열은 기본 `String`이다. `"00123"` 같은 숫자 문자열이나 식별자를 숫자로 바꾸지 않는다. ISO 날짜만 있는 문자열은 `LocalDate`, 오프셋이 있는 ISO 날짜·시간은 `OffsetDateTime`, 오프셋 없는 ISO 날짜·시간은 `LocalDateTime` 후보로 추론하되 모든 값의 형식을 확인한다.
- JSON boolean은 `Boolean`, 정수는 샘플 범위에 맞는 `Integer`/`Long`/`BigInteger`, 소수는 정밀도 보존을 위해 `BigDecimal`을 사용한다. 원시형 대신 nullable wrapper를 사용한다.
- 객체는 중첩 record, 배열은 전체 요소를 확인해 `List<T>`로 생성한다. 정수와 소수가 섞이면 `BigDecimal`처럼 값을 보존하는 공통 타입을 사용한다.
- null만 있는 필드, 빈 배열, 서로 호환되지 않는 타입은 임의로 확정하지 않는다. 해당 필드의 타입을 확인하고, 초안이 요청됐다면 `Object`/`List<Object>` 같은 임시 타입과 TODO를 명확히 표시한다.
- 샘플 값만으로 `@NotNull`, 최대 길이, 숫자의 precision/scale 같은 실제 validation 계약을 추가하지 않는다.

예를 들어 아래 입력에서:

```text
응답 샘플: {"EmployeeId":"0001","EmployeeName":"홍길동","HireDate":"2026-10-30"}
EmployeeId 사원번호[KEY]
EmployeeName 사원명[최대 100자]
HireDate 입사일[YYYY-MM-DD]
```

명세에는 sample과 description만 작성하면 된다. 아래는 전체 API 명세 중 DTO 입력 부분이다.

```yaml
sapResponse:
  dtoName: ODataEmployeeResponse
  sample:
    data:
      - EmployeeId: "0001"
        EmployeeName: "홍길동"
        HireDate: "2026-10-30"
  description: |
    EmployeeId 사원번호[KEY]
    EmployeeName 사원명[최대 100자]
    HireDate 입사일[YYYY-MM-DD]
webResponse:
  dtoName: EmployeeResponse
  type: LIST
```

이 명세로 Web DTO를 다음과 같이 생성한다. `ODataEmployeeResponse`도 같은 타입을 사용하되 OData Naming Strategy를 적용한다.

```java
public record EmployeeResponse(
        @Schema(description = "사원번호") String employeeId,
        @Schema(description = "사원명") String employeeName,
        @Schema(description = "입사일") LocalDate hireDate) {
}
```

샘플에서 작성한 YAML도 필드·타입·설명·매핑을 검증한다. DTO 전용 부분 명세에는 전체 API의 필수 Endpoint 검증을 적용하지 않는다. 전체 API를 요청했다면 샘플로 알 수 없는 Endpoint·키·Action·공통 클라이언트 계약은 기존 확인 절차를 따른다.

## 3. 1단계 — 기존 프로젝트 분석 (읽기 전용)

다음 파일과 실제 사용 예제를 확인한다.

1. `AGENTS.md` 및 하위 경로의 관련 `AGENTS.md`
2. Gradle 빌드 설정과 Java 버전
3. `RapClient` 선언, 구현, 호출 예제, 응답/예외 처리
4. `CommonApiResponse` 팩토리 메서드 및 제네릭 구조
5. `ErrorCode`, `RestControllerAdvice`, 공통 예외
6. 기존 Controller, ApiDoc, Service/impl, Repository/impl 패턴
7. 공통 MapStruct `@MapperConfig`, Jackson 설정
8. Swagger / springdoc-openapi 설정과 실제 ApiDoc 인터페이스 예제
9. 기존 테스트 스타일과 공통 페이징 타입
10. SAP `$metadata` 또는 실제 SAP JSON 샘플(접근 가능할 때)

**금지:** 존재하지 않는 `RapClient` 메서드, `CommonApiResponse.success(...)`, 예외 타입 등을 추측해서 사용하지 않는다. 실제 소스가 확인되지 않으면 사용자에게 필요한 정보를 요청한다.

## 4. 2단계 — YAML 명세 검증

각 YAML에서 다음을 확인한다.

- `specVersion`, `feature`, `api`, `sap` 등 필수 정보
- HTTP Method, REST Path, 중복 Endpoint 및 메서드명
- SAP Service Path, EntitySet, Entity Key, Action/Function 이름
- `webRequest`, `sapRequest`, `sapResponse`, `webResponse` 정의와 타입
- DTO 재사용 가능 여부, 필드명·타입·nullability·validation
- `converter`, `swagger`, `tests` 설정
- OData V4 `$select`, `$filter`, `$expand`, `$orderby`, `$top`, `$skip`, `$count`
- 페이지 번호 기준(1부터 시작), 최대 페이지 크기, 정렬 허용 필드
- SAP EDM 타입, 날짜/시간, Decimal, 복합키 및 문자열 이스케이프
- OData V4 목록의 `value`, `@odata.count`, `@odata.nextLink`
- POST/PATCH/DELETE의 CSRF, ETag/If-Match, 재시도 정책
- PATCH에서 **필드 미전달 / 명시적 null / 실제 값** 구분 정책

YAML은 이 프로젝트의 사용자 정의 명세다. 표준 OData 스키마라고 가정하지 않는다. 필드가 누락되거나 서로 모순되면 오류 목록을 작성하고 임의로 채우지 않는다.

SAP 메타데이터가 제공되면 그것을 기준으로 이름·키·타입을 검증한다. 접근할 수 없으면 **검증되지 않음**으로 표시한다.

## 5. 3단계 — 생성 계획 작성

코드를 수정하기 전에 다음을 한글로 보고한다.

- 처리할 YAML 목록 및 API Endpoint 목록
- 생성할 파일 / 수정할 파일 / 재사용할 파일 목록
- 실제로 사용할 `RapClient` 메서드와 공통 응답 처리 방식
- Web ↔ OData DTO 필드 매핑 표
- Swagger ApiDoc 구성 및 테스트 범위
- 중복 클래스, 기존 코드 충돌, 불확실한 사항

**기본 모드에서는 여기서 멈추고 승인을 요청한다.** 사용자가 `생성해줘`, `즉시 생성`, `generate`처럼 코드 생성을 명시했다면 필수 정보가 충분한 경우에 한해 계속 진행한다. 사용자 승인 없이 공통 모듈을 수정하지 않는다.

## 6. 4단계 — 코드 생성

### 6.1 표준 패키지 구조

`{basePackage}.{feature}` 아래에 다음 구조를 따른다.

```text
controller/
  {Feature}Controller.java
  {Feature}ApiDoc.java
service/
  {Feature}Service.java
  impl/{Feature}ServiceImpl.java
repository/
  {Feature}Repository.java
  impl/{Feature}RepositoryImpl.java
dto/
  web/request/
  web/response/
  odata/request/
  odata/response/
  converter/{Feature}Converter.java
```

기존 동일 기능의 파일을 우선 재사용한다. DTO가 불필요한 GET/DELETE 요청에서는 불필요한 클래스를 만들지 않는다.

### 6.2 Controller / ApiDoc

- Controller는 `@RestController`, HTTP 매핑, 입력 검증, Service 호출, `CommonApiResponse` 반환만 담당한다.
- Controller는 `{Feature}ApiDoc` 인터페이스를 구현한다.
- Swagger의 `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter` 등은 ApiDoc 인터페이스에 작성한다.
- ApiDoc의 메서드 시그니처는 Controller 구현과 일치시킨다.
- Repository나 RapClient를 Controller에서 직접 호출하지 않는다.
- springdoc-openapi에서 인터페이스 어노테이션이 실제 문서에 반영되는지 확인한다.

### 6.3 Service / Repository

- Service 및 Repository는 인터페이스와 `impl` 구현체로 분리한다.
- Service는 비즈니스 로직과 MapStruct 변환을 담당한다.
- Repository 구현체는 **기존 공통 RapClient만** 사용해 OData V4 요청을 수행한다.
- 별도 WebClient, RestTemplate, HTTP 클라이언트 또는 중복 인증 로직을 생성하지 않는다.
- OData 요청/응답 DTO를 Repository 경계에서 사용한다.
- 인증, CSRF, ETag, 오류 변환은 실제 공통 모듈 지원 방식에 맞춘다.

### 6.4 DTO / Jackson

- 연동 DTO는 연결 대상인 SAP 대신 통신 규격인 OData를 기준으로 명명한다.
- OData DTO 클래스에는 `OData` 접두사를 사용한다. 예: `ODataEmployeeCreateRequest`, `ODataEmployeeResponse`.
- Web DTO에는 접두사 없이 `EmployeeCreateRequest`, `EmployeeResponse`처럼 역할을 표시한다.
- YAML의 `sap`, `sapRequest`, `sapResponse`, `sapField` 키는 기존 명세와의 호환성을 위해 유지한다. `sap`은 SAP 서비스 경로·EntitySet·Action 등의 연결 정보다.
- YAML의 `dtoName` 및 `converter.mappings`의 타입·메서드명은 OData 명명 규칙을 따른다. 기존 `Sap...`/`toSap...` 값은 생성 시 `OData...`/`toOData...`로 정규화하고 대응 관계를 보고한다.
- DTO는 Java `record`를 사용한다.
- Web DTO: `dto.web.request`, `dto.web.response`; JSON lowerCamelCase.
- OData DTO: `dto.odata.request`, `dto.odata.response`.
- OData DTO record 타입에 `@JsonIgnoreProperties(ignoreUnknown = true)`를 적용한다.
- SAP JSON 필드가 `EmployeeId` 형태이면 record 타입에 `@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)`를 적용한다. 일반 필드마다 `@JsonProperty`를 반복해서 붙이지 않는다.
- SAP JSON 필드가 `EMPLOYEE_ID` 형태이면 `UpperSnakeCaseStrategy`를 적용한다.
- 컬렉션의 `value`는 `@JsonProperty("value")`로 명시하여 UpperCamelCase가 적용되어도 소문자를 유지한다. `@odata.count`, `@odata.nextLink` 및 Naming Strategy로 표현할 수 없는 예외 필드만 `@JsonProperty`로 명시한다.
- 현재 PATCH DTO는 일반 nullable 필드를 사용한다. `PatchField` 등의 presence-aware 타입과 `@JsonValue` Map 변환은 사용하지 않는다. 미전달과 명시적 null은 모두 변경하지 않는 것으로 처리하며, OData 수정 요청 record에 `@JsonInclude(JsonInclude.Include.NON_NULL)`을 적용해 null 필드를 전송에서 제외한다.
- 실제 SAP `$metadata`/응답을 확인하기 전에는 Naming Strategy를 임의로 확정하지 않는다. 사용자가 명시적으로 지정한 경우 그 전략을 적용하고 실제 SAP 계약은 미검증으로 보고한다.
- Jackson import는 실제 프로젝트 버전에 맞춘다. 현재 Jackson 3에서는 `tools.jackson.databind.PropertyNamingStrategies`, `tools.jackson.databind.annotation.JsonNaming`을 사용한다.
- 기존 직렬화·역직렬화 테스트로 일반 필드의 대소문자, 컬렉션 메타데이터 및 PATCH null 필드 생략 처리가 유지되는지 검증한다.
- 전역 ObjectMapper 설정을 임의로 변경하지 않는다.

```java
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public record ODataEmployeeResponse(
        String employeeId,
        String employeeName,
        String departmentCode) {
}
```

### 6.5 MapStruct

- YAML의 `converter`와 `converter.mappings`는 선택 사항이다. 생략하면 생성된 Web/OData DTO와 API 동작으로 `{Feature}Converter` 및 필요한 메서드·매핑을 자동 구성한다. 추론 가능한 mappings를 YAML에 다시 저장하지 않는다.
- 요청 본문 DTO가 있으면 Web Request → OData Request, 응답 DTO가 있으면 OData Response → Web Response 변환을 생성한다. 목록 응답에는 항목 변환과 List 변환을 생성한다. 조회 조건만 있는 요청이나 응답이 없는 작업에는 불필요한 변환을 만들지 않는다.
- Java 필드명과 타입이 같으면 MapStruct 자동 매핑을 사용한다. 원본 sample 키와 lowerCamelCase 정규화·source 정보로 대응이 명확한 이름 차이는 `@Mapping`으로 처리한다. JSON 필드명의 대소문자는 Jackson에서 처리하며 불필요한 `@Mapping`을 만들지 않는다.
- 명시적인 `converter`는 이름·메서드명·특수 매핑을 지정하거나 `enabled: false`로 변환을 끄는 override로 사용한다. 계산 필드나 모호한 대응 관계는 추측하지 않고 확인한다. 필요한 필드별 매핑 테스트는 기존 규칙대로 작성한다.
- Converter 위치는 반드시 `dto.converter`다.
- 변환 메서드는 `toODataCreateRequest`, `toODataUpdateRequest`, `toODataApproveRequest`, `toWebResponse`처럼 명명한다.
- 기존 공통 MapperConfig가 있으면 재사용한다.
- 기본 설정은 `componentModel = "spring"`, `unmappedSourcePolicy = IGNORE`, `unmappedTargetPolicy = IGNORE`다.
- Web Request → OData Request, OData Response → Web Response를 변환한다.
- 목록, 중첩 객체, 이름이 다른 필드는 필요한 매핑을 정의한다.
- Service에서 BeanUtils, ModelMapper 또는 수동 필드 복사를 사용하지 않는다.
- IGNORE 정책 때문에 매핑 누락이 컴파일에서 발견되지 않을 수 있으므로 필수 필드별 Converter 테스트를 작성한다.

### 6.6 OData V4 세부 처리

- `$filter` 문자열 리터럴 이스케이프 및 안전한 URL 인코딩을 적용한다.
- `$orderby`, `$select`, `$expand` 등 필드명은 명세/메타데이터의 허용 목록으로 검증한다.
- 1-based `page` → `$skip = (page - 1) * size`, `$top = size`로 변환한다.
- `@odata.nextLink`와 `$count`는 실제 응답 정책에 따라 처리한다.
- SAP V2 전용 `d.results` 구조를 사용하지 않는다.
- 현재 PATCH는 미전달과 명시적 null을 구분하지 않고 기존 값을 유지한다. 값이 있는 필드만 변경하며 빈 요청은 허용한다. 명세가 missing/null/value 구분을 요구하면 현재 정책과의 충돌을 보고하고 확인한 뒤 적용한다.
- 변경 요청에 대한 자동 재시도는 멱등성과 SAP 계약을 확인한 후에만 사용한다.

## 7. 5단계 — 테스트 생성

기존 테스트 패턴과 의존성을 재사용하고, 가능하면 실제 SAP 연결 없이 실행되는 테스트를 만든다.

- Controller: 매핑, 요청 검증, HTTP 상태, 공통 응답
- Service: Repository 및 Converter 호출, 정상/오류 흐름
- Repository: RapClient 호출 인자, OData 쿼리, 응답 처리
- Converter: 필드 매핑, 리스트, 중첩 객체, null, 타입 변환
- PATCH: 누락/null/값, ETag 충돌
- Action: 실제 Action 경로, 파라미터, 응답 타입

## 8. 6단계 — 검증 및 수정

프로젝트의 Gradle Wrapper가 존재하면 다음을 실행한다.

```bash
./gradlew compileJava
./gradlew test
```

Windows 환경 또는 프로젝트 규칙에 따라 실행 방법을 조정한다. 관련 테스트를 우선 실행한 후 전체 테스트를 확인할 수 있다.

- 실패 로그의 근본 원인을 분석한다.
- 이번 작업에서 변경한 파일 범위 내에서 수정한다.
- 동일 문제에 대한 자동 수정은 최대 3회 시도한다.
- 무관한 기존 테스트 실패를 숨기거나 임의 수정하지 않는다.
- 명령을 실행할 수 없으면 **미실행**이라고 보고한다.
- `git diff`와 `git status`로 변경 범위를 확인한다.

## 9. 안전 및 변경 관리

- 공통 모듈은 승인 없이 수정하지 않는다.
- 기존 파일을 분석 없이 덮어쓰지 않는다.
- 사용자 작성 변경 사항을 보존한다.
- 임의로 의존성을 추가하거나 API 계약을 변경하지 않는다.
- 명세와 실제 SAP 계약이 충돌하면 추측하지 않는다.
- 사용자가 요청하지 않은 Git commit, push, PR 생성, 배포는 수행하지 않는다.
- 테스트 통과를 위해 검증을 삭제하거나 실패를 무시하지 않는다.

## 10. 최종 보고 형식

결과는 반드시 한글로 아래 순서로 보고한다.

1. **처리 대상**: YAML 경로와 API Endpoint
2. **생성 파일**: 신규 파일 목록
3. **수정 파일**: 기존 파일 변경 목록
4. **재사용 항목**: RapClient, CommonApiResponse 등
5. **DTO 매핑**: 주요 Web/OData 필드 대응 관계
6. **검증 결과**: 컴파일, 테스트, Swagger 확인 여부(성공/실패/미실행)
7. **주의 사항**: 미검증 SAP 메타데이터, 미해결 문제
8. **다음 단계**: 사용자 확인이 필요한 사항

**완료 조건:** 실제 코드 생성이 요청된 경우에만 구현 및 검증을 수행하며, 검증되지 않은 항목을 성공으로 표시하지 않는다.


## 공통 모듈 미발견 시 Mock 기반 자동 생성 규칙

### 기본 정책

공통 모듈이나 SAP 연동 기능을 찾지 못하더라도 API 생성을 전체 중단하지 않는다.

기존 공통 모듈을 우선 탐색하고, 확인되지 않은 의존성은 개발 및 테스트 전용 Mock으로 대체한다.

단, Mock 구현은 운영 환경에서 사용하지 않는다.

### 1. Mock 적용 대상

다음 컴포넌트를 찾지 못하면 임시 구현을 허용한다.

- RapClient
- CommonApiResponse
- ErrorCode
- 공통 예외 클래스
- 공통 페이징 응답
- SAP 연동 Repository

### 2. Mock 구현 원칙

- 실제 공통 모듈이 존재하면 Mock을 생성하지 않는다.
- Mock 클래스는 기존 공통 모듈 패키지에 생성하지 않는다.
- 임시 구현은 별도 mock 또는 stub 패키지에 생성한다.
- Mock 클래스에는 임시 구현임을 명시한다.
- 실제 공통 모듈의 메서드 시그니처를 아는 것처럼 구현하지 않는다.
- 확인되지 않은 계약은 임시 계약으로 명확히 구분한다.
- 실제 구현으로 교체할 위치에 TODO를 남긴다.
- Mock으로 인해 기존 운영 코드의 동작을 변경하지 않는다.

### 3. Mock 실행 환경

개발용 Mock은 Spring Profile로 분리한다.

- mock: Mock Repository 및 임시 응답 사용
- local/dev: 실제 공통 모듈이 있으면 실제 구현 사용
- prod: Mock 구현 사용 금지

Mock 구현체에는 다음을 적용한다.

@Profile("mock")

실제 Repository 구현체와 Mock 구현체가 동시에 등록되지 않도록 Bean 등록 조건을 확인한다.

운영 환경에서 Mock이 활성화되지 않도록 검증한다.

### 4. 테스트 Mock

JUnit 테스트에서는 다음을 사용한다.

- Mockito @Mock
- Mockito @InjectMocks
- Spring @MockitoBean

Spring Boot 3.4.x에서는 @MockitoBean을 우선 사용한다.

### 5. Mock 데이터

SAP 응답을 확인할 수 없는 경우 YAML에 정의된 필드를 기준으로 샘플 데이터를 생성한다.

- 필드 타입을 준수한다.
- 샘플 데이터임을 명시한다.
- 실제 SAP 데이터라고 주장하지 않는다.
- 조회, 상세, 생성, 수정, 삭제, Action 테스트에 필요한 시나리오를 제공한다.
- 0건, 1건, 중복 결과, 오류 응답 시나리오를 포함한다.

### 6. 공통 응답 미발견

CommonApiResponse를 찾지 못하면 임시 응답 타입을 생성할 수 있다.

단, 임시 응답 타입은 기존 공통 타입과 동일한 것으로 간주하지 않는다.

- 임시 응답 계약을 문서화한다.
- 실제 CommonApiResponse 도입 시 변경해야 할 부분을 보고한다.
- 임시 타입과 실제 타입이 동시에 사용되어 충돌하지 않도록 한다.

### 7. YAML 명세 처리

- Markdown 코드 펜스는 제거한다.
- basePackage가 실제 프로젝트와 다르면 실제 프로젝트 패키지를 우선한다.
- 필수 필드가 없는 경우 확인 가능한 범위에서 생성한다.
- Mock에서도 현재 PATCH 정책을 동일하게 적용한다. 임시 presence-aware 타입을 자동 생성하지 않는다.
- Mock 처리 여부와 실제 계약 미검증 항목을 보고한다.

### 8. 코드 생성

Mock이 필요한 경우에도 다음 파일을 생성한다.

- Controller
- ApiDoc
- Service / ServiceImpl
- Repository / RepositoryImpl
- Web Request/Response DTO
- OData Request/Response DTO
- MapStruct Converter
- JUnit 테스트

RepositoryImpl이 실제 RapClient에 의존하여 구현 불가능하면 실제 연동 코드를 추측하지 않는다.

대신 별도의 Mock Repository 구현체를 생성하여 mock 프로파일에서 API가 동작하도록 한다.

### 9. 검증

다음 항목을 확인한다.

- Gradle compileJava
- Gradle test
- MapStruct 컴파일
- Mock Profile 실행 가능 여부
- Mock 데이터 반환 검증
- Swagger 문서 생성
- 운영 Profile에서 Mock 구현이 활성화되지 않는지 확인

검증할 수 없는 항목은 미검증으로 보고한다.

### 10. 완료 상태

- COMPLETE: 실제 공통 모듈 기반 생성 및 검증 완료
- MOCK_COMPLETE: Mock 환경에서 생성 및 검증 완료
- PARTIAL: 일부 기능만 생성됨
- BLOCKED: Mock으로도 안전하게 진행할 수 없는 경우

MOCK_COMPLETE를 실제 SAP 연동 완료로 보고하지 않는다.

### 11. 최종 보고

다음 내용을 한글로 보고한다.

1. 생성된 API 목록
2. 생성된 Mock 클래스 목록
3. Mock 적용 사유
4. Mock 실행 방법
5. 컴파일 및 테스트 결과
6. 실제 공통 모듈 연결 시 교체 대상
7. 실제 SAP 연동 전 확인 사항
