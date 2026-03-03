package words

import com.kms.katalon.core.annotation.Keyword
import static com.kms.katalon.core.model.FailureHandling.OPTIONAL
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.Alert

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                                      ║
 * ║   📌 클래스명  : RegExceptionHelper                                                  ║
 * ║   📌 클래스 역할: 관리자 등록 페이지의 다양한 입력 필드에 대해 비정상 데이터 및        ║
 * ║                   비정상 플로우를 입력/실행하여 시스템 방어 기제(Validation)가         ║
 * ║                   정상 작동하는지 검증합니다.                                         ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║   [검증 구성]                                                                        ║
 * ║   A. 필드 단위 예외 (runFieldTests)  : 각 입력 필드에 비정상값 입력 후 등록 시도      ║
 * ║   B. 플로우 단위 예외 (runFlowTests) : 정상값이지만 순서/조합이 잘못된 시나리오       ║
 * ║      ├─ [F1]  중복체크 없이 등록 시도                                                ║
 * ║      ├─ [F2]  중복체크 완료 후 아이디 수정하고 등록                                  ║
 * ║      ├─ [F3]  중복체크 완료 후 아이디 삭제하고 등록                                  ║
 * ║      ├─ [F4]  비밀번호만 입력하고 확인 비운 채 등록                                  ║
 * ║      ├─ [F5]  비밀번호 확인만 입력하고 비밀번호 비운 채 등록                          ║
 * ║      ├─ [F6]  비밀번호와 확인이 서로 불일치                                          ║
 * ║      ├─ [F7]  관리자 유형 미선택 + 나머지 정상 입력 후 등록                          ║
 * ║      ├─ [F8]  수신동의 미선택 + 나머지 정상 입력 후 등록                             ║
 * ║      ├─ [F9]  아무 입력 없이 등록 버튼 바로 클릭                                     ║
 * ║      ├─ [F10] 정상 입력 후 초기화 → 즉시 등록 시도                                   ║
 * ║      ├─ [F11] 중복체크 후 비밀번호만 변경 (확인과 불일치)                             ║
 * ║      ├─ [F12] 이름 입력 후 삭제하고 등록                                             ║
 * ║      ├─ [F13] 이메일 입력 후 삭제하고 등록                                           ║
 * ║      ├─ [F14] 중복체크 연속 3회 클릭 (안정성)                                        ║
 * ║      ├─ [F15] 등록 성공 후 동일 아이디로 재등록 시도                                  ║
 * ║      ├─ [F16] 중복체크 후 아이디 앞뒤 공백 추가 → 등록                                ║
 * ║      ├─ [F17] 중복체크 후 아이디 대소문자 변경 → 등록                                 ║
 * ║      ├─ [F18] 비밀번호 확인만 입력, 비밀번호는 비운 채 등록                            ║
 * ║      ├─ [F19] 비밀번호가 아이디와 동일한 값 → 등록                                    ║
 * ║      ├─ [F20] 중복체크 후 비밀번호를 규칙 위반 값으로 수정 → 등록                      ║
 * ║      ├─ [F21] 전체 입력 후 관리자 유형 초기 선택으로 되돌리고 등록                     ║
 * ║      ├─ [F22] 초기화 후 아이디/비밀번호만 입력하고 등록                                ║
 * ║      ├─ [F23] 유효 이메일 입력 후 '@' 삭제로 수정 → 등록                              ║
 * ║      ├─ [F24] 이름에 이모지 입력 → 등록                                               ║
 * ║      ├─ [F25] 이름 50자 초과 입력 → 등록                                              ║
 * ║      ├─ [F26] 아이디에 SQL Injection 패턴 입력 → 중복체크                             ║
 * ║      ├─ [F27] 아이디에 XSS 스크립트 입력 → 중복체크                                   ║
 * ║      └─ [F28] 등록 버튼 연속 2회 빠르게 클릭 (중복 요청 방어)                          ║
 * ║                                                                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════╝
 */
class RegExceptionHelper_1{

