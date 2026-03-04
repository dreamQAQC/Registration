package words

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.Alert
import org.openqa.selenium.NoAlertPresentException
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.ExpectedConditions
import java.time.Duration

class RegExceptionHelper {
	
	private static List<Map> allResults = new ArrayList<>()
	private static int totalFail = 0
	private static long sessionStart = 0
	private static String initialUrl = ""

	@Keyword
	static void runAll() {
		totalFail = 0
		allResults.clear()
		sessionStart = System.currentTimeMillis()

		KeywordUtil.logInfo("🚀 회원가입 예외 테스트 시작 (페이지 이탈 방지 & 스마트 필드 탐지 적용)")
		KeywordUtil.logInfo("═" * 70)

		WebUI.waitForPageLoad(10)
		initialUrl = WebUI.getUrl() // 최초의 회원가입 페이지 URL 기억
		
		def rawList = getTestCaseList()
		List<String> domOrder = getDynamicDomOrder()
		KeywordUtil.logInfo("🔍 화면 분석 완료! 감지된 입력 순서: " + (domOrder.join(' ➔ ') ?: '기본'))

		def testList = sortTestCasesByDomOrder(rawList, domOrder)
		KeywordUtil.logInfo("📋 총 ${testList.size()}개 테스트 실행 시작\n")

		for (int i = 0; i < testList.size(); i++) {
			boolean isSessionAlive = runTC(i + 1, testList[i])
			if (!isSessionAlive) {
				KeywordUtil.logInfo("🚨 브라우저 세션이 끊어지거나 치명적 오류가 발생하여 남은 테스트를 중지합니다.")
				break
			}
		}

		printSummary()
		
		if (totalFail > 0) KeywordUtil.markFailed("🚨 실패 ${totalFail}건")
		else KeywordUtil.markPassed("✅ 전체 통과!")
	}

