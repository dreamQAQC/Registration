package words

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType

import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions

class RegExceptionHelper {

	// =========================
	// 상태값
	// =========================
	private static List<Map> allResults = new ArrayList<>()
	private static int totalFail = 0
	private static long sessionStart = 0
	private static String initialUrl = ""

	// =========================
	// 공용 유틸
	// =========================
	private static WebDriver d() { DriverFactory.getWebDriver() }
	private static void sleep(double sec) { WebUI.delay(sec) }
	private static Object js(String script, List args = []) { WebUI.executeJavaScript(script, args) }

	private static String safeStr(String s) {
		return (s ?: "").replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ")
	}

	private static boolean waitUntil(long timeoutMs, Closure<Boolean> cond, long pollMs = 200) {
		long end = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < end) {
			try { if (cond.call()) return true } catch (ignored) {}
			sleep(pollMs / 1000.0)
		}
		return false
	}

	// =========================================================
	// ✅ (JOIN3 용) 우편번호 창/레이어/iframe 자동 탐지 + 검색 + 첫결과 선택
	// =========================================================
	@Keyword
	static boolean openAndPickAddress(String keyword) {
		String kw = (keyword ?: "").trim()
		if (!kw) kw = "a"

		try {
			closeFileDialog()
			closePopupBlockerAlert()
			closeAlert()

			WebDriver driver = d()
			if (driver == null) return false

			String mainHandle = driver.getWindowHandle()
			Set<String> beforeHandles = driver.getWindowHandles()

			// 1) 주소검색 버튼 클릭 (button/a/div 모두 대응)
			if (!clickAddressSearchButton()) {
				KeywordUtil.logInfo("⚠️ 주소검색 버튼 클릭 실패")
				return false
			}

			// 2) 우편번호 화면 컨텍스트 잡기 (새창 OR 현재창 레이어/iframe)
			Map ctx = switchToPostcodeContext(mainHandle, beforeHandles, 12000)
			if (!ctx.ok) {
				KeywordUtil.logInfo("⚠️ 우편번호 화면 전환 실패(창/레이어 감지 실패)")
				// 그래도 혹시 값이 들어갔는지 한번 체크해보려면 true 반환은 위험해서 false 유지
				return false
			}

			// 3) 검색어 입력 + Enter + 첫 결과 클릭
			boolean picked = postcodeSearchAndPickFirst(kw, 12000)

			// 4) 원복 (iframe/창)
			restoreFromPostcodeContext(ctx, mainHandle)

			if (!picked) {
				KeywordUtil.logInfo("⚠️ 우편번호 결과 선택 실패(검색/결과 클릭 실패)")
				return false
			}
			return true

		} catch (Exception e) {
			KeywordUtil.logInfo("⚠️ openAndPickAddress 예외: " + (e.message ?: ""))
			return false
		}
	}

	private static boolean clickAddressSearchButton() {
		try {
			WebDriver driver = d()
			if (driver == null) return false

			// 1) 텍스트 기반 XPath (주소 검색/우편번호/주소찾기 등)
			List<String> xps = [
				"//button[contains(normalize-space(.),'주소') and contains(normalize-space(.),'검색')]",
				"//button[contains(normalize-space(.),'우편') and contains(normalize-space(.),'검색')]",
				"//button[contains(normalize-space(.),'주소찾기') or contains(normalize-space(.),'우편번호')]",
				"//a[contains(normalize-space(.),'주소') and contains(normalize-space(.),'검색')]",
				"//*[@role='button' and (contains(normalize-space(.),'주소') and contains(normalize-space(.),'검색'))]"
			]

			for (String xp : xps) {
				List<WebElement> els = driver.findElements(By.xpath(xp))
				WebElement el = els.find { it != null && it.isDisplayed() }
				if (el != null) {
					js("arguments[0].scrollIntoView({block:'center'});", [el])
					js("arguments[0].click();", [el])
					sleep(0.3)
					return true
				}
			}

			// 2) “우편번호” 입력 옆에 붙은 버튼을 구조적으로 탐색
			boolean ok = (boolean) js("""
				function clickNearZip(){
					function isVisible(el){ return el && el.offsetParent !== null; }
					let zip = null;
					const inputs = document.querySelectorAll('input');
					for (const i of inputs){
						if(!isVisible(i)) continue;
						const ph = (i.placeholder||'').replace(/\\s/g,'');
						const id = (i.id||'').toLowerCase();
						const nm = (i.name||'').toLowerCase();
						if(ph.includes('우편번호') || id.includes('zip') || id.includes('post') || nm.includes('zip') || nm.includes('post')){
							zip = i; break;
						}
					}
					if(!zip) return false;

					// 같은 행/부모에서 버튼 찾기
					let base = zip.closest('div, li, td, tr') || zip.parentElement;
					for(let hop=0; hop<3 && base; hop++){
						const btn = base.querySelector('button, a, [role="button"]');
						if(btn && isVisible(btn)){
							btn.click(); return true;
						}
						base = base.parentElement;
					}
					return false;
				}
				return clickNearZip();
			""", [])

			if (ok) { sleep(0.3); return true }
			return false

		} catch (Exception e) {
			return false
		}
	}

	private static Map switchToPostcodeContext(String mainHandle, Set<String> beforeHandles, long timeoutMs) {
		WebDriver driver = d()
		Map ctx = [ok:false, mode:'', openedHandle:'', usedIframe:false]

		// A) 새 창이 뜨는 경우: windowHandles 증가 감지 후 “새로 생긴 핸들”로 이동
		boolean newWin = waitUntil(timeoutMs, {
			Set<String> now = driver.getWindowHandles()
			return now.size() > beforeHandles.size()
		}, 200)

		if (newWin) {
			Set<String> now = driver.getWindowHandles()
			String newHandle = (now - beforeHandles).toList().last()
			if (!newHandle) {
				// 혹시 diff가 비면, main 아닌 핸들로라도 이동
				newHandle = now.find { it != mainHandle }
			}
			if (newHandle) {
				driver.switchTo().window(newHandle)
				sleep(0.4)
				ctx.ok = true
				ctx.mode = 'window'
				ctx.openedHandle = newHandle

				// 새 창 안에 iframe이 있을 수 있음 → 있으면 그 iframe으로 들어가기
				boolean iframeOk = trySwitchToPostcodeIframeIfExists(6000)
				ctx.usedIframe = iframeOk
				return ctx
			}
		}

		// B) 새 창이 아니라 현재 창에 레이어/iframe으로 뜨는 경우: iframe 탐지 후 전환
		boolean iframeOk = trySwitchToPostcodeIframeIfExists(timeoutMs)
		if (iframeOk) {
			ctx.ok = true
			ctx.mode = 'iframe'
			ctx.usedIframe = true
			return ctx
		}

		// C) 마지막 보험: “about:blank” 새 창이 떴는데 핸들이 순간적으로 못 잡힌 케이스 대비
		boolean anyOther = waitUntil(timeoutMs, {
			Set<String> now = driver.getWindowHandles()
			return now.find { it != mainHandle } != null
		}, 200)

		if (anyOther) {
			String other = driver.getWindowHandles().find { it != mainHandle }
			if (other) {
				driver.switchTo().window(other)
				sleep(0.4)
				ctx.ok = true
				ctx.mode = 'window'
				ctx.openedHandle = other
				ctx.usedIframe = trySwitchToPostcodeIframeIfExists(4000)
				return ctx
			}
		}

		return ctx
	}

	private static boolean trySwitchToPostcodeIframeIfExists(long timeoutMs) {
		boolean found = waitUntil(timeoutMs, {
			return (boolean) js("""
				function isVisible(el){ return el && el.offsetParent !== null; }
				const iframes = Array.from(document.querySelectorAll('iframe'));
				for (const f of iframes){
					const src = (f.getAttribute('src')||'').toLowerCase();
					const id  = (f.id||'').toLowerCase();
					const nm  = (f.name||'').toLowerCase();
					// daum/kakao/postcode 류는 src/id/name에 흔히 남습니다
					if(src.includes('postcode') || src.includes('daum') || src.includes('kakao') || id.includes('daum') || id.includes('postcode') || nm.includes('daum') || nm.includes('postcode')){
						if(isVisible(f)) return true;
					}
				}
				// 혹시 src가 비어도 화면에 보이는 iframe이 1개면 그걸 우선 후보로
				const vis = iframes.filter(f => isVisible(f));
				return vis.length == 1;
			""", [])
		}, 200)

		if (!found) return false

		// 실제 전환: xpath로 iframe 잡아서 switchToFrame
		TestObject iframeObj = new TestObject("postcodeIframe")
		iframeObj.addProperty("xpath", ConditionType.EQUALS,
			"//iframe[contains(translate(@src,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'postcode') " +
			"or contains(translate(@src,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'daum') " +
			"or contains(translate(@src,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'kakao') " +
			"or contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'postcode') " +
			"or contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'daum') " +
			"or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'postcode') " +
			"or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'daum')]" +
			"|//iframe[not(@src) and not(@id) and not(@name)]" // fallback (단 1개 보이는 경우 위에서 체크함)
		)

		try {
			WebUI.switchToFrame(iframeObj, 5)
			sleep(0.2)
			return true
		} catch (Exception e) {
			// switchToFrame 실패하면 JS 클릭으로 포커스 유도 후 재시도
			try {
				js("""
					const f = document.querySelector('iframe');
					if(f) { try{ f.scrollIntoView({block:'center'}); }catch(e){} }
				""", [])
				WebUI.switchToFrame(iframeObj, 5)
				sleep(0.2)
				return true
			} catch (Exception e2) {
				return false
			}
		}
	}

	private static boolean postcodeSearchAndPickFirst(String keyword, long timeoutMs) {
		WebDriver driver = d()
		String kw = safeStr(keyword)

		// 1) 검색 input 찾기 + 입력 + Enter
		boolean typed = waitUntil(timeoutMs, {
			return (boolean) js("""
				function isVisible(el){ return el && el.offsetParent !== null; }
				function fire(el){
					el.dispatchEvent(new Event('input',{bubbles:true}));
					el.dispatchEvent(new Event('change',{bubbles:true}));
					el.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',code:'Enter',bubbles:true}));
					el.dispatchEvent(new KeyboardEvent('keyup',{key:'Enter',code:'Enter',bubbles:true}));
				}
				const kw = '${kw}';
				// 우편번호 서비스는 input name/id가 자주 바뀜: 보이는 text input 중 상단 후보 선택
				const candidates = Array.from(document.querySelectorAll('input[type="text"], input:not([type])'));
				let input = null;
				for(const el of candidates){
					if(!isVisible(el)) continue;
					const id=(el.id||'').toLowerCase(), nm=(el.name||'').toLowerCase(), ph=(el.placeholder||'').toLowerCase();
					if(id.includes('search')||id.includes('query')||id.includes('keyword')||nm.includes('search')||nm.includes('query')||nm.includes('keyword')||ph.includes('검색')){
						input = el; break;
					}
				}
				// 못 찾으면 보이는 text input 중 첫번째
				if(!input){
					const vis = candidates.filter(isVisible);
					if(vis.length>0) input = vis[0];
				}
				if(!input) return false;

				input.focus();
				input.value = kw;
				fire(input);
				return true;
			""", [])
		}, 200)

		if (!typed) return false
		sleep(0.8)

		// 2) 결과 “첫 줄” 클릭 (버튼/링크/리스트 항목 중 클릭 가능한 것)
		boolean clicked = waitUntil(timeoutMs, {
			return (boolean) js("""
				function isVisible(el){ return el && el.offsetParent !== null; }
				function clickEl(el){
					try{ el.scrollIntoView({block:'center'}); }catch(e){}
					try{ el.click(); return true; }catch(e){}
					try{ el.dispatchEvent(new MouseEvent('click',{bubbles:true})); return true; }catch(e2){}
					return false;
				}

				// 결과 컨테이너 후보들 (Daum Postcode 버전에 따라 다름)
				const roots = [
					document.querySelector('#searchResult'),
					document.querySelector('#result'),
					document.querySelector('#focusContent'),
					document.querySelector('.wrap_result'),
					document.querySelector('.result'),
					document
				].filter(Boolean);

				for(const r of roots){
					// 1) li 안의 a/button 우선
					let cand = r.querySelector('ul li a, ul li button, ul li [role="button"]');
					if(cand && isVisible(cand)) return clickEl(cand);

					// 2) li 자체가 클릭되는 구조
					cand = r.querySelector('ul li');
					if(cand && isVisible(cand)) return clickEl(cand);

					// 3) dd/span/button 구조 (사용자 에러났던 케이스의 일반형)
					cand = r.querySelector('ul li dl dd button, ul li dl dd span button, ul li dl dd span');
					if(cand && isVisible(cand)) return clickEl(cand);
				}
				return false;
			""", [])
		}, 250)

		return clicked
	}

	private static void restoreFromPostcodeContext(Map ctx, String mainHandle) {
		try {
			WebDriver driver = d()
			if (ctx?.usedIframe) {
				try { WebUI.switchToDefaultContent() } catch (ignored) {}
				sleep(0.1)
			}
			if (ctx?.mode == 'window') {
				// 새 창 닫고 메인으로
				try { driver.close() } catch (ignored) {}
				sleep(0.2)
				try { driver.switchTo().window(mainHandle) } catch (ignored) {}
			} else {
				// iframe이었으면 그냥 메인 컨텐츠로 끝
				try { driver.switchTo().window(mainHandle) } catch (ignored) {}
			}
			sleep(0.2)
		} catch (ignored) {}
	}

	// =========================================================
	// ✅ 기존 runAll 기능(테스트 케이스 유지) — 아래부터는 “기능 동일”
	// =========================================================

	@Keyword
	static void runAll() {
		totalFail = 0
		allResults.clear()
		sessionStart = System.currentTimeMillis()

		KeywordUtil.logInfo("🚀 회원가입 예외 테스트 시작 (페이지 이탈 방지 & 스마트 필드 탐지 적용)")
		KeywordUtil.logInfo("═" * 70)

		WebUI.waitForPageLoad(10)
		initialUrl = WebUI.getUrl()

		def rawList = getTestCaseList()
		List<String> domOrder = getDynamicDomOrder()
		KeywordUtil.logInfo("🔍 화면 분석 완료! 감지된 입력 순서: " + (domOrder.join(' ➔ ') ?: '기본'))

		def sorted = sortTestCasesByDomOrder(rawList, domOrder)

		def idTests = sorted.findAll { it.target in ['id', 'tripledup', 'skipDupCheck'] }
		def otherTests = sorted.findAll { !(it.target in ['id', 'tripledup', 'skipDupCheck']) }

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

		KeywordUtil.logInfo("\n🔒 아이디 검증 종료 → 중복확인 최종 1회 수행 (이후 테스트에서는 중복확인 미수행)")
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
			popup()
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

	private static List<String> getDynamicDomOrder() {
		String domOrderStr = (String) js("""
			var order = [];
			var inputs = document.querySelectorAll('input:not([type="hidden"]):not([type="radio"]):not([type="checkbox"]):not([type="button"]):not([type="submit"]):not([type="file"])');
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
			if (a.priority != b.priority) return a.priority <=> b.priority
			int idA = a.id.replace('TC-', '').toInteger()
			int idB = b.id.replace('TC-', '').toInteger()
			return idA <=> idB
		}

		rawList.eachWithIndex { tc, i -> tc.id = String.format("TC-%02d", i + 1) }
		return rawList
	}

	// ✅ 요청대로 “테스트 케이스 부분 그대로 유지”
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

			} else if (tc.target != 'success' && tc.target != 'tripledup') {
				setFieldValue(tc.target, tc.value)
			}

			if (tc.target == 'id') {
				clickDuplicateCheck()
				actual = popup()
				setRequiredFields()

			} else if (tc.target == 'tripledup') {
				for (int i = 0; i < 3; i++) {
					clickDuplicateCheck()
					actual = popup()
					sleep(0.2)
				}
				actual = '[중복확인 3회 완료] ' + actual
				setRequiredFields()

			} else {
				clickRegister()
				actual = popup()
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
		String actualClean = (actual ?: "").replaceAll('\n', ' ').trim()

		boolean isPositiveMsg =
			actualClean.contains('완료') || actualClean.contains('성공') || actualClean.contains('가능') ||
			actualClean.contains('사용 가능한') || actualClean.contains('일치')

		boolean passed = false
		String reason = ""

		if (tc.expect == '성공') {
			if (isPositiveMsg) { passed = true; reason = "정상 통과 확인 (긍정 메시지 반환)" }
			else { passed = false; reason = "성공해야 하나 에러/경고 발생" }
		} else {
			if (actualClean == '[팝업없음]' || actualClean.isEmpty()) { passed = false; reason = "예외 처리가 되어야 하나 아무 반응이 없음" }
			else if (isPositiveMsg) { passed = false; reason = "에러가 발생해야 하나 성공(긍정) 처리됨" }
			else { passed = true; reason = "예외(에러/경고) 처리 정상 방어 확인" }
		}

		if (!passed) totalFail++

		logResult(passed, tc.id, tc.type, tc.e ?: '미지정', tc.value?.toString(), actualClean, elapsed, tc.expect, reason)
		allResults << [tc: tc.id, type: tc.type, field: tc.e ?: '미정', popup: actualClean, passed: passed, elapsed: String.format('%.2f초', elapsed), expect: tc.expect, reason: reason]
		return true
	}

	// =========================
	// 필수값(성별/주소) 복구 (기존과 동일: 팝업 없이 직접입력)
	// =========================
	private static void setRequiredFields() {
		try {
			setGenderByText('남성')
			setAddressDummyDirect()
			sleep(0.1)
		} catch (ignored) {}
	}

	private static void setGenderByText(String genderText) {
		js("""
			var targetText = '${safeStr(genderText)}';
			var labels = document.querySelectorAll('label, button, a, span, div');
			for (var i=0; i<labels.length; i++) {
				var el = labels[i];
				if (el.offsetParent === null) continue;
				var t = (el.innerText || '').trim();
				if (t === targetText || t.includes(targetText)) {
					if (el.tagName === 'LABEL') {
						var fid = el.getAttribute('for');
						if (fid) {
							var r = document.getElementById(fid);
							if (r && r.type === 'radio') {
								r.checked = true;
								r.dispatchEvent(new Event('input', {bubbles:true}));
								r.dispatchEvent(new Event('change', {bubbles:true}));
								try { el.click(); } catch(e) {}
								return;
							}
						}
					}
					try { el.click(); } catch(e) {}
					return;
				}
			}
			var radios = document.querySelectorAll('input[type="radio"]');
			if (radios.length > 0) {
				radios[0].checked = true;
				radios[0].dispatchEvent(new Event('input', {bubbles:true}));
				radios[0].dispatchEvent(new Event('change', {bubbles:true}));
			}
		""", [])
	}

	private static void setAddressDummyDirect() {
		js("""
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
					if (el.offsetParent === null) continue;
					var ph = (el.placeholder || '').trim();
					if (ph.includes(txt)) return el;
				}
				return null;
			}
			function findByIdNameIncludes(keys) {
				var inputs = document.querySelectorAll('input');
				for (var i=0; i<inputs.length; i++) {
					var el = inputs[i];
					if (el.offsetParent === null) continue;
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

	// =========================
	// 기본 유틸/동작 함수들(기능 동일)
	// =========================
	private static void closeFileDialog() {
		try {
			new Actions(d()).sendKeys(Keys.ESCAPE).perform()
			sleep(0.2)
		} catch (ignored) {}
	}

	private static void closeAlert() {
		try { d().switchTo().alert().accept() } catch (ignored) {}
	}

	private static void closePopupBlockerAlert() {
		try {
			// 네이티브 Alert
			try {
				Alert alert = d().switchTo().alert()
				String alertText = alert.getText()
				if (alertText.contains('팝업') || alertText.contains('차단') || alertText.contains('열 수 없습니다')) {
					alert.accept()
					sleep(0.2)
					return
				}
			} catch (ignored) {}

			// DOM 모달
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

				if(el.type === 'password') el.value = 'Temp123!@';
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

			for(var i=0; i<inputs.length; i++) {
				var el = inputs[i];
				if(el.offsetParent === null) continue;

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
		closeFileDialog()
		closePopupBlockerAlert()

		js("""
			document.querySelectorAll('input').forEach(function(i) {
				if(i.type === 'checkbox' || i.type === 'radio') i.checked = false;
				else if(i.type === 'file') { try { i.value = ''; } catch(e) {} }
				else { i.value = ''; }

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
			if (elements.size() > 0) js("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", [elements[0]])
		} catch (ignored) {}
	}

	private static void clickDuplicateCheck() {
		closePopupBlockerAlert()
		try {
			def elements = d().findElements(By.xpath("//button[contains(translate(text(), ' ', ''), '중복확인') or contains(translate(text(), ' ', ''), '중복체크') or contains(translate(text(), ' ', ''), '중복')]"))
			if (elements.size() > 0) js("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", [elements[0]])
		} catch (ignored) {}
	}

	private static String popup(boolean needWait = true) {
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
			var btns = document.querySelectorAll('button, a, [class*="btn"], [class*="confirm"]');
			for (var i = 0; i < btns.length; i++) {
				var b = btns[i];
				var btnTxt = (b.innerText || '').split(' ').join('');
				if (b.offsetParent !== null && btnTxt.includes('확인')) {
					var parent = b.parentElement;
					while(parent && parent.tagName !== 'BODY') {
						var style = window.getComputedStyle(parent);
						if (style.position === 'fixed' || style.position === 'absolute' || parseInt(style.zIndex) > 0) {
							var rawTxt = parent.innerText || '';
							if (rawTxt.includes('팝업을 열 수 없습니다') || rawTxt.includes('팝업 차단')) {
								b.click();
								return '[팝업차단알림무시]';
							}
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

		if (result == '[팝업차단알림무시]') {
			sleep(0.3)
			return popup(false)
		}

		String extractedText = result
		if (result != null && result.startsWith('MODAL::')) {
			extractedText = result.substring(7)
			js("""
				var btns = document.querySelectorAll('button, a, [class*="btn"]');
				for (var i = 0; i < btns.length; i++) {
					var txt = (btns[i].innerText || '').split(' ').join('');
					if (btns[i].offsetParent !== null && txt.includes('확인')) { btns[i].click(); break; }
				}
			""", [])
			sleep(0.3)
		} else if (result != null && result.startsWith('HTML5::')) {
			extractedText = result.substring(7)
		}
		return extractedText ?: '[팝업없음]'
	}

	private static void checkAllCheckboxes() {
		js("document.querySelectorAll('input[type=\"checkbox\"]').forEach(function(c) { c.checked = true; c.dispatchEvent(new Event('change',{bubbles:true})); });", [])
	}

	private static void uncheckAllCheckboxes() {
		js("document.querySelectorAll('input[type=\"checkbox\"]').forEach(function(c) { c.checked = false; c.dispatchEvent(new Event('change',{bubbles:true})); });", [])
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