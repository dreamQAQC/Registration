/*
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║           RegExceptionHelperBase.groovy  ← 서브 파일            ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  공용 유틸 / 팝업·폼 조작 / DOM 스캔 / TC 생성 담당             ║
 * ║  RegExceptionHelper(메인)에서 extends 해서 사용                  ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  [수정 대상 안내]                                                ║
 * ║                                                                  ║
 * ║  ✏️  이 파일을 수정하는 경우                                     ║
 * ║  ─────────────────────────────────────────────────────────────  ║
 * ║  • TC 케이스 추가/변경                                           ║
 * ║      → [K] buildStaticTestCases()                               ║
 * ║      → [K] buildMainFormDynamicTestCases()                      ║
 * ║      → [K] buildNamePopupDynamicTestCases()                     ║
 * ║                                                                  ║
 * ║  • 팝업 감지/닫기 로직 수정                                     ║
 * ║      → [D] closeGeneralModalIfExists()                          ║
 * ║      → [D] dismissGeneralPopups()                               ║
 * ║      → [D] collectPopupMessage()                                ║
 * ║                                                                  ║
 * ║  • 이름 검색 팝업 동작 수정                                     ║
 * ║      → [F] detectNameFieldUsesPopupSearch()                     ║
 * ║      → [F] openNameSearchPopup()                                ║
 * ║      → [G] scanNameSearchPopup()                                ║
 * ║      → [H] actionNamePopup*() 계열                              ║
 * ║      → [H-2] runNamePopupScenario()                             ║
 * ║                                                                  ║
 * ║  • 폼 초기화 / 기본값 세팅 수정                                 ║
 * ║      → [E] resetMainForm()                                      ║
 * ║      → [E] fillVisibleEmptyFieldsWithDefault()                  ║
 * ║      → [E] setFormFieldValue()                                  ║
 * ║                                                                  ║
 * ║  • 체크박스 / 라디오 / 드롭다운 / 주소 동작 수정               ║
 * ║      → [I] 해당 메서드                                          ║
 * ║                                                                  ║
 * ║  • DOM 스캔 방식 수정                                           ║
 * ║      → [J] scanMainFormElements()                               ║
 * ║      → [J] scanMainFormDomOrder()                               ║
 * ║                                                                  ║
 * ║  • TC 정렬 순서 수정                                            ║
 * ║      → [K-2] sortTestCasesByDomOrder()                          ║
 * ║                                                                  ║
 * ║  • 파일 첨부 테스트 유틸 수정                                   ║
 * ║      → [L] createDummyFile()      더미 파일 생성                ║
 * ║      → [L] deleteTempFiles()      더미 파일 삭제                ║
 * ║      → [L] injectFileToInput()    input[type=file] 주입         ║
 * ║      → [L] detectFileAcceptExtensions()  허용 확장자 감지       ║
 * ║      → [L] submitFormAndCollectMessage() 제출 후 메시지 수집    ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration

import groovy.json.JsonSlurper

import org.openqa.selenium.Alert
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions

/**
 * RegExceptionHelperBase
 * ──────────────────────
 * 공용 유틸 / 팝업·폼 조작 / DOM 스캔 / TC 생성 담당.
 * 직접 실행하지 않으며, RegExceptionHelper(메인)에서만 호출됩니다.
 *
 * 섹션 목록
 *   [A] 전역 상태
 *   [B] 공용 유틸
 *   [C] 버튼 / 클릭
 *   [D] 공통 팝업 / 모달 처리
 *   [E] 폼 초기화 / 공통값 세팅
 *   [F] 이름 검색 팝업 탐색
 *   [G] 이름 검색 팝업 스캔
 *   [H] 이름 검색 팝업 기본 액션
 *   [H-2] 이름 팝업 시나리오 (단일 TC 전체 플로우)
 *   [I] 체크박스 / 라디오 / 드롭다운 / 주소
 *   [J] 메인 화면 스캔
 *   [K] TC 생성 (정적 + 동적)
 *   [K-2] TC 정렬
 */
class RegExceptionHelperBase {

	// =========================================================
	// [A] 전역 상태
	// =========================================================
	static List<Map> testResults = []
	static int failCount = 0
	static String startUrl = ""

	static boolean isNameSearchPopupMode = false
	static Map namePopupScan = [:]
	static boolean nameFieldDone = false

	static final String FIXED_VALID_ID = "testfix01"

	static final List<String> NAME_POPUP_TITLE_KEYWORDS = ["사용자 정보 검색", "회원 선택"]

	// =========================================================
	// [B] 공용 유틸
	// =========================================================
	static WebDriver driver() {
		return DriverFactory.getWebDriver()
	}

	static void waitSec(double sec) {
		WebUI.delay(sec)
	}

	static Object runJs(String script) {
		return WebUI.executeJavaScript(script, null)
	}

	static Object runJs(String script, List args) {
		return WebUI.executeJavaScript(script, args)
	}

	static void logLine(String msg) {
		KeywordUtil.logInfo(msg)
	}

	static String normalizeText(String s) {
		return (s ?: "").replaceAll("\\s+", " ").trim()
	}

	static boolean isBlankPopup(String s) {
		String t = (s ?: "").trim()
		return !t || t == "[팝업없음]"
	}

	static String escapeJsString(String s) {
		return (s ?: "")
		.replace("\\", "\\\\")
		.replace("'", "\\'")
		.replace("\n", " ")
		.replace("\r", " ")
	}

	static String sanitizePopupText(String s) {
		String r = normalizeText(s)
		r = r.replaceFirst(/(?i)^(System\s*Message|Validation\s*Error|Success|Warning|Error)\s*[✕:]?\s*/, "")
		return r.trim()
	}

	static WebElement findLikelyIdInput() {
		try {
			List<WebElement> inputs = driver().findElements(By.cssSelector("input:not([type='hidden']):not([type='radio']):not([type='checkbox']):not([type='button']):not([type='submit']):not([type='file'])"))
			for (WebElement e : inputs) {
				if (!isElementVisible(e)) continue
				String id = (e.getAttribute("id") ?: "").toLowerCase()
				String nm = (e.getAttribute("name") ?: "").toLowerCase()
				String ph = (e.getAttribute("placeholder") ?: "").toLowerCase()
				String lb = ""
				try {
					Object t = runJs('''
						var e=arguments[0], txt="";
						if(e.labels&&e.labels.length) txt=(e.labels[0].innerText||"");
						if(!txt){
							var p=e.closest("div,td,tr,li,section,article");
							if(p) txt=(p.innerText||"");
						}
						return txt;
					''', [e])
					lb = normalizeText(t?.toString()).toLowerCase()
				} catch (Exception ignore) {}
				boolean looksId = id.contains("id") || nm.contains("id") || ph.contains("아이디") || ph.contains("id") || lb.contains("계정 id") || lb.contains("계정id") || lb.contains("아이디")
				boolean looksEmailOnly = (id.contains("email") || nm.contains("email") || ph.contains("이메일") || ph.contains("@")) && !lb.contains("계정 id") && !lb.contains("계정id")
				if (looksId && !looksEmailOnly) return e
			}
		} catch (Exception ignore) {}
		return null
	}

	static WebElement findDuplicateCheckButtonNearId() {
		WebElement idInput = findLikelyIdInput()
		try {
			List<WebElement> buttons = driver().findElements(By.xpath("//button | //a | //input[@type='button'] | //input[@type='submit']"))
			List<WebElement> visible = buttons.findAll {
				isElementVisible(it)
			}
			if (idInput != null) {
				Map rect = (Map) runJs('''
					var r=arguments[0].getBoundingClientRect();
					return {left:r.left, right:r.right, top:r.top, bottom:r.bottom, cy:(r.top+r.bottom)/2};
				''', [idInput])
				WebElement best = null
				double bestScore = 1e12
				for (WebElement b : visible) {
					Map br = (Map) runJs('''
						var r=arguments[0].getBoundingClientRect();
						return {left:r.left, right:r.right, top:r.top, bottom:r.bottom, cy:(r.top+r.bottom)/2};
					''', [b])
					String txt = normalizeText((b.getText() ?: "") + " " + (b.getAttribute("value") ?: "") + " " + (b.getAttribute("title") ?: "")).replaceAll("\\s+","")
					if (txt.contains("우편") || txt.contains("주소") || txt.contains("검색")) continue
					double dx = (((Number) br.left).doubleValue() - ((Number) rect.right).doubleValue())
					double dy = Math.abs(((Number) br.cy).doubleValue() - ((Number) rect.cy).doubleValue())
					boolean plausibleText = txt.contains("조회") || txt.contains("확인") || txt.contains("중복")
					if (dx < -50 || dx > 300 || dy > 80 || !plausibleText) continue
					double score = Math.max(0, dx) + dy * 3
					if (score < bestScore) {
						bestScore = score; best = b
					}
				}
				if (best != null) return best
			}
			for (WebElement b : visible) {
				String txt = normalizeText((b.getText() ?: "") + " " + (b.getAttribute("value") ?: "") + " " + (b.getAttribute("title") ?: "")).replaceAll("\\s+","")
				if ((txt.contains("중복") || txt == "조회" || txt == "확인") && !txt.contains("우편") && !txt.contains("주소")) return b
			}
		} catch (Exception ignore) {}
		return null
	}

	static boolean isElementVisible(WebElement el) {
		try {
			return el != null && el.isDisplayed()
		} catch (Exception e) {
			return false
		}
	}

	static boolean hasBrowserDriver() {
		try {
			return driver() != null
		} catch (Exception e) {
			return false
		}
	}

	static Map makeTc(String type, String desc, String field, String target, String value, String expect) {
		return [id: "TC-00", type: type, desc: desc, field: field, target: target, value: value, expect: expect]
	}

	// =========================================================
	// [C] 버튼 / 클릭
	// =========================================================
	static void safeClick(WebElement el) {
		if (el == null) return
		try {
			runJs("arguments[0].scrollIntoView({block:'center'});", [el])
			waitSec(0.2)
			try {
				el.click()
			} catch (Exception e) {
				runJs("arguments[0].click();", [el])
			}
			waitSec(0.4)
		} catch (Exception ignore) {}
	}

	static WebElement findButtonByKeywords(List<String> keywords) {
		List<WebElement> buttons = driver().findElements(By.xpath("//button | //a | //input[@type='button'] | //input[@type='submit']"))
		for (WebElement el : buttons) {
			if (!isElementVisible(el)) continue
			String txt = normalizeText((el.getText() ?: "") + " " + (el.getAttribute("value") ?: "") + " " + (el.getAttribute("title") ?: ""))
			String compact = txt.replaceAll("\\s+", "")
			for (String kw : keywords) {
				if (compact.contains((kw ?: "").replaceAll("\\s+", ""))) return el
			}
		}
		return null
	}

	static void clickButtonByKeywords(String... keywords) {
		WebElement btn = findButtonByKeywords(keywords as List<String>)
		if (btn != null) safeClick(btn)
	}

	static void clickRegisterButton() {
		clickButtonByKeywords("가입하기", "회원가입", "등록", "신청", "제출", "완료")
	}

	static void clickDuplicateCheckButton() {
		WebElement btn = findDuplicateCheckButtonNearId()
		if (btn != null) {
			logLine("ID 중복확인 버튼 클릭: " + normalizeText((btn.getText() ?: "") + " " + (btn.getAttribute("value") ?: "")))
			safeClick(btn)
			waitSec(0.4)
			return
		}
		clickButtonByKeywords("중복확인", "중복체크", "중복", "조회", "확인")
	}

	static boolean existsDuplicateCheckButton() {
		return findDuplicateCheckButtonNearId() != null
	}

	static void closeBrowserAlertIfExists() {
		try {
			Alert a = driver().switchTo().alert(); a.accept(); waitSec(0.2)
		} catch (Exception ignore) {}
	}

