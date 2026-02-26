# RegExceptionHelper 예외 테스트 자동화 보고서

> **프로젝트:** Registration
> **대상 파일:** `Keywords/words/RegExceptionHelper.groovy`
> **참조 화면:** `Registration.html` (관리자 등록 폼)
> **작성일:** 2026-02-26
> **프레임워크:** Katalon Studio (Groovy 기반)

---

## 1. 개요

`RegExceptionHelper`는 **관리자 등록 화면**에서 발생할 수 있는 **예외 입력값(비정상 데이터)** 에 대한 유효성 검증을 자동화하는 Katalon Custom Keyword 클래스입니다.

등록 폼의 각 입력 필드에 잘못된 값을 순차적으로 입력하고, 시스템이 올바른 에러 메시지를 팝업으로 출력하는지 자동 검증합니다.

---

## 2. 대상 화면 구조 (Registration.html)

| 순서 | 필드명 | 타입 | 설명 |
|------|--------|------|------|
| 1 | 관리자 ID | `input[text]` | 4~20자, 영문/숫자/이모지 허용, 중복체크 필수 |
| 2 | 비밀번호 | `input[password]` | 영문+숫자+특수문자 조합 8자 이상 |
| 3 | 비밀번호 확인 | `input[password]` | 비밀번호와 동일해야 함 |
| 4 | 관리자 이름 | `input[text]` | 한글 실명 입력 |
| 5 | 이메일 주소 | `input[email]` | 표준 이메일 형식 |
| 6 | 관리자 유형 | `select` | 전체 관리자 / QA 담당자 선택 |
| 7 | 마케팅 수신 동의 | `radio` | SMS / 이메일 / 거부 중 선택 (거부 시 등록 차단) |

---

## 3. RegExceptionHelper 클래스 구조

```
RegExceptionHelper
├── execute()             ← 진입점 (Katalon @Keyword)
├── getValue()            ← 현재 값 읽기
├── setValue()            ← 값 입력 / 선택 / 복원
├── getTarget()           ← 동적 XPath로 요소 조회
├── action()              ← 버튼 클릭 (중복체크 / 등록)
├── getPopupText()        ← 팝업 메시지 텍스트 추출 및 닫기
├── js()                  ← JavaScript 실행 래퍼
└── printBox()            ← 결과 리포트 출력 (박스 형식)
```

---

## 4. 테스트 케이스 목록

### 4-1. 아이디 (input 1번째)

| # | 입력값 | 설명 | 예상 에러 키워드 |
|---|--------|------|-----------------|
| 1 | `""` | 빈 값 | 아이디 |
| 2 | `a` | 1자 (길이 미달) | 아이디 |
| 3 | `abcde` | 5자 (길이 미달 — 최소 6자 미만) | 아이디 |
| 4 | `관리자123` | 한글 불가 | 아이디 |
| 5 | `✨✨123` | 이모지 불가 | 아이디 |
| 6 | `user 01` | 공백 불가 | 아이디 |
| 7 | `user!@#` | 특수문자 불가 | 아이디 |
| 8 | 51자 문자열 | 길이 초과 | 아이디 |
| 9 | 101자 문자열 | 길이 초과 | 아이디 |
| 10 | 50자 문장 | 한글 문장 (길이 + 문자 불가) | 아이디 |
| 11 | 100자 문장 | 한글 문장 (길이 + 문자 불가) | 아이디 |

> 아이디 필드는 값 입력 후 **중복체크 버튼**을 클릭하여 팝업 메시지를 검증합니다.

---

### 4-2. 비밀번호 (input 2번째)

| # | 입력값 | 설명 | 예상 에러 키워드 |
|---|--------|------|-----------------|
| 1 | `""` | 빈 값 | 비밀번호 |
| 2 | `1234567` | 7자 (길이 부족) | 비밀번호 |
| 3 | `password` | 영문만 입력 (숫자/특수문자 없음) | 비밀번호 |
| 4 | `12345678` | 숫자만 입력 (영문/특수문자 없음) | 비밀번호 |
| 5 | ` !@#a$%^&* ` | 앞뒤 공백 포함 | 비밀번호 |
| 6 | 50자 문장 | 한글 문장 | 비밀번호 |
| 7 | 100자 문장 | 한글 문장 | 비밀번호 |

---

### 4-3. 비밀번호 확인 (input 3번째)

