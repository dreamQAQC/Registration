package words

import com.kms.katalon.core.annotation.Keyword
import static com.kms.katalon.core.model.FailureHandling.OPTIONAL // 코드를 줄이기 위해 OPTIONAL을 직접 임포트합니다.
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.Alert
import org.openqa.selenium.JavascriptExecutor

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * [회원가입 예외 테스트 전용 클래스] - RegExceptionHelper (최적화 버전)
 * ═══════════════════════════════════════════════════════════════════════════════
 */
class RegExceptionHelper {

	@Keyword
	static void execute() {
		WebUI.delay(1)

		// 긴 문장들은 변수로 빼서 코드를 획기적으로 줄입니다.
		def s50 = '안녕하세요. 오늘 하루도 행복하고 즐거운 시간이 되기를 진심으로 바랍니다. 늘 건강하세요.'
		def s100 = '오늘은 맑고 쾌청한 하늘이 아름다운 날입니다. 이런 날에는 가벼운 산책을 하며 기분 전환을 해보시는 것은 어떨까요? 소소한 일상 속에서 편안함과 행복을 찾는 멋진 하루를 보내세요.'

		def testData = [
			// --- [1] 아이디 ---
			[f:'아이디', i:1, v:'', e:'아이디', d:'빈 값'],
			[f:'아이디', i:1, v:'a', e:'아이디', d:'1자 (길이 미달)'],
			[f:'아이디', i:1, v:'abcde', e:'아이디', d:'5자 (길이 미달)'],
			[f:'아이디', i:1, v:'관리자123', e:'아이디', d:'한글 불가'],
			[f:'아이디', i:1, v:'✨✨123', e:'아이디', d:'이모지 불가'],
			[f:'아이디', i:1, v:'user 01', e:'아이디', d:'공백 불가'],
			[f:'아이디', i:1, v:'user!@#', e:'아이디', d:'특수문자 불가'],
			[f:'아이디', i:1, v:('a'*51), e:'아이디', d:'51자 (초과)'],
			[f:'아이디', i:1, v:('a'*101), e:'아이디', d:'101자 (초과)'],
			[f:'아이디', i:1, v:s50, e:'아이디', d:'50자 문장'],
			[f:'아이디', i:1, v:s100, e:'아이디', d:'100자 문장'],
			
			// --- [2] 비밀번호 ---
			[f:'비밀번호', i:2, v:'', e:'비밀번호', d:'빈 값'],
			[f:'비밀번호', i:2, v:'1234567', e:'비밀번호', d:'길이 부족'],
			[f:'비밀번호', i:2, v:'password', e:'비밀번호', d:'영문만 입력'],
			[f:'비밀번호', i:2, v:'12345678', e:'비밀번호', d:'숫자만 입력'],
			[f:'비밀번호', i:2, v:' !@#a$%^&* ', e:'비밀번호', d:'공백 포함 불가'],
			[f:'비밀번호', i:2, v:s50, e:'비밀번호', d:'50자 문장'],
			[f:'비밀번호', i:2, v:s100, e:'비밀번호', d:'100자 문장'],

			// --- [3] 비밀번호 확인 ---
			[f:'비밀번호 확인', i:3, v:'wrong!@#123456789012345', e:'비밀번호', d:'길이 초과'],
			[f:'비밀번호 확인', i:3, v:'✨✨✨✨dadad!@#', e:'비밀번호', d:'이모지 (불일치)'],
			[f:'비밀번호 확인', i:3, v:'wrongpassword', e:'비밀번호', d:'단어 다름'],
			[f:'비밀번호 확인', i:3, v:'alsgh12!@ ', e:'비밀번호', d:'끝에 공백 포함'],
			[f:'비밀번호 확인', i:3, v:'ALSGH12!@#', e:'비밀번호', d:'대소문자 다름'],
			[f:'비밀번호 확인', i:3, v:s50, e:'비밀번호', d:'50자 문장'],
			[f:'비밀번호 확인', i:3, v:s100, e:'비밀번호', d:'100자 문장'],

			// --- [4] 이름 ---
			[f:'이름', i:4, v:'', e:'이름', d:'빈 값'],
			[f:'이름', i:4, v:'123', e:'이름', d:'숫자 불가'],
			[f:'이름', i:4, v:'정', e:'이름', d:'한 글자 불가'],
			[f:'이름', i:4, v:'Jeong', e:'이름', d:'영문 불가'],
			[f:'이름', i:4, v:'정 민호', e:'이름', d:'중간 공백 불가'],
			[f:'이름', i:4, v:'정min호', e:'이름', d:'한영 혼용 불가'],
			[f:'이름', i:4, v:'정!@#', e:'이름', d:'특수문자 불가'],
			[f:'이름', i:4, v:s50, e:'이름', d:'50자 문장'],
			[f:'이름', i:4, v:s100, e:'이름', d:'100자 문장'],

			// --- [5] 이메일 ---
			[f:'이메일', i:5, v:'', e:'이메일', d:'빈 값'],
			[f:'이메일', i:5, v:'test@', e:'이메일', d:'도메인 누락'],
			[f:'이메일', i:5, v:'@gmail.com', e:'이메일', d:'계정명 누락'],
			[f:'이메일', i:5, v:'test.gmail.com', e:'이메일', d:'@ 누락'],
			[f:'이메일', i:5, v:'test@gmail', e:'이메일', d:'.com 누락'],
			[f:'이메일', i:5, v:'test@gmail..com', e:'이메일', d:'연속된 점(..) 불가'],
			[f:'이메일', i:5, v:s50, e:'이메일', d:'50자 문장'],
			[f:'이메일', i:5, v:s100, e:'이메일', d:'100자 문장'],

	// --- [6] 드롭다운(Select Box) 예외 검증 ---
			// 주의: 화면의 전체 입력창 순서와 무관하게, 'select' 태그 중 몇 번째인지 적습니다 (여기선 1번째)
			[f:'관리자 유형', i:1, tag:'select', v:'', e:'관리자 유형', d:'유형 미선택'],
			[f:'관리자 유형', i:2, tag:'select', v:'전체관리자', e:'전체관리자', d:'전체관리자'],
			[f:'관리자 유형', i:3, tag:'select', v:'QA 담당자', e:'QA 담당자', d:'QA 담당자'],
			// --- [7] 라디오 버튼(Radio) 예외 검증 ---
			// 주의: 화면의 'radio' 태그 중 몇 번째 그룹(또는 첫 버튼)인지 적습니다. 빈 값을 넣으면 강제로 체크가 해제됩니다.
			[f:'마케팅 수신 동의', i:1, tag:'radio', v:'SMS', e:'SMS', d:'SMS'],
			[f:'마케팅 수신 동의', i:2, tag:'radio', v:'이메일', e:'이메일', d:'이메일'],
			[f:'마케팅 수신 동의', i:3, tag:'radio', v:'거부', e:'SNS', d:'거부'],
			
		]

		// 1. 기존 정상 데이터 백업
		def org = [:] 
		testData.each { d ->
			String tag = d.tag ?: 'input'
			String key = "${tag}_${d.i}"
			if (!org.containsKey(key)) org[key] = getValue(tag, d.i)
		}

		boolean idChecked = false

		// 2. 예외 테스트 실행
		testData.each { d ->
			try {
				String tag = d.tag ?: 'input'
				def target = getTarget(d.i, tag)
				
				if (!WebUI.waitForElementPresent(target, 2, OPTIONAL)) {
					printBox(false, "오류: 요소를 찾을 수 없음", "요소 찾기 실패", d.f)
					return
				}

				boolean isIdField = (tag == 'input' && d.i == 1)
				if (!isIdField && !idChecked) {
					action("중복체크"); getPopupText(); idChecked = true
				}
				
				WebUI.scrollToElement(target, 2, OPTIONAL)
				
				// 값 셋팅 (입력/선택/체크)
				setValue(tag, target, d.i, d.v, false)
				WebUI.delay(0.5)

				// 팝업 트리거 액션 및 검증
				action(isIdField ? "중복체크" : "등록")
				String actual = getPopupText()
				
				// 예외 케이스 검증 로직 (성공/사용가능 문구 방어)
				boolean pass = (actual.contains(d.e) && !actual.contains("사용 가능한") && !actual.contains("성공적으로"))
				printBox(pass, "[${d.d}] 결과: ${actual ?: '팝업 없음'}", "예외 검증", d.f, d.v)
				
				// 원상 복구
				setValue(tag, target, d.i, org["${tag}_${d.i}"] ?: "", true)

			} catch (Exception e) {
				printBox(false, "시스템 오류: ${e.message}", "예외 검증 중단", d.f)
				getPopupText()
			}
		}
	}