	/**
	 * 🚀 execute() - 메인 진입점
	 * 필드 단위 예외 검증 → 플로우 단위 예외 검증 순서로 실행합니다.
	 */
	@Keyword
	static void execute() {
		WebUI.delay(1)

		int failCount = 0
		failCount += runFieldTests()
		failCount += runFlowTests()

		// ─── 최종 결과 선언 ───────────────────────────────────────────────────
		if (failCount > 0) {
			String summary = "\n" +
					"╔══════════════════════════════════════════════════════════╗\n" +
					"║  🚨  최종 결과: 총 ${String.format('%2d', failCount)}건의 결함 발견                  ║\n" +
					"║  📋  위쪽의 상세 박스 로그를 확인하세요.                    ║\n" +
					"╚══════════════════════════════════════════════════════════╝\n"
			KeywordUtil.logInfo(summary)
			KeywordUtil.markFailed("🚨 총 ${failCount}건의 검증 실패 - 상세 내용은 위 박스 로그 참조")
		} else {
			KeywordUtil.markPassed("✅ 모든 예외 검증 테스트를 완벽하게 통과했습니다!")
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════════════════
	// 🅐 필드 단위 예외 검증
	// ═══════════════════════════════════════════════════════════════════════════════════════

	/**
	 * 📋 runFieldTests() - 각 입력 필드에 비정상 값을 넣고 시스템 Validation을 확인합니다.
	 * @return 실패 건수
	 */
	private static int runFieldTests() {
		WebUI.comment("━━━ [A] 필드 단위 예외 검증 시작 ━━━")

		def testData = [
			// ── 아이디 ─────────────────────────────────────────────────────────
			[f:'아이디', i:1, v:'',              e:'아이디',    d:'빈 값'],
			[f:'아이디', i:1, v:'abcde',         e:'아이디',    d:'5자 (길이 미달)'],
			[f:'아이디', i:1, v:('a'*51),        e:'아이디',    d:'51자 (길이 초과)'],
			[f:'아이디', i:1, v:'관리자123',      e:'아이디',    d:'한글 불가'],
			[f:'아이디', i:1, v:'user!@#',       e:'아이디',    d:'특수문자 불가'],
			[f:'아이디', i:1, v:'user 01',       e:'아이디',    d:'공백 불가'],
			// ── 비밀번호 ────────────────────────────────────────────────────────
			[f:'비밀번호', i:2, v:'',             e:'비밀번호',  d:'빈 값'],
			[f:'비밀번호', i:2, v:'1234567',      e:'비밀번호',  d:'길이 미달 (7자)'],
			[f:'비밀번호', i:2, v:'password',     e:'비밀번호',  d:'영문만 입력'],
			[f:'비밀번호', i:2, v:'12345678',     e:'비밀번호',  d:'숫자만 입력'],
			[f:'비밀번호', i:2, v:' !@#a$%^&* ', e:'비밀번호',  d:'공백 포함 불가'],
			// ── 비밀번호 확인 ────────────────────────────────────────────────────
			[f:'비밀번호 확인', i:3, v:'',            e:'비밀번호 확인', d:'빈 값'],
			[f:'비밀번호 확인', i:3, v:'1234567',     e:'비밀번호 확인', d:'길이 미달 (7자)'],
			[f:'비밀번호 확인', i:3, v:'password',    e:'비밀번호 확인', d:'영문만 입력'],
			[f:'비밀번호 확인', i:3, v:'12345678',    e:'비밀번호 확인', d:'숫자만 입력'],
			[f:'비밀번호 확인', i:3, v:' !@#a$%^&* ', e:'비밀번호 확인', d:'공백 포함 불가'],
			[f:'비밀번호 확인', i:3, v:'wrong!@#1',   e:'비밀번호 확인', d:'불일치'],
			// ── 이름 ──────────────────────────────────────────────────────────
			[f:'이름', i:4, v:'',        e:'이름', d:'빈 값'],
			[f:'이름', i:4, v:'123',     e:'이름', d:'숫자 불가'],
			[f:'이름', i:4, v:'홍 길동',  e:'이름', d:'공백 불가'],
			// ── 이메일 ──────────────────────────────────────────────────────────
			[f:'이메일', i:5, v:'',                 e:'이메일', d:'빈 값'],
			[f:'이메일', i:5, v:'test@',            e:'이메일', d:'도메인 누락'],
			[f:'이메일', i:5, v:'te st@test.com',   e:'이메일', d:'공백 불가'],
			[f:'이메일', i:5, v:'te!!st@test.com',  e:'이메일', d:'특수문자 2개 불가'],
			// ── 관리자 유형 / 수신 동의 ─────────────────────────────────────────
			[f:'관리자 유형', i:1, tag:'select', v:'', e:'관리자 유형', d:'유형 미선택'],
			[f:'수신 동의',   i:1, tag:'radio',  v:'', e:'마케팅 정보', d:'동의 여부 미선택']
		]

		// 원본 백업
		def org    = [:]
		def fields = []
		testData.each { d ->
			String tag = d.tag ?: 'input'
			String key = "${tag}_${d.i}"
			if (!org.containsKey(key)) {
				org[key] = getValue(tag, d.i)
				fields << [tag: tag, index: d.i, name: d.f]
			}
		}

		int failCount     = 0
		boolean idChecked = false

		testData.each { d ->
			try {
				WebUI.comment("🔍 [필드 검증] 항목: ${d.f} | 시나리오: ${d.d}")
				String tag    = d.tag ?: 'input'
				def    target = getTarget(d.i, tag)

				if (!WebUI.waitForElementPresent(target, 2, OPTIONAL)) {
					printBox(false, "요소를 찾을 수 없음", "탐색 실패", d.f)
					failCount++
					return
				}

				boolean isIdField = (tag == 'input' && d.i == 1)
				if (!isIdField && !idChecked) {
					action("중복체크"); getPopupText(); idChecked = true
				}

				WebUI.scrollToElement(target, 2, OPTIONAL)
				setValue(tag, target, d.i, d.v, false)
				WebUI.delay(0.5)
				action(isIdField ? "중복체크" : "등록")

				String  actual = getPopupText()
				boolean pass   = actual.contains(d.e) &&
						!actual.contains("사용 가능한") &&
						!actual.contains("성공적으로")
				if (!pass) failCount++
				printBox(pass, "[${d.d}] 결과: ${actual ?: '팝업 없음'}", "필드 예외 검증", d.f, d.v)

				setValue(tag, target, d.i, org["${tag}_${d.i}"] ?: "", true)
			} catch (Exception ex) {
				failCount++
				printBox(false, "시스템 예외: ${ex.message}", "오류 발생", d.f)
			}
		}

		// 전체 빈값 시나리오
		WebUI.comment("🧪 [전체 빈값] 모든 필드를 비우고 등록 시도")
		try {
			fields.each { f -> setValue(f.tag, getTarget(f.index, f.tag), f.index, "", false) }
			WebUI.delay(0.5); action("등록")
			String  actual       = getPopupText()
			boolean passAllEmpty = actual != "" && (actual.contains("아이디") || actual.contains("필수"))
			if (!passAllEmpty) failCount++
			printBox(passAllEmpty, "결과: ${actual ?: '팝업 미발생'}", "전체 빈값 검증", "모든 필드", "ALL EMPTY")
		} catch (Exception ex) {
			failCount++
			printBox(false, "오류: ${ex.message}", "전체 빈값 검증 실패", "모든 필드")
		} finally {
			fields.each { f ->
				setValue(f.tag, getTarget(f.index, f.tag), f.index, org["${f.tag}_${f.index}"] ?: "", true)
			}
		}

		return failCount
	}

	// ═══════════════════════════════════════════════════════════════════════════════════════
	// 🅑 플로우 단위 예외 검증
	// ═══════════════════════════════════════════════════════════════════════════════════════

	/**
	 * 🔄 runFlowTests() - 정상 값이지만 입력 순서·조합이 잘못된 시나리오를 검증합니다.
	 *
	 * 각 시나리오는 독립 실행되며, 시나리오 종료 후 화면을 초기화합니다.
	 * @return 실패 건수
	 */
	private static int runFlowTests() {
		WebUI.comment("━━━ [B] 플로우 단위 예외 검증 시작 ━━━")
		int failCount = 0

		// ─────────────────────────────────────────────────────────────────────
		// [F1] 중복체크를 하지 않고 바로 등록 버튼 클릭
		// 기대: "중복체크" 또는 "아이디" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F1", "중복체크 없이 등록 시도", "중복체크|아이디") {
			fillAllFields(skipDuplicateCheck: true)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F2] 중복체크 완료 후 아이디를 다른 값으로 수정하고 등록
		// 기대: "중복체크" 또는 "아이디 확인" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F2", "중복체크 후 아이디 수정하고 등록", "중복체크|아이디") {
			fillAllFields()
			setValue('input', getTarget(1, 'input'), 1, "modified_id01", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F3] 중복체크 완료 후 아이디 필드를 비우고 등록
		// 기대: "아이디" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F3", "중복체크 후 아이디 삭제하고 등록", "아이디") {
			fillAllFields()
			setValue('input', getTarget(1, 'input'), 1, "", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F4] 비밀번호만 입력하고 비밀번호 확인은 비운 채 등록
		// 기대: "비밀번호 확인" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F4", "비밀번호만 입력, 확인 비움 후 등록", "비밀번호 확인|비밀번호") {
			fillAllFields()
			setValue('input', getTarget(3, 'input'), 3, "", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F5] 비밀번호 확인만 입력하고 비밀번호 필드를 비운 채 등록
		// 기대: "비밀번호" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F5", "비밀번호 비움, 확인만 입력 후 등록", "비밀번호") {
			fillAllFields()
			setValue('input', getTarget(2, 'input'), 2, "", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F6] 비밀번호와 비밀번호 확인이 서로 다른 유효한 값으로 입력 후 등록
		// 기대: "비밀번호가 일치하지 않습니다" 또는 "비밀번호 확인" 관련 경고
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F6", "비밀번호 ≠ 비밀번호 확인 불일치 등록", "비밀번호") {
			fillAllFields()
			setValue('input', getTarget(3, 'input'), 3, "Different1!", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F7] 관리자 유형 미선택 + 나머지 모두 정상 입력 후 등록
		// 기대: "관리자 유형" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F7", "관리자 유형 미선택 + 나머지 정상 입력 후 등록", "관리자 유형") {
			fillAllFields()
			js("var s=document.evaluate(\"(//select)[1]\",document,null,9,null).singleNodeValue; if(s){s.selectedIndex=0;s.dispatchEvent(new Event('change'));}")
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F8] 수신동의 라디오 모두 해제 + 나머지 모두 정상 입력 후 등록
		// 기대: "마케팅 정보" 또는 "수신 동의" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F8", "수신동의 미선택 + 나머지 정상 입력 후 등록", "마케팅|수신") {
			fillAllFields()
			js("document.querySelectorAll('input[type=\"radio\"]').forEach(function(r){r.checked=false;})")
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F9] 아무 입력 없이 등록 버튼 클릭 (페이지 첫 진입 상태)
		// 기대: 첫 번째 필수 필드(아이디)에 대한 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F9", "아무 입력 없이 등록 버튼 바로 클릭", "아이디|필수") {
			action("초기화")
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F10] 정상 입력 후 초기화 버튼 클릭 → 즉시 등록 시도
		// 기대: 필드가 모두 비워진 상태이므로 첫 번째 필수 필드 경고
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F10", "정상 입력 후 초기화 → 즉시 등록 시도", "아이디|필수") {
			fillAllFields()
			action("초기화")
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F11] 중복체크 후 비밀번호만 새 값으로 변경 (확인 필드는 그대로 → 불일치)
		// 기대: "비밀번호" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F11", "중복체크 후 비밀번호만 변경 (확인과 불일치)", "비밀번호") {
			fillAllFields()
			setValue('input', getTarget(2, 'input'), 2, "NewPass99!", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F12] 이름 입력 후 런타임에 삭제하고 등록
		// 기대: "이름" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F12", "이름 입력 후 삭제하고 등록", "이름") {
			fillAllFields()
			setValue('input', getTarget(4, 'input'), 4, "", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F13] 이메일 입력 후 런타임에 삭제하고 등록
		// 기대: "이메일" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F13", "이메일 입력 후 삭제하고 등록", "이메일") {
			fillAllFields()
			setValue('input', getTarget(5, 'input'), 5, "", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F14] 중복체크 버튼 연속 3회 클릭 (시스템 안정성 확인)
		// 기대: 예외 없이 매번 정상 응답 반환
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F14", "중복체크 연속 3회 클릭 (안정성)", null) {
			setValue('input', getTarget(1, 'input'), 1, "testuser01", false)
			WebUI.delay(0.3)
			3.times {
				action("중복체크")
				WebUI.delay(0.5)
				getPopupText()
			}
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F15] 등록 성공 후 동일 아이디로 중복체크 재시도 (중복 등록 방어)
		// 기대: "이미 사용 중" 또는 "중복" 관련 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F15", "등록 성공 후 동일 아이디로 재등록 시도", "중복|이미 사용") {
			fillAllFields()
			action("등록")
			getPopupText()   // 등록 성공 팝업 처리
			WebUI.delay(1)
			setValue('input', getTarget(1, 'input'), 1, "testuser01", false)
			action("중복체크")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F16] 중복체크 통과 후 아이디 앞뒤에 공백 추가 → 등록
		// 기대: 공백이 포함된 아이디 형식 오류 또는 중복체크 재요청 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F16", "중복체크 후 아이디 앞뒤 공백 추가 → 등록", "중복체크|아이디|공백") {
			fillAllFields()
			setValue('input', getTarget(1, 'input'), 1, " testuser01 ", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F17] 중복체크 통과 후 아이디 대소문자 변경 → 등록
		// 기대: 중복체크 재요청 팝업 (대소문자 구분 정책 검증)
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F17", "중복체크 후 아이디 대소문자 변경 → 등록", "중복체크|아이디") {
			fillAllFields()
			setValue('input', getTarget(1, 'input'), 1, "TESTUSER01", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F18] 비밀번호 필드를 비운 채 비밀번호 확인만 입력 후 등록
		// 기대: "비밀번호" 입력 요청 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F18", "비밀번호 확인만 입력, 비밀번호 필드 비움 → 등록", "비밀번호") {
			fillAllFields()
			setValue('input', getTarget(2, 'input'), 2, "", false)
			setValue('input', getTarget(3, 'input'), 3, "Test1234!", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F19] 비밀번호를 아이디와 동일한 값으로 입력 → 등록
		// 기대: 아이디=비밀번호 보안 정책 위반 경고 팝업 (정책 있는 경우)
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F19", "비밀번호가 아이디와 동일한 값 → 등록", "비밀번호|아이디") {
			fillAllFields()
			setValue('input', getTarget(2, 'input'), 2, "testuser01", false)
			setValue('input', getTarget(3, 'input'), 3, "testuser01", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F20] 중복체크 통과 후 비밀번호를 규칙 위반 값(영문만)으로 수정 → 등록
		// 기대: "비밀번호" 형식 오류 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F20", "중복체크 후 비밀번호를 규칙 위반값으로 수정 → 등록", "비밀번호") {
			fillAllFields()
			setValue('input', getTarget(2, 'input'), 2, "onlyletters", false)
			setValue('input', getTarget(3, 'input'), 3, "onlyletters", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F21] 전체 정상 입력 → 중복체크 → 관리자 유형을 초기값(미선택)으로 되돌리고 등록
		// 기대: "관리자 유형" 선택 요청 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F21", "전체 입력 후 관리자 유형 초기화 → 등록", "관리자 유형|유형") {
			fillAllFields()
			js("var s=document.evaluate(\"(//select)[1]\",document,null,9,null).singleNodeValue; if(s){s.selectedIndex=0;s.dispatchEvent(new Event('change'));}")
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F22] 초기화 후 아이디와 비밀번호만 재입력하고 등록
		// 기대: 미입력 필드(이름, 이메일 등) 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F22", "초기화 후 아이디/비밀번호만 재입력 → 등록", "이름|이메일|필수") {
			fillAllFields()
			resetForm()
			WebUI.delay(0.3)
			setValue('input', getTarget(1, 'input'), 1, "testuser01", false)
			action("중복체크")
			getPopupText()
			setValue('input', getTarget(2, 'input'), 2, "Test1234!", false)
			setValue('input', getTarget(3, 'input'), 3, "Test1234!", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F23] 유효한 이메일 입력 후 '@'를 삭제하여 형식 오류로 만들고 등록
		// 기대: "이메일" 형식 오류 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F23", "유효 이메일 입력 후 '@' 삭제로 수정 → 등록", "이메일|형식") {
			fillAllFields()
			setValue('input', getTarget(5, 'input'), 5, "validtestexample.com", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F24] 이름 필드에 이모지를 포함한 값 입력 → 등록
		// 기대: 특수문자/이모지 불가 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F24", "이름에 이모지 입력 → 등록", "이름|형식|특수문자") {
			fillAllFields()
			setValue('input', getTarget(4, 'input'), 4, "홍길동😊", false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F25] 이름 필드에 50자 초과 문자열 입력 → 등록
		// 기대: 최대 길이 초과 경고 팝업
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F25", "이름 50자 초과 입력 → 등록", "이름|길이|자") {
			fillAllFields()
			setValue('input', getTarget(4, 'input'), 4, "홍" * 51, false)
			WebUI.delay(0.5)
			action("등록")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F26] 아이디 필드에 SQL Injection 패턴 입력 → 중복체크
		// 기대: 아이디 형식 오류(특수문자 불가) 팝업 - SQL 실행되지 않아야 함
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F26", "아이디에 SQL Injection 패턴 입력 → 중복체크", "아이디|형식|특수문자") {
			setValue('input', getTarget(1, 'input'), 1, "' OR 1=1 --", false)
			WebUI.delay(0.3)
			action("중복체크")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F27] 아이디 필드에 XSS 스크립트 태그 입력 → 중복체크
		// 기대: 아이디 형식 오류(특수문자 불가) 팝업 - 스크립트 실행되지 않아야 함
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F27", "아이디에 XSS 스크립트 입력 → 중복체크", "아이디|형식|특수문자") {
			setValue('input', getTarget(1, 'input'), 1, "<script>alert(1)</script>", false)
			WebUI.delay(0.3)
			action("중복체크")
		}

		// ─────────────────────────────────────────────────────────────────────
		// [F28] 등록 버튼을 연속으로 2회 빠르게 클릭 (중복 요청 방어 검증)
		// 기대: 1회만 처리되거나 중복 요청 차단 - 시스템 오류 없어야 함
		// ─────────────────────────────────────────────────────────────────────
		failCount += flowTest("F28", "등록 버튼 연속 2회 빠르게 클릭", null) {
			fillAllFields()
			def btn = new TestObject().addProperty("xpath", ConditionType.EQUALS,
					"//button[contains(.,'등록')] | //input[contains(@value,'등록')]")
			if (WebUI.waitForElementClickable(btn, 2, OPTIONAL)) {
				try {
					WebUI.click(btn, OPTIONAL)
					WebUI.click(btn, OPTIONAL)
				} catch (e) {
					js("arguments[0].click(); arguments[0].click();", btn)
				}
				WebUI.delay(2)
			}
			getPopupText()  // 결과 팝업 처리
		}

		return failCount
	}

	// ═══════════════════════════════════════════════════════════════════════════════════════
	// 🔧 플로우 테스트 실행 래퍼
	// ═══════════════════════════════════════════════════════════════════════════════════════

	/**
	 * flowTest() - 단일 플로우 시나리오를 실행하고 결과를 반환합니다.
	 *
	 * @param id       시나리오 ID (예: "F1")
	 * @param desc     시나리오 설명
	 * @param expected 기대 팝업 키워드 정규식 패턴 (null 이면 팝업 무관 - 예외 없으면 PASS)
	 * @param steps    실행할 클로저
	 * @return 실패 시 1, 성공 시 0
	 */
	private static int flowTest(String id, String desc, String expected, Closure steps) {
		WebUI.comment("🔁 [플로우 검증 ${id}] ${desc}")

		// ① 케이스 시작 전 이전 케이스에서 잔류한 팝업을 모두 제거
		drainPopups()

		try {
			steps.call()
			String  actual = getPopupText()
			boolean pass   = (expected == null) ? true : (actual && actual.find(expected))

			printBox(pass,
					"결과: ${actual ?: '팝업 없음'}",
					"플로우 예외 검증 [${id}]",
					desc,
					"(플로우 시나리오)")

			// ② 정상 종료 후에도 잔류 팝업 가능성 있으므로 한 번 더 정리
			drainPopups()
			resetForm()
			return pass ? 0 : 1
		} catch (Exception ex) {
			// ③ 예외 발생(이모지 등 BMP 외 문자 입력 오류 포함) 시에도 팝업 정리 후 resetForm
			drainPopups()
			printBox(false, "시스템 예외: " + (ex.message ?: ex.class.simpleName), "플로우 오류 [${id}]", desc)
			resetForm()
			return 1
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════════════════
	// 🔧 공통 헬퍼 메서드
	// ═══════════════════════════════════════════════════════════════════════════════════════

	/**
	 * fillAllFields() - 화면의 모든 필드에 정상 데이터를 입력합니다.
	 * opts.skipDuplicateCheck = true 이면 중복체크 버튼 클릭을 건너뜁니다.
	 */
	private static void fillAllFields(Map opts = [:]) {
		boolean skip = opts.skipDuplicateCheck ?: false

		setValue('input', getTarget(1, 'input'), 1, "testuser01", false)
		WebUI.delay(0.3)

		if (!skip) {
			action("중복체크")
			String popupText = getPopupText()
			WebUI.comment("  ↳ 중복체크 결과: ${popupText}")
		}

		setValue('input', getTarget(2, 'input'), 2, "Test1234!", false)
		setValue('input', getTarget(3, 'input'), 3, "Test1234!", false)
		setValue('input', getTarget(4, 'input'), 4, "홍길동", false)
		setValue('input', getTarget(5, 'input'), 5, "test@example.com", false)

		// 관리자 유형 - 두 번째 옵션 선택
		def selectTarget = getTarget(1, 'select')
		if (WebUI.waitForElementPresent(selectTarget, 2, OPTIONAL)) {
			js("var s=arguments[0]; if(s.options.length>1){s.selectedIndex=1;s.dispatchEvent(new Event('change'));}", selectTarget)
		}

		// 수신동의 - 첫 번째 라디오 클릭
		def radioTarget = getTarget(1, 'radio')
		if (WebUI.waitForElementPresent(radioTarget, 2, OPTIONAL)) {
			js("arguments[0].click();", radioTarget)
		}

		WebUI.delay(0.5)
	}

	/**
	 * resetForm() - 초기화 버튼 또는 새로고침으로 폼을 리셋합니다.
	 */
	private static void resetForm() {
		try {
			def resetBtn = new TestObject().addProperty("xpath", ConditionType.EQUALS,
					"//button[contains(.,'초기화')] | //input[contains(@value,'초기화')] | //a[contains(.,'초기화')]")
			if (WebUI.waitForElementClickable(resetBtn, 2, OPTIONAL)) {
				WebUI.click(resetBtn, OPTIONAL)
				WebUI.delay(1)
				getPopupText()  // 초기화 확인 팝업 처리
			} else {
				WebUI.refresh(); WebUI.delay(1.5)
			}
		} catch (Exception ex) {
			WebUI.refresh(); WebUI.delay(1.5)
		}
	}

	private static String getValue(String tag, int i) {
		if (tag == 'radio') {
			String jsStr = "var el=document.evaluate(\"(//input[@type='radio'])[${i}]\",document,null,9,null).singleNodeValue; return el ? (document.querySelector('input[name=\"'+el.name+'\"]:checked')?.value || '') : '';"
			return (String) js(jsStr)
		}
		def obj = getTarget(i, tag)
		return WebUI.waitForElementPresent(obj, 1, OPTIONAL) ? WebUI.getAttribute(obj, 'value') : ""
	}

	private static void setValue(String tag, TestObject target, int i, String v, boolean isRestore) {
		if (tag == 'select') {
			if (!v || v == '선택') {
				js("arguments[0].selectedIndex=0; arguments[0].dispatchEvent(new Event('change'));", target)
			} else if (isRestore) {
				WebUI.selectOptionByValue(target, v, false, OPTIONAL)
			} else {
				WebUI.selectOptionByLabel(target, v, false, OPTIONAL)
			}
		} else if (tag == 'radio') {
			if (!v) {
				js("var el=document.evaluate(\"(//input[@type='radio'])[${i}]\",document,null,9,null).singleNodeValue; if(el) document.querySelectorAll('input[name=\"'+el.name+'\"]').forEach(function(r){r.checked=false;});")
			} else if (isRestore) {
				js("var el=document.evaluate(\"(//input[@type='radio'])[${i}]\",document,null,9,null).singleNodeValue; if(el) document.querySelectorAll('input[name=\"'+el.name+'\"]').forEach(function(r){r.checked=(r.value=='${v}');});")
			} else {
				js("arguments[0].click();", target)
			}
		} else {
			// BMP 외 문자(이모지 등) 포함 시 WebDriver sendKeys가 실패하므로 JS로 fallback
			try {
				WebUI.setText(target, v ?: "", OPTIONAL)
			} catch (Exception sendEx) {
				// ChromeDriver only supports characters in the BMP → JS value 직접 주입
				js("arguments[0].value=''; arguments[0].focus();", target)
				js("arguments[0].value=arguments[1]; arguments[0].dispatchEvent(new Event('input',{bubbles:true})); arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
						target)  // 주의: 이모지는 value에는 세팅되지만 실제 서버 전송 시 깨질 수 있음
				WebUI.comment("  ↳ [BMP fallback] JS로 값 주입: ${v?.take(20)}")
			}
		}
	}

	private static TestObject getTarget(int idx, String tag) {
		String xp = tag == 'select'
				? "(//select)[${idx}]"
				: tag == 'radio'
				? "(//input[@type='radio'])[${idx}]"
				: "(//input[@type='text' or @type='password' or @type='email' or @type='tel' or @type='number' or not(@type)])[${idx}]"
		return new TestObject().addProperty("xpath", ConditionType.EQUALS, xp)
	}

	private static void action(String t) {
		def btn = new TestObject().addProperty("xpath", ConditionType.EQUALS,
				"//button[contains(.,'${t}')] | //input[contains(@value,'${t}')] | //a[contains(.,'${t}')]")
		if (WebUI.waitForElementClickable(btn, 2, OPTIONAL)) {
			try {
				WebUI.click(btn, OPTIONAL)
			} catch (e) {
				js("arguments[0].click();", btn)
			}
			WebUI.delay(1.5)
		}
	}

	private static String getPopupText() {
		try {
			Alert a = DriverFactory.getWebDriver().switchTo().alert()
			def t = a.getText(); a.accept(); return t
		} catch (e) {}
		return (String) js("var b=document.evaluate(\"//button[contains(.,'확인') or contains(@class,'ok')]\",document,null,9,null).singleNodeValue;if(b){var t=b.parentElement.innerText;b.click();return t;}return '';")
				?.trim()?.replaceAll("\\s+", " ")
	}

	/**
	 * drainPopups() - alert 및 DOM 모달 팝업을 최대 5회까지 연속으로 닫습니다.
	 * 케이스 전환 시 이전 케이스의 잔류 팝업이 남아있는 경우를 방어합니다.
	 */
	private static void drainPopups() {
		for (int _i = 0; _i < 5; _i++) {
			boolean _consumed = false
			try {
				Alert _a = DriverFactory.getWebDriver().switchTo().alert()
				_a.accept()
				WebUI.delay(0.3)
				_consumed = true
			} catch (Exception _ignored) {
				// alert 없음 - DOM 모달 팝업 확인 버튼 탐색
				Boolean _closed = (Boolean) js(
						"var b=document.evaluate(\"//button[contains(.,'확인') or contains(@class,'ok') or contains(@class,'confirm')]\",document,null,9,null).singleNodeValue;" +
						"if(b){b.click();return true;}return false;"
						)
				if (_closed) {
					WebUI.delay(0.3)
					_consumed = true
				}
			}
			if (!_consumed) break
		}
	}

	private static String safeGetUrl() {
		try {
			return WebUI.getUrl()
		} catch (Exception e) {
			return ""
		}
	}

	private static String safeScreenshot() {
		try {
			String n = "FAIL_" + new Date().format("yyyyMMdd_HHmmss") + ".png"
			WebUI.takeScreenshot(RunConfiguration.getReportFolder() + "/" + n)
			return n
		} catch (Exception e) {
			return ""
		}
	}

	private static Object js(String script, TestObject obj = null) {
		return obj
				? WebUI.executeJavaScript(script, [WebUI.findWebElement(obj)])
				: WebUI.executeJavaScript(script, null)
	}

	/**
	 * 🖨️ printBox - 결과를 박스 형태로 출력합니다.
	 * 개별 실패는 markWarning으로 처리하고, 최종 실패는 execute()에서 markFailed 호출.
	 */
	private static void printBox(boolean pass, String msg, String type, String f = "미지정", String v = null) {
		String u = safeGetUrl()
		String c = !pass ? safeScreenshot() : ""

		def trim  = { String str -> str ? (str.take(40) + (str.length() > 40 ? "..." : "")) : "" }
		String safeV = v != null ? trim(v == "" ? "(빈 값)" : v) : ""

		String s = "\n╔══════════════════════════════════════════════════════════╗\n" +
				(pass ? "║  ✅  [PASS]  검증 성공                                   ║\n"
				: "║  ❌  [FAIL]  검증 실패                                   ║\n") +
				"╠══════════════════════════════════════════════════════════╣\n" +
				String.format("║  🔍  유형   : %-42s ║\n", type) +
				String.format("║  🎯  항목   : %-42s ║\n", f) +
				(v != null ? String.format("║  ⌨️  입력   : %-42s ║\n", safeV) : "") +
				String.format("║  📝  결과   : %-42s ║\n", trim(msg)) +
				(u ? String.format("║  🌐  URL    : %-42s ║\n", u) : "") +
				(c ? String.format("║  📸  캡처   : %-42s ║\n", c) : "") +
				"╚══════════════════════════════════════════════════════════╝\n"

		KeywordUtil.logInfo(s)
		if (!pass) KeywordUtil.markWarning("❌ [실패] ${f} 검증 실패")
	}
}
