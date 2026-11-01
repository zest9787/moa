# Employee API 생성 결과

## 상태와 범위

상태: **MOCK_COMPLETE**.

`api-specs/employee`의 6개 명세를 적용했다. 공통 모듈이 없어 스킬의 Mock 허용 규칙에 따라
개발·테스트용 구현을 생성했다. 실제 SAP 연동 완료를 의미하지 않는다.

- 실제 프로젝트 패키지 `net.lucid.moa.employee` 사용.
- 명세의 `dto.sap.request`, `dto.sap.response`, `dto.converter` 구조 사용.
- Java 21 / Spring Boot 4.1.2 / Jackson 3 등 실제 Gradle 의존성을 사용. 의존성 추가 없음.
- 명세의 Markdown 코드 펜스를 제거했으며 API 계약 필드는 변경하지 않음.
- `mock & !prod` 조건을 Controller, ServiceImpl, Mock Repository, 임시 Advice에 적용.
- 기본/local/dev/prod에서는 직원 Mock API가 노출되지 않음. `mock,prod` 동시 활성화 시에도 미등록.
- MapStruct 생성 구현체는 직접 관리하지 않음. IntelliJ의 `src/main/generated`도 Git에서 제외.

## API

| 명세 | Method | 경로 | 성공 상태 | data |
|---|---|---|---|---|
| search.yaml | GET | /api/v1/employees | 200 | 임시 페이지 응답 |
| detail.yaml | GET | /api/v1/employees/{employeeId} | 200 | EmployeeResponse |
| create.yaml | POST | /api/v1/employees | 201 | EmployeeResponse |
| update.yaml | PATCH | /api/v1/employees/{employeeId} | 200 | null |
| delete.yaml | DELETE | /api/v1/employees/{employeeId} | 200 | null |
| approve.yaml | POST | /api/v1/employees/{employeeId}/approve | 200 | EmployeeApproveResponse |

성공 상태 코드는 Mock의 임시 HTTP 계약이다. 실제 공통 API 정책 연결 시 확인한다.
경로 변수만 필요한 상세·삭제 요청에는 별도 DTO를 만들지 않았다.

## 실행

PowerShell, 프로젝트 루트:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=mock'
```

- API: `http://localhost:8080/api/v1/employees`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

초기 데이터는 `E0001`, `E0002` 두 건이며 실제 SAP 데이터가 아닌 샘플이다.
두 사원명은 `Sample Employee`, 부서는 각각 `D001`, `D002`이다.
수정 데이터는 메모리에만 보관되며 프로세스 재시작 시 초기화된다.

## 임시 응답 및 오류 계약

`MockApiResponse<T>`는 실제 CommonApiResponse를 모사한 타입이 아닌 임시 계약이다.

```json
{"success":true,"data":{"content":[],"totalElements":0,"page":1,"size":20},"error":null}
```

- 페이지 기본값: page=1, size=20. page>=1, 1<=size<=100.
- 목록은 EmployeeId 오름차순이며 employeeName은 contains, departmentCode는 정확히 일치.
- 같은 사원명은 중복 결과로 반환한다. 필터 값은 Mock에서 문자열로만 비교한다.
- 오류: 잘못된 요청 400, 사원 없음 404, 사전 조회 이후 ETag 변경 412.
- PATCH/DELETE 응답은 공통 응답을 전제로 한 `data:null`의 200 응답.

## DTO 및 변환

| Java/Web 필드 | YAML의 SAP 필드 | 타입 |
|---|---|---|
| employeeId | EmployeeId | String |
| employeeName | EmployeeName | String |
| departmentCode | DepartmentCode | String |
| hireDate | HireDate | LocalDate |
| approvalComment | ApprovalComment | String |

변환은 `EmployeeConverter`의 MapStruct 생성 코드를 사용한다.
명세의 승인 응답은 같은 DTO 이름을 쓰면서 hireDate가 없다. 응답 필드를 추가하지 않도록
`EmployeeApproveResponse`, `SapEmployeeApproveResponse`로 이름만 분리했다.

사용자가 지정한 record 단위 `@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)`를 적용했다.
일반 필드의 `@JsonProperty`는 제거하고 알 수 없는 SAP 필드는 무시한다. 실제 SAP 메타데이터는 미검증이다.
V4 목록의 `value`, `@odata.count`, `@odata.nextLink`는 규격의 이름을 유지하도록 `@JsonProperty`로 명시한다.
V4 목록의 value, @odata.count, @odata.nextLink를 위한 DTO를 제공한다.
Mock은 정확한 count와 전체 페이지를 반환하므로 nextLink는 항상 null이다.

PATCH는 일반 nullable String 필드를 사용하며, 미전달과 명시적 null을 구분하지 않는다.

- `{}`: 두 필드 모두 미전달, 기존 값 유지.
- `{"employeeName":null}`: 사원명 기존 값 유지.
- `{"employeeName":"Kim"}`: 사원명을 실제 값으로 변경.