	// ═══════════════════════════════════════════════════════════════════════
	// [도우미 메서드 모음] - 반복되는 코드를 대폭 줄였습니다.
	// ═══════════════════════════════════════════════════════════════════════

	/** 대상 요소의 현재 값을 읽어옵니다. */
	private static String getValue(String tag, int i) {
		if (tag == 'radio') {
			String jsStr = "var el=document.evaluate(\"(//input[@type='radio'])[${i}]\",document,null,9,null).singleNodeValue; return el ? (document.querySelector('input[name=\"'+el.name+'\"]:checked')?.value || '') : '';"
			return (String) js(jsStr)
		}
		def obj = getTarget(i, tag)
		return WebUI.waitForElementPresent(obj, 1, OPTIONAL) ? WebUI.getAttribute(obj, 'value') : ""
	}

	/** 대상 요소에 값을 입력하거나 선택합니다. (복원 여부 isRestore로 분기) */
	private static void setValue(String tag, TestObject target, int i, String v, boolean isRestore) {
		if (tag == 'select') {
			if (!v || v == '선택') js("arguments[0].selectedIndex=0; arguments[0].dispatchEvent(new Event('change'));", target)
			else if (isRestore) WebUI.selectOptionByValue(target, v, false, OPTIONAL)
			else WebUI.selectOptionByLabel(target, v, false, OPTIONAL)
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

	/** 요소(TestObject)를 동적으로 찾습니다. */
	private static TestObject getTarget(int idx, String tag) {
		String xp = tag == 'select' ? "(//select)[${idx}]" : tag == 'radio' ? "(//input[@type='radio'])[${idx}]" : "(//input[@type='text' or @type='password' or @type='email' or @type='tel' or @type='number' or not(@type)])[${idx}]"
		return new TestObject().addProperty("xpath", ConditionType.EQUALS, xp)
	}

	/** 버튼 클릭 (일반 클릭 실패 시 자바스크립트 클릭) */
	private static void action(String t) {
		def btn = new TestObject().addProperty("xpath", ConditionType.EQUALS, "//button[contains(.,'${t}')] | //input[contains(@value,'${t}')] | //a[contains(.,'${t}')]")
		if (WebUI.waitForElementClickable(btn, 2, OPTIONAL)) {
			try { WebUI.click(btn, OPTIONAL) } catch (e) { js("arguments[0].click();", btn) }
			WebUI.delay(1.5)
		}
	}

	/** 팝업 텍스트를 가져오고 닫습니다. */
	private static String getPopupText() {
		try { Alert a = DriverFactory.getWebDriver().switchTo().alert(); def t = a.getText(); a.accept(); return t } catch (e) {}
		return (String) js("var b=document.evaluate(\"//button[contains(.,'확인') or contains(@class,'ok')]\",document,null,9,null).singleNodeValue;if(b){var t=b.parentElement.innerText;b.click();return t;}return '';")?.trim()?.replaceAll("\\s+", " ")
	}

	/** 자바스크립트 실행을 간단하게 만들어주는 도우미 */
	private static Object js(String script, TestObject obj = null) {
		return obj ? WebUI.executeJavaScript(script, [WebUI.findWebElement(obj)]) : WebUI.executeJavaScript(script, null)
	}

	/** 리포트 출력 마법사 (에러 로그 차단 포함) */
	private static void printBox(boolean pass, String msg, String type, String f = "미지정", String v = null) {
		String u = { try { return WebUI.getUrl() } catch (e) { return "" } }()
		String c = !pass ? { try { def n="FAIL_${new Date().format('yyyyMMdd_HHmmss')}.png"; WebUI.takeScreenshot(RunConfiguration.getReportFolder()+"/"+n); return n } catch(e){return ""} }() : ""
		
		// 글자 수 제한 클로저 (표 깨짐 방지)
		def trim = { String str -> str ? (str.take(40) + (str.length() > 40 ? "..." : "")) : "" }
		String safeV = v != null ? trim(v == "" ? "(빈 값)" : v) : ""
		
		String s = "\n╔══════════════════════════════════════════════════════════╗\n" +
				   (pass ? "║  ✅  [PASS]  테스트 성공                                 ║\n" : "║  ❌  [FAIL]  테스트 실패                                 ║\n") +
				   "╠══════════════════════════════════════════════════════════╣\n" +
				   String.format("║  🔍  타입   : %-42s ║\n", type) +
				   String.format("║  🎯  항목   : %-42s ║\n", f) +
				   (v != null ? String.format("║  ⌨️  입력   : %-42s ║\n", safeV) : "") +
				   String.format("║  📝  결과   : %-42s ║\n", trim(msg)) +
				   (u ? String.format("║  🌐  URL    : %-42s ║\n", u) : "") +
				   (c ? String.format("║  📸  캡처   : %-42s ║\n", c) : "") +
				   "╚══════════════════════════════════════════════════════════╝\n"
		
		try { pass ? KeywordUtil.markPassed(s) : KeywordUtil.markWarning(s) } catch (Throwable t) {}
	}
}