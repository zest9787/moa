# Spring Boot SAP RAP OData V4 API 개발 규칙

## 1. 프로젝트 개요

본 프로젝트는 React 애플리케이션과 SAP RAP OData V4 서비스 사이에서 REST API를 제공하는 Spring Boot 기반 API Gateway 애플리케이션이다.

Codex는 본 문서의 규칙을 준수하여 API 코드를 생성하거나 수정해야 한다.

### 기본 원칙

- 기존 프로젝트의 아키텍처와 코딩 규칙을 우선한다.
- 공통 모듈 재사용을 최우선으로 한다.
- SAP 연동 로직과 Web API 로직을 분리한다.
- 모든 API는 기능별 패키지 구조를 따른다.
- 반복 생성되는 API의 구조와 명명 규칙을 일관되게 유지한다.
- 기존 소스코드와 API 계약의 하위 호환성을 최대한 보장한다.
- 확인되지 않은 인터페이스나 기능을 임의로 구현하지 않는다.

---

## 2. 기술 환경

| 항목 | 기술 |
|---|---|
| Framework | Spring Boot 3.4.x |
| Language | Java 17 이상 |
| Build | Gradle |
| SAP Integration | SAP RAP OData V4 |
| HTTP Client | 기존 공통 RapClient |
| DTO | Java record |
| DTO Mapping | MapStruct |
| JSON | Jackson |
| API Documentation | Swagger / springdoc-openapi |
| Validation | Jakarta Validation |
| Unit Test | JUnit 5 / Mockito |

### 기술 적용 원칙

- 프로젝트에 선언된 실제 의존성 버전을 우선한다.
- 불필요한 라이브러리를 추가하지 않는다.
- 기존 공통 설정이 있으면 재사용한다.
- Java 17 이상에서 지원하는 문법을 사용한다.
- Spring Boot 3.x의 Jakarta 패키지를 사용한다.

---

## 3. 표준 패키지 구조

모든 API는 기능별 패키지로 관리한다.

```text
{basePackage}
└── {feature}
    ├── controller
    │   ├── {Feature}Controller.java
    │   └── {Feature}ApiDoc.java
    │
    ├── service
    │   ├── {Feature}Service.java
    │   └── impl
    │       └── {Feature}ServiceImpl.java
    │
    ├── repository
    │   ├── {Feature}Repository.java
    │   └── impl
    │       └── {Feature}RepositoryImpl.java
    │
    └── dto
        ├── web
        │   ├── request
        │   └── response
        │
        ├── odata
        │   ├── request
        │   └── response
        │
        └── converter
```

### 패키지 규칙

- 연동 DTO는 연결 대상인 SAP 대신 통신 규격인 OData를 기준으로 명명한다.
- 요청·응답 패키지는 `dto.odata.request`, `dto.odata.response`를 사용한다.
- OData DTO 클래스는 `OData` 접두사를 사용한다. 예: `ODataEmployeeCreateRequest`, `ODataEmployeeResponse`.
- Web DTO는 `EmployeeCreateRequest`, `EmployeeResponse`처럼 접두사 없이 역할을 표시한다.
- Converter는 `dto.converter`에 두며, 변환 메서드는 `toODataCreateRequest`, `toODataUpdateRequest`, `toODataApproveRequest`, `toWebResponse`처럼 명명한다.
- YAML의 `sap`, `sapRequest`, `sapResponse`, `sapField` 키는 기존 명세와의 호환성을 위해 유지한다. `sap`은 SAP 서비스 경로·EntitySet·Action 등의 연결 정보를 나타낸다.
- YAML의 `dtoName` 및 `converter.mappings`의 타입·메서드명은 OData 명명 규칙을 따른다. 기존 `Sap...`/`toSap...` 값은 생성 시 `OData...`/`toOData...`로 정규화하고 대응 관계를 보고한다.