| # | 입력값 | 설명 | 예상 에러 키워드 |
|---|--------|------|-----------------|
| 1 | `wrong!@#123456789012345` | 길이 초과 + 불일치 | 비밀번호 |
| 2 | `✨✨✨✨dadad!@#` | 이모지 포함 + 불일치 | 비밀번호 |
| 3 | `wrongpassword` | 단어 다름 | 비밀번호 |
| 4 | `alsgh12!@ ` | 끝에 공백 포함 | 비밀번호 |
| 5 | `ALSGH12!@#` | 대소문자 다름 | 비밀번호 |
| 6 | 50자 문장 | 한글 문장 | 비밀번호 |
| 7 | 100자 문장 | 한글 문장 | 비밀번호 |

---

### 4-4. 이름 (input 4번째)

| # | 입력값 | 설명 | 예상 에러 키워드 |
|---|--------|------|-----------------|
| 1 | `""` | 빈 값 | 이름 |
| 2 | `123` | 숫자 불가 | 이름 |
| 3 | `정` | 한 글자 불가 | 이름 |
| 4 | `Jeong` | 영문 불가 | 이름 |
| 5 | `정 민호` | 중간 공백 불가 | 이름 |
| 6 | `정min호` | 한영 혼용 불가 | 이름 |
| 7 | `정!@#` | 특수문자 불가 | 이름 |
| 8 | 50자 문장 | 길이 초과 | 이름 |
| 9 | 100자 문장 | 길이 초과 | 이름 |

---

### 4-5. 이메일 (input 5번째)

| # | 입력값 | 설명 | 예상 에러 키워드 |
|---|--------|------|-----------------|
| 1 | `""` | 빈 값 | 이메일 |
| 2 | `test@` | 도메인 누락 | 이메일 |
| 3 | `@gmail.com` | 계정명 누락 | 이메일 |
| 4 | `test.gmail.com` | @ 기호 누락 | 이메일 |
| 5 | `test@gmail` | .com 없음 | 이메일 |
| 6 | `test@gmail..com` | 연속 점(..) 불가 | 이메일 |
| 7 | 50자 문장 | 길이 초과 | 이메일 |
| 8 | 100자 문장 | 길이 초과 | 이메일 |

---

### 4-6. 관리자 유형 (select 태그)

| # | 선택값 | 설명 | 예상 결과 |
|---|--------|------|-----------|
| 1 | `""` (미선택) | 유형 미선택 | 관리자 유형 에러 |
| 2 | `전체관리자` | 전체 관리자 선택 | 전체관리자 포함 메시지 |
| 3 | `QA 담당자` | QA 담당자 선택 | QA 담당자 포함 메시지 |

> `select` 태그는 페이지 내 select 태그 순서(index)를 기준으로 대상 요소를 찾습니다.

---

### 4-7. 마케팅 수신 동의 (radio 태그)

| # | 선택값 | 설명 | 예상 결과 |
|---|--------|------|-----------|
| 1 | `SMS` | SMS 선택 | SMS 포함 메시지 |
| 2 | `이메일` | 이메일 선택 | 이메일 포함 메시지 |
| 3 | `거부` | 거부 선택 | SNS 포함 메시지 (거부 차단) |

> 빈 값(`""`)을 넣으면 JavaScript로 라디오 버튼 강제 해제 후 검증합니다.

---

## 5. 실행 흐름

```
execute() 시작
    │
    ├─ 1. 현재 입력값 백업 (org 맵에 저장)
    │
    ├─ 2. testData 배열 순차 반복
    │       │
    │       ├─ 요소 존재 확인 (waitForElementPresent)
    │       ├─ 아이디 필드인 경우 → "중복체크" 버튼 클릭
    │       ├─ 아이디 외 첫 번째 필드 → 아이디 중복체크 선행 처리
    │       ├─ 예외값 입력 (setValue)
    │       ├─ 버튼 클릭 → 팝업 발생 대기
    │       ├─ 팝업 메시지 추출 (getPopupText)
    │       ├─ 검증 로직 실행
    │       │     ✅ PASS: 팝업에 예상 키워드 포함 + "사용 가능한" / "성공적으로" 미포함
    │       │     ❌ FAIL: 위 조건 불충족
    │       ├─ 결과 출력 (printBox)
    │       └─ 원래 값으로 복원 (setValue with isRestore=true)
    │
    └─ 완료
```

---

## 6. 검증 판정 기준

