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
import org.openqa.selenium.JavascriptExecutor

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                                      ║
 * ║   📌 클래스명  : RegExceptionHelper                                                  ║
 * ║   📌 클래스 역할: 회원가입 페이지의 다양한 입력 필드에 대해 비정상 데이터를 입력하여     ║
 * ║                   시스템의 방어 기제(Validation)가 정상 작동하는지 검증합니다.          ║
 * ║                                                                                      ║
 * ╠══════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                      ║
 * ║   [주요 특징]                                                                        ║
 * ║   1. 상세한 한글 주석: 각 로직의 목적과 XPath 전략을 상세히 설명                     ║
 * ║   2. 전체 빈 값 검증: 모든 필수 필드를 비운 상태에서의 등록 시도 시나리오 포함         ║
 * ║   3. 로그 최적화: 개별 실패 시 스택 트래이스 도배를 막기 위해 markWarning 사용       ║
 * ║   4. 최종 결과 선언: 모든 테스트 종료 후 실패가 있다면 딱 한 번 markFailed 호출      ║
 * ║                                                                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════╝
 */
class RegExceptionHelper {

	/**
	 * 🚀 execute() - 테스트 실행 메인 컨트롤러
	 * 개별 필드 예외 테스트와 전체 빈값 테스트를 순차적으로 수행합니다.
	 */
	@Keyword
	static void execute() {
		// 페이지 로딩 및 안정화를 위해 1초 대기
		WebUI.delay(1)

		// 테스트용 긴 문자열 (경계값 검사 시 사용)
		def s50  = '안녕하세요. 오늘 하루도 행복하고 즐거운 시간이 되기를 진심으로 바랍니다. 늘 건강하세요.'
		def s100 = '오늘은 맑고 쾌청한 하늘이 아름다운 날입니다. 이런 날에는 가벼운 산책을 하며 기분 전환을 해보시는 것은 어떨까요?'

		/**
		 * 📋 [1] 테스트 시나리오 데이터 정의
		 * f: 필드명, i: 순번, v: 입력값, e: 기대팝업키워드, d: 설명, tag: 요소타입
		 */
		def testData = [
			// --- [아이디] ---
			[f:'아이디', i:1, v:'', e:'아이디', d:'빈 값'],
			[f:'아이디', i:1, v:'abcde', e:'아이디', d:'5자 (길이 미달)'],
			[f:'아이디', i:1, v:('a'*51), e:'아이디', d:'51자 (길이 초과)'],
			[f:'아이디', i:1, v:'관리자123', e:'아이디', d:'한글 불가'],
			[f:'아이디', i:1, v:'user!@#', e:'아이디', d:'특수문자 불가'],
			[f:'아이디', i:1, v:'user 01', e:'아이디', d:'공백 불가'],

			// --- [비밀번호] ---
			[f:'비밀번호', i:2, v:'', e:'비밀번호', d:'빈 값'],
			[f:'비밀번호', i:2, v:'1234567', e:'비밀번호', d:'길이 미달 (7자)'],
			[f:'비밀번호', i:2, v:'password', e:'비밀번호', d:'영문만 입력'],
			[f:'비밀번호', i:2, v:'12345678', e:'비밀번호', d:'숫자만 입력'],
			[f:'비밀번호', i:2, v:' !@#a$%^&* ', e:'비밀번호', d:'공백 포함 불가'],

			// --- [비밀번호 확인] ---
			[f:'비밀번호 확인', i:3, v:'', e:'비밀번호 확인', d:'빈 값'],
			[f:'비밀번호 확인', i:3, v:'1234567', e:'비밀번호 확인', d:'길이 미달 (7자)'],
			[f:'비밀번호 확인', i:3, v:'password', e:'비밀번호 확인', d:'영문만 입력'],
			[f:'비밀번호 확인', i:3, v:'12345678', e:'비밀번호 확인', d:'숫자만 입력'],
			[f:'비밀번호 확인', i:3, v:' !@#a$%^&* ', e:'비밀번호 확인', d:'공백 포함 불가'],
			[f:'비밀번호 확인', i:3, v:'wrong!@#1', e:'비밀번호 확인', d:'불일치'],

			// --- [이름] ---
			[f:'이름', i:4, v:'', e:'이름', d:'빈 값'],
			[f:'이름', i:4, v:'123', e:'이름', d:'숫자 불가'],
			[f:'이름', i:4, v:'홍 길동', e:'이름', d:'공백 불가'],

			// --- [이메일] ---
			[f:'이메일', i:5, v:'', e:'이메일', d:'빈 값'],
			[f:'이메일', i:5, v:'test@', e:'이메일', d:'도메인 누락'],
			[f:'이메일', i:5, v:'te st@test.com', e:'이메일', d:'공백 불가'],
			[f:'이메일', i:5, v:'te!!st@test.com', e:'이메일', d:'특수문자 2개 불가'],

			// --- [콤보박스/라디오] ---
			[f:'관리자 유형', i:1, tag:'select', v:'', e:'관리자 유형', d:'유형 미선택'],
			[f:'수신 동의', i:1, tag:'radio', v:'', e:'마케팅 정보', d:'동의 여부 미선택']
		]

		/**
		 * 📦 [2] 원본 데이터 백업 및 필드 목록 수집
		 * 테스트 후 원복을 위해 현재 화면의 상태를 저장합니다.
		 */
		def org = [:]
		def fields = []
		testData.each { d ->
			String tag = d.tag ?: 'input'
			String key = "${tag}_${d.i}"
			if (!org.containsKey(key)) {
				org[key] = getValue(tag, d.i)
				fields << [tag: tag, index: d.i, name: d.f]
			}
		}

		int failCount = 0
		boolean idChecked = false

		/**
		 * 🔄 [3] 메인 시나리오 반복 루프
		 */
		testData.each { d ->
			try {
				WebUI.comment("🔍 [검증 진행] 항목: ${d.f} | 시나리오: ${d.d}")
				String tag = d.tag ?: 'input'
				def target = getTarget(d.i, tag)

				if (!WebUI.waitForElementPresent(target, 2, OPTIONAL)) {
					printBox(false, "요소를 찾을 수 없음", "탐색 실패", d.f)
					return
				}

				// 타 필드 검증을 위해 아이디 중복체크 선행
				boolean isIdField = (tag == 'input' && d.i == 1)
				if (!isIdField && !idChecked) {
					action("중복체크")
					getPopupText()
					idChecked = true
				}

				WebUI.scrollToElement(target, 2, OPTIONAL)
				setValue(tag, target, d.i, d.v, false)
				WebUI.delay(0.5)

				// 버튼 클릭 시 시나리오에 맞는 버튼 선택 (아이디는 중복체크, 나머지는 등록)
				action(isIdField ? "중복체크" : "등록")

				String actual = getPopupText()

				// PASS 조건 판단 (기대 키워드 포함 + 성공 키워드 미포함)
				boolean pass = (actual.contains(d.e)
						&& !actual.contains("사용 가능한")
						&& !actual.contains("성공적으로"))

				if (!pass) failCount++

				// 결과 출력 (스택 트래이스 방지를 위해 printBox 내부 로직 최적화됨)
				printBox(pass, "[${d.d}] 결과: ${actual ?: '팝업 없음'}", "개별 예외 검증", d.f, d.v)

				// 해당 필드 원래 값으로 복원
				setValue(tag, target, d.i, org["${tag}_${d.i}"] ?: "", true)
			} catch (Exception e) {
				failCount++
				printBox(false, "시스템 예외: ${e.message}", "오류 발생", d.f)
			}
		}

		/**
		 * 🧪 [4] 전체 필드 빈 값 검증 시나리오
		 * 모든 필수 필드를 비운 채 등록했을 때 시스템 방어 확인
		 */
		WebUI.comment("🧪 [전체 검증] 모든 입력 필드를 비우고 등록을 시도합니다.")
		try {
			fields.each { field ->
				def target = getTarget(field.index, field.tag)
				setValue(field.tag, target, field.index, "", false)
			}
			WebUI.delay(0.5)
			action("등록")

			String actual = getPopupText()

			// 모든 필드가 비었을 때 적절한 경고 팝업이 뜨면 PASS
			boolean passAllEmpty = (actual != "" && (actual.contains("아이디") || actual.contains("필수")))

			if (!passAllEmpty) failCount++
			printBox(passAllEmpty, "결과: ${actual ?: '팝업 미발생'}", "전체 빈값 검증", "모든 필드", "ALL EMPTY")
		} catch (Exception e) {
			failCount++
			printBox(false, "오류: ${e.message}", "전체 검증 실패", "모든 필드")
		} finally {
			// [5] 테스트 종료 후 모든 필드를 최종 복구
			fields.each { field ->
				String key = "${field.tag}_${field.index}"
				def target = getTarget(field.index, field.tag)
				setValue(field.tag, target, field.index, org[key] ?: "", true)
			}
		}

		/**
		 * 🏁 [6] 최종 결과 선언
		 * ⚠️ 수정 포인트: failCount > 0 일 때 logInfo만 호출하면 스텝이 PASS(초록)로
		 *    유지되므로, 박스 요약 출력 후 반드시 markFailed를 호출해야 빨간색으로 표시됩니다.
		 */
		if (failCount > 0) {
			String summary = "\n" +
					"╔══════════════════════════════════════════════════════════╗\n" +
					"║  🚨  최종 결과: 총 ${failCount}건의 결함 발견                 ║\n" +
					"║  📋  위쪽의 상세 박스 로그를 확인하세요.                    ║\n" +
					"╚══════════════════════════════════════════════════════════╝\n"
			KeywordUtil.logInfo(summary)
			KeywordUtil.markFailed("🚨 총 ${failCount}건의 검증 실패 - 상세 내용은 위 박스 로그 참조")
		} else {
			KeywordUtil.markPassed("✅ 모든 예외 검증 테스트를 완벽하게 통과했습니다!")
		}
	}