	private static List<String> getDynamicDomOrder() {
		String domOrderStr = (String) WebUI.executeJavaScript("""
			var order = [];
			var inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"]):not([type="button"]):not([type="submit"])');
			for(var i=0; i<inputs.length; i++) {
				var el = inputs[i];
				if(el.offsetParent === null) continue;
				
				var idStr = (el.id || '').toLowerCase();
				var nameStr = (el.name || '').toLowerCase();
				var phStr = (el.placeholder || '').toLowerCase();

				if (el.type === 'password') {
					if (idStr.includes('confirm') || nameStr.includes('confirm') || phStr.includes('확인') || phStr.includes('재입력')) {
						if(order.indexOf('pwConf') === -1) order.push('pwConf');
					} else {
						if(order.indexOf('pw') === -1) order.push('pw');
					}
				} else {
					if (idStr.includes('id') || nameStr.includes('id') || phStr.includes('아이디') || phStr.includes('4~')) {
						if(order.indexOf('id') === -1) order.push('id');
					} else if (idStr.includes('name') || nameStr.includes('name') || phStr.includes('이름') || phStr.includes('성명') || phStr.includes('실명')) {
						if(order.indexOf('name') === -1) order.push('name');
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
			if (a.priority != b.priority) {
				return a.priority <=> b.priority
			} else {
				int idA = a.id.replace('TC-', '').toInteger()
				int idB = b.id.replace('TC-', '').toInteger()
				return idA <=> idB
			}
		}

		rawList.eachWithIndex { tc, i ->
			tc.id = String.format("TC-%02d", i + 1)
		}
		
		return rawList
	}

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

	private static boolean runTC(int num, Map tc) {
		long t0 = System.currentTimeMillis()
		String actual = ''

		try {
			// 🎯 1. 페이지 이탈 시 원래 페이지로 복귀 (가입 성공 후 넘어간 경우 등)
			String currentUrl = WebUI.getUrl()
			if (initialUrl && currentUrl != initialUrl && !currentUrl.contains(initialUrl.split("\\?")[0])) {
				WebUI.navigateToUrl(initialUrl)
				WebUI.waitForPageLoad(5)
			}
			
			// 남아있는 네이티브 Alert 닫기
			try { DriverFactory.getWebDriver().switchTo().alert().accept() } catch(Exception e) {}

			// 2. 폼 초기화
			resetForm()
			WebUI.delay(0.2)

			// 3. 화면에 있는 모든 input 요소를 찾아 더미 값으로 완벽하게 채움 (말풍선 블로킹 방어)
			fillAllEmptyInputs()
			checkAllCheckboxes()
			
			// 4. 검증 대상이 아닌 주요 필드에 정상값을 다시 덮어씌움
			setFieldValue('name', '홍길동')
			setFieldValue('id', "testuser${num}")
			setFieldValue('pw', 'Test123!@')
			setFieldValue('pwConf', 'Test123!@')
			WebUI.delay(0.2)

			// 5. 아이디 검증이 아닌 경우, '중복 확인'을 미리 조용히 통과시킴
			if (tc.dupCheck && tc.target != 'id' && tc.target != 'tripledup' && tc.target != 'skipDupCheck' && tc.target != 'clear') {
				clickDuplicateCheck()
				silentClosePopup() 
				WebUI.delay(0.3) 
			}

			// 6. 타겟 필드에 사용자가 원한(TC) 예외 값을 덮어씌움
			if (tc.target == 'clear') {
				resetForm() 
			} else if (tc.target == 'uncheck') {
				uncheckAllCheckboxes() 
			} else if (tc.target == 'skipDupCheck') {
				setFieldValue('id', tc.value) 
			} else if (tc.target != 'success' && tc.target != 'tripledup') {
				setFieldValue(tc.target, tc.value)
			}

			// 7. 액션 수행
			if (tc.target == 'id') {
				clickDuplicateCheck()
				actual = popup()
			} else if (tc.target == 'tripledup') {
				for (int i = 0; i < 3; i++) {
					clickDuplicateCheck()
					actual = popup()
					WebUI.delay(0.2)
				}
				actual = '[중복확인 3회 완료] ' + actual
			} else {
				clickRegister()
				actual = popup() // 1.5초 대기 포함
			}
			
		} catch (Exception ex) {
			String errMsg = ex.message ?: ""
			actual = "[에러] ${errMsg.take(30)}"
			
			if (errMsg.contains("invalid session id") || errMsg.contains("Unable to execute JavaScript") || errMsg.contains("Session")) {
				logResult(false, tc.id, tc.type, tc.e ?: '미지정', tc.value?.toString(), actual, 0.0, tc.expect, "브라우저 세션 끊김 오류")
				allResults << [tc: tc.id, type: tc.type, field: tc.e ?: '미정', popup: actual, passed: false, elapsed: "0.00초", expect: tc.expect, reason: "세션/스크립트 오류"]
				return false
			}
		}

		long t1 = System.currentTimeMillis()
		double elapsed = (t1 - t0) / 1000.0
		String actualClean = actual.replaceAll('\n', ' ').trim()
		
		boolean isPositiveMsg = actualClean.contains('완료') || actualClean.contains('성공') || actualClean.contains('가능') || actualClean.contains('사용 가능한') || actualClean.contains('일치')
		boolean passed = false
		String reason = ""

		if (tc.expect == '성공') {
			if (isPositiveMsg) {
				passed = true; reason = "정상 통과 확인 (긍정 메시지 반환)"
			} else {
				passed = false; reason = "성공해야 하나 에러/경고 발생"
			}
		} else { 
			if (actualClean == '[팝업없음]' || actualClean.isEmpty()) {
				passed = false; reason = "예외 처리가 되어야 하나 아무 반응이 없음"
			} else if (isPositiveMsg) {
				passed = false; reason = "에러가 발생해야 하나 성공(긍정) 처리됨"
			} else {
				passed = true; reason = "예외(에러/경고) 처리 정상 방어 확인"
			}
		}

		if (!passed) totalFail++
		
		logResult(passed, tc.id, tc.type, tc.e ?: '미지정', tc.value?.toString(), actualClean, elapsed, tc.expect, reason)
		allResults << [tc: tc.id, type: tc.type, field: tc.e ?: '미정', popup: actualClean, passed: passed, elapsed: String.format('%.2f초', elapsed), expect: tc.expect, reason: reason]
		
		return true 
	}

	private static void fillAllEmptyInputs() {
		WebUI.executeJavaScript("""
			var inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"]):not([type="button"]):not([type="submit"])');
			for(var i=0; i<inputs.length; i++) {
				if(!inputs[i].value) {
					if(inputs[i].type === 'email') inputs[i].value = 'test@example.com';
					else if(inputs[i].type === 'password') inputs[i].value = 'Temp123!@';
					else inputs[i].value = 'dummyData12';
					inputs[i].dispatchEvent(new Event('input', { bubbles: true }));
					inputs[i].dispatchEvent(new Event('change', { bubbles: true }));
				}
			}
			
			var radios = document.querySelectorAll('input[type="radio"]');
			var groups = {};
			for(var k=0; k<radios.length; k++) {
				var name = radios[k].name;
				if(name && !groups[name]) {
					var checked = document.querySelector('input[name="'+name+'"]:checked');
					if(!checked && radios[k].offsetParent !== null) {
						radios[k].checked = true;
						radios[k].dispatchEvent(new Event('change', { bubbles: true }));
					}
					groups[name] = true;
				}
			}
			
			var selects = document.querySelectorAll('select');
			for(var s=0; s<selects.length; s++) {
				if(selects[s].offsetParent !== null && selects[s].options.length > 1 && selects[s].selectedIndex <= 0) {
					selects[s].selectedIndex = 1;
					selects[s].dispatchEvent(new Event('change', { bubbles: true }));
				}
			}
		""", [])
	}

	private static void setFieldValue(String type, String value) {
		String safeVal = value ? value.toString().replace('\\', '\\\\').replace("'", "\\'").replace('\n', '') : ""
		
		// 🎯 요소 주변의 텍스트(Label, Placeholder 등)를 정밀하게 스캔하여 완벽 타겟팅
		WebUI.executeJavaScript("""
			var type = '${type}';
			var val = '${safeVal}';
			var target = null;
			var inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"]):not([type="button"]):not([type="submit"])');
			
			for(var i=0; i<inputs.length; i++) {
				var el = inputs[i];
				if(el.offsetParent === null) continue; // 보이지 않는 요소 무시
				
				var idStr = (el.id || '').toLowerCase();
				var nameStr = (el.name || '').toLowerCase();
				var phStr = (el.placeholder || '').toLowerCase();
				
				var labelTxt = '';
				if(el.labels && el.labels.length > 0) labelTxt = el.labels[0].innerText.toLowerCase();
				else {
					var parent = el.closest('div, tr, li, td');
					if(parent) labelTxt = parent.innerText.toLowerCase();
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
		WebUI.executeJavaScript("""
			document.querySelectorAll('input').forEach(i => {
				if(i.type === 'checkbox' || i.type === 'radio') i.checked = false;
				else i.value = '';
				i.dispatchEvent(new Event('input', { bubbles: true }));
				i.dispatchEvent(new Event('change', { bubbles: true }));
			});
			
			var btns = document.querySelectorAll('button');
			for(var i=0; i<btns.length; i++) {
				var txt = (btns[i].innerText || '').split(' ').join(''); 
				if(btns[i].offsetParent !== null && (txt.includes('확인') || txt.includes('닫기'))) {
					btns[i].click();
				}
			}
			
			document.querySelectorAll('.modal, .popup, [role="dialog"], [class*="layer"], .modal-backdrop').forEach(m => {
				m.style.display = 'none';
			});
		""", [])
	}

	private static void clickRegister() {
		try {
			def driver = DriverFactory.getWebDriver()
			def elements = driver.findElements(By.xpath("//button[contains(translate(text(), ' ', ''), '가입하기') or contains(translate(text(), ' ', ''), '등록')]"))
			if(elements.size() > 0) {
				WebUI.executeJavaScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", [elements[0]])
			}
		} catch (Exception e) {}
	}

	private static void clickDuplicateCheck() {
		try {
			def driver = DriverFactory.getWebDriver()
			def elements = driver.findElements(By.xpath("//button[contains(translate(text(), ' ', ''), '중복확인') or contains(translate(text(), ' ', ''), '중복체크')]"))
			if(elements.size() > 0) {
				WebUI.executeJavaScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", [elements[0]])
			}
		} catch (Exception e) {}
	}

	private static void silentClosePopup() {
		WebUI.delay(0.5) 
		try {
			def driver = DriverFactory.getWebDriver()
			Alert alert = new WebDriverWait(driver, Duration.ofMillis(300)).until(ExpectedConditions.alertIsPresent())
			if (alert != null) alert.accept()
		} catch (Exception e) {}
		
		WebUI.executeJavaScript("""
			var btns = document.querySelectorAll('button, a, [class*="btn"]');
			for (var i = 0; i < btns.length; i++) {
				var txt = (btns[i].innerText || '').split(' ').join('');
				if (btns[i].offsetParent !== null && txt.includes('확인')) {
					btns[i].click(); 
					break;
				}
			}
			document.querySelectorAll('.modal, .popup, [role="dialog"], [class*="layer"]').forEach(m => m.style.display = 'none');
		""", [])
	}

	private static String popup(boolean needWait = true) {
		if (needWait) {
			WebUI.delay(1.5) 
		}
		
		try {
			def driver = DriverFactory.getWebDriver()
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(500))
			Alert alert = wait.until(ExpectedConditions.alertIsPresent())
			if (alert != null) {
				String text = alert.getText()
				alert.accept() 
				return text
			}
		} catch (Exception e) {}
		
		String result = (String) WebUI.executeJavaScript("""
			var btns = document.querySelectorAll('button, a, [class*="btn"], [class*="confirm"]');
			
			for (var i = 0; i < btns.length; i++) {
				var b = btns[i];
				var btnTxt = (b.innerText || '').split(' ').join(''); 
				if (b.offsetParent !== null && btnTxt.includes('확인')) {
					var parent = b.parentElement;
					while(parent && parent.tagName !== 'BODY') {
						var style = window.getComputedStyle(parent);
						if (style.position === 'fixed' || style.position === 'absolute' || parseInt(style.zIndex) > 0) {
							// 정규식 에러를 완벽히 막기 위한 split/join 방식 사용
							var rawTxt = parent.innerText || '';
							var txt = rawTxt.split('확인').join('').split('\\n').join(' ').split('\\r').join(' ').trim();
							return 'MODAL::' + txt;
						}
						parent = parent.parentElement;
					}
				}
			}

			var inputs = document.querySelectorAll('input');
			for (var k = 0; k < inputs.length; k++) {
				if (!inputs[k].validity.valid && inputs[k].validationMessage) {
					return 'HTML5::' + inputs[k].validationMessage; 
				}
			}

			return '[팝업없음]';
		""", [])

		String extractedText = result
		
		if (result != null && result.startsWith('MODAL::')) {
			extractedText = result.substring(7)
			WebUI.executeJavaScript("""
				var btns = document.querySelectorAll('button, a, [class*="btn"]');
				for (var i = 0; i < btns.length; i++) {
					var txt = (btns[i].innerText || '').split(' ').join('');
					if (btns[i].offsetParent !== null && txt.includes('확인')) {
						btns[i].click(); 
						break;
					}
				}
			""", [])
			WebUI.delay(0.3) 
		} else if (result != null && result.startsWith('HTML5::')) {
			extractedText = result.substring(7)
		}

		return extractedText ?: '[팝업없음]'
	}

	private static void checkAllCheckboxes() {
		WebUI.executeJavaScript("document.querySelectorAll('input[type=\"checkbox\"]').forEach(c => { c.checked = true; c.dispatchEvent(new Event('change')); });", [])
	}

	private static void uncheckAllCheckboxes() {
		WebUI.executeJavaScript("document.querySelectorAll('input[type=\"checkbox\"]').forEach(c => { c.checked = false; c.dispatchEvent(new Event('change')); });", [])
	}

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