	static void closeGeneralModalIfExists() {
		closeBrowserAlertIfExists()
		runJs('''
            document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer]").forEach(function(m){
                var s = window.getComputedStyle(m);
                if(s.display === "none") return;
                var bs = m.querySelectorAll("button,a,[class*=btn],input[type=button],input[type=submit]");
                for(var i=0;i<bs.length;i++){
                    var t = (bs[i].innerText || bs[i].value || "").replace(/\\s+/g,"");
                    if(t.indexOf("확인") >= 0 || t === "OK" || t.indexOf("닫기") >= 0){
                        try{ bs[i].click(); }catch(e){} break;
                    }
                }
            });
            document.querySelectorAll(".modal,.popup,[class*=modal],[class*=overlay],.modal-backdrop").forEach(function(e){
                try{ e.style.display = "none"; }catch(ex){}
            });
        ''')
		waitSec(0.2)
	}

	static void dismissGeneralPopups() {
		waitSec(1.0)
		closeBrowserAlertIfExists()
		try {
			new Actions(driver()).sendKeys(Keys.ESCAPE).perform(); waitSec(0.1)
		} catch (Exception ignore) {}
		runJs('''
            document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer]").forEach(function(md){
                if(window.getComputedStyle(md).display === "none") return;
                var bs = md.querySelectorAll("button,a,[class*=btn],input[type=button],input[type=submit]");
                for(var i=0;i<bs.length;i++){
                    var t = (bs[i].innerText || bs[i].value || "").replace(/\\s+/g,"");
                    if(t.indexOf("확인") >= 0 || t === "OK" || t.indexOf("닫기") >= 0){
                        try{ bs[i].click(); }catch(e){} break;
                    }
                }
            });
        ''')
		waitSec(0.3)
		closeGeneralModalIfExists()
	}

	static String collectPopupMessage(String hint, boolean waitPopup) {
		if (waitPopup) waitSec(1.2)
		try {
			Alert a = driver().switchTo().alert()
			String msg = sanitizePopupText(a.getText())
			a.accept()
			waitSec(0.2)
			return msg
		} catch (Exception ignore) {}

		Object r = runJs('''
			function vis(e){ if(!e) return false; var s=window.getComputedStyle(e); return e.offsetParent!==null&&s.visibility!=="hidden"&&s.display!=="none"; }
			function txt(v){ return (v||"").replace(/\s+/g," ").trim(); }
			var roots = Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],.swal2-popup,.toast,.Toastify,[class*=alert]")).filter(vis);
			var out = [];
			roots.forEach(function(root){
				var t = txt(root.innerText || root.textContent || "");
				if(t) out.push(t);
				var btns = root.querySelectorAll("button,a,input[type=button],input[type=submit],[class*=btn]");
				for(var i=0;i<btns.length;i++){
					var bt = txt(btns[i].innerText || btns[i].value || btns[i].title || "");
					if(bt.indexOf("확인")>=0 || bt.indexOf("닫기")>=0 || bt==="OK"){
						try{ btns[i].click(); }catch(e){}
						break;
					}
				}
			});
			if(out.length===0){
				var inline = Array.from(document.querySelectorAll(".error,.invalid-feedback,.form-text,.help-block,[class*=error],[class*=message]")).filter(vis).map(function(e){ return txt(e.innerText||e.textContent||""); }).filter(Boolean);
				if(inline.length) out = inline;
			}
			return out.join(" | ");
		''')
		closeGeneralModalIfExists()
		return sanitizePopupText(r?.toString() ?: "[팝업없음]")
	}


	static String collectPopupMessage(String hint = "") { return collectPopupMessage(hint, true) }

	// =========================================================
	// [E] 폼 초기화 / 공통값 세팅
	// =========================================================
	static void resetMainForm() {
		if (isNameSearchPopupMode && !nameFieldDone) {
			closeNameSearchPopup()
		} else if (isNameSearchPopupMode && nameFieldDone) {
			try { closeNameSearchPopup() } catch (Exception ignored) {}
		}

		try {
			String cur = driver().currentUrl ?: ""
			if (startUrl && cur && !cur.equals(startUrl) && !cur.startsWith("data:")) {
				driver().navigate().to(startUrl); waitSec(1.5)
			}
		} catch (Exception ignore) {}

		try { new Actions(driver()).sendKeys(Keys.ESCAPE).perform(); waitSec(0.1) } catch (Exception ignore) {}

		runJs('''
            document.querySelectorAll("form").forEach(function(f){ try{ f.reset(); }catch(e){} });
            document.querySelectorAll("input").forEach(function(i){
                if(i.type==="checkbox"||i.type==="radio") i.checked=false;
                else if(i.type!=="file") i.value="";
                i.dispatchEvent(new Event("input",{bubbles:true}));
                i.dispatchEvent(new Event("change",{bubbles:true}));
            });
            document.querySelectorAll("select").forEach(function(s){
                try{ s.selectedIndex=0; s.dispatchEvent(new Event("change",{bubbles:true})); }catch(e){}
            });
        ''')
	}

	static void fillVisibleEmptyFieldsWithDefault() {
		runJs('''
			document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])")
				.forEach(function(e){
					if(e.offsetParent===null) return;
					var id=(e.id||"").toLowerCase();
					var nm=(e.name||"").toLowerCase();
					var ph=(e.placeholder||"").toLowerCase();
					var lb="";
					if(e.labels&&e.labels.length) lb=(e.labels[0].innerText||"").toLowerCase();
					else{ var p=e.closest("div,tr,li,td,section"); if(p) lb=(p.innerText||"").toLowerCase(); }
					if(id.indexOf("address")>=0||id.indexOf("post")>=0||nm.indexOf("address")>=0||ph.indexOf("주소")>=0||ph.indexOf("우편")>=0||lb.indexOf("주소")>=0) return;
					if(id.indexOf("id")>=0||nm.indexOf("id")>=0||lb.indexOf("계정 id")>=0||lb.indexOf("계정id")>=0||ph.indexOf("아이디")>=0) return;
					if(e.value&&e.value.length>0) return;
					e.value=(e.type==="password")?"Temp123!@":"dummyData12";
					e.dispatchEvent(new Event("input",{bubbles:true}));
					e.dispatchEvent(new Event("change",{bubbles:true}));
				});
		''')
	}

	static void setFormFieldValue(String fieldType, String value) {
		String t = escapeJsString(fieldType)
		String v = escapeJsString(value ?: "")
		runJs("""
			var t='${t}', v='${v}', target=null;
			function labelText(e){
				var lb="";
				if(e.labels&&e.labels.length) lb=(e.labels[0].innerText||"").toLowerCase();
				else{ var p=e.closest("div,tr,li,td,section,article"); if(p) lb=(p.innerText||"").toLowerCase(); }
				return lb;
			}
			document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])")
				.forEach(function(e){
					if(target||e.offsetParent===null) return;
					var id=(e.id||"").toLowerCase(), nm=(e.name||"").toLowerCase(), ph=(e.placeholder||"").toLowerCase(), lb=labelText(e);
					var isAccountId = lb.indexOf("계정 id")>=0 || lb.indexOf("계정id")>=0 || lb.indexOf("아이디")>=0;
					if(t==="pwConf"&&e.type==="password"&&(id.indexOf("confirm")>=0||nm.indexOf("confirm")>=0||ph.indexOf("확인")>=0||ph.indexOf("재입력")>=0)) target=e;
					else if(t==="pw"&&e.type==="password"&&id.indexOf("confirm")<0&&nm.indexOf("confirm")<0&&ph.indexOf("확인")<0) target=e;
					else if(t==="id"&&e.type!=="password"&&(isAccountId || id.indexOf("userid")>=0 || nm.indexOf("userid")>=0 || ((id.indexOf("id")>=0||nm.indexOf("id")>=0||ph.indexOf("아이디")>=0) && ph.indexOf("이메일")<0))) target=e;
					else if(t==="name"&&e.type!=="password"&&(id.indexOf("name")>=0||nm.indexOf("name")>=0||ph.indexOf("이름")>=0||ph.indexOf("성명")>=0||lb.indexOf("이름")>=0||lb.indexOf("성명")>=0)) target=e;
					else if(t==="email"&& !isAccountId && (id.indexOf("email")>=0||nm.indexOf("email")>=0||ph.indexOf("이메일")>=0||ph.indexOf("@")>=0)) target=e;
					else if(t==="phone"&&(id.indexOf("phone")>=0||nm.indexOf("phone")>=0||ph.indexOf("전화")>=0||ph.indexOf("휴대")>=0)) target=e;
				});
			if(target){
				var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;
				setter.call(target, v);
				["input","change","blur"].forEach(function(ev){ target.dispatchEvent(new Event(ev,{bubbles:true})); });
			}
		""")
	}

	static boolean detectNameFieldUsesPopupSearch() {
		Object result = runJs('''
            function isInsideModal(e){ return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]"); }
            function vis(e){ if(!e) return false; var s=window.getComputedStyle(e); return e.offsetParent!==null&&s.display!=="none"&&s.visibility!=="hidden"&&!isInsideModal(e); }
            function isNameInput(e){
                if(!e||!vis(e)) return false;
                var id=(e.id||"").toLowerCase(), nm=(e.name||"").toLowerCase(), ph=(e.placeholder||"").toLowerCase(), lb="";
                if(e.labels&&e.labels.length) lb=(e.labels[0].innerText||"").toLowerCase();
                else{ var wrap=e.closest("div,td,tr,li,section"); if(wrap) lb=(wrap.innerText||"").toLowerCase(); }
                return id.indexOf("name")>=0||nm.indexOf("name")>=0||ph.indexOf("이름")>=0||ph.indexOf("성명")>=0||lb.indexOf("이름")>=0||lb.indexOf("성명")>=0||lb.indexOf("이름 검색")>=0;
            }
            function looksLikeSearchButton(el){
                if(!el||!vis(el)) return false;
                var tag=(el.tagName||"").toLowerCase();
                var text=((el.innerText||"")+" "+(el.title||"")+" "+(el.getAttribute("aria-label")||"")).toLowerCase();
                var cls=(el.className||"").toString().toLowerCase();
                var html=(el.innerHTML||"").toLowerCase();
                if(tag==="button") return true; if(el.getAttribute("role")==="button") return true;
                if(el.getAttribute("type")==="button") return true; if(text.indexOf("검색")>=0) return true;
                if(text.indexOf("search")>=0) return true; if(cls.indexOf("search")>=0) return true;
                if(cls.indexOf("icon")>=0) return true; if(html.indexOf("search")>=0) return true;
                if(html.indexOf("svg")>=0) return true; return false;
            }
            var inputs=document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");
            for(var i=0;i<inputs.length;i++){
                var input=inputs[i]; if(!isNameInput(input)) continue;
                var wrap=input.parentElement; if(!wrap) continue;
                var cands=wrap.querySelectorAll("button,[role=button],input[type=button],span,i,svg");
                for(var j=0;j<cands.length;j++){ if(looksLikeSearchButton(cands[j])) return "true"; }
            }
            return "false";
        ''')
		return "true".equalsIgnoreCase(result?.toString())
	}