- Controller는 `controller` 패키지에 생성한다.
- Swagger 문서 인터페이스는 Controller와 동일한 패키지에 생성한다.
- Service 인터페이스는 `service`에 생성한다.
- Service 구현체는 `service.impl`에 생성한다.
- Repository 인터페이스는 `repository`에 생성한다.
- Repository 구현체는 `repository.impl`에 생성한다.
- Web DTO는 `dto.web` 하위에 생성한다.
- OData DTO는 `dto.odata` 하위에 생성한다.
- MapStruct Converter는 반드시 `dto.converter`에 생성한다.
- 기존 프로젝트에 같은 기능의 클래스가 있으면 중복 생성하지 않는다.

---

## 4. 공통 모듈 재사용 규칙

다음 공통 컴포넌트는 기존 구현을 반드시 확인하고 재사용한다.

- RapClient
- WebClient 관련 공통 설정
- CommonApiResponse
- ErrorCode
- RestControllerAdvice
- 인증 및 인가 관련 공통 기능
- 공통 예외 처리
- 공통 로깅
- 공통 Swagger 설정
- 공통 MapStruct 설정
- 공통 OData Query 관련 기능

### 필수 규칙

1. 기존 공통 RapClient를 사용한다.
2. 새로운 WebClient 또는 별도 HTTP Client를 생성하지 않는다.
3. 기존 CommonApiResponse를 사용한다.
4. 기존 ErrorCode 및 RestControllerAdvice를 사용한다.
5. 공통 모듈 소스는 사용자 승인 없이 수정하지 않는다.
6. RapClient의 실제 메서드 시그니처를 확인한다.
7. 존재하지 않는 공통 메서드를 임의로 생성하지 않는다.
8. 공통 모듈에서 지원하지 않는 기능은 임의로 대체하지 않는다.
9. 공통 기능 확장이 필요하면 구현 전에 변경 사항을 제안한다.

### 공통 모듈 확인 절차

API 구현 전에 다음 항목을 확인한다.

- RapClient 인터페이스 및 구현체
- RapClient 호출 예제
- SAP 인증 및 요청 헤더 처리 방식
- CommonApiResponse 구조
- 공통 예외 처리 방식
- 기존 OData Query 구성 방식
- MapStruct 설정
- Swagger 설정

확인할 수 없는 필수 사항이 있으면 추측하지 말고 사용자에게 질문한다.

---

## 5. Controller 개발 규칙

Controller는 React 애플리케이션에 REST API를 제공한다.

### 책임

- HTTP 요청 수신
- 요청 파라미터 및 Request Body 검증
- Service 호출
- CommonApiResponse 반환
- HTTP 상태 코드 처리

### 필수 규칙

- `@RestController`를 사용한다.
- `@RequestMapping`으로 기본 경로를 정의한다.
- 요청 검증에 `@Valid`, `@Validated`를 적절히 사용한다.
- 생성자 주입을 사용한다.
- Service 인터페이스에 의존한다.
- Repository 및 RapClient를 직접 호출하지 않는다.
- Controller에서 비즈니스 로직을 구현하지 않는다.
- Controller에서 OData DTO를 직접 사용하거나 반환하지 않는다.
- 응답은 기존 CommonApiResponse 규격을 따른다.
- Swagger 문서화는 별도 ApiDoc 인터페이스에서 관리한다.

---

## 6. Swagger / ApiDoc 규칙

Swagger 문서화는 Controller와 분리한다.

### 파일 위치

```text
controller/
├── EmployeeController.java
└── EmployeeApiDoc.java
```

### 명명 규칙

- `{Feature}Controller.java`
- `{Feature}ApiDoc.java`

### 구현 규칙

1. ApiDoc은 Java interface로 작성한다.
2. Controller는 해당 ApiDoc 인터페이스를 구현한다.
3. `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter` 등 API 문서화 어노테이션은 ApiDoc에 작성한다.
4. Controller에는 실제 HTTP 처리 로직만 작성한다.
5. ApiDoc 메서드의 반환 타입과 파라미터는 Controller 구현 메서드와 일치시킨다.
6. API별 요청 및 응답 스키마를 문서화한다.
7. 공통 응답 구조는 기존 CommonApiResponse를 기준으로 작성한다.
8. OData DTO는 외부 Swagger 응답 스키마에 노출하지 않는다.
9. 기존 springdoc-openapi 설정과 호환되는지 확인한다.
10. 생성 후 OpenAPI 문서에 어노테이션이 정상 반영되는지 검증한다.

