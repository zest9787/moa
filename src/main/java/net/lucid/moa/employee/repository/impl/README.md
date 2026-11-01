# 실제 Repository 연결 지점

현재는 공통 RapClient와 호출 계약이 없어 `employee.mock.MockEmployeeRepositoryImpl`만 제공합니다.
실제 클라이언트 메서드를 추측한 `EmployeeRepositoryImpl`은 생성하지 않았습니다.

공통 모듈 연결 후 이 패키지에 `EmployeeRepositoryImpl`을 구현하고 실제 프로파일에서 등록하세요.
서비스 경로·키·Action·쿼리는 `api-specs/employee`를 기준으로 하고, CSRF와 ETag는 공통 모듈 정책을 따릅니다.
Mock 계약 교체 항목과 검증하지 못한 내용은 프로젝트의 `docs/employee-api-generation.md`를 참고하세요.