```groovy
boolean pass = (
    actual.contains(d.e)              // 팝업에 예상 키워드 포함
    && !actual.contains("사용 가능한") // 정상 통과 문구 없어야 함
    && !actual.contains("성공적으로")  // 성공 문구 없어야 함
)
```

| 조건 | 판정 |
|------|------|
| 팝업에 예상 키워드 포함 + 성공 문구 없음 | ✅ PASS |
| 팝업에 예상 키워드 없음 | ❌ FAIL |
| 팝업에 "사용 가능한" 또는 "성공적으로" 포함 | ❌ FAIL |

---

## 7. 리포트 출력 형식

테스트 케이스별로 아래 형식의 박스가 Katalon 로그에 출력됩니다.

```
╔══════════════════════════════════════════════════════════╗
║  ✅  [PASS]  테스트 성공                                 ║
╠══════════════════════════════════════════════════════════╣
║  🔍  타입   : 예외 검증                                  ║
║  🎯  항목   : 아이디                                     ║
║  ⌨️  입력   : (빈 값)                                    ║
║  📝  결과   : [빈 값] 결과: 아이디 형식이 올바르지 않...  ║
║  🌐  URL    : file:///C:/Users/.../Registration.html     ║
╚══════════════════════════════════════════════════════════╝
```

- **FAIL 시**: 자동으로 스크린샷을 캡처하고 `FAIL_yyyyMMdd_HHmmss.png` 파일로 저장

---

## 8. 사용 방법

### 8-1. 사전 준비

1. Katalon Studio에서 `Registration` 프로젝트 오픈
2. `Registration.html` 파일을 로컬 경로에 배치 (기본값: `C:/Users/ssr0128/Desktop/Registration.html`)
3. Object Repository에 폼 요소 등록 확인

### 8-2. 테스트 케이스 스크립트 (Scripts/Registration)

```groovy
// 1. 브라우저 열기
WebUI.openBrowser('')
WebUI.navigateToUrl('file:///C:/Users/ssr0128/Desktop/Registration.html')

// 2. 정상 데이터 입력 (예외 테스트 복원 기준값)
WebUI.setText(findTestObject('...adminId'), 'ssr0128')
WebUI.setEncryptedText(findTestObject('...password'), '암호화값')
WebUI.setEncryptedText(findTestObject('...passwordConfirm'), '암호화값')
WebUI.setText(findTestObject('...adminName'), '정민호')
WebUI.setText(findTestObject('...adminEmail'), 'sssr0123@ad.com')
WebUI.selectOptionByValue(findTestObject('...select_QA'), 'super', true)
WebUI.click(findTestObject('...input_SMS_marketing'))

// 3. 예외 테스트 실행
CustomKeywords.'words.RegExceptionHelper.execute'()
```

### 8-3. 직접 호출

Katalon Custom Keyword로 등록되어 있으므로, 테스트 스크립트 어디서든 호출 가능합니다.

```groovy
CustomKeywords.'words.RegExceptionHelper.execute'()
```

---

## 9. 내부 도우미 메서드 상세

| 메서드 | 역할 |
|--------|------|
| `getValue(tag, i)` | 지정 필드의 현재 값 읽기. radio는 JS로 checked 값 반환 |
| `setValue(tag, target, i, v, isRestore)` | input은 setText, select는 selectOptionBy*, radio는 JS click |
| `getTarget(idx, tag)` | 동적 XPath 생성: input은 text/password/email/tel/number 타입 통합 |
| `action(t)` | 버튼 텍스트로 요소 찾아 클릭. 실패 시 JS click 폴백 |
| `getPopupText()` | 브라우저 Alert → CSS 기반 커스텀 팝업 순으로 메시지 추출 |
| `js(script, obj)` | `WebUI.executeJavaScript` 래퍼. TestObject 전달 시 arguments[0]으로 주입 |
| `printBox(pass, msg, type, f, v)` | 박스 형식 로그 출력. FAIL 시 스크린샷 자동 저장 |

---

## 10. 총 테스트 케이스 수

| 필드 | 케이스 수 |
|------|-----------|
| 아이디 | 11건 |
| 비밀번호 | 7건 |
| 비밀번호 확인 | 7건 |
| 이름 | 9건 |
| 이메일 | 8건 |
| 관리자 유형 (select) | 3건 |
| 마케팅 수신 동의 (radio) | 3건 |
| **합계** | **48건** |