---

## 7. Service 개발 규칙

Service는 업무 로직과 OData/Web DTO 변환을 담당한다.

### 구조

```text
service/
├── EmployeeService.java
└── impl/
    └── EmployeeServiceImpl.java
```

### 필수 규칙

- Service 인터페이스와 구현체를 분리한다.
- 구현체에는 `@Service`를 적용한다.
- 생성자 주입을 사용한다.
- Repository 인터페이스에 의존한다.
- MapStruct Converter를 사용한다.
- 비즈니스 로직을 Service에서 처리한다.
- SAP 통신을 Service에서 직접 수행하지 않는다.
- Web DTO와 OData DTO 사이의 필드 변환을 수동으로 구현하지 않는다.
- 공통 예외 처리 정책을 따른다.
- SAP 조회 결과를 Web 응답 규격으로 변환한다.

### DTO 변환 흐름

**요청**

Web Request DTO → MapStruct Converter → OData Request DTO → Repository

**응답**

Repository → OData Response DTO → MapStruct Converter → Web Response DTO

단, GET 조회에서 OData Query Option만 사용하는 경우 불필요한 OData Request Body DTO를 생성하지 않는다.

---

## 8. Repository 개발 규칙

Repository는 SAP RAP OData V4 서비스와의 통신을 담당한다.

### 구조

```text
repository/
├── EmployeeRepository.java
└── impl/
    └── EmployeeRepositoryImpl.java
```

### 필수 규칙

- Repository 인터페이스와 구현체를 분리한다.
- 구현체에는 `@Repository`를 적용한다.
- 생성자 주입을 사용한다.
- 기존 공통 RapClient를 사용한다.
- SAP 서비스 경로 및 EntitySet 정보를 명세에 따라 구성한다.
- OData V4 요청 옵션을 구성한다.
- OData Request DTO를 요청 본문으로 전달한다.
- SAP 응답을 OData Response DTO로 역직렬화한다.
- SAP 오류는 기존 공통 예외 처리 정책에 따라 전달한다.
- Repository에서 Web Response DTO를 생성하지 않는다.
- Repository에서 MapStruct를 이용한 Web DTO 변환을 수행하지 않는다.

### OData V4 지원 항목

필요한 경우 다음 기능을 지원한다.

- `$select`
- `$filter`
- `$expand`
- `$orderby`
- `$top`
- `$skip`
- `$count`
- Entity Key
- Composite Key
- Action
- Function

### OData 처리 주의 사항

- OData V4 문법을 사용한다.
- 문자열 필터 값은 올바르게 이스케이프한다.
- 요청 URL은 안전하게 인코딩한다.
- 날짜와 시간의 SAP EDM 타입을 확인한다.
- Decimal 타입의 정밀도를 보존한다.
- OData 목록 응답의 `value` 구조를 처리한다.
- 필요한 경우 `@odata.count`를 처리한다.
- `@odata.nextLink`가 반환되는 경우 기존 페이징 정책을 확인한다.
- PATCH 요청의 ETag 및 `If-Match` 처리는 SAP 서비스와 공통 RapClient 정책을 따른다.
- PATCH DTO는 일반 nullable 필드를 사용한다. 현재 `PatchField`와 presence-aware 처리는 사용하지 않는다. 미전달과 명시적 null은 모두 기존 값을 유지하며, OData 수정 요청 record의 `@JsonInclude(JsonInclude.Include.NON_NULL)`로 null 필드를 전송에서 제외한다. 빈 요청은 허용한다.
- CSRF 토큰 처리는 기존 공통 모듈 정책을 따른다.
- POST/PATCH 요청의 자동 재시도는 업무 중복 및 멱등성을 검토한 후 적용한다.