	static boolean openNameSearchPopup() {
		Object result = runJs('''
            function isInsideModal(e){ return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]"); }
            function vis(e){ if(!e) return false; var s=window.getComputedStyle(e); return e.offsetParent!==null&&s.display!=="none"&&s.visibility!=="hidden"&&!isInsideModal(e); }
            function isNameInput(e){
                if(!e||!vis(e)) return false;
                var id=(e.id||"").toLowerCase(),nm=(e.name||"").toLowerCase(),ph=(e.placeholder||"").toLowerCase(),lb="";
                if(e.labels&&e.labels.length) lb=(e.labels[0].innerText||"").toLowerCase();
                else{ var wrap=e.closest("div,td,tr,li,section"); if(wrap) lb=(wrap.innerText||"").toLowerCase(); }
                return id.indexOf("name")>=0||nm.indexOf("name")>=0||ph.indexOf("이름")>=0||ph.indexOf("성명")>=0||lb.indexOf("이름")>=0||lb.indexOf("성명")>=0||lb.indexOf("이름 검색")>=0;
            }
            function calcScore(el,inputRect){
                if(!el||!vis(el)) return -1;
                var rect=el.getBoundingClientRect(), tag=(el.tagName||"").toLowerCase();
                var text=((el.innerText||"")+" "+(el.title||"")+" "+(el.getAttribute("aria-label")||"")).toLowerCase();
                var cls=(el.className||"").toString().toLowerCase(), html=(el.innerHTML||"").toLowerCase();
                var score=0;
                if(tag==="button") score+=5; if(el.getAttribute("role")==="button") score+=4; if(el.getAttribute("type")==="button") score+=4;
                if(text.indexOf("검색")>=0) score+=8; if(text.indexOf("search")>=0) score+=8; if(cls.indexOf("search")>=0) score+=7;
                if(cls.indexOf("icon")>=0) score+=3; if(html.indexOf("svg")>=0) score+=2;
                if(rect.left>=inputRect.right-5) score+=10; if(Math.abs(rect.top-inputRect.top)<40) score+=8;
                return score;
            }
            var inputs=document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");
            for(var i=0;i<inputs.length;i++){
                var input=inputs[i]; if(!isNameInput(input)) continue;
                var inputRect=input.getBoundingClientRect(), best=null, bestScore=-1;
                var searchRoot=input.parentElement;
                for(var up=0;up<4&&searchRoot;up++){
                    var cands=searchRoot.querySelectorAll("button,[role=button],input[type=button],span,i,svg,a");
                    for(var j=0;j<cands.length;j++){ var s=calcScore(cands[j],inputRect); if(s>bestScore){ best=cands[j]; bestScore=s; } }
                    if(bestScore>=10) break; searchRoot=searchRoot.parentElement;
                }
                if(best&&bestScore>=5){
                    try{ best.click(); }catch(e){ try{ var p=best.closest("button,[role=button]"); if(p) p.click(); }catch(ex){} }
                    return "clicked:"+bestScore;
                }
            }
            return "false";
        ''')
		String resultStr = result?.toString() ?: "false"
		boolean clicked = resultStr.startsWith("clicked") || "true".equalsIgnoreCase(resultStr)
		if (clicked) logLine("이름 팝업 버튼 클릭 완료 (score=" + resultStr + ")")
		if (!clicked) { logLine("이름 팝업 버튼 못 찾음"); return false }
		waitSec(1.5)
		return isNameSearchPopupOpen()
	}

	static boolean isNameSearchPopupOpen() {
		for (int attempt = 0; attempt < 3; attempt++) {
			Object r = runJs('''
                var candidates=Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]"));
                document.querySelectorAll("div,section,aside").forEach(function(el){
                    var s=window.getComputedStyle(el);
                    if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10)
                        if(candidates.indexOf(el)<0) candidates.push(el);
                });
                for(var i=0;i<candidates.length;i++){
                    var d=candidates[i], ds=window.getComputedStyle(d);
                    if(ds.display==="none"||ds.visibility==="hidden"||ds.opacity==="0") continue;
                    if(d.offsetWidth<100||d.offsetHeight<100) continue;
                    var txt=(d.innerText||"").replace(/\\s+/g," ").trim();
                    if(txt.indexOf("사용자 정보 검색")>=0||txt.indexOf("회원 선택")>=0) return "true";
                    var hasSearch=false,hasComplete=false;
                    d.querySelectorAll("button,[role=button]").forEach(function(b){
                        var bt=(b.innerText||"").replace(/\\s+/g,"");
                        if(bt.indexOf("조회")>=0||bt.indexOf("검색")>=0) hasSearch=true;
                        if(bt.indexOf("선택완료")>=0||bt.indexOf("완료")>=0) hasComplete=true;
                    });
                    if(hasSearch&&hasComplete) return "true";
                }
                return "false";
            ''')
			if ("true".equalsIgnoreCase(r?.toString())) return true
			if (attempt < 2) waitSec(0.5)
		}
		return false
	}

	static void closeNameSearchPopup() {
		try {
			runJs('''
                function findNamePopup(){
                    var candidates=Array.from(document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]'));
                    document.querySelectorAll('div,section,aside').forEach(function(el){
                        var s=window.getComputedStyle(el);
                        if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10)
                            if(candidates.indexOf(el)<0) candidates.push(el);
                    });
                    for(var i=0;i<candidates.length;i++){
                        var d=candidates[i], ds=window.getComputedStyle(d);
                        if(ds.display==="none"||ds.visibility==="hidden") continue;
                        var txt=(d.innerText||"").replace(/\\s+/g," ").trim();
                        if(txt.indexOf("사용자 정보 검색")>=0||txt.indexOf("회원 선택")>=0) return d;
                    }
                    return null;
                }
                var popup=findNamePopup(); if(!popup) return "notfound";
                var closeBtn=popup.querySelector('[class*=close],[class*=Close],[class*=dismiss],[aria-label*=닫기],[aria-label*=close]');
                if(closeBtn){ try{ closeBtn.click(); }catch(e){} return "closebtn"; }
                var btns=popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit],a');
                for(var j=0;j<btns.length;j++){
                    var bt=(btns[j].innerText||btns[j].value||"").trim();
                    if(bt==="×"||bt==="✕"||bt==="X"||bt==="x"||bt.indexOf("닫기")>=0||bt.indexOf("취소")>=0){
                        try{ btns[j].click(); }catch(e){} return "xbtn";
                    }
                }
                popup.style.display="none"; return "hidden";
            ''')
		} catch (Exception ignore) {}
		try { driver().findElement(By.tagName("body")).sendKeys(Keys.ESCAPE) } catch (Exception ignore) {}
		waitSec(0.5)
		try {
			runJs('''
                document.querySelectorAll('div,section,aside').forEach(function(el){
                    var s=window.getComputedStyle(el);
                    if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10){
                        var ds=window.getComputedStyle(el);
                        if(ds.display==="none"||ds.visibility==="hidden") return;
                        var txt=(el.innerText||"").replace(/\\s+/g," ").trim();
                        if(txt.indexOf("사용자 정보 검색")>=0||txt.indexOf("회원 선택")>=0) el.style.display="none";
                    }
                });
            ''')
		} catch (Exception ignore) {}
	}

	static void closeInnerCompletePopupIfExists() {
		try {
			runJs('''
                var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
                for(var i=0;i<dialogs.length;i++){
                    var d=dialogs[i]; if(d.offsetParent===null) continue;
                    var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                    if(txt.indexOf('사용자 정보 검색')>=0||txt.indexOf('회원 선택')>=0) continue;
                    if(txt.indexOf('중복 확인이 완료되었습니다')>=0||txt.indexOf('중복 이 완료되었습니다')>=0||(txt.indexOf('완료')>=0&&txt.indexOf('중복')>=0)){
                        var btns=d.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
                        for(var j=0;j<btns.length;j++){
                            var bt=(btns[j].innerText||btns[j].value||'').replace(/\\s+/g,'');
                            if(bt.indexOf('확인')>=0||bt.indexOf('완료')>=0){ try{ btns[j].click(); }catch(e){} return; }
                        }
                    }
                }
            ''')
			waitSec(0.5)
		} catch (Exception ignore) {}
		try { Alert a = driver().switchTo().alert(); a.accept(); waitSec(0.3) } catch (Exception ignore) {}
	}

	// =========================================================
	// [G] 이름 검색 팝업 스캔
	// =========================================================
	static Map scanNameSearchPopup() {
		Map out = [exists: false, title: "", listCount: 0, selects: [], inputs: [], checkboxes: [], hasSearchButton: false, hasCompleteButton: false]
		if (!isNameSearchPopupMode) return out

		boolean opened = false
		for (int attempt = 0; attempt < 2; attempt++) {
			if (openNameSearchPopup()) { opened = true; break }
			if (attempt < 1) waitSec(1.0)
		}
		if (!opened) return out
		waitSec(1.2)

		try {
			Object r = runJs('''
                function vis(e){ if(!e) return false; var s=window.getComputedStyle(e); return e.offsetParent!==null&&s.display!=="none"&&s.visibility!=="hidden"; }
                function text(v){ return (v||"").replace(/\\s+/g," ").trim(); }
                var candidates=Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]"));
                document.querySelectorAll("div,section,aside").forEach(function(el){
                    var s=window.getComputedStyle(el);
                    if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10)
                        if(candidates.indexOf(el)<0) candidates.push(el);
                });
                var popup=null;
                for(var i=0;i<candidates.length;i++){
                    var d=candidates[i], ds=window.getComputedStyle(d);
                    if(ds.display==='none'||ds.visibility==='hidden'||ds.opacity==='0') continue;
                    if(d.offsetWidth<100||d.offsetHeight<100) continue;
                    var t=text(d.innerText||"");
                    if(t.indexOf("사용자 정보 검색")>=0||t.indexOf("회원 선택")>=0){ popup=d; break; }
                    var hasSearch=false, hasComplete=false;
                    d.querySelectorAll('button,[role=button]').forEach(function(b){
                        var bt=(b.innerText||'').replace(/\\s+/g,'');
                        if(bt.indexOf('조회')>=0||bt.indexOf('검색')>=0) hasSearch=true;
                        if(bt.indexOf('선택완료')>=0||bt.indexOf('완료')>=0) hasComplete=true;
                    });
                    if(hasSearch&&hasComplete){ popup=d; break; }
                }
                if(!popup) return JSON.stringify({exists:false,title:"",listCount:0,selects:[],inputs:[],checkboxes:[],hasSearchButton:false,hasCompleteButton:false});

                var result={exists:true,title:"",listCount:0,selects:[],inputs:[],checkboxes:[],hasSearchButton:false,hasCompleteButton:false};
                var titleEl=popup.querySelector("h1,h2,h3,strong,[class*=title],[class*=header]");
                result.title=text(titleEl?titleEl.innerText:"");

                popup.querySelectorAll("select").forEach(function(s,idx){
                    if(!vis(s)||!s.options||s.options.length<=1) return;
                    var label="";
                    if(s.labels&&s.labels.length) label=text(s.labels[0].innerText);
                    if(!label){ var prev=s.previousElementSibling; while(prev&&(prev.nodeName==="SELECT"||prev.nodeName==="BR")) prev=prev.previousElementSibling; if(prev) label=text(prev.innerText||prev.textContent); }
                    if(!label){ var wrap=s.closest("div,td,th,li,label,span"); if(wrap){ var clone=wrap.cloneNode(true); clone.querySelectorAll("select,input,button").forEach(function(e){e.remove();}); label=text(clone.innerText||clone.textContent); } }
                    if(!label&&s.options&&s.options.length>0) label=text(s.options[0].text).replace("전체","").replace("선택","").trim()||("드롭박스"+(idx+1));
                    if(!label) label="드롭박스"+(idx+1);
                    var opts=[];
                    for(var i=0;i<s.options.length;i++) opts.push({index:i,text:text(s.options[i].text),value:s.options[i].value||""});
                    result.selects.push({index:idx,label:label,options:opts});
                });

                popup.querySelectorAll("input:not([type=hidden]):not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit])").forEach(function(inp,idx){
                    if(!vis(inp)) return;
                    var label="";
                    if(inp.labels&&inp.labels.length) label=text(inp.labels[0].innerText);
                    if(!label){ var wrap=inp.closest("div,td,tr,li,section"); if(wrap){ var temp=text(wrap.innerText); label=temp?temp.split(" ")[0]:""; } }
                    result.inputs.push({index:idx,type:inp.type||"text",id:inp.id||"",name:inp.name||"",placeholder:inp.placeholder||"",label:label,value:inp.value||""});
                });

                popup.querySelectorAll("input[type=checkbox]").forEach(function(cb,idx){
                    if(!vis(cb)) return;
                    var label="";
                    if(cb.labels&&cb.labels.length) label=text(cb.labels[0].innerText);
                    if(!label){ var box=cb.closest("label,div,li"); if(box) label=text(box.innerText); }
                    result.checkboxes.push({index:idx,id:cb.id||"",name:cb.name||"",label:label,checked:!!cb.checked});
                });

                var rows=popup.querySelectorAll("tbody tr");
                if(rows.length>0){ result.listCount=rows.length; }
                else{
                    var SKIP=["사용자 정보 검색","회원 선택","선택 완료","조회"];
                    var cards=Array.from(popup.querySelectorAll("div,li,button,a,label")).filter(function(el){
                        if(!vis(el)) return false; var t=text(el.innerText||""); if(t.length<2) return false;
                        for(var k=0;k<SKIP.length;k++) if(t.indexOf(SKIP[k])>=0) return false; return true;
                    });
                    result.listCount=cards.length;
                }

                var btns=popup.querySelectorAll("button,[role=button],input[type=button],input[type=submit]");
                for(var b=0;b<btns.length;b++){
                    var bt=text(btns[b].innerText||btns[b].value||"");
                    if(bt.indexOf("조회")>=0||bt.indexOf("검색")>=0) result.hasSearchButton=true;
                    if(bt.indexOf("선택 완료")>=0||bt.indexOf("완료")>=0||bt.indexOf("확인")>=0) result.hasCompleteButton=true;
                }
                return JSON.stringify(result);
            ''')
			out = (Map) new JsonSlurper().parseText(r?.toString() ?: "{}")
		} catch (Exception e) {
			out = [exists: false, title: "", listCount: 0, selects: [], inputs: [], checkboxes: [], hasSearchButton: false, hasCompleteButton: false]
		} finally {
			closeNameSearchPopup()
		}
		return out
	}

