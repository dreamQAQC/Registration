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
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.util.List
import java.util.ArrayList

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║  RegExceptionHelper (필드 ID 기반 입력)                                   ║
 * ║  - Object Repository의 필드 ID를 감지                                    ║
 * ║  - JavaScript로 직접 값 입력 (화면 변화 자동 대응)                         ║
 * ║  - 54개 TC (53개 예외 + 1개 성공)                                        ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
class RegExceptionHelper {

	private static List<Map>        fieldElements   = new ArrayList<>()
	private static List<Map>        allResults      = new ArrayList<>()
	private static int              totalFail       = 0
	private static long             sessionStart    = 0

	// ★ 필드 ID와 기본값 매핑 (이미지의 Object Repository ID 기반)
	// 화면 구성이 변해도 자동으로 대응합니다
	private static Map<String, String> fieldDefaultValues = [
		'adminId':              'ssr0128',
		'password':             'Valid!@1',
		'passwordConfirm':      'Valid!@1',
		'adminName':            '정민호',
		'adminEmail':           'sssr0123@ad.com',
		'qa':                   'super'
	]

	// ════════════════════════════════════════════════════════════════════════════
	//  ★ 테스트 케이스 데이터 정의 (총 54개)
	// ════════════════════════════════════════════════════════════════════════════