---

## 9. DTO 개발 규칙

모든 DTO는 기본적으로 Java record로 작성한다.

### 9.1 Web Request DTO

위치:

`dto.web.request`

- React에서 전달하는 요청 데이터를 정의한다.
- lowerCamelCase를 사용한다.
- 요청 필드에 적절한 Validation을 적용한다.
- 필드 설명이 제공되면 record component에 `@Schema(description = "설명")`을 적용한다. 설명의 `[...]` 부분은 대괄호와 내부 내용을 함께 제거하고 앞뒤 공백을 정리한다.
- SAP 내부 필드명을 직접 노출하지 않는다.

### 9.2 Web Response DTO

위치:

`dto.web.response`

- React에 반환하는 응답 데이터를 정의한다.
- lowerCamelCase를 사용한다.
- SAP 내부 메타데이터를 노출하지 않는다.
- 필드 설명이 제공되면 record component에 `@Schema(description = "설명")`을 적용한다. 설명의 `[...]` 부분은 대괄호와 내부 내용을 함께 제거하고 앞뒤 공백을 정리한다.
- 기존 공통 페이징 및 응답 규격을 우선 사용한다.

### 9.3 OData Request DTO

위치:

`dto.odata.request`

- SAP에 전달하는 Request Body를 정의한다.
- Java 필드명은 lowerCamelCase를 사용한다.
- SAP JSON 필드명은 Jackson Naming Strategy를 적용한다.
- SAP 생성, 수정, Action 요청에 필요한 필드를 정의한다.
- Web Request DTO를 OData Request Body로 직접 사용하지 않는다.

### 9.4 OData Response DTO

위치:

`dto.odata.response`

- SAP OData 응답 구조를 정의한다.
- Java 필드명은 lowerCamelCase를 사용한다.
- SAP JSON 필드명은 Jackson Naming Strategy를 적용한다.
- SAP에서 전달하는 추가 필드는 역직렬화 시 무시할 수 있도록 설정한다.
- OData V4의 메타데이터 필드는 필요에 따라 명시적으로 매핑한다.
- OData Response DTO에는 `@Schema`를 붙이지 않는다. description이 제공되면 각 record component 선언 맨 뒤에 `// 설명` 주석으로 추가한다. 쉼표가 있으면 쉼표 뒤에 주석을 두고 마지막 필드의 닫는 괄호는 다음 줄에 둔다. 설명의 줄바꿈은 공백으로 정리한다.
- description은 선택 사항이다. 설명이 없거나 `[...]` 제거 후 비어 있으면 OData 필드 주석과 Web 필드의 `@Schema(description = ...)`을 생략한다. 타입을 확인할 수 있으면 설명 없이 생성하며, 설명 누락을 오류로 처리하거나 설명을 임의로 만들지 않는다.

### 9.5 OData DTO Jackson 어노테이션

OData Request DTO와 Response DTO에 다음 어노테이션을 적용한다.

```java
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(
    PropertyNamingStrategies.UpperCamelCaseStrategy.class
)
public record ODataEmployeeResponse(
    String employeeId,
    String employeeName,
    String departmentCode
) {
}
```

### 적용 원칙

- SAP 필드명이 `EmployeeId` 형식이면 UpperCamelCaseStrategy를 사용한다.
- SAP 필드명이 `EMPLOYEE_ID` 형식이면 UpperSnakeCaseStrategy를 사용한다.
- 실제 SAP 메타데이터 및 JSON 응답을 기준으로 Naming Strategy를 결정한다.
- Naming Strategy로 처리할 수 없는 필드는 `@JsonProperty`를 적용한다.
- `@odata.count`, `@odata.nextLink` 같은 OData 메타데이터는 명시적으로 매핑한다.
- 전역 ObjectMapper 설정을 변경하지 않는다.

---

## 10. MapStruct 개발 규칙

OData DTO와 Web DTO 사이의 변환은 반드시 MapStruct를 사용한다.