	// ═══════════════════════════════════════════════════════════════════════════════════════
	// 🔧 도우미 메서드 (Helper Methods)
	// ───────────────────────────────────────────────────────────────────────────────────────

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
			WebUI.setText(target, v ?: "", OPTIONAL)
		}
	}

	private static TestObject getTarget(int idx, String tag) {
		String xp = tag == 'select' ? "(//select)[${idx}]" : tag == 'radio' ? "(//input[@type='radio'])[${idx}]" : "(//input[@type='text' or @type='password' or @type='email' or @type='tel' or @type='number' or not(@type)])[${idx}]"
		return new TestObject().addProperty("xpath", ConditionType.EQUALS, xp)
	}

	private static void action(String t) {
		def btn = new TestObject().addProperty("xpath", ConditionType.EQUALS, "//button[contains(.,'${t}')] | //input[contains(@value,'${t}')] | //a[contains(.,'${t}')]")
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
			def t = a.getText()
			a.accept()
			return t
		} catch (e) {}
		return (String) js("var b=document.evaluate(\"//button[contains(.,'확인') or contains(@class,'ok')]\",document,null,9,null).singleNodeValue;if(b){var t=b.parentElement.innerText;b.click();return t;}return '';")
				?.trim()?.replaceAll("\\s+", " ")
	}

	private static Object js(String script, TestObject obj = null) {
		return obj ? WebUI.executeJavaScript(script, [WebUI.findWebElement(obj)]) : WebUI.executeJavaScript(script, null)
	}

	/**
	 * 🖨️ printBox - 결과를 박스 형태로 출력합니다.
	 * markFailed를 반복 호출하면 생기는 로그 도배(Stack Trace)를 방지하기 위해
	 * logInfo와 markWarning의 조합을 사용합니다.
	 */
	private static void printBox(boolean pass, String msg, String type, String f = "미지정", String v = null) {
		String u = {
			try {
				return WebUI.getUrl()
			} catch (e) {
				return ""
			}
		}()
		String c = !pass ? {
			try {
				def n = "FAIL_${new Date().format('yyyyMMdd_HHmmss')}.png"
				WebUI.takeScreenshot(RunConfiguration.getReportFolder() + "/" + n)
				return n
			} catch (e) {
				return ""
			}
		}() : ""

		def trim = { String str -> str ? (str.take(40) + (str.length() > 40 ? "..." : "")) : "" }
		String safeV = v != null ? trim(v == "" ? "(빈 값)" : v) : ""

		String s = "\n╔══════════════════════════════════════════════════════════╗\n" +
				(pass ? "║  ✅  [PASS]  검증 성공                                   ║\n" : "║  ❌  [FAIL]  검증 실패                                   ║\n") +
				"╠══════════════════════════════════════════════════════════╣\n" +
				String.format("║  🔍  유형   : %-42s ║\n", type) +
				String.format("║  🎯  항목   : %-42s ║\n", f) +
				(v != null ? String.format("║  ⌨️  입력   : %-42s ║\n", safeV) : "") +
				String.format("║  📝  결과   : %-42s ║\n", trim(msg)) +
				(u ? String.format("║  🌐  URL    : %-42s ║\n", u) : "") +
				(c ? String.format("║  📸  캡처   : %-42s ║\n", c) : "") +
				"╚══════════════════════════════════════════════════════════╝\n"

		// 스택 트래이스가 붙지 않는 일반 텍스트 로그로 박스를 출력합니다.
		KeywordUtil.logInfo(s)

		// 실패인 경우 상태 표시줄에 경고 아이콘만 표시하고 콘솔에 ERROR 로그를 뿜지 않게 합니다.
		if (!pass) {
			KeywordUtil.markWarning("❌ [실패] ${f} 검증 실패")
		}
	}
}