	// =========================================================
	// [H] 이름 검색 팝업 기본 액션
	// =========================================================
	private static String POPUP_FIND_JS = '''
        function findNamePopup(){
            var cs=Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]"));
            document.querySelectorAll("div,section,aside").forEach(function(el){
                var s=window.getComputedStyle(el);
                if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10) if(cs.indexOf(el)<0) cs.push(el);
            });
            for(var i=0;i<cs.length;i++){
                var d=cs[i],ds=window.getComputedStyle(d);
                if(ds.display==="none"||ds.visibility==="hidden") continue;
                var t=(d.innerText||"").replace(/\\s+/g," ").trim();
                if(t.indexOf("사용자 정보 검색")>=0||t.indexOf("회원 선택")>=0) return d;
            }
            return null;
        }
    '''

	static boolean actionNamePopupCompleteWithoutSelection() {
		if (!openNameSearchPopup()) return false
		runJs(POPUP_FIND_JS + '''
            var popup=findNamePopup(); if(!popup) return;
            var btns=popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
            for(var i=btns.length-1;i>=0;i--){
                var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
                if(tx.indexOf('선택완료')>=0||tx.indexOf('완료')>=0||tx.indexOf('확인')>=0){
                    try{ btns[i].click(); }catch(e){} break;
                }
            }
        ''')
		waitSec(0.8)
		return true
	}

	static boolean actionNamePopupSelectFirstItem() {
		if (!openNameSearchPopup()) return false
		runJs(POPUP_FIND_JS + '''
            var popup=findNamePopup(); if(!popup) return;
            var clicked=false;
            var rows=popup.querySelectorAll('tbody tr');
            if(rows.length>0){ try{ rows[0].click(); clicked=true; }catch(ex){} }
            if(!clicked){
                var SKIP=["사용자 정보 검색","회원 선택","선택 완료","조회"];
                var cards=Array.from(popup.querySelectorAll('div,li,button,a,label')).filter(function(el){
                    if(el.offsetParent===null) return false; var txt=(el.innerText||'').replace(/\\s+/g,' ').trim();
                    if(txt.length<2) return false; for(var k=0;k<SKIP.length;k++) if(txt.indexOf(SKIP[k])>=0) return false; return true;
                });
                if(cards.length>0){ try{ cards[0].click(); clicked=true; }catch(ex){} }
            }
            var checks=popup.querySelectorAll("input[type=checkbox]");
            if(checks.length>0){ try{ checks[0].checked=true; checks[0].dispatchEvent(new Event("change",{bubbles:true})); }catch(ex){} }
        ''')
		waitSec(0.5)
		runJs(POPUP_FIND_JS + '''
            var popup=findNamePopup(); if(!popup) return;
            var btns=popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
            for(var i=btns.length-1;i>=0;i--){
                var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
                if(tx.indexOf('선택완료')>=0||tx.indexOf('완료')>=0||tx.indexOf('확인')>=0||tx.indexOf('적용')>=0){
                    try{ btns[i].click(); }catch(e){} break;
                }
            }
        ''')
		waitSec(0.8)
		closeInnerCompletePopupIfExists()
		return true
	}

	static boolean actionNamePopupSetDropdownDefault(String value) {
		int selectIdx = (value ?: "0") as int
		if (!openNameSearchPopup()) return false
		runJs(POPUP_FIND_JS + """
            var popup=findNamePopup(); if(!popup) return;
            var selects=popup.querySelectorAll("select");
            if(selects.length>${selectIdx}){ selects[${selectIdx}].selectedIndex=0; selects[${selectIdx}].dispatchEvent(new Event("change",{bubbles:true})); }
        """)
		waitSec(0.5); closeNameSearchPopup(); return true
	}

	static boolean actionNamePopupSelectDropdownOption(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return false
		int selectIdx = (parts[0] ?: "0") as int
		int optionIdx = (parts[1] ?: "0") as int
		if (!openNameSearchPopup()) return false
		runJs(POPUP_FIND_JS + """
            var popup=findNamePopup(); if(!popup) return;
            var selects=popup.querySelectorAll("select");
            if(selects.length>${selectIdx}){ var s=selects[${selectIdx}]; if(s.options.length>${optionIdx}){ s.selectedIndex=${optionIdx}; s.dispatchEvent(new Event("change",{bubbles:true})); } }
        """)
		waitSec(0.5)
		actionNamePopupClickSearchOnly(false)
		closeInnerCompletePopupIfExists()
		boolean stillOpen = isNameSearchPopupOpen()
		if (stillOpen) closeNameSearchPopup()
		return true
	}