### 패키지 위치

`dto.converter`

### 기본 설정

```java
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EmployeeConverter {
}
```

### 필수 규칙

- 기존 공통 MapperConfig가 있으면 우선 재사용한다.
- YAML의 `converter`와 `converter.mappings`는 선택 사항이다. 생략하면 기능명·Web/OData DTO·API 동작에서 Converter와 필요한 요청·응답·목록 변환을 자동 구성한다. 추론 결과를 YAML에 중복 저장하지 않는다.
- 이름·타입이 같은 필드는 MapStruct 자동 매핑을 사용하고 대응이 명확한 이름 차이만 `@Mapping`으로 처리한다. 명시적인 converter 설정은 특수 매핑·이름 지정·`enabled: false` 등의 override에 사용한다. 모호한 매핑은 임의로 결정하지 않는다.
- Converter는 반드시 dto.converter에 생성한다.
- Service에서 DTO를 수동으로 변환하지 않는다.
- BeanUtils 및 ModelMapper를 사용하지 않는다.
- Web Request → OData Request 변환을 지원한다.
- OData Response → Web Response 변환을 지원한다.
- 목록 변환을 지원한다.
- 중첩 객체 변환을 지원한다.
- 필드명이 다른 경우 `@Mapping`을 사용한다.
- 날짜, 숫자, Enum 등 타입이 다른 경우 명시적 변환 규칙을 적용한다.
- MapStruct가 생성한 구현체를 수동 수정하지 않는다.

### MapStruct IGNORE 정책 보완

`unmappedSourcePolicy`와 `unmappedTargetPolicy`는 IGNORE를 사용한다.

단, 자동 코드 생성 시 다음 사항을 반드시 확인한다.

1. API 명세에 정의된 필수 필드가 매핑되었는지 확인한다.
2. Web Request 필드가 OData Request에 올바르게 전달되는지 확인한다.
3. OData Response 필드가 Web Response에 올바르게 전달되는지 확인한다.
4. 필드명이 다른 경우 명시적으로 매핑한다.
5. 매핑 누락을 방지하기 위한 Converter 테스트를 작성한다.

---

## 11. API 명세 YAML 규칙

Codex는 API별 YAML 명세를 기준으로 코드를 생성한다.

### 명세 위치

```text
api-specs/
└── {feature}/
    ├── search.yaml
    ├── detail.yaml
    ├── create.yaml
    ├── update.yaml
    └── delete.yaml
```

### 명세에 포함할 항목

- 기능명
- API명
- HTTP Method
- REST API Path
- SAP Service Path
- SAP EntitySet
- SAP Operation
- Web Request 필드
- OData Request 필드
- OData Response 필드
- Web Response 필드
- 필드 매핑 관계
- 검색 조건
- 정렬 조건
- 페이징
- Swagger 설명

### 명세 처리 원칙