MapStruct가 값을 SAP 요청으로 전달한다. SAP 수정 요청 record의
`@JsonInclude(JsonInclude.Include.NON_NULL)`로 null 필드를 전송에서 제외한다.
`PatchField`, delegating JSON factory, `@JsonValue` Map 변환은 사용하지 않는다.
현재 정책으로는 명시적 null을 이용한 값 삭제를 지원하지 않는다. 전역 ObjectMapper는 변경하지 않았다.

## 생성 파일

모든 Java 경로는 `src/main/java/net/lucid/moa/employee` 기준이다.

- controller: EmployeeController, EmployeeApiDoc
- service: EmployeeService, impl/EmployeeServiceImpl
- repository: EmployeeRepository, EmployeeQuery, impl/README.md
- dto/web/request: EmployeeSearchRequest, EmployeeCreateRequest, EmployeeUpdateRequest, EmployeeApproveRequest
- dto/web/response: EmployeeResponse, EmployeeApproveResponse
- dto/sap/request: SapEmployeeCreateRequest, SapEmployeeUpdateRequest, SapEmployeeApproveRequest
- dto/sap/response: SapEmployeeResponse, SapEmployeeApproveResponse, SapEmployeeCollectionResponse
- dto/converter: EmployeeConverter
- mock: MockEmployeeRepositoryImpl, MockEmployeeException, MockEmployeeExceptionHandler
- mock/contract: MockApiResponse, MockPageResponse, MockVersionedEmployee

테스트는 `src/test/java/net/lucid/moa/employee`의 다음 클래스에 작성했다.

- EmployeeApiIntegrationTest: 6개 API, validation, PATCH null 무시, Swagger 문서 생성.
- EmployeeControllerTest: Service mock을 사용한 HTTP 412와 검증 실패 시 호출 차단.
- EmployeeServiceTest: 조회·생성·승인 매핑, 수정·삭제 사전 ETag 조회, 오류 전달.
- MockEmployeeRepositoryTest: 0건/1건/중복 검색, 페이징, CRUD, 승인, 오래된 ETag 충돌.
- EmployeeConverterTest: 전체 필드, 목록, null, 날짜, JSON 키, V4 메타데이터, PATCH 직렬화.
- EmployeeProfileTest: local/dev/prod 및 prod+mock에서 Mock 컴포넌트 미등록.
- EmployeeSpecificationTest: 6개 YAML 파싱 및 Endpoint 중복 검증.

기존 파일 수정은 5개 YAML의 코드 펜스·들여쓰기 정리와 `.gitignore`의 생성 코드 제외다.
기존 사용자 변경 사항, AGENT.md, SKILL.md, Gradle 의존성 및 애플리케이션 설정은 수정하지 않았다.

## 검증

```powershell
.\gradlew.bat compileJava --offline
.\gradlew.bat test --offline
```

컴파일 및 MapStruct 구현체 생성 성공. 기존 테스트를 포함한 전체 23개 테스트 성공(실패/오류 0).
Mock 프로파일 Spring 컨텍스트 기동, MockMvc API 호출 및 `/v3/api-docs` 문서 생성 성공.
prod 및 prod+mock에서 Mock 미등록 확인. `git diff --check` 통과.
실제 서버 포트로 실행한 수동 HTTP 확인과 SAP 연결 검증은 미실행이다.

## 실제 SAP 연결 시 교체 및 확인 사항

1. 실제 RapClient 의존성·메서드·공통 응답/오류/페이지 계약을 확인한다.
2. repository.impl에 실제 EmployeeRepositoryImpl을 추가한다. Mock RapClient나 임의 HTTP 클라이언트는 생성하지 않았다.
3. MockApiResponse, MockPageResponse, MockVersionedEmployee 및 임시 Advice를 실제 계약으로 교체한다.
4. Controller와 Service의 Mock 전용 프로파일 조건을 실제 배포 정책에 맞춰 교체한다.
5. SAP 서비스 경로 `/sap/opu/odata4/example/employee/srvd/sap/employee/0001`, Employees EntitySet,
   EmployeeId 문자열 키, bound Action `ExampleService.Approve`는 YAML 값이며 실제 계약은 미검증이다.
6. 실제 Repository의 $select/$filter/$orderby/$top/$skip/$count, 문자열 이스케이프·URL 인코딩을 확인하고 테스트한다.
   현재 Mock은 HTTP 요청을 만들지 않으므로 OData URL 처리 검증은 하지 않았다.
7. 서버 주도 페이징과 @odata.nextLink 처리, count 누락 정책을 정한다. 실제 Service 연결 전에 Mock 페이지 가정을 교체한다.
8. CSRF, 실제 ETag/If-Match 헤더, SAP 오류 변환, 변경 요청 재시도 정책을 공통 모듈로 연결한다.
   현재 ETag는 Mock의 동시성 비교이며 실제 SAP 헤더 검증이 아니다. 변경 요청은 자동 재시도하지 않는다.
9. ApprovalComment를 포함한 SAP 승인 업무 처리를 확인한다. Mock 승인은 존재 확인·응답·버전 변경만 수행한다.
10. metadata/실제 JSON으로 필드명, 날짜 타입, nullable 여부 및 승인 응답의 hireDate 제외를 확인한다.
