package words

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory

import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions

class RegExceptionHelper {

	// =========================================================
	// [A] 실행 상태값
	// =========================================================
	private static List<Map> allResults = new ArrayList<>()
	private static int totalFail = 0
	private static long sessionStart = 0
	private static String initialUrl = ""

	// =========================================================
	// [B] 공용 유틸
	// =========================================================
	private static WebDriver d() {
		return DriverFactory.getWebDriver()
	}

	private static void sleep(double sec) {
		WebUI.delay(sec)
	}

	private static Object js(String script, List args = []) {
		return WebUI.executeJavaScript(script, args)
	}

	private static String safeStr(String s) {
		return (s ?: "")
				.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\n", " ")
				.replace("\r", " ")
	}

	private static boolean waitUntil(long timeoutMs, Closure<Boolean> cond, long pollMs = 200) {
		long end = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < end) {
			try {
				if (cond.call()) return true
			} catch (ignored) {}
			sleep(pollMs / 1000.0)
		}
		return false
	}

	// =========================================================
	// [C] 전체 테스트 실행
	// =========================================================
	@Keyword
	static void runAll() {
		totalFail = 0
		allResults.clear()
		sessionStart = System.currentTimeMillis()

		KeywordUtil.logInfo("🚀 회원가입 예외 테스트 시작")
		KeywordUtil.logInfo("═" * 70)

		WebUI.waitForPageLoad(10)
		initialUrl = WebUI.getUrl()

		def rawList = getTestCaseList()
		List<String> domOrder = getDynamicDomOrder()
		KeywordUtil.logInfo("🔍 화면 분석 완료! 감지된 입력 순서: " + (domOrder.join(' ➔ ') ?: '기본'))

		def sorted = sortTestCasesByDomOrder(rawList, domOrder)

		def idTests = sorted.findAll { it.target in ['id','tripledup','skipDupCheck'] }
		def otherTests = sorted.findAll { !(it.target in ['id','tripledup','skipDupCheck']) }

		KeywordUtil.logInfo("📋 아이디 관련 테스트 ${idTests.size()}개 선행 실행\n")
		for (int i = 0; i < idTests.size(); i++) {
			boolean alive = runTC(i + 1, idTests[i])
			if (!alive) {
				KeywordUtil.logInfo("🚨 브라우저 세션이 끊어지거나 치명적 오류가 발생하여 남은 테스트를 중지합니다.")
				printSummary()
				if (totalFail > 0) KeywordUtil.markFailed("🚨 실패 ${totalFail}건")
				else KeywordUtil.markPassed("✅ 전체 통과!")
				return
			}
		}

		KeywordUtil.logInfo("\n🔒 아이디 검증 종료 → 중복확인 최종 1회 수행")
		confirmDuplicateOnce()

		KeywordUtil.logInfo("\n📋 나머지 테스트 ${otherTests.size()}개 실행 시작\n")
		int base = idTests.size()
		for (int i = 0; i < otherTests.size(); i++) {
			boolean alive = runTC(base + i + 1, otherTests[i])
			if (!alive) {
				KeywordUtil.logInfo("🚨 브라우저 세션이 끊어지거나 치명적 오류가 발생하여 남은 테스트를 중지합니다.")
				break
			}
		}

		printSummary()
		if (totalFail > 0) KeywordUtil.markFailed("🚨 실패 ${totalFail}건")
		else KeywordUtil.markPassed("✅ 전체 통과!")
	}

	// =========================================================
	// [D] 중복확인 1회 마무리
	// =========================================================
	private static void confirmDuplicateOnce() {
		try {
			closeFileDialog()
			closePopupBlockerAlert()
			closeAlert()

			backToInitialUrlIfNeeded()

			resetForm()
			sleep(0.2)

			fillAllEmptyInputs()
			checkAllCheckboxes()
			setRequiredFields()

			setFieldValue('name', '홍길동')
			setFieldValue('id', "finaldup${System.currentTimeMillis() % 100000}")
			setFieldValue('pw', 'Test123!@')
			setFieldValue('pwConf', 'Test123!@')
			sleep(0.2)

			clickDuplicateCheck()
			popupFor('id')
			sleep(0.2)
		} catch (Exception e) {
			KeywordUtil.logInfo("⚠️ confirmDuplicateOnce 실패: " + (e.message ?: ""))
		}
	}

	private static void backToInitialUrlIfNeeded() {
		try {
			String currentUrl = WebUI.getUrl()
			if (initialUrl && currentUrl != initialUrl && !currentUrl.contains(initialUrl.split("\\?")[0])) {
				WebUI.navigateToUrl(initialUrl)
				WebUI.waitForPageLoad(5)
			}
		} catch (ignored) {}
	}

	// =========================================================
	// [E] 입력순서 감지 / TC 정렬
	// =========================================================
	private static List<String> getDynamicDomOrder() {
		String domOrderStr = (String) js("""
			var order = [];
			var inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"]):not([type="button"]):not([type="submit"]):not([type="file"])');

			for (var i=0; i<inputs.length; i++) {
				var el = inputs[i];
				if (el.offsetParent === null) continue;

				var idStr = (el.id || '').toLowerCase();
				var nameStr = (el.name || '').toLowerCase();
				var phStr = (el.placeholder || '').toLowerCase();

				if (el.type === 'password') {
					if (idStr.includes('confirm') || nameStr.includes('confirm') || phStr.includes('확인') || phStr.includes('재입력')) {
						if (order.indexOf('pwConf') === -1) order.push('pwConf');
					} else {
						if (order.indexOf('pw') === -1) order.push('pw');
					}
				} else {
					if (idStr.includes('id') || nameStr.includes('id') || phStr.includes('아이디') || phStr.includes('4~')) {
						if (order.indexOf('id') === -1) order.push('id');
					} else if (idStr.includes('name') || nameStr.includes('name') || phStr.includes('이름') || phStr.includes('성명') || phStr.includes('실명')) {
						if (order.indexOf('name') === -1) order.push('name');
					}
				}
			}
			return order.join(',');
		""", [])

		return domOrderStr ? domOrderStr.split(',').toList() : []
	}

	private static List<Map> sortTestCasesByDomOrder(List<Map> rawList, List<String> domOrder) {
		rawList.each { tc ->
			if (tc.type.startsWith('단위-') && tc.type != '단위-약관') {
				int idx = domOrder.indexOf(tc.target)
				tc.priority = (idx != -1) ? idx : 10
			} else if (tc.type == '단위-약관') {
				tc.priority = 50
			} else if (tc.type == '시나리오') {
				tc.priority = 60
			} else if (tc.type == '성공') {
				tc.priority = 70
			} else {
				tc.priority = 100
			}
		}

		rawList.sort { a, b ->
			if (a.priority != b.priority) return a.priority <=> b.priority
			int idA = a.id.replace('TC-', '').toInteger()
			int idB = b.id.replace('TC-', '').toInteger()
			return idA <=> idB
		}

		rawList.eachWithIndex { tc, i -> tc.id = String.format("TC-%02d", i + 1) }
		return rawList
	}

	// =========================================================
	// [F] 테스트 케이스
	// =========================================================
	private static List<Map> getTestCaseList() {
		return [
			[id:'TC-00', type:'단위-이름', desc:'빈 값', e:'이름', target:'name', value:'', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-이름', desc:'숫자 포함', e:'이름', target:'name', value:'123', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-이름', desc:'공백 포함', e:'이름', target:'name', value:'홍 길동', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-이름', desc:'이모지 포함', e:'이름', target:'name', value:'홍길동😊', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-이름', desc:'51자 초과', e:'이름', target:'name', value:'홍'*51, expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-이름', desc:'특수문자 포함', e:'이름', target:'name', value:'홍#길동', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-이름', desc:'영문만 입력', e:'이름', target:'name', value:'hongkildon', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-이름', desc:'2자 정상값', e:'이름', target:'name', value:'김철', expect:'성공', dupCheck:true],

			[id:'TC-00', type:'단위-아이디', desc:'빈 값', e:'아이디', target:'id', value:'', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'4자 미달', e:'아이디', target:'id', value:'tes', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'5자 미달', e:'아이디', target:'id', value:'abcd', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'51자 초과', e:'아이디', target:'id', value:'a'*51, expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'100자 초과', e:'아이디', target:'id', value:'a'*100, expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'한글 포함', e:'아이디', target:'id', value:'관리자12', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'이모지 불가', e:'아이디', target:'id', value:'✨✨123', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'특수문자 포함', e:'아이디', target:'id', value:'user!@#', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'공백 포함', e:'아이디', target:'id', value:'use 01', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'6자 정상', e:'아이디', target:'id', value:'user12', expect:'성공', dupCheck:false],
			[id:'TC-00', type:'단위-아이디', desc:'50자 초과', e:'아이디', target:'id', value:'a'*50, expect:'실패', dupCheck:false],

			[id:'TC-00', type:'단위-비밀번호', desc:'빈 값', e:'비밀번호', target:'pw', value:'', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'7자 미달', e:'비밀번호', target:'pw', value:'123456', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'영문만', e:'비밀번호', target:'pw', value:'password', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'숫자만', e:'비밀번호', target:'pw', value:'12345678', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'특수문자만', e:'비밀번호', target:'pw', value:'!@#\$%^&*', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'공백 포함', e:'비밀번호', target:'pw', value:'Pass 12!', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'영문+숫자만', e:'비밀번호', target:'pw', value:'pass1234', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'8자 정상값', e:'비밀번호', target:'pw', value:'Test12!@', expect:'성공', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'한글 포함', e:'비밀번호', target:'pw', value:'비밀123!@', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'중복 특수문자', e:'비밀번호', target:'pw', value:'!@!@!@12', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호', desc:'대문자만', e:'비밀번호', target:'pw', value:'PASSWORD', expect:'실패', dupCheck:true],

			[id:'TC-00', type:'단위-비밀번호확인', desc:'빈 값', e:'비밀번호', target:'pwConf', value:'', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호확인', desc:'7자 미달', e:'비밀번호', target:'pwConf', value:'1234567', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호확인', desc:'불일치', e:'비밀번호', target:'pwConf', value:'wrong!@#1', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호확인', desc:'한글 포함', e:'비밀번호', target:'pwConf', value:'비밀123!@', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호확인', desc:'한 글자만', e:'비밀번호', target:'pwConf', value:'T', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호확인', desc:'공백만', e:'비밀번호', target:'pwConf', value:'       ', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호확인', desc:'대소 혼용 불일치', e:'비밀번호', target:'pwConf', value:'test123!@', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-비밀번호확인', desc:'정상값 일치', e:'비밀번호', target:'pwConf', value:'Test123!@', expect:'성공', dupCheck:true],

			[id:'TC-00', type:'단위-성별', desc:'선택형 버튼 미선택', e:'성별', target:'gender', value:'', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-성별', desc:'왼쪽 첫 버튼 선택 유지', e:'성별', target:'genderKeep', value:'leftFirst', expect:'성공', dupCheck:true],

			[id:'TC-00', type:'단위-주소', desc:'주소 전체 미입력', e:'주소', target:'address', value:'clear', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-주소', desc:'우편번호만 입력', e:'주소', target:'address', value:'zipOnly', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-주소', desc:'도로명주소만 입력', e:'주소', target:'address', value:'roadOnly', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'단위-주소', desc:'상세주소만 입력', e:'주소', target:'address', value:'detailOnly', expect:'실패', dupCheck:true],

			[id:'TC-00', type:'단위-약관', desc:'필수 약관 미체크', e:'약관', target:'uncheck', value:'uncheck', expect:'실패', dupCheck:true],

			[id:'TC-00', type:'시나리오', desc:'모든 필드 빈값', e:'이름', target:'clear', value:'clear', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'시나리오', desc:'중복확인 생략', e:'중복확인', target:'skipDupCheck', value:'testus01', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'시나리오', desc:'아이디만 변경', e:'중복확인', target:'id', value:'user02', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'아이디 삭제', e:'아이디', target:'id', value:'', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'중복확인 3회 반복', e:'중복확인', target:'tripledup', value:'tripledup', expect:'성공', dupCheck:false],
			[id:'TC-00', type:'시나리오', desc:'아이디 공백', e:'중복확인', target:'id', value:' use 01 ', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'아이디 대소문자', e:'중복확인', target:'id', value:'USER01', expect:'성공', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'비밀번호 확인 삭제', e:'비밀번호', target:'pwConf', value:'', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'비밀번호 불일치', e:'비밀번호', target:'pwConf', value:'Diff!@#1', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'비밀번호만 변경', e:'비밀번호', target:'pw', value:'NewPwd!@1', expect:'성공', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'비밀번호 삭제', e:'비밀번호', target:'pw', value:'', expect:'실패', dupCheck:true],
			[id:'TC-00', type:'시나리오', desc:'전체 필드 재시도', e:'이름', target:'clear', value:'clear', expect:'실패', dupCheck:false],
			[id:'TC-00', type:'시나리오', desc:'약관 미체크 후 가입', e:'약관', target:'uncheck', value:'uncheck', expect:'실패', dupCheck:true],

			[id:'TC-00', type:'성공', desc:'정상 회원가입', e:'성공', target:'success', value:'success', expect:'성공', dupCheck:true]
		]
	}

	// =========================================================
	// [G] 테스트 1건 실행
	// =========================================================
	private static boolean runTC(int num, Map tc) {
		long t0 = System.currentTimeMillis()
		String actual = ''
		String precheck = ''

		try {
			closeFileDialog()
			closePopupBlockerAlert()
			backToInitialUrlIfNeeded()
			closeAlert()

			resetForm()
			sleep(0.2)

			fillAllEmptyInputs()
			checkAllCheckboxes()
			setRequiredFields()

			setFieldValue('name', '홍길동')
			setFieldValue('id', "testuser${num}")
			setFieldValue('pw', 'Test123!@')
			setFieldValue('pwConf', 'Test123!@')
			sleep(0.2)

			if (tc.target == 'clear') {
				resetForm()
				sleep(0.2)
				fillAllEmptyInputs()
				checkAllCheckboxes()
				setRequiredFields()

			} else if (tc.target == 'uncheck') {
				uncheckAllCheckboxes()

			} else if (tc.target == 'skipDupCheck') {
				setFieldValue('id', tc.value)

			} else if (tc.target == 'gender') {
				clearSelectableButtonGroup()

			} else if (tc.target == 'genderKeep') {
				clearSelectableButtonGroup()
				boolean kept = selectLeftMostSelectableButton()
				precheck = kept ? "[선택형버튼유지] " : "[선택유지실패] "

			} else if (tc.target == 'address') {
				setAddressCase(tc.value?.toString())

			} else if (tc.target != 'success' && tc.target != 'tripledup') {
				setFieldValue(tc.target, tc.value)
			}

			if (tc.target == 'id') {
				clickDuplicateCheck()
				actual = popupFor('id')
				setRequiredFields()

			} else if (tc.target == 'tripledup') {
				for (int i = 0; i < 3; i++) {
					clickDuplicateCheck()
					actual = popupFor('id')
					sleep(0.2)
				}
				actual = '[중복확인 3회 완료] ' + actual
				setRequiredFields()

			} else if (tc.target == 'genderKeep') {
				actual = precheck

			} else if (tc.target == 'success') {
				setAddressDummyDirect()

				clearSelectableButtonGroup()
				boolean keepOk = selectLeftMostSelectableButton()
				precheck += keepOk ? "[선택형버튼유지] " : "[선택유지실패] "

				clickRegister()
				actual = precheck + popupFor('success')

			} else {
				clickRegister()
				actual = precheck + popupFor(tc.target?.toString())
			}

		} catch (Exception ex) {
			String errMsg = ex.message ?: ""
			actual = "[에러] ${errMsg.take(60)}"

			if (errMsg.contains("invalid session id") ||
				errMsg.contains("Unable to execute JavaScript") ||
				errMsg.contains("Session")) {

				logResult(false, tc.id, tc.type, tc.e ?: '미지정', tc.value?.toString(), actual, 0.0, tc.expect, "브라우저 세션 끊김 오류")
				allResults << [tc: tc.id, type: tc.type, field: tc.e ?: '미정', popup: actual, passed: false, elapsed: "0.00초", expect: tc.expect, reason: "세션/스크립트 오류"]
				return false
			}
		}

		long t1 = System.currentTimeMillis()
		double elapsed = (t1 - t0) / 1000.0
		String actualClean = (actual ?: "").replaceAll('\n', ' ').trim()

		boolean forcedFail =
				actualClean.contains('[선택유지실패')

		boolean isPositiveMsg =
				actualClean.contains('완료') || actualClean.contains('성공') || actualClean.contains('가능') ||
				actualClean.contains('사용 가능한') || actualClean.contains('일치') ||
				actualClean.contains('[선택형버튼유지]')

		boolean isNegativeMsg =
				actualClean.contains('필수') || actualClean.contains('선택') || actualClean.contains('입력') ||
				actualClean.contains('확인') || actualClean.contains('실패') || actualClean.contains('오류') ||
				actualClean.contains('불가') || actualClean.contains('없습니다')

		boolean passed = false
		String reason = ""

		if (forcedFail) {
			passed = false
			reason = "선택형 버튼 유지 실패"
		}
		else if (tc.expect == '성공') {
			if (tc.target == 'genderKeep') {
				passed = actualClean.contains('[선택형버튼유지]')
				reason = passed ? "선택형 버튼 상태 유지 확인" : "선택형 버튼 상태 유지 실패"
			}
			else if (isPositiveMsg) {
				passed = true
				reason = "정상 통과 확인"
			}
			else if (actualClean == '[팝업없음]' && !isNegativeMsg) {
				passed = true
				reason = "오류 팝업 없이 정상 진행"
			}
			else {
				passed = false
				reason = "성공해야 하나 에러/경고 발생"
			}
		} else {
			if (actualClean == '[팝업없음]' || actualClean.isEmpty()) {
				passed = false
				reason = "예외 처리가 되어야 하나 아무 반응이 없음"
			}
			else if (isPositiveMsg && !isNegativeMsg) {
				passed = false
				reason = "에러가 발생해야 하나 성공 처리됨"
			}
			else {
				passed = true
				reason = "예외(에러/경고) 처리 정상 방어 확인"
			}
		}

		if (!passed) totalFail++

		logResult(passed, tc.id, tc.type, tc.e ?: '미지정', tc.value?.toString(), actualClean, elapsed, tc.expect, reason)
		allResults << [tc: tc.id, type: tc.type, field: tc.e ?: '미정', popup: actualClean, passed: passed, elapsed: String.format('%.2f초', elapsed), expect: tc.expect, reason: reason]
		return true
	}

	// =========================================================
	// [H] 필수값 기본 세팅
	// =========================================================
	private static void setRequiredFields() {
		try {
			selectLeftMostSelectableButton()
			setAddressDummyDirect()
			sleep(0.1)
		} catch (ignored) {}
	}

	// =========================================================
	// [H-1] 선택형 버튼 감지/선택
	// =========================================================
	private static void clearSelectableButtonGroup() {
		js("""
			function visible(el){
				if(!el) return false;
				const st = window.getComputedStyle(el);
				return el.offsetParent !== null && st.visibility !== 'hidden' && st.display !== 'none';
			}

			document.querySelectorAll('input[type="radio"]').forEach(function(r){
				r.checked = false;
				r.dispatchEvent(new Event('input', {bubbles:true}));
				r.dispatchEvent(new Event('change', {bubbles:true}));
			});

			const nodes = Array.from(document.querySelectorAll('button, [role="button"], label, div, span')).filter(visible);
			nodes.forEach(function(el){
				try { el.classList.remove('active'); } catch(e){}
				try { el.classList.remove('selected'); } catch(e){}
				try { el.classList.remove('checked'); } catch(e){}
				try { el.classList.remove('on'); } catch(e){}
				try { el.setAttribute('aria-pressed', 'false'); } catch(e){}
				try { el.setAttribute('aria-checked', 'false'); } catch(e){}
			});
		""", [])
	}

	private static boolean selectLeftMostSelectableButton() {
		boolean clicked = (boolean) js("""
			function visible(el){
				if(!el) return false;
				const st = window.getComputedStyle(el);
				return el.offsetParent !== null && st.visibility !== 'hidden' && st.display !== 'none';
			}

			function hasText(el){
				return ((el.innerText || '').replace(/\\s+/g, ' ').trim()).length > 0;
			}

			function isChoiceLike(el){
				if(!el) return false;

				const tag = (el.tagName || '').toLowerCase();
				const role = (el.getAttribute('role') || '').toLowerCase();
				const cls = (el.className || '').toString().toLowerCase();

				if (tag === 'label') return true;
				if (tag === 'button') return true;
				if (role === 'button') return true;
				if (role === 'radio') return true;
				if (role === 'tab') return true;
				if (cls.includes('radio')) return true;
				if (cls.includes('toggle')) return true;
				if (cls.includes('segment')) return true;
				if (cls.includes('option')) return true;
				if (cls.includes('choice')) return true;
				if (cls.includes('select')) return true;

				const radio = el.querySelector('input[type="radio"]');
				if (radio) return true;

				return false;
			}

			function looksLikeThreeChoiceRow(el){
				const rect = el.getBoundingClientRect();
				if (rect.width < 180 || rect.height < 28) return false;
				return true;
			}

			const all = Array.from(document.querySelectorAll('label, button, [role="button"], [role="radio"], [role="tab"], div, span'))
				.filter(visible)
				.filter(hasText)
				.filter(isChoiceLike)
				.filter(looksLikeThreeChoiceRow);

			if (all.length === 0) return false;

			all.sort(function(a, b){
				const ra = a.getBoundingClientRect();
				const rb = b.getBoundingClientRect();

				if (Math.abs(ra.top - rb.top) > 6) return ra.top - rb.top;
				return ra.left - rb.left;
			});

			const target = all[0];
			try { target.scrollIntoView({block:'center'}); } catch(e){}
			try { target.click(); } catch(e){}

			try {
				const fid = target.getAttribute('for');
				if (fid) {
					const input = document.getElementById(fid);
					if (input && input.type === 'radio') {
						input.checked = true;
						input.dispatchEvent(new Event('input', {bubbles:true}));
						input.dispatchEvent(new Event('change', {bubbles:true}));
					}
				}
			} catch(e){}

			try {
				const radio = target.querySelector('input[type="radio"]');
				if (radio) {
					radio.checked = true;
					radio.dispatchEvent(new Event('input', {bubbles:true}));
					radio.dispatchEvent(new Event('change', {bubbles:true}));
				}
			} catch(e){}

			return true;
		""", [])

		if (!clicked) return false

		return waitUntil(2000, {
			return hasAnySelectableChosen()
		}, 150)
	}

	private static boolean hasAnySelectableChosen() {
		try {
			return (boolean) js("""
				function visible(el){
					if(!el) return false;
					const st = window.getComputedStyle(el);
					return el.offsetParent !== null && st.visibility !== 'hidden' && st.display !== 'none';
				}

				const radios = Array.from(document.querySelectorAll('input[type="radio"]')).filter(visible);
				for (const r of radios) {
					if (r.checked === true) return true;
				}

				const nodes = Array.from(document.querySelectorAll('label, button, [role="button"], [role="radio"], [role="tab"], div, span')).filter(visible);
				for (const el of nodes) {
					const cls = (el.className || '').toString().toLowerCase();
					const ariaPressed = (el.getAttribute('aria-pressed') || '').toLowerCase();
					const ariaChecked = (el.getAttribute('aria-checked') || '').toLowerCase();

					if (ariaPressed == 'true' || ariaChecked == 'true') return true;
					if (cls.includes('active') || cls.includes('selected') || cls.includes('checked') || cls.includes('on')) return true;
				}
				return false;
			""", [])
		} catch (ignored) {
			return false
		}
	}

	// =========================================================
	// [H-2] 주소 필드 기본 세팅
	// =========================================================
	private static void setAddressDummyDirect() {
		js("""
			function isVisible(el){
				if(!el) return false;
				const st = window.getComputedStyle(el);
				return el.offsetParent !== null && st.visibility !== 'hidden' && st.display !== 'none';
			}

			function setVal(el, v) {
				if (!el) return;
				el.value = v;
				el.dispatchEvent(new Event('input', { bubbles:true }));
				el.dispatchEvent(new Event('change', { bubbles:true }));
				el.dispatchEvent(new Event('blur', { bubbles:true }));
			}

			function findByPlaceholderIncludes(txt) {
				var inputs = document.querySelectorAll('input');
				for (var i=0; i<inputs.length; i++) {
					var el = inputs[i];
					if (!isVisible(el)) continue;
					var ph = (el.placeholder || '').trim();
					if (ph.includes(txt)) return el;
				}
				return null;
			}

			function findByIdNameIncludes(keys) {
				var inputs = document.querySelectorAll('input');
				for (var i=0; i<inputs.length; i++) {
					var el = inputs[i];
					if (!isVisible(el)) continue;
					var id = (el.id || '').toLowerCase();
					var nm = (el.name || '').toLowerCase();
					for (var k=0; k<keys.length; k++) {
						if (id.includes(keys[k]) || nm.includes(keys[k])) return el;
					}
				}
				return null;
			}

			var zip = findByPlaceholderIncludes('우편번호') || findByIdNameIncludes(['post','zip','zipcode']);
			var road = findByPlaceholderIncludes('도로명주소') || findByIdNameIncludes(['road','addr1','address1','address']);
			var jibun = findByPlaceholderIncludes('지번주소') || findByIdNameIncludes(['jibun','addr2','address2']);
			var detail = findByPlaceholderIncludes('상세주소') || findByIdNameIncludes(['detail','addr3','address3']);

			if (zip && (zip.value || '').trim().length === 0) setVal(zip, '06000');
			if (road && (road.value || '').trim().length === 0) setVal(road, '서울특별시 강남구 테헤란로 123');
			if (jibun && (jibun.value || '').trim().length === 0) setVal(jibun, '서울특별시 강남구 역삼동 123-45');
			if (detail && (detail.value || '').trim().length === 0) setVal(detail, '101동 101호');
		""", [])
	}

	// =========================================================
	// [I] 주소 예외 케이스 유틸
	// =========================================================
	private static void clearAddressFields() {
		js("""
			function isVisible(el){
				if(!el) return false;
				const st = window.getComputedStyle(el);
				return el.offsetParent !== null && st.visibility !== 'hidden' && st.display !== 'none';
			}

			function setEmpty(el){
				if(!el) return;
				el.value = '';
				el.dispatchEvent(new Event('input', { bubbles:true }));
				el.dispatchEvent(new Event('change', { bubbles:true }));
				el.dispatchEvent(new Event('blur', { bubbles:true }));
			}

			const inputs = document.querySelectorAll('input');
			for (const el of inputs) {
				if (!isVisible(el)) continue;

				const ph = (el.placeholder || '').toLowerCase();
				const id = (el.id || '').toLowerCase();
				const nm = (el.name || '').toLowerCase();

				if (
					ph.includes('우편번호') || ph.includes('도로명주소') || ph.includes('지번주소') || ph.includes('상세주소') ||
					id.includes('post') || id.includes('zip') || id.includes('road') || id.includes('addr') || id.includes('address') || id.includes('detail') ||
					nm.includes('post') || nm.includes('zip') || nm.includes('road') || nm.includes('addr') || nm.includes('address') || nm.includes('detail')
				) {
					setEmpty(el);
				}
			}
		""", [])
	}

	private static void setAddressCase(String mode) {
		if (mode == 'clear') {
			clearAddressFields()
			return
		}

		clearAddressFields()

		js("""
			function isVisible(el){
				if(!el) return false;
				const st = window.getComputedStyle(el);
				return el.offsetParent !== null && st.visibility !== 'hidden' && st.display !== 'none';
			}

			function setVal(el, v) {
				if (!el) return;
				el.value = v;
				el.dispatchEvent(new Event('input', { bubbles:true }));
				el.dispatchEvent(new Event('change', { bubbles:true }));
				el.dispatchEvent(new Event('blur', { bubbles:true }));
			}

			function findByPlaceholderIncludes(txt) {
				var inputs = document.querySelectorAll('input');
				for (var i=0; i<inputs.length; i++) {
					var el = inputs[i];
					if (!isVisible(el)) continue;
					var ph = (el.placeholder || '').trim();
					if (ph.includes(txt)) return el;
				}
				return null;
			}

			function findByIdNameIncludes(keys) {
				var inputs = document.querySelectorAll('input');
				for (var i=0; i<inputs.length; i++) {
					var el = inputs[i];
					if (!isVisible(el)) continue;
					var id = (el.id || '').toLowerCase();
					var nm = (el.name || '').toLowerCase();
					for (var k=0; k<keys.length; k++) {
						if (id.includes(keys[k]) || nm.includes(keys[k])) return el;
					}
				}
				return null;
			}

			var zip = findByPlaceholderIncludes('우편번호') || findByIdNameIncludes(['post','zip','zipcode']);
			var road = findByPlaceholderIncludes('도로명주소') || findByIdNameIncludes(['road','addr1','address1','address']);
			var detail = findByPlaceholderIncludes('상세주소') || findByIdNameIncludes(['detail','addr3','address3']);

			var mode = '${safeStr(mode)}';

			if (mode === 'zipOnly') {
				setVal(zip, '06000');
			} else if (mode === 'roadOnly') {
				setVal(road, '서울특별시 강남구 테헤란로 123');
			} else if (mode === 'detailOnly') {
				setVal(detail, '101동 101호');
			}
		""", [])
	}

	// =========================================================
	// [J] 기본 동작 유틸
	// =========================================================
	private static void closeFileDialog() {
		try {
			new Actions(d()).sendKeys(Keys.ESCAPE).perform()
			sleep(0.2)
		} catch (ignored) {}
	}

	private static void closeAlert() {
		try {
			d().switchTo().alert().accept()
		} catch (ignored) {}
	}

	private static void closePopupBlockerAlert() {
		try {
			try {
				Alert alert = d().switchTo().alert()
				String alertText = alert.getText()
				if (alertText.contains('팝업') || alertText.contains('차단') || alertText.contains('열 수 없습니다')) {
					alert.accept()
					sleep(0.2)
					return
				}
			} catch (ignored) {}

			js("""
				var btns = document.querySelectorAll('button');
				for (var i = 0; i < btns.length; i++) {
					var btn = btns[i];
					var txt = (btn.innerText || '').trim();
					if (btn.offsetParent !== null && (txt === '확인' || txt === 'OK' || txt === '닫기')) {
						var parent = btn.closest('div[class*="modal"], div[class*="popup"], div[class*="dialog"], div[role="dialog"], div[style*="fixed"], div[style*="absolute"]');
						if (parent) {
							var parentText = parent.innerText || '';
							if (parentText.includes('팝업') || parentText.includes('차단') || parentText.includes('localhost') || parentText.includes('열 수 없습니다')) {
								btn.click();
								return;
							}
						}
					}
				}
			""", [])
			sleep(0.2)
		} catch (ignored) {}
	}

	private static void fillAllEmptyInputs() {
		js("""
			var inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"]):not([type="button"]):not([type="submit"]):not([type="file"])');

			for(var i=0; i<inputs.length; i++) {
				var el = inputs[i];
				if (el.offsetParent === null) continue;

				var idStr = (el.id || '').toLowerCase();
				if (idStr.includes('sample') || idStr.includes('address') || idStr.includes('post')) continue;
				if (el.value && el.value.length > 0) continue;

				if (el.type === 'password') el.value = 'Temp123!@';
				else el.value = 'dummyData12';

				el.dispatchEvent(new Event('input', { bubbles: true }));
				el.dispatchEvent(new Event('change', { bubbles: true }));
			}
		""", [])
	}

	private static void setFieldValue(String type, String value) {
		String safeVal = safeStr(value ?: "")

		js("""
			var type = '${safeStr(type)}';
			var val = '${safeVal}';
			var target = null;

			var inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"]):not([type="button"]):not([type="submit"]):not([type="file"])');

			for (var i=0; i<inputs.length; i++) {
				var el = inputs[i];
				if (el.offsetParent === null) continue;

				var idStr = (el.id || '').toLowerCase();
				var nameStr = (el.name || '').toLowerCase();
				var phStr = (el.placeholder || '').toLowerCase();

				var labelTxt = '';
				if (el.labels && el.labels.length > 0) labelTxt = el.labels[0].innerText.toLowerCase();
				else {
					var parent = el.closest('div, tr, li, td');
					if (parent) labelTxt = parent.innerText.toLowerCase();
				}

				if (type === 'pwConf') {
					if (el.type === 'password' && (idStr.includes('confirm') || nameStr.includes('confirm') || phStr.includes('확인') || phStr.includes('재입력') || labelTxt.includes('확인'))) { target = el; break; }
				} else if (type === 'pw') {
					if (el.type === 'password' && !idStr.includes('confirm') && !nameStr.includes('confirm') && !phStr.includes('확인') && !phStr.includes('재입력') && !labelTxt.includes('확인')) { target = el; break; }
				} else if (type === 'id') {
					if (el.type !== 'password' && (idStr.includes('id') || nameStr.includes('id') || phStr.includes('아이디') || phStr.includes('4~') || labelTxt.includes('id') || labelTxt.includes('아이디'))) { target = el; break; }
				} else if (type === 'name') {
					if (el.type !== 'password' && (idStr.includes('name') || nameStr.includes('name') || phStr.includes('이름') || phStr.includes('성명') || phStr.includes('실명') || labelTxt.includes('이름') || labelTxt.includes('성명'))) { target = el; break; }
				}
			}

			if (target) {
				target.value = val;
				target.dispatchEvent(new Event('input', { bubbles: true }));
				target.dispatchEvent(new Event('change', { bubbles: true }));
				target.dispatchEvent(new Event('blur', { bubbles: true }));
			}
		""", [])
	}

	private static void resetForm() {
		closeFileDialog()
		closePopupBlockerAlert()

		js("""
			document.querySelectorAll('input').forEach(function(i) {
				if (i.type === 'checkbox' || i.type === 'radio') i.checked = false;
				else if (i.type === 'file') { try { i.value = ''; } catch(e) {} }
				else i.value = '';

				i.dispatchEvent(new Event('input', { bubbles: true }));
				i.dispatchEvent(new Event('change', { bubbles: true }));
			});

			document.querySelectorAll('.modal, .popup, [role="dialog"], [class*="layer"], .modal-backdrop').forEach(function(m) {
				m.style.display = 'none';
			});
		""", [])
	}

	private static void clickRegister() {
		closePopupBlockerAlert()
		try {
			def elements = d().findElements(By.xpath("//button[contains(translate(text(), ' ', ''), '가입하기') or contains(translate(text(), ' ', ''), '등록')]"))
			if (elements.size() > 0) {
				js("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", [elements[0]])
			}
		} catch (ignored) {}
	}

	private static void clickDuplicateCheck() {
		closePopupBlockerAlert()
		try {
			def elements = d().findElements(By.xpath("//button[contains(translate(text(), ' ', ''), '중복확인') or contains(translate(text(), ' ', ''), '중복체크') or contains(translate(text(), ' ', ''), '중복')]"))
			if (elements.size() > 0) {
				js("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", [elements[0]])
			}
		} catch (ignored) {}
	}

	// =========================================================
	// [K] 팝업 / 인라인 에러 판독
	// =========================================================
	private static String popupFor(String targetHint = '', boolean needWait = true) {
		if (needWait) sleep(1.5)
		closeFileDialog()

		try {
			try {
				Alert alert = d().switchTo().alert()
				String alertText = alert.getText()

				if (alertText.contains('팝업') || alertText.contains('차단') || alertText.contains('열 수 없습니다')) {
					alert.accept()
					sleep(0.3)

					try {
						alert = d().switchTo().alert()
						String newText = alert.getText()
						alert.accept()
						return newText
					} catch (ignored) {}
				} else {
					alert.accept()
					return alertText
				}
			} catch (ignored) {}
		} catch (ignored) {}

		String result = (String) js("""
			function visible(el){
				if(!el) return false;
				const st = window.getComputedStyle(el);
				return el.offsetParent !== null && st.visibility !== 'hidden' && st.display !== 'none';
			}

			function cleanText(t){
				return (t || '').replace(/\\s+/g, ' ').trim();
			}

			const hint = '${safeStr(targetHint)}';

			var btns = document.querySelectorAll('button, a, [class*="btn"], [class*="confirm"]');
			for (var i = 0; i < btns.length; i++) {
				var b = btns[i];
				var btnTxt = cleanText(b.innerText || '');

				if (visible(b) && (btnTxt.includes('확인') || btnTxt === 'OK')) {
					var parent = b.parentElement;
					while (parent && parent.tagName !== 'BODY') {
						var style = window.getComputedStyle(parent);
						if (style.position === 'fixed' || style.position === 'absolute' || parseInt(style.zIndex) > 0) {
							var rawTxt = cleanText(parent.innerText || '');

							if (rawTxt.includes('팝업을 열 수 없습니다') || rawTxt.includes('팝업 차단')) {
								try { b.click(); } catch(e){}
								return '[팝업차단알림무시]';
							}

							var txt = rawTxt.replace(/확인/g, '').trim();
							return 'MODAL::' + txt;
						}
						parent = parent.parentElement;
					}
				}
			}

			var inputs = document.querySelectorAll('input, select, textarea');
			for (var k = 0; k < inputs.length; k++) {
				if (!inputs[k].validity.valid && inputs[k].validationMessage) {
					return 'HTML5::' + inputs[k].validationMessage;
				}
			}

			var selectors = [
				'[role="alert"]',
				'[aria-live]',
				'.error',
				'.invalid',
				'.tooltip',
				'.toast',
				'.message',
				'.help',
				'.feedback',
				'.ant-form-item-explain-error',
				'.Mui-error',
				'[class*="error"]',
				'[class*="invalid"]',
				'[class*="tooltip"]',
				'[class*="toast"]',
				'[class*="warn"]',
				'[class*="message"]'
			].join(',');

			var nodes = Array.from(document.querySelectorAll(selectors))
				.filter(el => visible(el))
				.map(el => cleanText(el.innerText || ''))
				.filter(t => t.length > 0 && t.length <= 160);

			function pickByHint(list, hint) {
				if (hint === 'gender' || hint === 'genderKeep') {
					for (const t of list) {
						if (t.includes('선택') || t.includes('옵션')) return t;
					}
				}
				if (hint === 'address') {
					for (const t of list) {
						if (t.includes('주소') || t.includes('우편번호')) return t;
					}
				}
				if (hint === 'id') {
					for (const t of list) {
						if (t.includes('아이디') || t.includes('중복')) return t;
					}
				}
				return '';
			}

			var picked = pickByHint(nodes, hint);
			if (picked) return 'INLINE::' + picked;

			for (const t of nodes) {
				if (
					t.includes('필수') || t.includes('선택') || t.includes('입력') || t.includes('확인') ||
					t.includes('주소') || t.includes('아이디') || t.includes('비밀번호') || t.includes('이름')
				) {
					return 'INLINE::' + t;
				}
			}

			return '[팝업없음]';
		""", [])

		if (result == '[팝업차단알림무시]') {
			sleep(0.3)
			return popupFor(targetHint, false)
		}

		String extractedText = result

		if (result != null && result.startsWith('MODAL::')) {
			extractedText = result.substring(7)

			js("""
				var btns = document.querySelectorAll('button, a, [class*="btn"]');
				for (var i = 0; i < btns.length; i++) {
					var txt = ((btns[i].innerText || '').replace(/\\s+/g,'')).trim();
					if (btns[i].offsetParent !== null && (txt.includes('확인') || txt === 'OK')) {
						try { btns[i].click(); } catch(e){}
						break;
					}
				}
			""", [])
			sleep(0.3)

		} else if (result != null && result.startsWith('HTML5::')) {
			extractedText = result.substring(7)

		} else if (result != null && result.startsWith('INLINE::')) {
			extractedText = result.substring(8)
		}

		return extractedText ?: '[팝업없음]'
	}

	private static void checkAllCheckboxes() {
		js("""
			document.querySelectorAll('input[type="checkbox"]').forEach(function(c) {
				c.checked = true;
				c.dispatchEvent(new Event('change',{bubbles:true}));
			});
		""", [])
	}

	private static void uncheckAllCheckboxes() {
		js("""
			document.querySelectorAll('input[type="checkbox"]').forEach(function(c) {
				c.checked = false;
				c.dispatchEvent(new Event('change',{bubbles:true}));
			});
		""", [])
	}

	// =========================================================
	// [L] 로그 출력
	// =========================================================
	private static void logResult(boolean passed, String tcId, String type, String field, String value, String popupText, double elapsed, String expectKey, String reason) {
		String icon = passed ? '✅  PASS' : '❌  FAIL'
		String sep = '═' * 62
		String inp = (value == '' ? '(빈 값)' : (value ?: '-')).take(45)
		String res = (popupText ?: '팝업 없음').take(45)

		StringBuilder sb = new StringBuilder()
		sb.append("\n╔${sep}╗\n")
		sb.append("║  ${icon.padRight(60)}║\n")
		sb.append("╠${sep}╣\n")
		sb.append("║  🆔  TC ID    : ${tcId.padRight(45)}║\n")
		sb.append("║  🔍  유형     : ${type.padRight(45)}║\n")
		sb.append("║  🎯  검증항목 : ${field.padRight(45)}║\n")
		sb.append("║  ⌨️   입력값   : ${inp.padRight(45)}║\n")
		sb.append("║  🎯  기대결과 : ${expectKey.padRight(44)}║\n")
		sb.append("║  📝  팝업내용  : ${res.padRight(44)}║\n")
		sb.append("║  💬  판정이유  : ${reason.padRight(44)}║\n")
		sb.append("╚${sep}╝\n")
		KeywordUtil.logInfo(sb.toString())
	}

	private static void printSummary() {
		int total = allResults.size()
		int passed = allResults.count { it.passed }
		int failed = total - passed
		double totalSec = (System.currentTimeMillis() - sessionStart) / 1000.0
		String w = '=' * 64

		StringBuilder sb = new StringBuilder()
		sb.append("\n\n${w}\n 🏁 최종 테스트 결과 요약\n${w}\n")
		sb.append(" 전체 실행 : ${total} 건 | ✅ PASS : ${passed} | ❌ FAIL : ${failed}\n")
		sb.append(" 통과율 : ${(total > 0 ? (int)(passed/total*100) : 0)}% | 총 소요 : ${String.format('%.1f', totalSec)}초\n${w}\n")

		if (failed > 0) {
			sb.append("\n ❌ 실패 목록 요약:\n")
			allResults.findAll { !it.passed }.each { r ->
				sb.append(" [${r.tc}] ${r.type} - ${r.field} (이유: ${r.reason})\n")
			}
		}

		KeywordUtil.logInfo(sb.toString())
	}
}