- `webResponse`, `swagger`, `converter`는 선택 사항이며 생략된 출력 설정을 YAML에 자동으로 추가하지 않는다. 명시된 설정은 override로 적용한다.
- Web 응답 DTO와 Swagger Tag/ApiDoc 이름은 기존 동일 기능의 명명 규칙을 재사용하고 없으면 기능명으로 결정한다. 응답 형태는 API 동작과 페이징 설정으로 판단한다: 페이징 목록 PAGE, 일반 목록 LIST, 단건 OBJECT, 응답 없음 EMPTY. 배열 샘플만으로 PAGE를 가정하지 않는다.
- Swagger는 프로젝트에서 사용 중이면 기본 활성화하며 `{Feature}ApiDoc`을 생성한다. 이름 변경이나 비활성화가 필요할 때만 명세에 설정한다.
- `sapResponse.sample`에 응답 예시, 선택 사항인 `sapResponse.description`에 `EmployeeID 사번[...]` 형태의 설명 목록만 제공하면 된다. 응답 fields는 생성 시 추론하며 YAML에 중복 저장하지 않는다. Web DTO는 description을 `@Schema`로, OData 응답 DTO는 필드 뒤 주석으로 사용한다.
- 배열 래퍼는 `value` 또는 제공된 `data`를 인식하고 실제 이름을 `collectionProperty`에 기록한다. 설명의 `EmployeeID`와 샘플의 `EmployeeId`처럼 대소문자만 다른 경우 정확 매칭을 우선한 뒤 유일한 키에 한해 연결한다. 설명 없는 필드는 그대로 생성한다.
- DTO 생성 입력으로 YAML 외에 OData 응답 JSON 샘플과 `필드명 설명[...]` 목록도 허용한다. 기존 YAML 경로는 필수가 아니며, `api-specs/{feature}`에 sample·description 보관 → 필드·타입·설명 추론 → DTO 생성 순서로 처리한다. API 전체 생성에 필요한 HTTP/연동 계약은 별도로 확인한다.
- `sapResponse.fields`, `webResponse.fields`는 타입 override·이름 변경·필드 선택 등 필요한 경우에만 명시한다. 샘플로 추론할 수 있으면 생략하고 자동으로 추가하지 않는다. 요청 fields는 별도로 유지한다. Web DTO의 `@Schema(description = "...")`는 설명 목록에서 생성 시 정리하여 재생성할 때도 유지한다.
- DTO만 요청되면 API 계약을 추측하지 않고 DTO 정의만 담은 부분 명세로 저장한다. 원본 응답 샘플은 `api-specs/{feature}/samples`에 타입과 형식을 보존한 예시 데이터로 보관하고 YAML 주석에서 참조한다. 확정된 기존 계약과 충돌하면 임의로 덮어쓰지 않는다.
- 필드명으로 응답 샘플과 설명을 연결하고 설명의 대괄호 구간은 내용까지 제거한다. JSON 필드명과 샘플 값 자체는 변경하지 않는다. Web DTO의 대응 필드에 정리한 설명을 `@Schema(description = "...")`로 넣는다.
- 기본 샘플은 `{"value":[{"name":"홍길동","age":23}]}` 형태다. 업무 DTO는 `value` 배열 내부 객체를 기준으로 생성하고, 별도 설명 `name 이름[...]`은 내부 필드 name의 description `이름`으로 연결한다. envelope는 항목 DTO와 분리하며 명세에 `collection: true`, `collectionProperty: value`를 기록한다.
- 샘플의 `name`, `age`처럼 lowerCamelCase인 JSON 키는 그대로 유지한다. UpperCamelCase Naming Strategy는 실제 JSON 필드가 UpperCamelCase인 경우에 적용한다.
- 타입은 명시된 타입·OData 메타데이터를 우선하고, 없으면 전체 샘플에서 추론한다. 숫자처럼 보이는 문자열은 String으로 유지하며, null만 있거나 빈 배열 등으로 타입을 알 수 없는 필드는 추론 불가로 보고한다. 샘플만으로 필수 여부·길이 제한·정밀도 계약을 확정하지 않는다.
- 상세한 샘플 처리·타입 추론 규칙과 예제는 `.agents/skills/generate-sap-api/SKILL.md`의 `2.1 응답 샘플 기반 DTO 생성`을 따른다.
- 필수 항목이 누락되면 사용자에게 확인한다.
- SAP 메타데이터가 있으면 필드 및 타입을 검증한다.
- 기존 DTO 및 Converter 재사용 여부를 먼저 판단한다.
- API 경로와 메서드 중복을 확인한다.
- 명세에 없는 SAP 필드를 임의로 추가하지 않는다.

---

## 12. 테스트 작성 규칙

모든 API는 JUnit 5 기반 테스트를 작성한다.

### Controller 테스트

- HTTP Method 및 URL 검증
- 요청 파라미터 검증
- Validation 검증
- CommonApiResponse 응답 검증
- HTTP 상태 코드 검증

### Service 테스트

- Repository 호출 검증
- Converter 호출 검증
- 정상 처리 검증
- 오류 처리 검증