	private static List<Map> getTestCaseList() {
		return [
			[id:'TC#01', field:'ADMIN_ID', type:'단위', desc:'아이디 빈 값', 
			 body:{ fillAllFields(false); setFieldValue('adminId', ''); delay(); clickButton('등록'); popup() },
			 exp:'아이디|필수'],

			[id:'TC#02', field:'ADMIN_ID', type:'단위', desc:'아이디 길이 미달 (5자)', 
			 body:{ fillAllFields(false); setFieldValue('adminId', 'abcde'); delay(); clickButton('등록'); popup() },
			 exp:'아이디|길이'],

			[id:'TC#03', field:'ADMIN_ID', type:'단위', desc:'아이디 길이 초과 (51자)', 
			 body:{ fillAllFields(false); setFieldValue('adminId', 'a'*51); delay(); clickButton('등록'); popup() },
			 exp:'아이디|길이'],

			[id:'TC#04', field:'ADMIN_ID', type:'단위', desc:'아이디 한글 포함', 
			 body:{ fillAllFields(false); setFieldValue('adminId', '관리자123'); delay(); clickButton('등록'); popup() },
			 exp:'아이디|형식'],

			[id:'TC#05', field:'PASSWORD', type:'단위', desc:'비밀번호 빈 값', 
			 body:{ fillAllFields(true); setFieldValue('password', ''); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|필수'],

			[id:'TC#06', field:'PASSWORD', type:'단위', desc:'비밀번호 길이 미달 (7자)', 
			 body:{ fillAllFields(true); setFieldValue('password', '1234567'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|길이'],

			[id:'TC#07', field:'PASSWORD', type:'단위', desc:'비밀번호 영문만 입력', 
			 body:{ fillAllFields(true); setFieldValue('password', 'password'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|조합'],

			[id:'TC#08', field:'PASSWORD', type:'단위', desc:'비밀번호 숫자만 입력', 
			 body:{ fillAllFields(true); setFieldValue('password', '12345678'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|조합'],

			[id:'TC#09', field:'PASSWORD', type:'단위', desc:'비밀번호 특수문자만 입력', 
			 body:{ fillAllFields(true); setFieldValue('password', '!@#$%^&*'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|조합'],

			[id:'TC#10', field:'PASSWORD', type:'단위', desc:'비밀번호 공백 포함', 
			 body:{ fillAllFields(true); setFieldValue('password', 'pass word1'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|공백'],

			[id:'TC#11', field:'PW_CONFIRM', type:'단위', desc:'확인 비밀번호 빈 값', 
			 body:{ fillAllFields(true); setFieldValue('passwordConfirm', ''); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|필수'],

			[id:'TC#12', field:'PW_CONFIRM', type:'단위', desc:'확인 비밀번호 1자 다름', 
			 body:{ fillAllFields(true); setFieldValue('passwordConfirm', 'Valid!@2'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|일치'],

			[id:'TC#13', field:'PW_CONFIRM', type:'단위', desc:'확인 비밀번호 전체 다름', 
			 body:{ fillAllFields(true); setFieldValue('passwordConfirm', 'Wrong!@1'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|일치'],

			[id:'TC#14', field:'PW_CONFIRM', type:'단위', desc:'확인 비밀번호 길이 미달', 
			 body:{ fillAllFields(true); setFieldValue('passwordConfirm', 'Valid!@'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|일치|길이'],

			[id:'TC#15', field:'PW_CONFIRM', type:'단위', desc:'확인 비밀번호 길이 초과', 
			 body:{ fillAllFields(true); setFieldValue('passwordConfirm', 'Valid!@111'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|일치|길이'],

			[id:'TC#16', field:'PW_CONFIRM', type:'단위', desc:'확인 비밀번호 공백 포함', 
			 body:{ fillAllFields(true); setFieldValue('passwordConfirm', 'Valid!@1 '); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|일치|공백'],

			[id:'TC#17', field:'PW_CONFIRM', type:'단위', desc:'확인 비밀번호 대소문자 다름', 
			 body:{ fillAllFields(true); setFieldValue('passwordConfirm', 'valid!@1'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|일치'],

			[id:'TC#18', field:'NAME', type:'단위', desc:'이름 빈 값', 
			 body:{ fillAllFields(true); setFieldValue('adminName', ''); delay(); clickButton('등록'); popup() },
			 exp:'이름|필수'],

			[id:'TC#19', field:'NAME', type:'단위', desc:'이름 1자 입력 (미달)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', '정'); delay(); clickButton('등록'); popup() },
			 exp:'이름|길이'],

			[id:'TC#20', field:'NAME', type:'단위', desc:'이름 31자 입력 (초과)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', '정민호정민호정민호정민호정민호정민호1'); delay(); clickButton('등록'); popup() },
			 exp:'이름|길이'],

			[id:'TC#21', field:'EMAIL', type:'단위', desc:'이메일 빈 값', 
			 body:{ fillAllFields(true); setFieldValue('adminEmail', ''); delay(); clickButton('등록'); popup() },
			 exp:'이메일|필수'],

			[id:'TC#22', field:'EMAIL', type:'단위', desc:'이메일 @ 기호 누락', 
			 body:{ fillAllFields(true); setFieldValue('adminEmail', 'test.com'); delay(); clickButton('등록'); popup() },
			 exp:'이메일|형식|@'],

			[id:'TC#23', field:'EMAIL', type:'단위', desc:'이메일 도메인 누락', 
			 body:{ fillAllFields(true); setFieldValue('adminEmail', 'test@'); delay(); clickButton('등록'); popup() },
			 exp:'이메일|형식|도메인'],

			[id:'TC#24', field:'EMAIL', type:'단위', desc:'이메일 로컬 부분 누락', 
			 body:{ fillAllFields(true); setFieldValue('adminEmail', '@test.com'); delay(); clickButton('등록'); popup() },
			 exp:'이메일|형식|로컬'],

			[id:'TC#25', field:'ADMIN_TYPE', type:'단위', desc:'관리자 유형 미선택', 
			 body:{ fillAllFields(true); deselectField('qa'); delay(); clickButton('등록'); popup() },
			 exp:'관리자|유형|필수'],

			[id:'TC#26', field:'ADMIN_TYPE', type:'단위', desc:'관리자 유형 선택', 
			 body:{ fillAllFields(true); selectFieldByValue('qa', 'super'); delay(); clickButton('등록'); popup() },
			 exp:''],

			[id:'TC#27', field:'MARKETING', type:'단위', desc:'마케팅 동의 미선택', 
			 body:{ fillAllFields(true); deselectField('marketing'); delay(); clickButton('등록'); popup() },
			 exp:'마케팅|동의|필수'],

			[id:'TC#28', field:'MARKETING', type:'단위', desc:'마케팅 동의 선택', 
			 body:{ fillAllFields(true); clickField('marketing'); delay(); clickButton('등록'); popup() },
			 exp:''],

			[id:'TC#29', field:'DUPLICATE', type:'시나리오', desc:'중복체크 생략 후 등록', 
			 body:{ fillAllFields(false); delay(); clickButton('등록'); popup() },
			 exp:'중복|체크'],

			[id:'TC#30', field:'DUPLICATE', type:'시나리오', desc:'사용 중인 아이디로 중복체크', 
			 body:{ fillAllFields(false); setFieldValue('adminId', 'alreadyused01'); clickButton('중복체크'); popup() },
			 exp:'중복|사용중'],

			[id:'TC#31', field:'SECURITY', type:'시나리오', desc:'비밀번호 = 아이디', 
			 body:{ fillAllFields(true); setFieldValue('password', 'ssr0128'); setFieldValue('passwordConfirm', 'ssr0128'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|아이디|동일'],

			[id:'TC#32', field:'SECURITY', type:'시나리오', desc:'비밀번호 = 이름', 
			 body:{ fillAllFields(true); setFieldValue('password', '정민호'); setFieldValue('passwordConfirm', '정민호'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|이름|동일'],

			[id:'TC#33', field:'SECURITY', type:'시나리오', desc:'비밀번호 = 이메일', 
			 body:{ fillAllFields(true); setFieldValue('password', 'sssr0123@ad.com'); setFieldValue('passwordConfirm', 'sssr0123@ad.com'); delay(); clickButton('등록'); popup() },
			 exp:'비밀번호|이메일|동일'],

			[id:'TC#34', field:'SPECIAL_CHAR', type:'시나리오', desc:'아이디에 특수문자 (!)', 
			 body:{ fillAllFields(false); setFieldValue('adminId', 'user!123'); delay(); clickButton('등록'); popup() },
			 exp:'아이디|특수|형식'],

			[id:'TC#35', field:'SPECIAL_CHAR', type:'시나리오', desc:'이름에 특수문자 (#)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', '정#민호'); delay(); clickButton('등록'); popup() },
			 exp:'이름|특수|형식'],

			[id:'TC#36', field:'SPECIAL_CHAR', type:'시나리오', desc:'이름에 이모지 (😊)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', '정민호😊'); delay(); clickButton('등록'); popup() },
			 exp:'이름|이모지|특수'],

			[id:'TC#37', field:'BOUNDARY', type:'시나리오', desc:'아이디 경계값 최소 (6자)', 
			 body:{ fillAllFields(false); setFieldValue('adminId', 'test01'); delay(); clickButton('등록'); popup() },
			 exp:''],

			[id:'TC#38', field:'BOUNDARY', type:'시나리오', desc:'아이디 경계값 최대 (50자)', 
			 body:{ fillAllFields(false); setFieldValue('adminId', 'testuser0123456789012345678901234567890123456789'); delay(); clickButton('등록'); popup() },
			 exp:''],

			[id:'TC#39', field:'BOUNDARY', type:'시나리오', desc:'비밀번호 경계값 최소 (8자)', 
			 body:{ fillAllFields(true); setFieldValue('password', 'Test1234'); setFieldValue('passwordConfirm', 'Test1234'); delay(); clickButton('등록'); popup() },
			 exp:''],

			[id:'TC#40', field:'BOUNDARY', type:'시나리오', desc:'이름 경계값 최소 (2자)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', '정민'); delay(); clickButton('등록'); popup() },
			 exp:''],

			[id:'TC#41', field:'BOUNDARY', type:'시나리오', desc:'이름 경계값 최대 (30자)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', '정민호정민호정민호정민호정민호'); delay(); clickButton('등록'); popup() },
			 exp:''],

			[id:'TC#42', field:'WHITESPACE', type:'시나리오', desc:'아이디 앞 공백', 
			 body:{ fillAllFields(false); setFieldValue('adminId', ' user123'); delay(); clickButton('등록'); popup() },
			 exp:'아이디|공백|형식'],

			[id:'TC#43', field:'WHITESPACE', type:'시나리오', desc:'아이디 뒤 공백', 
			 body:{ fillAllFields(false); setFieldValue('adminId', 'user123 '); delay(); clickButton('등록'); popup() },
			 exp:'아이디|공백|형식'],

			[id:'TC#44', field:'WHITESPACE', type:'시나리오', desc:'이름 앞 공백', 
			 body:{ fillAllFields(true); setFieldValue('adminName', ' 정민호'); delay(); clickButton('등록'); popup() },
			 exp:'이름|공백'],

			[id:'TC#45', field:'WHITESPACE', type:'시나리오', desc:'이메일 공백 포함', 
			 body:{ fillAllFields(true); setFieldValue('adminEmail', 'test @test.com'); delay(); clickButton('등록'); popup() },
			 exp:'이메일|공백'],

			[id:'TC#46', field:'ENCODING', type:'시나리오', desc:'이름에 중국어 (王小明)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', '王小明'); delay(); clickButton('등록'); popup() },
			 exp:'이름|언어|형식'],

			[id:'TC#47', field:'ENCODING', type:'시나리오', desc:'이름에 일본어 (たなか)', 
			 body:{ fillAllFields(true); setFieldValue('adminName', 'たなか'); delay(); clickButton('등록'); popup() },
			 exp:'이름|언어|형식'],

			[id:'TC#48', field:'COMBINATION', type:'시나리오', desc:'아이디+비밀번호+확인 모두 빈 값', 
			 body:{ fillAllFields(true); setFieldValue('adminId', ''); setFieldValue('password', ''); setFieldValue('passwordConfirm', ''); delay(); clickButton('등록'); popup() },
			 exp:'아이디|비밀번호'],

			[id:'TC#49', field:'COMBINATION', type:'시나리오', desc:'이름+이메일 모두 빈 값', 
			 body:{ fillAllFields(true); setFieldValue('adminName', ''); setFieldValue('adminEmail', ''); delay(); clickButton('등록'); popup() },
			 exp:'이름|이메일'],

			[id:'TC#50', field:'RETRY', type:'시나리오', desc:'잘못된 아이디 수정 후 재등록', 
			 body:{ fillAllFields(true); setFieldValue('adminId', 'invalid'); delay(); clickButton('등록'); popup(); WebUI.delay(0.5); setFieldValue('adminId', 'valid01'); delay(); clickButton('등록'); popup() },
			 exp:'성공|완료'],

			[id:'TC#51', field:'RESET', type:'시나리오', desc:'초기화 버튼으로 모든 필드 초기화', 
			 body:{ fillAllFields(false); clickButton('초기화'); WebUI.delay(0.5); delay(); clickButton('등록'); popup() },
			 exp:'필수|아이디'],

			[id:'TC#52', field:'COMBINATION', type:'시나리오', desc:'모든 필드의 길이 초과', 
			 body:{ fillAllFields(true); setFieldValue('adminId', 'a'*51); setFieldValue('password', 'a'*51); setFieldValue('adminName', 'a'*31); delay(); clickButton('등록'); popup() },
			 exp:'길이|아이디|비밀번호'],

			[id:'TC#53', field:'GLOBAL', type:'시나리오', desc:'모든 필드 빈 값으로 등록 시도', 
			 body:{ clearAllFieldsByID(); delay(); clickButton('등록'); popup() },
			 exp:'필수|아이디'],

			[id:'TC#54', field:'SUCCESS', type:'성공', desc:'전체 필드 정상값으로 등록 성공', 
			 body:{ fillAllFields(true); delay(); clickButton('등록'); popup() },
			 exp:'성공|완료|등록됨|완료되었|가능']
		]
	}

	@Keyword
	static void runAll() {
		totalFail = 0
		allResults.clear()
		sessionStart = System.currentTimeMillis()

		KeywordUtil.logInfo("🚀 [필드 ID 기반 동적 테스트 시작]")
		KeywordUtil.logInfo("═" * 80)

		WebUI.waitForPageLoad(10)

		def masterTCList = getTestCaseList()
		
		KeywordUtil.logInfo("🔍 테스트 케이스 실행 (총 ${masterTCList.size()}개: 53개 예외 + 1개 성공)")
		KeywordUtil.logInfo("═" * 80)

		masterTCList.each { tc ->
			runTC(tc.id, tc.type, tc.field ?: '', tc.desc, tc.exp, tc.body)
		}

		printSummary()
		if (totalFail > 0) KeywordUtil.markFailed("🚨 실패 ${totalFail}건")
		else KeywordUtil.markPassed("✅ 54개 TC 모두 통과!")
	}

	private static void runTC(String tcId, String type, String field, String desc, String expectKey, Closure body) {
		logStart(tcId, type, field, desc)
		long t0 = System.currentTimeMillis()
		String actual = ''

		try {
			WebUI.delay(0.2)
			actual = (String) body.call() ?: ''
		} catch (Exception e) {
			actual = "[예외] ${e.message?.take(40) ?: '알 수 없음'}"
		}

		double elapsed = (System.currentTimeMillis() - t0) / 1000.0
		
		boolean passed = expectKey == null || expectKey.isEmpty() || 
					(actual && expectKey.split('\\|').any { actual.contains(it.trim()) } && 
					 !['성공','완료','가능','등록됨'].any { actual.contains(it) })

		if (!passed) totalFail++

		logResult(passed, tcId, actual, elapsed)
		allResults << [tc: tcId, type: type, field: field, desc: desc, expect: expectKey ?: '-', 
					   popup: actual, passed: passed, elapsed: String.format('%.2f초', elapsed)]
	}

	// ════════════════════════════════════════════════════════════════════════════
	//  ★ 필드 ID 기반 값 입력 (JavaScript 직접 조작)
	// ════════════════════════════════════════════════════════════════════════════

	/**
	 * ★ 필드 ID로 직접 값 입력 (가장 안정적)
	 * 사용 예시:
	 *   setFieldValue('adminId', 'ssr0128')
	 *   setFieldValue('password', 'Valid!@1')
	 *   setFieldValue('adminEmail', 'test@ad.com')
	 */
	private static void setFieldValue(String fieldId, String value) {
		try {
			String safeValue = (value ?: '').replace('\\', '\\\\').replace("'", "\\'")
			WebUI.executeJavaScript("""
				var el = document.getElementById('${fieldId}');
				if (el) {
					el.value = '${safeValue}';
					el.dispatchEvent(new Event('input', { bubbles: true }));
					el.dispatchEvent(new Event('change', { bubbles: true }));
					el.dispatchEvent(new Event('blur', { bubbles: true }));
				}
			""", null)
			KeywordUtil.logInfo("  → ${fieldId} = '${value}'")
		} catch (Exception e) {
			KeywordUtil.logInfo("⚠️ 필드 입력 실패: ${fieldId}")
		}
	}

	/**
	 * ★ 모든 필드를 기본값으로 자동 입력
	 * fieldDefaultValues 맵 사용
	 */
	private static void fillAllFields(boolean withDuplicateCheck) {
		fieldDefaultValues.each { fieldId, defaultValue ->
			setFieldValue(fieldId, defaultValue)
		}

		// Select 필드 (드롭다운)
		WebUI.executeJavaScript("""
			var qaSelect = document.getElementById('qa');
			if (qaSelect) {
				qaSelect.value = 'super';
				qaSelect.dispatchEvent(new Event('change', { bubbles: true }));
			}
		""", null)
		KeywordUtil.logInfo("  → qa = 'super'")

		// Radio 필드 (마케팅)
		WebUI.executeJavaScript("""
			var marketing = document.getElementById('marketing');
			if (marketing) {
				marketing.checked = true;
				marketing.dispatchEvent(new Event('change', { bubbles: true }));
			}
		""", null)
		KeywordUtil.logInfo("  → marketing = checked")

		if (withDuplicateCheck) {
			delay()
			clickButton('중복체크')
			popup()
			WebUI.delay(0.3)
		}
	}

	private static void clearAllFieldsByID() {
		WebUI.executeJavaScript("""
			['adminId', 'password', 'passwordConfirm', 'adminName', 'adminEmail'].forEach(id => {
				var el = document.getElementById(id);
				if (el) {
					el.value = '';
					el.dispatchEvent(new Event('change', { bubbles: true }));
				}
			});
		""", null)
	}

	private static void selectFieldByValue(String fieldId, String value) {
		WebUI.executeJavaScript("""
			var el = document.getElementById('${fieldId}');
			if (el) {
				el.value = '${value}';
				el.dispatchEvent(new Event('change', { bubbles: true }));
			}
		""", null)
	}

	private static void clickField(String fieldId) {
		WebUI.executeJavaScript("""
			var el = document.getElementById('${fieldId}');
			if (el) {
				el.click();
				el.dispatchEvent(new Event('change', { bubbles: true }));
			}
		""", null)
	}

	private static void deselectField(String fieldId) {
		WebUI.executeJavaScript("""
			var el = document.getElementById('${fieldId}');
			if (el) {
				if (el.type === 'radio' || el.type === 'checkbox') {
					el.checked = false;
				} else if (el.tagName === 'SELECT') {
					el.selectedIndex = -1;
				}
				el.dispatchEvent(new Event('change', { bubbles: true }));
			}
		""", null)
	}

	private static void clickButton(String label) {
		String xp = "//button[contains(., '${label}')] | //input[contains(@value, '${label}')]"
		def btn = new TestObject().addProperty('xpath', ConditionType.EQUALS, xp)

		if (WebUI.waitForElementPresent(btn, 2, OPTIONAL)) {
			try {
				WebUI.click(btn)
				KeywordUtil.logInfo("  → 버튼: '${label}'")
			} catch (Exception e) {
				try {
					WebUI.executeJavaScript("document.evaluate(\"${xp}\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue?.click();", null)
				} catch (Exception ignored) { }
			}
			WebUI.delay(0.8)
		}
	}

	private static String popup() {
		WebUI.delay(0.6)
		try {
			Alert a = DriverFactory.getWebDriver().switchTo().alert()
			String t = a.getText()
			a.accept()
			return t
		} catch (Exception ignored) { }

		try {
			String r = (String) WebUI.executeJavaScript("""
				var btn = Array.from(document.querySelectorAll('button, a')).find(b => {
					var t = b.textContent.trim();
					return b.offsetParent !== null && ['확인','닫기','OK','Close'].includes(t);
				});
				if (!btn) return '';
				var txt = document.body.innerText;
				if (btn.textContent.trim() !== '등록') btn.click();
				return txt;
			""", null)
			return r?.trim() ?: ''
		} catch (Exception ignored) { 
			return '' 
		}
	}

	private static void delay() { WebUI.delay(0.3) }

	private static void logStart(String id, String type, String field, String desc) {
		KeywordUtil.logInfo("\n┌──────────────────────────┐")
		KeywordUtil.logInfo("│ ▶ ${id} [${type}]")
		KeywordUtil.logInfo("│ ${desc}")
		KeywordUtil.logInfo("└──────────────────────────┘")
	}

	private static void logResult(boolean pass, String id, String res, double elap) {
		KeywordUtil.logInfo("[${pass ? '✅ PASS' : '❌ FAIL'}] ${id} | ${res.take(30)}")
	}

	private static void printSummary() {
		int total = allResults.size()
		int pass = allResults.count { it.passed }
		KeywordUtil.logInfo("\n" + "═" * 80)
		KeywordUtil.logInfo("📊 최종 요약: ${total}건 중 ${pass}건 통과")
		KeywordUtil.logInfo("═" * 80)
	}
}