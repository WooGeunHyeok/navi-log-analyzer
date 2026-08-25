# Navi Log Analyzer

이슈 로그를 기반으로 소스코드 흐름을 분석하여 이슈 원인 파악을 지원하는 로그 분석 프로그램

## 프로젝트 소개

내비게이션 개발 및 유지보수 과정에서 발생하는 이슈 로그를 보다 효율적으로 분석하기 위해 개발한 로그 분석 프로그램입니다.
기존에는 이슈 로그를 확인한 후 관련 소스코드를 직접 찾아 로그 발생 흐름과 소스코드 로직을 파악해야 했습니다.
이러한 분석 과정을 자동화하여 이슈 로그의 흐름에 따른 소스코드 흐름을 확인할 수 있도록 개발하고 있습니다.

---

## 기술 스택

- Java
- Spring Boot
- Spring Batch
- Spring Data JPA
- MyBatis
- MariaDB
- REST API

---

## Spring Batch

소스코드 기반 로그 데이터를 추출하고 분석에 필요한 데이터를 MariaDB에 저장하는 Batch를 구현했습니다.

### 담당 업무

- 소스코드 기반 로그 데이터 추출
- 추출한 로그 데이터를 JSON 형식으로 저장
- Spring Batch 기반 데이터 처리
- Reader / Processor / Writer 구조 구현
- MyBatis를 활용한 MariaDB 데이터 적재

---

## Spring Boot Backend

사용자가 업로드한 이슈 로그를 분석하고 소스코드 흐름을 확인할 수 있도록 Backend API를 구현했습니다.

### 담당 업무

- 이슈 로그 파일 업로드 API 구현
- 업로드된 로그 파일 파싱
- Container Layer 파싱
- JNI Layer 파싱
- Java Layer 파싱
- 소스코드 흐름도 생성을 위한 파싱 데이터 처리

---

## 개발 현황

### 개발 중

- 소스코드 흐름도 Response API 정확도 개선

### 예정

- React 기반 Frontend 개발
- Frontend - REST API 연동
- 다양한 이슈 로그를 활용한 테스트
- 소스코드 흐름도 정확도 개선