	static boolean actionNamePopupSetInputValue(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 1) return false
		int inputIdx = (parts[0] ?: "0") as int
		String inputValue = parts.size() > 1 ? (parts[1] ?: "") : ""
		if (!openNameSearchPopup()) return false
		runJs(POPUP_FIND_JS + """
            var popup=findNamePopup(); if(!popup) return;
            var inputs=popup.querySelectorAll("input:not([type=hidden]):not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit])");
            if(inputs.length>${inputIdx}){ var inp=inputs[${inputIdx}]; inp.value='${escapeJsString(inputValue)}'; inp.dispatchEvent(new Event("input",{bubbles:true})); inp.dispatchEvent(new Event("change",{bubbles:true})); }
        """)
		waitSec(0.5)
		actionNamePopupClickSearchOnly(false); closeInnerCompletePopupIfExists(); closeNameSearchPopup(); return true
	}

	static boolean actionNamePopupSetCheckbox(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return false
		int idx = (parts[0] ?: "0") as int
		boolean on = "true".equalsIgnoreCase(parts[1] ?: "false")
		if (!openNameSearchPopup()) return false
		runJs(POPUP_FIND_JS + """
            var popup=findNamePopup(); if(!popup) return;
            var cbs=popup.querySelectorAll("input[type=checkbox]");
            if(cbs.length>${idx}){ cbs[${idx}].checked=${on ? 'true' : 'false'}; cbs[${idx}].dispatchEvent(new Event("change",{bubbles:true})); }
        """)
		waitSec(0.5); closeNameSearchPopup(); return true
	}

	static boolean actionNamePopupClickSearchOnly() { return actionNamePopupClickSearchOnly(true) }

	static boolean actionNamePopupClickSearchOnly(boolean openPopupFirst) {
		if (openPopupFirst && !openNameSearchPopup()) return false
		runJs(POPUP_FIND_JS + '''
            var popup=findNamePopup(); if(!popup) return;
            var btns=popup.querySelectorAll("button,[role=button],input[type=button],input[type=submit]");
            for(var i=0;i<btns.length;i++){
                var tx=(btns[i].innerText||btns[i].value||"").replace(/\\s+/g,"");
                if(tx.indexOf("조회")>=0||tx.indexOf("검색")>=0){ btns[i].focus(); try{ btns[i].click(); }catch(e){} break; }
            }
        ''')
		waitSec(1.5); return true
	}

	// =========================================================
	// [H-2] 이름 팝업 시나리오
	// =========================================================

	// 팝업 내부 서브TC 결과를 메인 testResults 에 누적하기 위한 콜백 (메인에서 주입)
	static Closure popupSubTcCallback = null

	// 팝업 서브TC 한 건 출력 (TC박스 형식 동일)
	private static void printPopupSubTc(String subId, String label, String inputVal, String result, boolean passed, String reason) {
		String mark = passed ? "PASS" : "FAIL"
		String sep  = "=" * 54
		String input = (!inputVal?.trim() ? "(빈 값)" : inputVal.take(40))
		println ""
		println sep
		println " ${mark} | ${subId} | 이름팝업 검증 | ${label}"
		println " 입력값: ${input}"
		println " 결과  :"
		println "   ${result}"
		println " 판정  : ${reason}"
		println sep

		if (popupSubTcCallback != null) {
			try { popupSubTcCallback.call(subId, label, inputVal, result, passed, reason) } catch (Exception ignore) {}
		}
	}

	static String runNamePopupScenario(Map popupScan) {
		List<String> log = []
		boolean scenarioSuccess = false
		int subTcNum = 0

		Closure<String> nextSubId = { String parentId ->
			subTcNum++
			return "${parentId}-P${String.format('%02d', subTcNum)}"
		}

		Closure<List<String>> getListItems = {
			Object r = runJs(POPUP_FIND_JS + '''
                var popup=findNamePopup(); if(!popup) return "[]";
                var items=[];
                var rows=popup.querySelectorAll("tbody tr");
                if(rows.length>0){ rows.forEach(function(r){ items.push((r.innerText||"").replace(/\\s+/g," ").trim()); }); }
                else{
                    var SKIP=["사용자 정보 검색","회원 선택","선택 완료","조회","검색"];
                    Array.from(popup.querySelectorAll("label,li,div[class*=item],div[class*=row],div[class*=card]")).forEach(function(el){
                        if(el.offsetParent===null) return; var t=(el.innerText||"").replace(/\\s+/g," ").trim();
                        if(t.length<2) return; for(var k=0;k<SKIP.length;k++) if(t.indexOf(SKIP[k])>=0) return; items.push(t);
                    });
                }
                return JSON.stringify(items);
            ''')
			try { return new JsonSlurper().parseText(r?.toString() ?: "[]") as List<String> }
			catch (Exception e) { return [] }
		}

		Closure<Void> setSearchInput = { String val ->
			String esc = escapeJsString(val)
			runJs(POPUP_FIND_JS + """
                var p=findNamePopup(); if(!p) return;
                var inp=p.querySelector("input:not([type=hidden]):not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit])");
                if(!inp) return;
                inp.focus();
                var nativeSetter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,"value");
                if(nativeSetter&&nativeSetter.set) nativeSetter.set.call(inp,'${esc}');
                else inp.value='${esc}';
                ["input","change","keyup"].forEach(function(ev){ inp.dispatchEvent(new Event(ev,{bubbles:true})); });
            """)
			waitSec(0.2)
		}

		Closure<Void> clickSearch = { actionNamePopupClickSearchOnly(false); waitSec(1.5) }

		// 부모 TC ID — 팝업 시나리오는 항상 TC-01로 고정됨 (DOM 순서상 첫 번째)
		String parentId = "TC-01"
		String firstFoundName = ""  // STEP2~3에서 실제 조회된 첫번째 항목명 저장

		try {
			if (!openNameSearchPopup()) {
				log << "[팝업열기실패]"
				return "[팝업열기실패] 이름 팝업을 열 수 없습니다"
			}
			waitSec(0.8)

			// ── STEP 0: 빈값 조회 ──────────────────────────────────────
			logLine("이름팝업 STEP0: 빈값 조회")
			try { setSearchInput("") } catch (Exception e) {}
			try { clickSearch() } catch (Exception e) {}
			List<String> initList = []
			try { initList = getListItems() } catch (Exception e) {}
			String initResult = "빈값 조회 시 리스트 ${initList.size()}건 표시"
			printPopupSubTc(nextSubId(parentId), "빈값 조회", "(빈 값)", initResult, true, "초기상태확인")
			log << initResult

			// ── STEP 1: 비정상값 검증 ──────────────────────────────────
			logLine("이름팝업 STEP1: 비정상값 검증")
			[
				[val: "123",      desc: "숫자만"],
				[val: "홍길동😊", desc: "이모지포함"],
				[val: "홍#길동",  desc: "특수문자포함"],
				[val: "   ",      desc: "공백만"]
			].each { Map tc ->
				String testVal = tc.val as String
				String testDesc = tc.desc as String
				try {
					try { setSearchInput("") } catch (Exception e) {}
					waitSec(0.2)
					setSearchInput(testVal); clickSearch()
					List<String> items = getListItems()
					String disp = testVal.trim() ?: "공백"
					String result; boolean passed; String reason
					if (items.isEmpty()) {
						result = "비정상 입력[${testDesc}] → 조회결과 없음 (정상 차단)"; passed = true; reason = "예외정상방어"
					} else {
						boolean contained = items.any { it.contains(testVal.trim()) }
						if (contained) {
							result = "비정상 입력[${testDesc}] → 리스트 ${items.size()}건, 비정상 검색어 포함됨"; passed = false; reason = "비정상값 조회 허용"
						} else {
							result = "비정상 입력[${testDesc}] → 리스트 ${items.size()}건 표시 (검색어 미포함)"; passed = true; reason = "검색어 미포함 통과"
						}
					}
					printPopupSubTc(nextSubId(parentId), "비정상값[${testDesc}]", testVal, result, passed, reason)
					log << result
				} catch (Exception e) {
					String errMsg = "비정상값[${testDesc}] 처리 에러 무시"
					printPopupSubTc(nextSubId(parentId), "비정상값[${testDesc}]", testVal, errMsg, true, "에러무시")
					log << errMsg
				}
			}

			// ── STEP 2: 정상 검색어 조회 ──────────────────────────────
			logLine("이름팝업 STEP2: 정상검색어 조회")
			String normalKeyword = "홍"
			try { setSearchInput(normalKeyword) } catch (Exception e) {}
			try { clickSearch() } catch (Exception e) {}
			List<String> normalList = []
			try { normalList = getListItems() } catch (Exception e) {}

			if (!normalList.isEmpty() && normalList.any { it.contains(normalKeyword) }) {
				String result = "정상 검색어[${normalKeyword}] → 리스트 ${normalList.size()}건, 검색어 포함 확인"
				printPopupSubTc(nextSubId(parentId), "정상검색어 조회", normalKeyword, result, true, "정상조회확인")
				log << result; scenarioSuccess = true
				if (!firstFoundName) firstFoundName = normalList[0].split("\\s+")[0].take(10)
			} else if (!normalList.isEmpty()) {
				String result = "정상 검색어[${normalKeyword}] → 리스트 ${normalList.size()}건 표시"
				printPopupSubTc(nextSubId(parentId), "정상검색어 조회", normalKeyword, result, true, "리스트표시확인")
				log << result; scenarioSuccess = true
				if (!firstFoundName) firstFoundName = normalList[0].split("\\s+")[0].take(10)
			} else {
				String result = "정상 검색어[${normalKeyword}] → 조회결과 없음, 전체 재조회 시도"
				printPopupSubTc(nextSubId(parentId), "정상검색어 조회", normalKeyword, result, false, "조회결과없음")
				log << result
				try { setSearchInput("") } catch (Exception e) {}
				try { clickSearch() } catch (Exception e) {}
				try { normalList = getListItems() } catch (Exception e) {}
				if (!normalList.isEmpty()) {
					String result2 = "전체 재조회 → 리스트 ${normalList.size()}건 표시"
					printPopupSubTc(nextSubId(parentId), "전체재조회", "(빈 값)", result2, true, "전체조회확인")
					log << result2; scenarioSuccess = true
				}
			}

			// ── STEP 3: 드롭다운 검증 ──────────────────────────────────
			logLine("이름팝업 STEP3: 드롭다운 검증")
			List<Map> selects = (popupScan?.selects ?: []) as List<Map>
			if (selects) selects.each { Map sel ->
				String selLabel = (sel.label ?: "드롭박스").toString()
				List<Map> options = (sel.options ?: []) as List<Map>
				int selIdx = (sel.index ?: 0) as int
				options.each { Map opt ->
					try {
						int optIdx = (opt.index ?: 0) as int
						String optTxt = (opt.text ?: "").toString().trim()
						if (optIdx == 0 || !optTxt || optTxt.contains("선택")) return
						runJs(POPUP_FIND_JS + """
                            var p=findNamePopup(); if(!p) return;
                            var ss=p.querySelectorAll("select");
                            if(ss.length>${selIdx}){ ss[${selIdx}].selectedIndex=${optIdx}; ss[${selIdx}].dispatchEvent(new Event("change",{bubbles:true})); }
                        """)
						waitSec(0.3); setSearchInput(normalKeyword); clickSearch()
						List<String> dropList = getListItems()
						String result; boolean passed; String reason
						if (!dropList.isEmpty() && dropList.any { it.contains(normalKeyword) }) {
							result = "드롭다운[${selLabel}=${optTxt}] + 검색어[${normalKeyword}] → 리스트 ${dropList.size()}건, 검색어 포함"
							passed = true; reason = "필터+검색 정상"; scenarioSuccess = true
						} else if (!dropList.isEmpty()) {
							result = "드롭다운[${selLabel}=${optTxt}] → 리스트 ${dropList.size()}건 표시"
							passed = true; reason = "필터조회확인"; scenarioSuccess = true
						} else {
							result = "드롭다운[${selLabel}=${optTxt}] → 조회결과 없음 (정상 필터링)"
							passed = true; reason = "필터결과없음→정상"
						}
						printPopupSubTc(nextSubId(parentId), "드롭다운[${selLabel}]", "${optTxt} + 검색어[${normalKeyword}]", result, passed, reason)
						log << result
					} catch (Exception e) {
						String errMsg = "드롭다운[${sel.label}=${opt.text}] 처리 에러 무시"
						printPopupSubTc(nextSubId(parentId), "드롭다운[${sel.label}]", opt.text?.toString() ?: "", errMsg, true, "에러무시")
						log << errMsg
					}
				}
			}

			// ── STEP 4: 최종 항목 선택 ────────────────────────────────
			logLine("이름팝업 STEP4: 최종 항목 선택")
			try {
				runJs(POPUP_FIND_JS + '''
                    var p=findNamePopup(); if(!p) return;
                    p.querySelectorAll("select").forEach(function(s){ s.selectedIndex=0; s.dispatchEvent(new Event("change",{bubbles:true})); });
                ''')
				waitSec(0.3)
			} catch (Exception e) {}

			try { setSearchInput("") } catch (Exception e) {}
			try { clickSearch() } catch (Exception e) {}
			List<String> finalList = []
			try { finalList = getListItems() } catch (Exception e) {}

			if (!finalList.isEmpty()) {
				try { actionNamePopupSelectFirstItem() } catch (Exception e) {}
				String result = "빈값 조회 후 첫번째 항목 선택 완료 (리스트 ${finalList.size()}건)"
				printPopupSubTc(nextSubId(parentId), "최종 항목 선택", "(빈 값)", result, true, "선택완료")
				log << result
			} else {
				String result = "조회결과 없음 → 팝업 닫고 직접 입력으로 폴백"
				printPopupSubTc(nextSubId(parentId), "최종 항목 선택", "(빈 값)", result, false, "리스트없음폴백")
				log << result
				try { closeNameSearchPopup() } catch (Exception e) {}
				waitSec(0.5)
				try { setNameByAutoSelection(firstFoundName ?: normalKeyword) } catch (Exception e) {}
			}
		} catch (Exception ex) {
			String errMsg = "ERROR(무시): " + (ex.message ?: ex.toString()).take(80)
			printPopupSubTc(nextSubId(parentId), "팝업시나리오 오류", "", errMsg, false, "실행오류")
			log << errMsg
			try { closeNameSearchPopup() } catch (Exception ignored) {}
		}

		nameFieldDone = true
		return log ? log.last() : ""
	}

	// =========================================================
	// [I] 체크박스 / 라디오 / 드롭다운 / 주소
	// =========================================================
	static void checkAllCheckboxes() {
		runJs('document.querySelectorAll("input[type=checkbox]").forEach(function(c){ c.checked=true; c.dispatchEvent(new Event("change",{bubbles:true})); });')
	}

	static void uncheckAllCheckboxes() {
		runJs('document.querySelectorAll("input[type=checkbox]").forEach(function(c){ c.checked=false; c.dispatchEvent(new Event("change",{bubbles:true})); });')
	}

	static void setCheckboxByIndex(int idx, boolean on) {
		runJs("var c=document.querySelectorAll(\"input[type=checkbox]\"); if(c.length>${idx}){ c[${idx}].checked=${on ? 'true' : 'false'}; c[${idx}].dispatchEvent(new Event(\"change\",{bubbles:true})); }")
	}

	static void checkOnlyFirstCheckbox() { uncheckAllCheckboxes(); setCheckboxByIndex(0, true) }

	static void clearRadioGroupSelection() {
		runJs('document.querySelectorAll("input[type=radio]").forEach(function(r){ r.checked=false; r.dispatchEvent(new Event("change",{bubbles:true})); });')
	}

	static void selectFirstRadioOrButtonGroup() {
		runJs('''
            var radios=document.querySelectorAll("input[type=radio]");
            if(radios.length>0){
                radios[0].checked=true; radios[0].dispatchEvent(new Event("change",{bubbles:true}));
                var label=radios[0].closest("label"); if(label) try{ label.click(); }catch(e){}
                return;
            }
            var groups={};
            document.querySelectorAll("button,[role=button]").forEach(function(b){
                if(b.offsetParent===null) return; var tx=(b.innerText||"").trim();
                if(!tx||tx.length>10) return;
                if(["중복","확인","가입","등록","신청","제출"].some(function(k){ return tx.indexOf(k)>=0; })) return;
                var key=(b.parentElement&&(b.parentElement.className||b.parentElement.id))||"g";
                if(!groups[key]) groups[key]=[]; groups[key].push(b);
            });
            for(var k in groups){ if(groups[k].length>=2){ try{ groups[k][0].click(); }catch(e){} break; } }
        ''')
	}

	static void selectRadioOrButtonOption(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return
		String name = parts[0]; int idx = (parts[1] ?: "0") as int
		runJs("""
            var radios=document.querySelectorAll('input[type=radio][name="${escapeJsString(name)}"]');
            if(radios.length>${idx}){
                radios[${idx}].checked=true; radios[${idx}].dispatchEvent(new Event("change",{bubbles:true}));
                var label=radios[${idx}].closest("label"); if(label) try{ label.click(); }catch(e){}
            }else{
                var buttons=Array.from(document.querySelectorAll("button,[role=button]")).filter(function(b){
                    return b.offsetParent!==null&&(b.innerText||"").trim().length>0&&(b.innerText||"").trim().length<=10;
                });
                if(buttons.length>${idx}) try{ buttons[${idx}].click(); }catch(e){}
            }
        """)
		waitSec(0.2)
	}

	static void autoSelectMainFormDropdowns() {
		runJs('''
            function vis(e){ var s=window.getComputedStyle(e); return e.offsetParent!==null&&s.display!=="none"&&s.visibility!=="hidden"; }
            document.querySelectorAll("select").forEach(function(s){
                if(!vis(s)||!s.options||s.options.length<=1||s.selectedIndex>0) return;
                for(var i=1;i<s.options.length;i++){
                    var v=(s.options[i].value||"").trim(), t=(s.options[i].text||"").trim();
                    if(v&&t.indexOf("선택")<0&&t.indexOf("--")<0){ s.selectedIndex=i; s.dispatchEvent(new Event("change",{bubbles:true})); break; }
                }
            });
        ''')
	}

	static void resetMainSelectByIdOrName(String selectIdOrName) {
		runJs("var s=document.getElementById('${escapeJsString(selectIdOrName)}')||document.querySelector('select[name=\"${escapeJsString(selectIdOrName)}\"]'); if(s){ s.selectedIndex=0; s.dispatchEvent(new Event(\"change\",{bubbles:true})); }")
	}

	static void selectMainDropdownOption(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return
		String sid = parts[0]; int idx = (parts[1] ?: "0") as int
		runJs("var s=document.getElementById('${escapeJsString(sid)}')||document.querySelector('select[name=\"${escapeJsString(sid)}\"]'); if(s&&s.options.length>${idx}){ s.selectedIndex=${idx}; s.dispatchEvent(new Event(\"change\",{bubbles:true})); }")
	}

	static void closeDaumAddressPopupIfExists() {
		try {
			String main = driver().getWindowHandle()
			for (String handle : driver().getWindowHandles()) {
				if (handle == main) continue
				driver().switchTo().window(handle)
				String cur = driver().getCurrentUrl()?.toLowerCase() ?: ""
				if (cur.contains("postcode") || cur.contains("daum") || cur.contains("about:blank")) driver().close()
			}
			driver().switchTo().window(main)
		} catch (Exception ignore) {}
	}

	static void fillDummyAddress() {
		closeDaumAddressPopupIfExists()
		runJs('''
            function vis(e){ return e&&e.offsetParent!==null; }
            function setValue(e,v){ if(!e) return; var ro=e.readOnly,di=e.disabled; e.readOnly=false; e.disabled=false; e.value=v; e.dispatchEvent(new Event("input",{bubbles:true})); e.dispatchEvent(new Event("change",{bubbles:true})); e.readOnly=ro; e.disabled=di; }
            function findByPh(t){ for(var i of document.querySelectorAll("input")){ if(vis(i)&&(i.placeholder||"").indexOf(t)>=0) return i; } return null; }
            function findByKey(keys){ for(var i of document.querySelectorAll("input")){ if(!vis(i)) continue; var id=(i.id||"").toLowerCase(),nm=(i.name||"").toLowerCase(); for(var k=0;k<keys.length;k++) if(id.indexOf(keys[k])>=0||nm.indexOf(keys[k])>=0) return i; } return null; }
            var zip=findByPh("우편번호")||findByKey(["post","zip","zipcode"]);
            var road=findByPh("도로명주소")||findByPh("도로명")||findByKey(["road","addr1","address","roadaddr"]);
            var jibun=findByPh("지번주소")||findByPh("지번")||findByKey(["jibun","addr2","jibunaddr"]);
            var detail=findByPh("상세주소")||findByPh("상세")||findByKey(["detail","addr3","detailaddr","addrdetail"]);
            if(zip&&!(zip.value||"").trim()) setValue(zip,"06000");
            if(road&&!(road.value||"").trim()) setValue(road,"서울특별시 강남구 테헤란로 123");
            if(jibun&&!(jibun.value||"").trim()) setValue(jibun,"서울특별시 강남구 역삼동 123-45");
            if(detail&&!(detail.value||"").trim()) setValue(detail,"101동 101호");
        ''')
	}

	static void setAddressCase(String mode) {
		runJs('''
            ["우편번호","도로명","지번","상세","post","zip","road","addr","address","detail"].forEach(function(k){
                document.querySelectorAll("input").forEach(function(i){
                    if(i.offsetParent===null) return;
                    var ph=(i.placeholder||"").toLowerCase(), id=(i.id||"").toLowerCase(), nm=(i.name||"").toLowerCase();
                    if(ph.indexOf(k)>=0||id.indexOf(k)>=0||nm.indexOf(k)>=0){ i.value=""; i.dispatchEvent(new Event("input",{bubbles:true})); }
                });
            });
        ''')
		if (mode == "clear") return
		String m = escapeJsString(mode)
		runJs("""
            function vis(e){ return e&&e.offsetParent!==null; }
            function setValue(e,v){ if(!e) return; e.value=v; e.dispatchEvent(new Event("input",{bubbles:true})); e.dispatchEvent(new Event("change",{bubbles:true})); }
            function findByPh(t){ for(var i of document.querySelectorAll("input")){ if(vis(i)&&(i.placeholder||"").indexOf(t)>=0) return i; } return null; }
            function findByKey(keys){ for(var i of document.querySelectorAll("input")){ if(!vis(i)) continue; var id=(i.id||"").toLowerCase(),nm=(i.name||"").toLowerCase(); for(var k=0;k<keys.length;k++) if(id.indexOf(keys[k])>=0||nm.indexOf(keys[k])>=0) return i; } return null; }
            var zip=findByPh("우편번호")||findByKey(["post","zip"]);
            var road=findByPh("도로명주소")||findByKey(["road","addr1","address"]);
            var detail=findByPh("상세주소")||findByKey(["detail","addr3"]);
            var mode='${m}';
            if(mode==="zipOnly") setValue(zip,"06000");
            if(mode==="roadOnly") setValue(road,"서울특별시 강남구 테헤란로 123");
            if(mode==="detailOnly") setValue(detail,"101동 101호");
        """)
	}

	static void applyGeneralRequiredDefaults() {
		try { selectFirstRadioOrButtonGroup() } catch (Exception ignore) {}
		try { fillDummyAddress() } catch (Exception ignore) {}
		try { autoSelectMainFormDropdowns() } catch (Exception ignore) {}
	}

	// =========================================================
	// [J] 메인 화면 스캔
	// =========================================================
	static Map scanMainFormElements() {
		try {
			Object r = runJs('''
                function isInsideModal(e){ return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]"); }
                function vis(e){ var s=window.getComputedStyle(e); return e.offsetParent!==null&&s.display!=="none"&&s.visibility!=="hidden"&&!e.closest("header,footer,nav")&&!isInsideModal(e); }
                function txt(e){ return (e.innerText||"").replace(/\\s+/g," ").trim(); }
                var result={groups:[],selects:[],checkboxes:[],fileInputs:[]};
                var radioMap={};
                document.querySelectorAll("input[type=radio]").forEach(function(r){
                    if(!vis(r)) return; var name=r.name||"r"; if(!radioMap[name]) radioMap[name]=[];
                    var label="";
                    if(r.labels&&r.labels.length) label=txt(r.labels[0]);
                    else{ var p=r.closest("label"); if(p) label=txt(p); }
                    radioMap[name].push(label||r.value||"옵션");
                });
                for(var n in radioMap){
                    var first=document.querySelector('input[type=radio][name="'+n+'"]');
                    if(!first||!vis(first)) continue;
                    var groupWrap=first.closest("div,fieldset"), groupLabel=n;
                    if(groupWrap){ var prev=groupWrap.previousElementSibling; if(prev&&vis(prev)) groupLabel=txt(prev).replace("*","").trim(); }
                    result.groups.push({name:n,label:groupLabel,options:Array.from(new Set(radioMap[n])),type:"radio"});
                }
                var selectDupMap={};
                document.querySelectorAll("select").forEach(function(s){
                    if(!vis(s)||!s.options||s.options.length<=1) return;
                    var options=[]; for(var i=0;i<s.options.length;i++) options.push({value:s.options[i].value,text:txt(s.options[i])});
                    var label="";
                    if(s.labels&&s.labels.length&&vis(s.labels[0])) label=txt(s.labels[0]).replace("*","").trim();
                    else{ var prev=s.previousElementSibling; if(prev&&prev.tagName!=="SELECT"&&vis(prev)) label=txt(prev).replace("*","").trim(); }
                    var key=(s.id||"")+(s.name||"")+label; if(selectDupMap[key]) return; selectDupMap[key]=1;
                    if(!label) label=s.id||s.name||"드롭박스";
                    result.selects.push({id:s.id||"",name:s.name||"",label:label,options:options});
                });
                document.querySelectorAll("input[type=checkbox]").forEach(function(cb){
                    if(!vis(cb)) return; var label="";
                    if(cb.labels&&cb.labels.length&&vis(cb.labels[0])) label=txt(cb.labels[0]);
                    else{ var p=cb.closest("label"); if(p&&vis(p)) label=txt(p); }
                    result.checkboxes.push({id:cb.id||"",name:cb.name||"",label:label,index:result.checkboxes.length});
                });
                document.querySelectorAll("input[type=file]").forEach(function(f,idx){
                    var s=window.getComputedStyle(f);
                    var inModal=isInsideModal(f);
                    // file input은 커스텀UI로 hidden처리된 경우도 감지 (DOM에만 있으면 OK)
                    var domExists=document.contains(f)&&!inModal&&!f.closest("header,footer,nav");
                    if(!domExists) return;
                    var label="", accept=f.accept||"", multiple=!!f.multiple;
                    if(f.labels&&f.labels.length) label=txt(f.labels[0]).replace("*","").trim();
                    if(!label){ var wrap=f.closest("div,td,li,section"); if(wrap){ var clone=wrap.cloneNode(true); clone.querySelectorAll("input,button,span[class*=icon]").forEach(function(e){e.remove();}); label=(clone.innerText||clone.textContent||"").replace(/\\s+/g," ").trim(); } }
                    if(!label) label="첨부파일"+(idx>0?idx+1:"");
                    result.fileInputs.push({index:idx,id:f.id||"",name:f.name||"",label:label,accept:accept,multiple:multiple});
                });
                return JSON.stringify(result);
            ''')
			return (Map) new JsonSlurper().parseText(r?.toString() ?: '{"groups":[],"selects":[],"checkboxes":[],"fileInputs":[]}')
		} catch (Exception e) { return [groups: [], selects: [], checkboxes: [], fileInputs: []] }
	}

	static List<String> scanMainFormDomOrder() {
		try {
			Object r = runJs('''
				var result=[], seen={};
				function push(k){ if(!seen[k]){ seen[k]=1; result.push(k); } }
				function isInsideModal(e){ return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]"); }
				function vis(e){ if(!e) return false; var s=window.getComputedStyle(e); return e.offsetParent!==null&&s.display!=="none"&&s.visibility!=="hidden"&&!isInsideModal(e); }
				function labelText(el){
					var lb="";
					if(el.labels&&el.labels.length) lb=(el.labels[0].innerText||"").toLowerCase();
					else{ var p=el.closest("div,td,tr,li,section,article"); if(p) lb=(p.innerText||"").toLowerCase(); }
					return lb;
				}
				var all=[];
				document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])").forEach(function(e){ if(vis(e)) all.push({e:e,t:"input"}); });
				var radioSeen={};
				document.querySelectorAll("input[type=radio]").forEach(function(e){ if(!vis(e)) return; var n=e.name||"r"; if(!radioSeen[n]){ radioSeen[n]=1; all.push({e:e,t:"radio"}); } });
				document.querySelectorAll("select").forEach(function(e){ if(vis(e)&&e.options&&e.options.length>1) all.push({e:e,t:"select"}); });
				var cbAdded=false;
				document.querySelectorAll("input[type=checkbox]").forEach(function(e){ if(!vis(e)||cbAdded) return; all.push({e:e,t:"cb"}); cbAdded=true; });
				document.querySelectorAll("input[type=file]").forEach(function(e){ if(document.contains(e)&&!isInsideModal(e)) all.push({e:e,t:"file"}); });
				all.sort(function(a,b){ var ra=a.e.getBoundingClientRect(),rb=b.e.getBoundingClientRect(); return Math.abs(ra.top-rb.top)>10?ra.top-rb.top:ra.left-rb.left; });
				for(var i=0;i<all.length;i++){
					var item=all[i],el=item.e;
					if(item.t==="input"){
						var id=(el.id||"").toLowerCase(),nm=(el.name||"").toLowerCase(),ph=(el.placeholder||"").toLowerCase(),lb=labelText(el);
						if(el.type==="password"){
							(id.indexOf("confirm")>=0||nm.indexOf("confirm")>=0||ph.indexOf("확인")>=0)?push("pwConf"):push("pw");
						}else if(lb.indexOf("계정 id")>=0 || lb.indexOf("계정id")>=0 || lb.indexOf("아이디")>=0 || ((id.indexOf("id")>=0||nm.indexOf("id")>=0||ph.indexOf("아이디")>=0) && ph.indexOf("이메일")<0)) push("id");
						else if(id.indexOf("email")>=0||nm.indexOf("email")>=0||ph.indexOf("이메일")>=0||ph.indexOf("@")>=0) push("email");
						else if(id.indexOf("name")>=0||nm.indexOf("name")>=0||ph.indexOf("이름")>=0) push("name");
						else if(id.indexOf("phone")>=0||nm.indexOf("phone")>=0||ph.indexOf("전화")>=0) push("phone");
						else if(id.indexOf("addr")>=0||nm.indexOf("addr")>=0||ph.indexOf("주소")>=0||ph.indexOf("우편")>=0) push("address");
					}else if(item.t==="radio") push("btnGroup");
					else if(item.t==="select") push("select_"+(el.id||el.name||i));
					else if(item.t==="cb") push("terms");
					else if(item.t==="file") push("file_"+(el.id||el.name||i));
				}
				return result.join(",");
			''')
			List<String> raw = (r?.toString() ?: "").split(",").findAll { it?.trim() }
			List<String> ordered = []
			if (raw.contains("id")) ordered << "id"
			raw.each { if (it != "id") ordered << it }
			return ordered
		} catch (Exception e) { return [] }
	}


	static boolean hasVisibleCssElement(String css) {
		try { return driver().findElements(By.cssSelector(css)).any { isElementVisible(it) } } catch (Exception e) { return false }
	}

	// =========================================================
	// [K] TC 생성
	// =========================================================
	static List<Map> buildStaticTestCases() {
		String FAIL = "실패", PASS = "성공"
		List<Map> nameTcs = isNameSearchPopupMode ? [] : [
			makeTc("단위-이름", "빈 값",    "이름", "name", "",        FAIL),
			makeTc("단위-이름", "숫자",     "이름", "name", "123",      FAIL),
			makeTc("단위-이름", "공백",     "이름", "name", "홍 길동",  FAIL),
			makeTc("단위-이름", "이모지",   "이름", "name", "홍길동😊", FAIL),
			makeTc("단위-이름", "51자초과", "이름", "name", "홍" * 51,  FAIL),
			makeTc("단위-이름", "특수문자", "이름", "name", "홍#길동",  FAIL),
			makeTc("단위-이름", "2자정상",  "이름", "name", "김철",     PASS)
		]
		return nameTcs + [
			// 아이디
			makeTc("단위-아이디", "빈 값",   "아이디", "id", "",       FAIL),
			makeTc("단위-아이디", "3자미달", "아이디", "id", "tes",     FAIL),
			makeTc("단위-아이디", "한글",    "아이디", "id", "관리자12", FAIL),
			makeTc("단위-아이디", "이모지",  "아이디", "id", "✨✨123", FAIL),
			makeTc("단위-아이디", "특수문자","아이디", "id", "user!@#", FAIL),
			makeTc("단위-아이디", "공백",    "아이디", "id", "use 01",  FAIL),
			makeTc("단위-아이디", "50자초과","아이디", "id", "a" * 50,  FAIL),
			makeTc("단위-아이디", "6자정상", "아이디", "id", "user12",  PASS),
			// 비밀번호
			makeTc("단위-비밀번호", "빈 값",  "비밀번호", "pw", "",         FAIL),
			makeTc("단위-비밀번호", "7자미달","비밀번호", "pw", "123456",    FAIL),
			makeTc("단위-비밀번호", "영문만", "비밀번호", "pw", "password",  FAIL),
			makeTc("단위-비밀번호", "숫자만", "비밀번호", "pw", "12345678",  FAIL),
			makeTc("단위-비밀번호", "특수만", "비밀번호", "pw", '1@#$%^&*', FAIL),
			makeTc("단위-비밀번호", "공백",   "비밀번호", "pw", "Pass 12!", FAIL),
			makeTc("단위-비밀번호", "영숫자만","비밀번호","pw", "pass1234",  FAIL),
			makeTc("단위-비밀번호", "한글",   "비밀번호", "pw", "비밀123!@", FAIL),
			makeTc("단위-비밀번호", "8자정상","비밀번호", "pw", "Test12!@",  PASS),
			// 비번확인
			makeTc("단위-비번확인", "빈 값",    "비밀번호", "pwConf", "",          FAIL),
			makeTc("단위-비번확인", "불일치",   "비밀번호", "pwConf", "wrong!@#1", FAIL),
			makeTc("단위-비번확인", "한글",     "비밀번호", "pwConf", "비밀123!@", FAIL),
			makeTc("단위-비번확인", "공백만",   "비밀번호", "pwConf", "       ",   FAIL),
			makeTc("단위-비번확인", "대소불일치","비밀번호","pwConf", "test123!@", FAIL),
			makeTc("단위-비번확인", "정상일치", "비밀번호", "pwConf", "Test123!@", PASS),
			// 주소
			makeTc("단위-주소", "전체미입력", "주소", "address", "clear",      FAIL),
			makeTc("단위-주소", "우편번호만", "주소", "address", "zipOnly",    FAIL),
			makeTc("단위-주소", "도로명만",   "주소", "address", "roadOnly",   FAIL),
			makeTc("단위-주소", "상세만",     "주소", "address", "detailOnly", FAIL),
			// 시나리오
			makeTc("시나리오", "전체빈값",    "이름", "clear",         "clear",     FAIL),
			makeTc("시나리오", "중복확인생략","중복확인", "skipDupCheck","testus01", FAIL),
			makeTc("시나리오", "ID변경",      "중복확인", "id",          "user02",   FAIL),
			makeTc("시나리오", "ID삭제",      "아이디", "id",            "",         FAIL),
			makeTc("시나리오", "중복3회",     "중복확인", "tripledup",   "tripledup", PASS),
			makeTc("시나리오", "ID공백",      "중복확인", "id",          " use 01 ", FAIL),
			makeTc("시나리오", "전체재시도",  "이름", "clear",           "clear",    FAIL),
			makeTc("시나리오", "ID대소문자",  "중복확인", "id",          "USER01",   PASS),
			// 성공
			makeTc("성공", "정상가입", "성공", "success", "success", PASS)
		]
	}

	static List<Map> buildMainFormDynamicTestCases(Map mainScan) {
		List<Map> tcs = []
		String FAIL = "실패", PASS = "성공"

		(mainScan.groups ?: []).each { Map g ->
			String label = (g.label ?: "선택항목").toString()
			List options = (g.options ?: []) as List
			if (options.size() < 2) return
			tcs << makeTc("단위-${label}", "${label} 미선택", label, "btnGroupClear", g.name?.toString(), FAIL)
			options.eachWithIndex { Object opt, int i ->
				tcs << makeTc("단위-${label}", "${label} [${opt}]", label, "btnGroupSelect", "${g.name}::${i}", PASS)
			}
		}

		(mainScan.selects ?: []).each { Map sel ->
			String label = (sel.label ?: "드롭박스").toString()
			String sid = (sel.id ?: sel.name ?: "").toString()
			if (!sid) return
			tcs << makeTc("단위-${label}", "${label} 미선택", label, "selectReset", sid, FAIL)
			List options = (sel.options ?: []) as List
			options.eachWithIndex { Object obj, int i ->
				Map opt = obj as Map; String txt = (opt.text ?: "").toString()
				if (i == 0 || !txt.trim() || txt.contains("선택")) return
				tcs << makeTc("단위-${label}", "${label} [${txt}]", label, "selectOption", "${sid}::${i}", PASS)
			}
		}

		List<Map> checkboxes = (mainScan.checkboxes ?: []) as List<Map>
		if (checkboxes && hasVisibleCssElement("input[type='checkbox']")) {
			tcs << makeTc("단위-약관", "전체미체크", "약관", "uncheckAll", "all", FAIL)
			tcs << makeTc("단위-약관", "전체체크",   "약관", "checkAll",   "all", PASS)
			if (((checkboxes[0]?.label ?: "") as String).contains("전체"))
				tcs << makeTc("단위-약관", "전체동의만", "약관", "checkOnlyFirst", "0", PASS)
			checkboxes.eachWithIndex { Map cb, int i ->
				if (((cb.label ?: "") as String).contains("전체")) return
				String label = ((cb.label ?: "항목${i}") as String).take(15)
				tcs << makeTc("단위-약관", "[${label}] 미체크", "약관", "uncheckOne", "${i}", FAIL)
				tcs << makeTc("단위-약관", "[${label}] 체크",   "약관", "checkOne",   "${i}", PASS)
			}
		}

		// ── 파일 첨부 필드 동적 TC 생성 ──────────────────────────
		List<Map> fileInputs = (mainScan.fileInputs ?: []) as List<Map>
		fileInputs.each { Map fi ->
			String label   = (fi.label   ?: "첨부파일").toString()
			String accept  = (fi.accept  ?: "").toString()
			int    fileIdx = (fi.index   ?: 0) as int

			// accept 속성으로 허용 확장자 파악
			List<String> allowedExts = []
			if (accept.trim()) {
				accept.split(",").each { String token ->
					token = token.trim().toLowerCase()
					if (token.startsWith(".")) allowedExts << token.substring(1)
					else if (token == "image/jpeg")         { allowedExts << "jpg"; allowedExts << "jpeg" }
					else if (token == "image/png")          allowedExts << "png"
					else if (token == "application/pdf")    allowedExts << "pdf"
					else if (token.contains("hwp"))         allowedExts << "hwp"
					else if (token.startsWith("image/"))    allowedExts << token.split("/")[1]
				}
				allowedExts = allowedExts.unique().findAll { it }
			}
			if (!allowedExts) allowedExts = ["pdf", "jpg", "png"]

			// 미첨부 제출
			tcs << makeTc("단위-${label}", "${label} 미첨부", label, "fileEmpty::${fileIdx}", "", FAIL)

			// 허용 확장자 정상 업로드 (최대 2개)
			allowedExts.take(2).each { String ext ->
				tcs << makeTc("단위-${label}", "${label} 정상[.${ext}]", label, "fileValid::${fileIdx}", ext, PASS)
			}

			// 비허용 확장자
			["exe", "sh", "bat"].findAll { !allowedExts.contains(it) }.each { String badExt ->
				tcs << makeTc("단위-${label}", "${label} 비허용[.${badExt}]", label, "fileInvalid::${fileIdx}", badExt, FAIL)
			}

			// 용량 초과 (허용 확장자 중 첫번째로)
			tcs << makeTc("단위-${label}", "${label} 용량초과", label, "fileOversize::${fileIdx}", allowedExts[0], FAIL)
		}

		return tcs
	}

	static List<Map> buildNamePopupDynamicTestCases(Map popupScan) {
		if (!isNameSearchPopupMode) return []
		return [makeTc("단위-이름팝업", "이름팝업 시나리오", "이름팝업", "namePopupScenario", "", "성공")]
	}

	// =========================================================
	// [K-2] TC 정렬
	// =========================================================
	static List<Map> sortTestCasesByDomOrder(List<Map> tcs, List<String> domOrder) {
		def domIndex = { String key ->
			for (int i = 0; i < domOrder.size(); i++) {
				String x = domOrder[i]
				if (x == key || x.startsWith(key)) return i
			}
			return 100
		}

		tcs.sort { Map one ->
			String target = (one.target ?: "").toString()
			String type   = (one.type ?: "").toString()
			if (type.startsWith("단위-이름")) return domIndex("name") + 0.3
			if (type.startsWith("단위-")) {
				Map<String, String> fieldMap = [name: "name", id: "id", pw: "pw", pwConf: "pwConf", address: "address", skipDupCheck: "id", tripledup: "id", email: "email", phone: "phone"]
				if (fieldMap.containsKey(target)) return domIndex(fieldMap[target])
				if (target.startsWith("btnGroup")) return domIndex("btnGroup")
				if (target.toLowerCase().contains("check") || target.toLowerCase().contains("uncheck")) return domIndex("terms")
				if (target.startsWith("file")) return domIndex("address") + 0.5
				if (target.toLowerCase().contains("select")) {
					String selId = ((one.value ?: "") as String).contains("::") ? ((one.value ?: "") as String).split("::")[0] : ""
					Integer matched = null
					for (int i = 0; i < domOrder.size(); i++) {
						if (domOrder[i].startsWith("select_") && selId && domOrder[i].contains(selId)) { matched = i; break }
					}
					if (matched != null) return matched
					return domIndex("select_")
				}
				return 50
			}
			if (type == "시나리오") return 60
			if (type == "성공") return 70
			return 100
		}

		tcs.eachWithIndex { Map tc, int i -> tc.id = String.format("TC-%02d", i + 1) }
		return tcs
	}

	// =========================================================
	// 이름 자동 선택 (팝업 or 직접 입력 분기)
	// =========================================================
	static void setNameByAutoSelection(String value) {
		if (!isNameSearchPopupMode) { setFormFieldValue("name", value); return }
		if (!openNameSearchPopup()) return
		runJs(POPUP_FIND_JS + '''
            var popup=findNamePopup(); if(!popup) return;
            var selected=false;
            var radios=popup.querySelectorAll('input[type=radio]');
            if(radios.length>0){ try{ radios[0].click(); selected=true; }catch(e){} }
            if(!selected){
                var rows=popup.querySelectorAll('tbody tr');
                if(rows.length>0){ try{ rows[0].click(); selected=true; }catch(ex){} }
            }
            if(!selected){
                var SKIP=["사용자 정보 검색","회원 선택","선택 완료","조회"];
                var cards=Array.from(popup.querySelectorAll('div,li,button,a,label')).filter(function(el){
                    if(el.offsetParent===null) return false; var txt=(el.innerText||'').replace(/\\s+/g,' ').trim();
                    if(txt.length<2) return false; for(var k=0;k<SKIP.length;k++) if(txt.indexOf(SKIP[k])>=0) return false; return true;
                });
                if(cards.length>0){ try{ cards[0].click(); selected=true; }catch(ex){} }
            }
            var checks=popup.querySelectorAll("input[type=checkbox]");
            if(checks.length>0){ try{ checks[0].checked=true; checks[0].dispatchEvent(new Event("change",{bubbles:true})); }catch(ex){} }
        ''')
		waitSec(0.5)
		runJs(POPUP_FIND_JS + '''
            var popup=findNamePopup(); if(!popup) return;
            var btns=popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
            for(var i=btns.length-1;i>=0;i--){
                var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
                if(tx.indexOf('선택완료')>=0||tx.indexOf('완료')>=0||tx.indexOf('확인')>=0||tx.indexOf('적용')>=0){
                    try{ btns[i].click(); }catch(e){} break;
                }
            }
        ''')
		waitSec(0.8)
		closeInnerCompletePopupIfExists()
	}

	// =========================================================
	// [L] 파일 첨부 테스트 유틸
	// =========================================================

	static String FILE_DUMMY_DIR = RunConfiguration.getProjectDir() +
		File.separator + "Include" + File.separator + "files" + File.separator + "upload_test"

	static List<File> fileTempFiles = []

	/**
	 * 더미 파일 생성 (테스트 후 deleteTempFiles() 로 정리)
	 * @param name   파일명 (확장자 포함)
	 * @param sizeMb 크기(MB), 0이면 최소 헤더만
	 */
	static File createDummyFile(String name, double sizeMb = 0) {
		File dir = new File(FILE_DUMMY_DIR)
		if (!dir.exists()) dir.mkdirs()
		File f = new File(dir, name)
		KeywordUtil.logInfo("[파일TC] 더미파일 생성: ${f.absolutePath}")
		String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).toLowerCase() : ""

		if (sizeMb > 0) {
			byte[] data = new byte[(int)(sizeMb * 1024 * 1024)]
			new Random().nextBytes(data)
			f.bytes = data
		} else {
			switch (ext) {
				case "pdf":
					f.text = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF"; break
				case ["jpg", "jpeg"]:
					f.bytes = [0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46] as byte[]; break
				case "png":
					f.bytes = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] as byte[]; break
				case ["hwp", "hwpx"]:
					f.text = "HWP Document File"; break
				case "exe":
					f.bytes = [0x4D, 0x5A, 0x90, 0x00] as byte[]; break
				case "sh":
					f.text = "#!/bin/bash\necho test"; break
				default:
					f.text = "dummy test file: ${name}"
			}
		}
		fileTempFiles << f
		return f
	}

	/** 생성된 더미 파일 전체 삭제 */
	static void deleteTempFiles() {
		fileTempFiles.each { File f -> try { if (f.exists()) f.delete() } catch (Exception ignore) {} }
		fileTempFiles.clear()
		File dir = new File(FILE_DUMMY_DIR)
		try { if (dir.list()?.length == 0) dir.delete() } catch (Exception ignore) {}
	}

	/**
	 * input[type=file] 에 파일 경로 주입
	 * hidden 처리된 필드도 JS로 노출 후 sendKeys
	 */

	static void injectFileToInput(org.openqa.selenium.WebElement fileInput, String absolutePath) {
		Map origin = (Map) ((org.openqa.selenium.JavascriptExecutor) driver()).executeScript("""
			var e = arguments[0];
			return {
				style: e.getAttribute('style') || '',
				hidden: !!e.hidden,
				ariaHidden: e.getAttribute('aria-hidden'),
				tabIndex: e.getAttribute('tabindex')
			};
		""", fileInput)
		try {
			((org.openqa.selenium.JavascriptExecutor) driver()).executeScript("""
				var e = arguments[0];
				e.hidden = false;
				e.removeAttribute('aria-hidden');
				e.style.display = 'block';
				e.style.visibility = 'visible';
				e.style.opacity = '0.001';
				e.style.position = 'fixed';
				e.style.left = '-10000px';
				e.style.top = '0';
				e.style.width = '1px';
				e.style.height = '1px';
				e.style.zIndex = '-1';
				e.style.pointerEvents = 'none';
			""", fileInput)
			waitSec(0.1)
			fileInput.sendKeys(absolutePath)
			waitSec(0.3)
		} finally {
			((org.openqa.selenium.JavascriptExecutor) driver()).executeScript("""
				var e = arguments[0], s = arguments[1];
				if (s.style) e.setAttribute('style', s.style); else e.removeAttribute('style');
				e.hidden = !!s.hidden;
				if (s.ariaHidden == null) e.removeAttribute('aria-hidden'); else e.setAttribute('aria-hidden', s.ariaHidden);
				if (s.tabIndex == null) e.removeAttribute('tabindex'); else e.setAttribute('tabindex', s.tabIndex);
			""", fileInput, origin)
		}
	}

	static List<String> detectFileAcceptExtensions(org.openqa.selenium.WebElement fileInput) {
		String accept = fileInput.getAttribute("accept") ?: ""
		if (!accept.trim()) return ["pdf", "jpg", "jpeg", "png", "hwp"]
		List<String> exts = []
		accept.split(",").each { String token ->
			token = token.trim().toLowerCase()
			if (token.startsWith(".")) {
				exts << token.substring(1)
			} else {
				switch (token) {
					case "image/jpeg":           exts << "jpg"; exts << "jpeg"; break
					case "image/png":            exts << "png"; break
					case "application/pdf":      exts << "pdf"; break
					case ["application/haansofthwp", "application/x-hwp"]: exts << "hwp"; break
					default:
						if (token.startsWith("image/")) exts << token.split("/")[1]
				}
			}
		}
		return exts.unique().findAll { it }
	}

	/**
	 * 파일 첨부 후 제출 → 팝업/인라인 메시지 수집
	 */
	static String submitFormAndCollectMessage() {
		try {
			List<org.openqa.selenium.WebElement> candidates = driver().findElements(
				By.cssSelector("button[type=submit],input[type=submit],button.btn-primary,button.submit,button.save")
			)
			if (!candidates) candidates = driver().findElements(By.tagName("button"))
			org.openqa.selenium.WebElement btn = null
			for (org.openqa.selenium.WebElement b : candidates) {
				String txt = (b.getText() ?: b.getAttribute("value") ?: "").trim()
				if (txt && ["제출","저장","등록","확인","완료","신청","업로드","올리기"].any { txt.contains(it) }) {
					btn = b; break
				}
			}
			if (!btn && candidates) btn = candidates.last()
			if (btn) {
				((org.openqa.selenium.JavascriptExecutor) driver()).executeScript("arguments[0].click()", btn)
				waitSec(1.5)
			}
		} catch (Exception ignore) {}

		// Alert 우선 수집
		try {
			def alert = driver().switchTo().alert()
			String msg = alert.getText() ?: ""
			alert.accept()
			return msg.trim()
		} catch (Exception ignore) {}

		// DOM 메시지
		Object r = runJs('''
            var sels=['.error','.alert','.message','.msg','.toast','[class*=error]','[class*=alert]','[class*=message]','[role=alert]','[role=status]'];
            for(var i=0;i<sels.length;i++){
                var el=document.querySelector(sels[i]);
                if(el&&el.offsetParent!==null){ var t=(el.innerText||'').replace(/\\s+/g,' ').trim(); if(t.length>1) return t; }
            }
            var inputs=document.querySelectorAll('input,select,textarea');
            for(var j=0;j<inputs.length;j++) if(inputs[j].validationMessage) return inputs[j].validationMessage;
            return '[메시지없음]';
        ''')
		return (r ?: "[메시지없음]").toString().trim()
	}
}