### Repository 테스트

- RapClient 호출 검증
- SAP 서비스 경로 검증
- OData Query 검증
- 요청 파라미터 검증
- SAP 응답 역직렬화 검증

### Converter 테스트

- Web Request → OData Request
- OData Response → Web Response
- 목록 변환
- null 처리
- 필드명 변경 매핑
- 날짜 및 숫자 변환
- 중첩 객체 변환

### 테스트 원칙

- 실제 SAP 시스템 없이 실행 가능한 단위 테스트를 우선한다.
- 기존 테스트 환경과 라이브러리를 재사용한다.
- 테스트를 통과시키기 위해 업무 코드를 임의로 변경하지 않는다.
- 실패한 테스트는 원인을 분석하여 보고한다.

---

## 13. Codex 코드 생성 절차

Codex는 다음 순서를 따른다.

### Step 1. 프로젝트 분석

- AGENTS.md 확인
- 기존 공통 모듈 확인
- 기존 구현 패턴 분석
- Gradle 설정 확인

### Step 2. API 명세 검증

- YAML 구조 검증
- 필수 필드 검증
- SAP 메타데이터 검증
- 기존 코드와 중복 여부 확인

### Step 3. 코드 생성 계획

생성할 파일과 수정할 파일을 구분한다.

- Controller
- ApiDoc
- Service
- ServiceImpl
- Repository
- RepositoryImpl
- Web Request/Response DTO
- OData Request/Response DTO
- MapStruct Converter
- JUnit 테스트

### Step 4. 코드 생성

- 프로젝트 규칙에 따라 구현한다.
- 기존 공통 모듈을 재사용한다.
- 중복 코드를 최소화한다.
- 기존 API의 동작을 유지한다.

### Step 5. 자동 검증

다음 명령을 실행한다.

```bash
./gradlew compileJava
./gradlew test
```

### Step 6. 오류 수정

- 컴파일 오류를 분석한다.
- 생성한 코드와 관련된 오류를 우선 수정한다.
- 공통 모듈을 임의로 수정하지 않는다.
- 동일 오류에 대한 자동 수정은 최대 3회 시도한다.
- 해결할 수 없는 오류는 사용자에게 보고한다.

### Step 7. 결과 보고

다음 내용을 한글로 보고한다.

1. 생성 파일 목록
2. 수정 파일 목록
3. 재사용한 공통 모듈
4. API Endpoint 목록
5. DTO 및 Converter 매핑 내용
6. Gradle 컴파일 결과
7. JUnit 테스트 결과
8. 추가 확인이 필요한 사항

---

## 14. 기존 코드 보호 규칙

- 기존 파일을 분석 없이 덮어쓰지 않는다.
- 사용자 작성 코드를 임의로 삭제하지 않는다.
- 공통 모듈은 승인 없이 수정하지 않는다.
- 기존 API의 URL과 응답 구조를 임의로 변경하지 않는다.
- 프로젝트 전체를 불필요하게 리팩터링하지 않는다.
- 새로운 라이브러리를 추가하기 전에 기존 의존성을 확인한다.
- 변경 범위는 요청된 기능으로 제한한다.
- 다른 기능에서 사용 중인 DTO 및 Converter를 변경할 때는 영향도를 확인한다.
- Git 변경 사항을 확인하고 기존 사용자 변경 내용을 보존한다.

---

## 15. 코드 생성 완료 조건

API 생성 작업은 다음 조건을 만족해야 완료로 판단한다.

- 프로젝트 아키텍처 준수
- 기존 RapClient 재사용
- CommonApiResponse 적용
- Controller / ApiDoc 분리
- Service / ServiceImpl 분리
- Repository / RepositoryImpl 분리
- Web/OData DTO 분리
- MapStruct Converter 적용
- Swagger 문서화
- JUnit 테스트 작성
- Gradle 컴파일 성공
- 관련 테스트 성공
- 변경 파일 목록 보고

검증되지 않은 항목은 성공으로 보고하지 않는다.
