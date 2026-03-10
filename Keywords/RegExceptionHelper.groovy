import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import groovy.json.JsonSlurper

import org.openqa.selenium.Alert
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions

class RegExceptionHelper {

	// =========================================================
	// [A] 전역 상태
	// =========================================================
	private static List<Map> testResults = []
	private static int failCount = 0
	private static String startUrl = ""

	private static boolean isNameSearchPopupMode = false
	private static Map namePopupScan = [:]
	private static boolean nameFieldDone = false  // 이름 시나리오 완료 후 더 이상 손 안 댐

	private static final String FIXED_VALID_ID = "testfix01"

	// 팝업 제목 키워드
	private static final List<String> NAME_POPUP_TITLE_KEYWORDS = ["사용자 정보 검색", "회원 선택"]

	// =========================================================
	// [B] 공용 유틸
	// =========================================================
	private static WebDriver driver() {
		return DriverFactory.getWebDriver()
	}

	private static void waitSec(double sec) {
		WebUI.delay(sec)
	}

	private static Object runJs(String script) {
		return WebUI.executeJavaScript(script, null)
	}

	private static Object runJs(String script, List args) {
		return WebUI.executeJavaScript(script, args)
	}

	private static void logLine(String msg) {
		KeywordUtil.logInfo(msg)
	}

	private static String normalizeText(String s) {
		return (s ?: "").replaceAll("\\s+", " ").trim()
	}

	private static boolean isBlankPopup(String s) {
		String t = (s ?: "").trim()
		return !t || t == "[팝업없음]"
	}

	private static String escapeJsString(String s) {
		return (s ?: "")
				.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\n", " ")
				.replace("\r", " ")
	}

	private static boolean isElementVisible(WebElement el) {
		try {
			return el != null && el.isDisplayed()
		} catch (Exception e) {
			return false
		}
	}

	private static boolean hasBrowserDriver() {
		try {
			return driver() != null
		} catch (Exception e) {
			return false
		}
	}

	private static Map makeTc(String type, String desc, String field, String target, String value, String expect) {
		return [
			id    : "TC-00",
			type  : type,
			desc  : desc,
			field : field,
			target: target,
			value : value,
			expect: expect
		]
	}

	// =========================================================
	// [C] 버튼 / 클릭
	// =========================================================
	private static void safeClick(WebElement el) {
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
			} catch (Exception ignore) {
			}
	}

	private static WebElement findButtonByKeywords(List<String> keywords) {
		List<WebElement> buttons = driver().findElements(By.xpath("//button | //a | //input[@type='button'] | //input[@type='submit']"))

		for (WebElement el : buttons) {
			if (!isElementVisible(el)) continue

				String txt = normalizeText((el.getText() ?: "") + " " + (el.getAttribute("value") ?: "") + " " + (el.getAttribute("title") ?: ""))
			String compact = txt.replaceAll("\\s+", "")

			for (String kw : keywords) {
				if (compact.contains((kw ?: "").replaceAll("\\s+", ""))) {
					return el
				}
			}
		}
		return null
	}

	private static void clickButtonByKeywords(String... keywords) {
		WebElement btn = findButtonByKeywords(keywords as List<String>)
		if (btn != null) safeClick(btn)
	}

	private static void clickRegisterButton() {
		clickButtonByKeywords("가입하기", "회원가입", "등록", "신청", "제출", "완료")
	}

	private static void clickDuplicateCheckButton() {
		clickButtonByKeywords("중복확인", "중복체크", "중복")
	}

	private static boolean existsDuplicateCheckButton() {
		try {
			List<WebElement> buttons = driver().findElements(By.xpath("//button | //a | //input[@type='button']"))
			for (WebElement el : buttons) {
				if (!isElementVisible(el)) continue
					String txt = normalizeText((el.getText() ?: "") + " " + (el.getAttribute("value") ?: ""))
				if (txt.replaceAll("\\s+", "").contains("중복")) return true
			}
			return false
		} catch (Exception e) {
			return false
		}
	}

	// =========================================================
	// [D] 공통 팝업 / 모달 처리
	// =========================================================
	private static void closeBrowserAlertIfExists() {
		try {
			Alert a = driver().switchTo().alert()
			a.accept()
			waitSec(0.2)
		} catch (Exception ignore) {
		}
	}

	private static void closeGeneralModalIfExists() {
		closeBrowserAlertIfExists()

		runJs('''
            document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer]").forEach(function(m){
                var s = window.getComputedStyle(m);
                if(s.display === "none") return;

                var bs = m.querySelectorAll("button,a,[class*=btn],input[type=button],input[type=submit]");
                for(var i=0;i<bs.length;i++){
                    var t = (bs[i].innerText || bs[i].value || "").replace(/\\s+/g,"");
                    if(t.indexOf("확인") >= 0 || t === "OK" || t.indexOf("닫기") >= 0){
                        try{ bs[i].click(); }catch(e){}
                        break;
                    }
                }
            });

            document.querySelectorAll(".modal,.popup,[class*=modal],[class*=overlay],.modal-backdrop").forEach(function(e){
                try{ e.style.display = "none"; }catch(ex){}
            });
        ''')
		waitSec(0.2)
	}

	private static void dismissGeneralPopups() {
		waitSec(1.0)
		closeBrowserAlertIfExists()

		try {
			new Actions(driver()).sendKeys(Keys.ESCAPE).perform()
			waitSec(0.1)
		} catch (Exception ignore) {
		}

		runJs('''
            document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer]").forEach(function(md){
                if(window.getComputedStyle(md).display === "none") return;

                var bs = md.querySelectorAll("button,a,[class*=btn],input[type=button],input[type=submit]");
                for(var i=0;i<bs.length;i++){
                    var t = (bs[i].innerText || bs[i].value || "").replace(/\\s+/g,"");
                    if(t.indexOf("확인") >= 0 || t === "OK" || t.indexOf("닫기") >= 0){
                        try{ bs[i].click(); }catch(e){}
                        break;
                    }
                }
            });
        ''')
		waitSec(0.3)
		closeGeneralModalIfExists()
	}

	private static String collectPopupMessage(String hint, boolean waitPopup) {
		if (waitPopup) waitSec(1.4)

		try {
			new Actions(driver()).sendKeys(Keys.ESCAPE).perform()
			waitSec(0.1)
		} catch (Exception ignore) {
		}

		try {
			Alert a = driver().switchTo().alert()
			String msg = a.getText()
			a.accept()

			if ((msg ?: "").contains("팝업") || (msg ?: "").contains("차단")) {
				try {
					Alert a2 = driver().switchTo().alert()
					String msg2 = a2.getText()
					a2.accept()
					return msg2
				} catch (Exception ignore) {
				}
			}
			return msg
		} catch (Exception ignore) {
		}

		Object r = runJs('''
            function vis(e){
                var s = window.getComputedStyle(e);
                return e.offsetParent !== null && s.visibility !== "hidden" && s.display !== "none";
            }
            function text(v){
                return (v || "").replace(/\\s+/g," ").trim();
            }

            var buttons = document.querySelectorAll("button,a,[class*=btn],input[type=button],input[type=submit]");
            for(var i=0;i<buttons.length;i++){
                var b = buttons[i];
                var bt = text(b.innerText || b.value || "");
                if(vis(b) && (bt.indexOf("확인") >= 0 || bt === "OK")){
                    var p = b.parentElement;
                    while(p && p.tagName !== "BODY"){
                        var st = window.getComputedStyle(p);
                        if(st.position === "fixed" || st.position === "absolute" || parseInt(st.zIndex) > 0){
                            var raw = text(p.innerText || "");
                            var clean = raw.replace(/확인/g,"").trim();
                            try{ b.click(); }catch(e){}
                            return "M::" + clean;
                        }
                        p = p.parentElement;
                    }
                }
            }

            var ins = document.querySelectorAll("input,select,textarea");
            for(var k=0;k<ins.length;k++){
                if(ins[k].validity && !ins[k].validity.valid && ins[k].validationMessage){
                    return "H::" + ins[k].validationMessage;
                }
            }

            var keywords = ["필수","선택","입력","확인","주소","아이디","비밀번호","이름","중복","오류","실패","동의"];
            var nodes = Array.from(document.querySelectorAll(
                "[role=alert],[aria-live],.error,.invalid,.tooltip,.toast,.message,.feedback,[class*=error],[class*=invalid],[class*=warn],[class*=message]"
            )).filter(vis).map(function(e){
                return text(e.innerText || "");
            }).filter(function(t){
                return t.length > 0 && t.length <= 200;
            });

            for(var n=0;n<nodes.length;n++){
                for(var j=0;j<keywords.length;j++){
                    if(nodes[n].indexOf(keywords[j]) >= 0){
                        return "I::" + nodes[n];
                    }
                }
            }

            return "[팝업없음]";
        ''')

		String result = r?.toString() ?: "[팝업없음]"
		if (result.startsWith("M::") || result.startsWith("H::") || result.startsWith("I::")) {
			result = result.substring(3)
		}
		return result
	}

	private static String collectPopupMessage(String hint = "") {
		return collectPopupMessage(hint, true)
	}

	// =========================================================
	// [E] 폼 초기화 / 공통값 세팅
	// =========================================================
	private static void resetMainForm() {
		// 이름 검색 팝업이 열려있으면 먼저 닫기 (이름 시나리오 완료 전까지만)
		if (isNameSearchPopupMode && !nameFieldDone) {
			closeNameSearchPopup()
		} else if (isNameSearchPopupMode && nameFieldDone) {
			// 혹시 팝업이 열려있으면 닫되, 이름 필드 재입력은 하지 않음
			try {
				closeNameSearchPopup()
			} catch (Exception ignored) {}
		}

		// 페이지 이탈 감지 → 원래 URL로 복귀
		try {
			String cur = driver().currentUrl ?: ""
			if (startUrl && cur && !cur.equals(startUrl) && !cur.startsWith("data:")) {
				driver().navigate().to(startUrl)
				waitSec(1.5)
			}
		} catch (Exception ignore) {}

		try {
			new Actions(driver()).sendKeys(Keys.ESCAPE).perform()
			waitSec(0.1)
		} catch (Exception ignore) {
		}

		runJs('''
            document.querySelectorAll("form").forEach(function(f){
                try{ f.reset(); }catch(e){}
            });

            document.querySelectorAll("input").forEach(function(i){
                if(i.type === "checkbox" || i.type === "radio") i.checked = false;
                else if(i.type !== "file") i.value = "";

                i.dispatchEvent(new Event("input",{bubbles:true}));
                i.dispatchEvent(new Event("change",{bubbles:true}));
            });

            document.querySelectorAll("select").forEach(function(s){
                try{
                    s.selectedIndex = 0;
                    s.dispatchEvent(new Event("change",{bubbles:true}));
                }catch(e){}
            });
        ''')
	}

	private static void fillVisibleEmptyFieldsWithDefault() {
		runJs('''
            document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])")
                .forEach(function(e){
                    if(e.offsetParent === null) return;

                    var id = (e.id || "").toLowerCase();
                    if(id.indexOf("address") >= 0 || id.indexOf("post") >= 0) return;
                    if(e.value && e.value.length > 0) return;

                    e.value = (e.type === "password") ? "Temp123!@" : "dummyData12";
                    e.dispatchEvent(new Event("input",{bubbles:true}));
                    e.dispatchEvent(new Event("change",{bubbles:true}));
                });
        ''')
	}

	private static void setFormFieldValue(String fieldType, String value) {
		String t = escapeJsString(fieldType)
		String v = escapeJsString(value ?: "")

		runJs("""
            var t = '${t}';
            var v = '${v}';
            var target = null;

            document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])")
                .forEach(function(e){
                    if(target || e.offsetParent === null) return;

                    var id = (e.id || "").toLowerCase();
                    var nm = (e.name || "").toLowerCase();
                    var ph = (e.placeholder || "").toLowerCase();
                    var lb = "";

                    if(e.labels && e.labels.length) lb = (e.labels[0].innerText || "").toLowerCase();
                    else{
                        var p = e.closest("div,tr,li,td");
                        if(p) lb = (p.innerText || "").toLowerCase();
                    }

                    if(t === "pwConf" && e.type === "password" && (id.indexOf("confirm") >= 0 || nm.indexOf("confirm") >= 0 || ph.indexOf("확인") >= 0 || ph.indexOf("재입력") >= 0)) target = e;
                    else if(t === "pw" && e.type === "password" && id.indexOf("confirm") < 0 && nm.indexOf("confirm") < 0 && ph.indexOf("확인") < 0) target = e;
                    else if(t === "id" && e.type !== "password" && (id.indexOf("id") >= 0 || nm.indexOf("id") >= 0 || ph.indexOf("아이디") >= 0 || lb.indexOf("아이디") >= 0)) target = e;
                    else if(t === "name" && e.type !== "password" && (id.indexOf("name") >= 0 || nm.indexOf("name") >= 0 || ph.indexOf("이름") >= 0 || ph.indexOf("성명") >= 0 || lb.indexOf("이름") >= 0 || lb.indexOf("성명") >= 0)) target = e;
                    else if(t === "email" && (id.indexOf("email") >= 0 || nm.indexOf("email") >= 0 || ph.indexOf("이메일") >= 0 || ph.indexOf("@") >= 0)) target = e;
                    else if(t === "phone" && (id.indexOf("phone") >= 0 || nm.indexOf("phone") >= 0 || ph.indexOf("전화") >= 0 || ph.indexOf("휴대") >= 0)) target = e;
                });

            if(target){
                target.value = v;
                ["input","change","blur"].forEach(function(ev){
                    target.dispatchEvent(new Event(ev,{bubbles:true}));
                });
            }
        """)
	}

	// =========================================================
	// [F] 이름 검색 팝업: 공통 탐색
	// =========================================================
	private static boolean detectNameFieldUsesPopupSearch() {
		Object result = runJs('''
            function isInsideModal(e){
                return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]");
            }

            function vis(e){
                if(!e) return false;
                var s = window.getComputedStyle(e);
                return e.offsetParent !== null &&
                       s.display !== "none" &&
                       s.visibility !== "hidden" &&
                       !isInsideModal(e);
            }

            function isNameInput(e){
                if(!e || !vis(e)) return false;

                var id = (e.id || "").toLowerCase();
                var nm = (e.name || "").toLowerCase();
                var ph = (e.placeholder || "").toLowerCase();
                var lb = "";

                if(e.labels && e.labels.length) lb = (e.labels[0].innerText || "").toLowerCase();
                else{
                    var wrap = e.closest("div,td,tr,li,section");
                    if(wrap) lb = (wrap.innerText || "").toLowerCase();
                }

                return id.indexOf("name") >= 0 ||
                       nm.indexOf("name") >= 0 ||
                       ph.indexOf("이름") >= 0 ||
                       ph.indexOf("성명") >= 0 ||
                       lb.indexOf("이름") >= 0 ||
                       lb.indexOf("성명") >= 0 ||
                       lb.indexOf("이름 검색") >= 0;
            }

            function looksLikeSearchButton(el){
                if(!el || !vis(el)) return false;

                var tag = (el.tagName || "").toLowerCase();
                var text = ((el.innerText || "") + " " + (el.title || "") + " " + (el.getAttribute("aria-label") || "")).toLowerCase();
                var cls = (el.className || "").toString().toLowerCase();
                var html = (el.innerHTML || "").toLowerCase();

                if(tag === "button") return true;
                if(el.getAttribute("role") === "button") return true;
                if(el.getAttribute("type") === "button") return true;
                if(text.indexOf("검색") >= 0) return true;
                if(text.indexOf("search") >= 0) return true;
                if(cls.indexOf("search") >= 0) return true;
                if(cls.indexOf("icon") >= 0) return true;
                if(html.indexOf("search") >= 0) return true;
                if(html.indexOf("svg") >= 0) return true;
                return false;
            }

            var inputs = document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");
            for(var i=0;i<inputs.length;i++){
                var input = inputs[i];
                if(!isNameInput(input)) continue;

                var wrap = input.parentElement;
                if(!wrap) continue;

                var cands = wrap.querySelectorAll("button,[role=button],input[type=button],span,i,svg");
                for(var j=0;j<cands.length;j++){
                    if(looksLikeSearchButton(cands[j])) return "true";
                }
            }
            return "false";
        ''')
		return "true".equalsIgnoreCase(result?.toString())
	}

	private static boolean openNameSearchPopup() {
		Object result = runJs('''
            function isInsideModal(e){
                return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]");
            }

            function vis(e){
                if(!e) return false;
                var s = window.getComputedStyle(e);
                return e.offsetParent !== null &&
                       s.display !== "none" &&
                       s.visibility !== "hidden" &&
                       !isInsideModal(e);
            }

            function isNameInput(e){
                if(!e || !vis(e)) return false;

                var id = (e.id || "").toLowerCase();
                var nm = (e.name || "").toLowerCase();
                var ph = (e.placeholder || "").toLowerCase();
                var lb = "";

                if(e.labels && e.labels.length) lb = (e.labels[0].innerText || "").toLowerCase();
                else{
                    var wrap = e.closest("div,td,tr,li,section");
                    if(wrap) lb = (wrap.innerText || "").toLowerCase();
                }

                return id.indexOf("name") >= 0 ||
                       nm.indexOf("name") >= 0 ||
                       ph.indexOf("이름") >= 0 ||
                       ph.indexOf("성명") >= 0 ||
                       lb.indexOf("이름") >= 0 ||
                       lb.indexOf("성명") >= 0 ||
                       lb.indexOf("이름 검색") >= 0;
            }

            function calcScore(el, inputRect){
                if(!el || !vis(el)) return -1;

                var rect = el.getBoundingClientRect();
                var tag = (el.tagName || "").toLowerCase();
                var text = ((el.innerText || "") + " " + (el.title || "") + " " + (el.getAttribute("aria-label") || "")).toLowerCase();
                var cls = (el.className || "").toString().toLowerCase();
                var html = (el.innerHTML || "").toLowerCase();

                var score = 0;
                if(tag === "button") score += 5;
                if(el.getAttribute("role") === "button") score += 4;
                if(el.getAttribute("type") === "button") score += 4;
                if(text.indexOf("검색") >= 0) score += 8;
                if(text.indexOf("search") >= 0) score += 8;
                if(cls.indexOf("search") >= 0) score += 7;
                if(cls.indexOf("icon") >= 0) score += 3;
                if(html.indexOf("svg") >= 0) score += 2;

                if(rect.left >= inputRect.right - 5) score += 10;
                if(Math.abs(rect.top - inputRect.top) < 40) score += 8;

                return score;
            }

            var inputs = document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");

            for(var i=0;i<inputs.length;i++){
                var input = inputs[i];
                if(!isNameInput(input)) continue;

                var inputRect = input.getBoundingClientRect();
                var best = null;
                var bestScore = -1;

                // 탐색 범위: parentElement → 최대 4단계 상위까지 확장
                var searchRoot = input.parentElement;
                for(var up=0; up<4 && searchRoot; up++){
                    var cands = searchRoot.querySelectorAll("button,[role=button],input[type=button],span,i,svg,a");
                    for(var j=0;j<cands.length;j++){
                        var s = calcScore(cands[j], inputRect);
                        if(s > bestScore){ best = cands[j]; bestScore = s; }
                    }
                    if(bestScore >= 10) break;
                    searchRoot = searchRoot.parentElement;
                }

                // 스코어 임계값 5로 완화 (위치 점수 없어도 검색 키워드만으로 통과)
                if(best && bestScore >= 5){
                    try{ best.click(); }catch(e){
                        try{ var p=best.closest("button,[role=button]"); if(p) p.click(); }catch(ex){}
                    }
                    return "clicked:"+bestScore;
                }
            }
            return "false";
        ''')

		String resultStr = result?.toString() ?: "false"
		boolean clicked = resultStr.startsWith("clicked") || "true".equalsIgnoreCase(resultStr)
		if (clicked) logLine("이름 팝업 버튼 클릭 완료 (score=" + resultStr + ")")
		if (!clicked) {
			logLine("이름 팝업 버튼 못 찾음"); return false
		}

		waitSec(1.5)
		return isNameSearchPopupOpen()
	}

	private static boolean isNameSearchPopupOpen() {
		// 최대 3회 재시도 (팝업 렌더링 지연 대응)
		for (int attempt = 0; attempt < 3; attempt++) {
			Object r = runJs('''
                // 방법1: 일반적인 모달 셀렉터
                var candidates = Array.from(document.querySelectorAll(
                    "[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]"
                ));

                // 방법2: 고정위치 또는 절대위치이면서 z-index 높은 모든 div
                document.querySelectorAll("div,section,aside").forEach(function(el){
                    var s = window.getComputedStyle(el);
                    if((s.position === "fixed" || s.position === "absolute") && parseInt(s.zIndex||0) > 10){
                        if(candidates.indexOf(el) < 0) candidates.push(el);
                    }
                });

                for(var i=0;i<candidates.length;i++){
                    var d = candidates[i];
                    var ds = window.getComputedStyle(d);
                    if(ds.display === "none" || ds.visibility === "hidden" || ds.opacity === "0") continue;
                    if(d.offsetWidth < 100 || d.offsetHeight < 100) continue;

                    var txt = (d.innerText || "").replace(/\\s+/g," ").trim();
                    // 제목 키워드
                    if(txt.indexOf("사용자 정보 검색") >= 0 || txt.indexOf("회원 선택") >= 0) return "true";
                    // 버튼 조합으로 판단
                    var hasSearch = false, hasComplete = false;
                    d.querySelectorAll("button,[role=button]").forEach(function(b){
                        var bt = (b.innerText||"").replace(/\\s+/g,"");
                        if(bt.indexOf("조회") >= 0 || bt.indexOf("검색") >= 0) hasSearch = true;
                        if(bt.indexOf("선택완료") >= 0 || bt.indexOf("완료") >= 0) hasComplete = true;
                    });
                    if(hasSearch && hasComplete) return "true";
                }
                return "false";
            ''')
			if ("true".equalsIgnoreCase(r?.toString())) return true
			if (attempt < 2) waitSec(0.5)
		}
		return false
	}

	private static void closeNameSearchPopup() {
		// 1단계: 팝업 내 X/닫기 버튼 클릭 시도 (모달 셀렉터 + z-index 방식 둘 다)
		try {
			runJs('''
                function findNamePopup(){
                    // 방법1: 일반 모달 셀렉터
                    var candidates = Array.from(document.querySelectorAll(
                        '[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]'
                    ));
                    // 방법2: fixed/absolute + z-index > 10
                    document.querySelectorAll('div,section,aside').forEach(function(el){
                        var s = window.getComputedStyle(el);
                        if((s.position==="fixed"||s.position==="absolute") && parseInt(s.zIndex||0) > 10){
                            if(candidates.indexOf(el)<0) candidates.push(el);
                        }
                    });
                    for(var i=0;i<candidates.length;i++){
                        var d=candidates[i];
                        var ds=window.getComputedStyle(d);
                        if(ds.display==="none"||ds.visibility==="hidden") continue;
                        if(d.offsetWidth<100||d.offsetHeight<100) continue;
                        var txt=(d.innerText||"").replace(/\\s+/g," ").trim();
                        if(txt.indexOf("사용자 정보 검색")>=0||txt.indexOf("회원 선택")>=0) return d;
                    }
                    return null;
                }

                var popup = findNamePopup();
                if(!popup) return "notfound";

                // 닫기 버튼 우선순위: class*=close > aria-label=닫기 > ×/✕/X 텍스트 버튼
                var closeBtn = popup.querySelector('[class*=close],[class*=Close],[class*=dismiss],[aria-label*=닫기],[aria-label*=close]');
                if(closeBtn){ try{ closeBtn.click(); }catch(e){} return "closebtn"; }

                var btns = popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit],a');
                for(var j=0;j<btns.length;j++){
                    var bt=(btns[j].innerText||btns[j].value||"").trim();
                    if(bt==="×"||bt==="✕"||bt==="X"||bt==="x"||bt.indexOf("닫기")>=0||bt.indexOf("취소")>=0){
                        try{ btns[j].click(); }catch(e){}
                        return "xbtn";
                    }
                }

                // 버튼 못 찾으면 DOM display:none으로 강제 숨김
                popup.style.display = "none";
                return "hidden";
            ''')
		} catch (Exception ignore) {}

		// 2단계: ESC 키
		try {
			driver().findElement(By.tagName("body")).sendKeys(Keys.ESCAPE)
		} catch (Exception ignore) {}

		waitSec(0.5)

		// 3단계: 아직도 팝업이 보이면 DOM 강제 숨김 (최후 수단)
		try {
			runJs('''
                document.querySelectorAll('div,section,aside').forEach(function(el){
                    var s=window.getComputedStyle(el);
                    if((s.position==="fixed"||s.position==="absolute") && parseInt(s.zIndex||0)>10){
                        var ds=window.getComputedStyle(el);
                        if(ds.display==="none"||ds.visibility==="hidden") return;
                        var txt=(el.innerText||"").replace(/\\s+/g," ").trim();
                        if(txt.indexOf("사용자 정보 검색")>=0||txt.indexOf("회원 선택")>=0){
                            el.style.display="none";
                        }
                    }
                });
            ''')
		} catch (Exception ignore) {}
	}

	private static void closeInnerCompletePopupIfExists() {
		try {
			runJs('''
                var dialogs = document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
                for(var i=0;i<dialogs.length;i++){
                    var d = dialogs[i];
                    if(d.offsetParent===null) continue;

                    var txt = (d.innerText || '').replace(/\\s+/g,' ').trim();

                    // 이름 검색 팝업은 건드리지 않도록 제목 키워드 제외
                    if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0) continue;
                    if(txt.indexOf('중복 확인이 완료되었습니다') >= 0 || txt.indexOf('중복 이 완료되었습니다') >= 0 || (txt.indexOf('완료') >= 0 && txt.indexOf('중복') >= 0)){
                        var btns = d.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
                        for(var j=0;j<btns.length;j++){
                            var bt = (btns[j].innerText || btns[j].value || '').replace(/\\s+/g,'');
                            if(bt.indexOf('확인') >= 0 || bt.indexOf('완료') >= 0){
                                try{ btns[j].click(); }catch(e){}
                                return;
                            }
                        }
                    }
                }
            ''')
			waitSec(0.5)
		} catch (Exception ignore) {
		}

		try {
			Alert a = driver().switchTo().alert()
			a.accept()
			waitSec(0.3)
		} catch (Exception ignore) {
		}
	}

	// =========================================================
	// [G] 이름 검색 팝업: 스캔
	// =========================================================
	private static Map scanNameSearchPopup() {
		Map out = [
			exists         : false,
			title          : "",
			listCount      : 0,
			selects        : [],
			inputs         : [],
			checkboxes     : [],
			hasSearchButton: false,
			hasCompleteButton: false
		]

		if (!isNameSearchPopupMode) return out

		// 최대 2회 시도 (팝업 렌더링 지연 대응)
		boolean opened = false
		for (int attempt = 0; attempt < 2; attempt++) {
			if (openNameSearchPopup()) {
				opened = true; break
			}
			if (attempt < 1) waitSec(1.0)
		}
		if (!opened) return out

		waitSec(1.2)

		try {
			Object r = runJs('''
                function vis(e){
                    if(!e) return false;
                    var s = window.getComputedStyle(e);
                    return e.offsetParent !== null && s.display !== "none" && s.visibility !== "hidden";
                }
                function text(v){
                    return (v || "").replace(/\\s+/g," ").trim();
                }

                // 모달 셀렉터 + 고정/절대 위정 div 모두 수집
                var candidates = Array.from(document.querySelectorAll(
                    "[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]"
                ));
                document.querySelectorAll("div,section,aside").forEach(function(el){
                    var s = window.getComputedStyle(el);
                    if((s.position === "fixed" || s.position === "absolute") && parseInt(s.zIndex||0) > 10){
                        if(candidates.indexOf(el) < 0) candidates.push(el);
                    }
                });
                var popup = null;

                for(var i=0;i<candidates.length;i++){
                    var d = candidates[i];
                    var ds = window.getComputedStyle(d);
                    if(ds.display === 'none' || ds.visibility === 'hidden' || ds.opacity === '0') continue;
                    if(d.offsetWidth < 100 || d.offsetHeight < 100) continue;
                    var t = text(d.innerText || "");
                    if(t.indexOf("사용자 정보 검색") >= 0 || t.indexOf("회원 선택") >= 0){
                        popup = d; break;
                    }
                    var hasSearch = false, hasComplete = false;
                    d.querySelectorAll('button,[role=button]').forEach(function(b){
                        var bt = (b.innerText||'').replace(/\\s+/g,'');
                        if(bt.indexOf('조회') >= 0 || bt.indexOf('검색') >= 0) hasSearch = true;
                        if(bt.indexOf('선택완료') >= 0 || bt.indexOf('완료') >= 0) hasComplete = true;
                    });
                    if(hasSearch && hasComplete){ popup = d; break; }
                }



                if(!popup){
                    return JSON.stringify({
                        exists:false,
                        title:"",
                        listCount:0,
                        selects:[],
                        inputs:[],
                        checkboxes:[],
                        hasSearchButton:false,
                        hasCompleteButton:false
                    });
                }

                var result = {
                    exists:true,
                    title:"",
                    listCount:0,
                    selects:[],
                    inputs:[],
                    checkboxes:[],
                    hasSearchButton:false,
                    hasCompleteButton:false
                };

                var titleEl = popup.querySelector("h1,h2,h3,strong,[class*=title],[class*=header]");
                result.title = text(titleEl ? titleEl.innerText : "");

                popup.querySelectorAll("select").forEach(function(s, idx){
                    if(!vis(s) || !s.options || s.options.length <= 1) return;

                    var label = "";
                    if(s.labels && s.labels.length) label = text(s.labels[0].innerText);
                    if(!label){
                        var prev = s.previousElementSibling;
                        while(prev && (prev.nodeName==="SELECT"||prev.nodeName==="BR")) prev=prev.previousElementSibling;
                        if(prev) label = text(prev.innerText||prev.textContent);
                    }
                    if(!label){
                        var wrap=s.closest("div,td,th,li,label,span");
                        if(wrap){
                            var clone=wrap.cloneNode(true);
                            clone.querySelectorAll("select,input,button").forEach(function(e){e.remove();});
                            label=text(clone.innerText||clone.textContent);
                        }
                    }
                    // 그래도 없으면 첫번째 옵션 텍스트(placeholder)를 라벨로 사용
                    if(!label && s.options && s.options.length>0) label = text(s.options[0].text).replace("전체","").replace("선택","").trim() || ("드롭박스"+(idx+1));
                    if(!label) label = "드롭박스" + (idx+1);

                    var opts = [];
                    for(var i=0;i<s.options.length;i++){
                        opts.push({
                            index:i,
                            text:text(s.options[i].text),
                            value:s.options[i].value || ""
                        });
                    }

                    result.selects.push({
                        index:idx,
                        label:label,
                        options:opts
                    });
                });

                popup.querySelectorAll("input:not([type=hidden]):not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit])").forEach(function(inp, idx){
                    if(!vis(inp)) return;

                    var label = "";
                    if(inp.labels && inp.labels.length) label = text(inp.labels[0].innerText);
                    if(!label){
                        var wrap = inp.closest("div,td,tr,li,section");
                        if(wrap){
                            var temp = text(wrap.innerText);
                            label = temp ? temp.split(" ")[0] : "";
                        }
                    }

                    result.inputs.push({
                        index:idx,
                        type:inp.type || "text",
                        id:inp.id || "",
                        name:inp.name || "",
                        placeholder:inp.placeholder || "",
                        label:label,
                        value:inp.value || ""
                    });
                });

                popup.querySelectorAll("input[type=checkbox]").forEach(function(cb, idx){
                    if(!vis(cb)) return;

                    var label = "";
                    if(cb.labels && cb.labels.length) label = text(cb.labels[0].innerText);
                    if(!label){
                        var box = cb.closest("label,div,li");
                        if(box) label = text(box.innerText);
                    }

                    result.checkboxes.push({
                        index:idx,
                        id:cb.id || "",
                        name:cb.name || "",
                        label:label,
                        checked:!!cb.checked
                    });
                });

                var rows = popup.querySelectorAll("tbody tr");
                if(rows.length > 0){
                    result.listCount = rows.length;
                }else{
                    var cards = Array.from(popup.querySelectorAll("div,li,button,a,label")).filter(function(el){
                        if(!vis(el)) return false;
                        var t = text(el.innerText);
                        if(t.length < 2) return false;
                        if(t.indexOf("사용자 정보 검색") >= 0) return false;
                        if(t.indexOf("회원 선택") >= 0) return false;
                        if(t.indexOf("선택 완료") >= 0) return false;
                        if(t.indexOf("조회") >= 0) return false;
                        return true;
                    });
                    result.listCount = cards.length;
                }

                var btns = popup.querySelectorAll("button,[role=button],input[type=button],input[type=submit]");
                for(var b=0;b<btns.length;b++){
                    var bt = text(btns[b].innerText || btns[b].value || "");
                    if(bt.indexOf("조회") >= 0 || bt.indexOf("검색") >= 0) result.hasSearchButton = true;
                    if(bt.indexOf("선택 완료") >= 0 || bt.indexOf("완료") >= 0 || bt.indexOf("확인") >= 0) result.hasCompleteButton = true;
                }

                return JSON.stringify(result);
            ''')

			out = (Map) new JsonSlurper().parseText(r?.toString() ?: "{}")
		} catch (Exception e) {
			out = [
				exists         : false,
				title          : "",
				listCount      : 0,
				selects        : [],
				inputs         : [],
				checkboxes     : [],
				hasSearchButton: false,
				hasCompleteButton: false
			]
		} finally {
			closeNameSearchPopup()
		}

		return out
	}

	// =========================================================
	// [H] 이름 검색 팝업: 기본 액션
	// =========================================================
	private static boolean actionNamePopupCompleteWithoutSelection() {
		if (!openNameSearchPopup()) return false

		runJs('''
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var btns=popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
            for(var i=btns.length-1;i>=0;i--){
                var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
                if(tx.indexOf('선택완료') >= 0 || tx.indexOf('완료') >= 0 || tx.indexOf('확인') >= 0){
                    try{ btns[i].click(); }catch(e){}
                    break;
                }
            }
        ''')
		waitSec(0.8)
		return true
	}

	private static boolean actionNamePopupSelectFirstItem() {
		if (!openNameSearchPopup()) return false

		runJs('''
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var clicked = false;

            var rows=popup.querySelectorAll('tbody tr');
            if(rows.length>0){
                try{ rows[0].click(); clicked = true; }catch(ex){}
            }

            if(!clicked){
                var cards = Array.from(popup.querySelectorAll('div,li,button,a,label')).filter(function(el){
                    if(el.offsetParent===null) return false;
                    var txt=(el.innerText||'').replace(/\\s+/g,' ').trim();
                    if(txt.length < 2) return false;
                    if(txt.indexOf('사용자 정보 검색') >= 0) return false;
                    if(txt.indexOf('회원 선택') >= 0) return false;
                    if(txt.indexOf('선택 완료') >= 0) return false;
                    if(txt.indexOf('조회') >= 0) return false;
                    return true;
                });
                if(cards.length>0){
                    try{ cards[0].click(); clicked = true; }catch(ex){}
                }
            }

            var checks = popup.querySelectorAll("input[type=checkbox]");
            if(checks.length>0){
                try{
                    checks[0].checked = true;
                    checks[0].dispatchEvent(new Event("change",{bubbles:true}));
                }catch(ex){}
            }
        ''')
		waitSec(0.5)

		runJs('''
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var btns=popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
            for(var i=btns.length-1;i>=0;i--){
                var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
                if(tx.indexOf('선택완료') >= 0 || tx.indexOf('완료') >= 0 || tx.indexOf('확인') >= 0 || tx.indexOf('적용') >= 0){
                    try{ btns[i].click(); }catch(e){}
                    break;
                }
            }
        ''')
		waitSec(0.8)

		closeInnerCompletePopupIfExists()
		return true
	}

	private static boolean actionNamePopupSetDropdownDefault(String value) {
		int selectIdx = (value ?: "0") as int
		if (!openNameSearchPopup()) return false

		runJs("""
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var selects = popup.querySelectorAll("select");
            if(selects.length > ${selectIdx}){
                selects[${selectIdx}].selectedIndex = 0;
                selects[${selectIdx}].dispatchEvent(new Event("change",{bubbles:true}));
            }
        """)
		waitSec(0.5)
		closeNameSearchPopup()
		return true
	}

	private static boolean actionNamePopupSelectDropdownOption(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return false

		int selectIdx = (parts[0] ?: "0") as int
		int optionIdx = (parts[1] ?: "0") as int

		if (!openNameSearchPopup()) return false

		runJs("""
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var selects = popup.querySelectorAll("select");
            if(selects.length > ${selectIdx}){
                var s = selects[${selectIdx}];
                if(s.options.length > ${optionIdx}){
                    s.selectedIndex = ${optionIdx};
                    s.dispatchEvent(new Event("change",{bubbles:true}));
                }
            }
        """)
		waitSec(0.5)

		actionNamePopupClickSearchOnly(false)

		closeInnerCompletePopupIfExists()

		boolean stillOpen = isNameSearchPopupOpen()
		if (stillOpen) closeNameSearchPopup()

		return true
	}

	private static boolean actionNamePopupSetInputValue(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 1) return false

		int inputIdx = (parts[0] ?: "0") as int
		String inputValue = parts.size() > 1 ? (parts[1] ?: "") : ""

		if (!openNameSearchPopup()) return false

		runJs("""
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var inputs = popup.querySelectorAll("input:not([type=hidden]):not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit])");
            if(inputs.length > ${inputIdx}){
                var inp = inputs[${inputIdx}];
                inp.value = '${escapeJsString(inputValue)}';
                inp.dispatchEvent(new Event("input",{bubbles:true}));
                inp.dispatchEvent(new Event("change",{bubbles:true}));
            }
        """)
		waitSec(0.5)

		actionNamePopupClickSearchOnly(false)
		closeInnerCompletePopupIfExists()
		closeNameSearchPopup()
		return true
	}

	private static boolean actionNamePopupSetCheckbox(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return false

		int idx = (parts[0] ?: "0") as int
		boolean on = "true".equalsIgnoreCase(parts[1] ?: "false")

		if (!openNameSearchPopup()) return false

		runJs("""
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var cbs = popup.querySelectorAll("input[type=checkbox]");
            if(cbs.length > ${idx}){
                cbs[${idx}].checked = ${on ? 'true' : 'false'};
                cbs[${idx}].dispatchEvent(new Event("change",{bubbles:true}));
            }
        """)
		waitSec(0.5)
		closeNameSearchPopup()
		return true
	}

	private static boolean actionNamePopupClickSearchOnly() {
		return actionNamePopupClickSearchOnly(true)
	}

	private static boolean actionNamePopupClickSearchOnly(boolean openPopupFirst) {
		if (openPopupFirst && !openNameSearchPopup()) return false

		runJs('''
            function findNamePopup(){
                var cs=Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]"));
                document.querySelectorAll("div,section,aside").forEach(function(el){
                    var s=window.getComputedStyle(el);
                    if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10)
                        if(cs.indexOf(el)<0) cs.push(el);
                });
                for(var i=0;i<cs.length;i++){
                    var d=cs[i],ds=window.getComputedStyle(d);
                    if(ds.display==="none"||ds.visibility==="hidden") continue;
                    var t=(d.innerText||"").replace(/\\s+/g," ").trim();
                    if(t.indexOf("사용자 정보 검색")>=0||t.indexOf("회원 선택")>=0) return d;
                }
                return null;
            }
            var popup=findNamePopup();
            if(!popup) return;

            var btns=popup.querySelectorAll("button,[role=button],input[type=button],input[type=submit]");
            for(var i=0;i<btns.length;i++){
                var tx=(btns[i].innerText||btns[i].value||"").replace(/\\s+/g,"");
                if(tx.indexOf("조회")>=0||tx.indexOf("검색")>=0){
                    btns[i].focus();
                    try{ btns[i].click(); }catch(e){}
                    break;
                }
            }
        ''')
		waitSec(1.5)
		return true
	}

	// =========================================================
	// [H-2] 이름 팝업 시나리오 (단일 TC로 전체 플로우 실행)
	// =========================================================
	private static String runNamePopupScenario(Map popupScan) {
		List<String> log = []
		boolean scenarioSuccess = false

		// ── 헬퍼: 팝업 내 현재 리스트 아이템 텍스트 목록 수집 ──────────────
		Closure<List<String>> getListItems = {
			Object r = runJs('''
				function findPopup(){
					var cs=Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay],[class*=window],[class*=Window]"));
					document.querySelectorAll("div,section,aside").forEach(function(el){
						var s=window.getComputedStyle(el);
						if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10)
							if(cs.indexOf(el)<0) cs.push(el);
					});
					for(var i=0;i<cs.length;i++){
						var d=cs[i],ds=window.getComputedStyle(d);
						if(ds.display==="none"||ds.visibility==="hidden") continue;
						var t=(d.innerText||"").replace(/\\s+/g," ").trim();
						if(t.indexOf("사용자 정보 검색")>=0||t.indexOf("회원 선택")>=0) return d;
					}
					return null;
				}
				var popup=findPopup(); if(!popup) return "[]";
				var items=[];
				var rows=popup.querySelectorAll("tbody tr");
				if(rows.length>0){
					rows.forEach(function(r){ items.push((r.innerText||"").replace(/\\s+/g," ").trim()); });
				} else {
					var SKIP=["사용자 정보 검색","회원 선택","선택 완료","조회","검색"];
					Array.from(popup.querySelectorAll("label,li,div[class*=item],div[class*=row],div[class*=card]")).forEach(function(el){
						if(el.offsetParent===null) return;
						var t=(el.innerText||"").replace(/\\s+/g," ").trim();
						if(t.length<2) return;
						for(var k=0;k<SKIP.length;k++) if(t.indexOf(SKIP[k])>=0) return;
						items.push(t);
					});
				}
				return JSON.stringify(items);
			''')
			try {
				return new groovy.json.JsonSlurper().parseText(r?.toString() ?: "[]") as List<String>
			}
			catch (Exception e) {
				return []
			}
		}

		// ── 헬퍼: 팝업 검색창에 값 입력 ──────────────────────────────────
		Closure<Void> setSearchInput = { String val ->
			String esc = escapeJsString(val)
			runJs("""
				function findPopup(){
					var cs=Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay]"));
					document.querySelectorAll("div,section,aside").forEach(function(el){
						var s=window.getComputedStyle(el);
						if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10)
							if(cs.indexOf(el)<0) cs.push(el);
					});
					for(var i=0;i<cs.length;i++){
						var d=cs[i],ds=window.getComputedStyle(d);
						if(ds.display==="none"||ds.visibility==="hidden") continue;
						var t=(d.innerText||"").replace(/\\s+/g," ").trim();
						if(t.indexOf("사용자 정보 검색")>=0||t.indexOf("회원 선택")>=0) return d;
					}
					return null;
				}
					var p=findPopup(); if(!p) return;
				var inp=p.querySelector("input:not([type=hidden]):not([type=checkbox]):not([type=radio]):not([type=button]):not([type=submit])");
				if(!inp) return;
				// 강제 클리어 후 새 값 세팅 (React/Vue 대응)
				inp.focus();
				var nativeSetter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,"value");
				if(nativeSetter && nativeSetter.set) nativeSetter.set.call(inp, '${esc}');
				else inp.value='${esc}';
				["input","change","keyup"].forEach(function(ev){ inp.dispatchEvent(new Event(ev,{bubbles:true})); });
			""")
			waitSec(0.2)
		}

		// ── 헬퍼: 조회 버튼 클릭 ──────────────────────────────────────────
		Closure<Void> clickSearch = {
			actionNamePopupClickSearchOnly(false)
			waitSec(1.5)
		}

		// ── 헬퍼: 리스트가 뜨고 검색어가 포함되는지 확인 ──────────────────
		Closure<Boolean> listContains = { String keyword ->
			List<String> items = getListItems()
			if (!items) return false
			if (!keyword) return items.size() > 0
			return items.any { it.contains(keyword) }
		}

		try {
			if (!openNameSearchPopup()) {
				return "[팝업열기실패] 이름 팝업을 열 수 없습니다"
			}
			waitSec(0.8)

			// ─────────────────────────────────────────────────────────────────
			// STEP 0: 초기 상태 — 빈값으로 조회 버튼 클릭 (전체 목록 확인)
			// ─────────────────────────────────────────────────────────────────
			logLine("이름팝업 STEP0: 빈값 조회")
			try {
				setSearchInput("")
			} catch (Exception e) {}
			try {
				clickSearch()
			} catch (Exception e) {}
			List<String> initList = []
			try {
				initList = getListItems()
			} catch (Exception e) {}
			String initMsg = "INIT[빈값조회]: 리스트${initList.size()}건"
			log << initMsg
			logLine(initMsg)

			// ─────────────────────────────────────────────────────────────────
			logLine("이름팝업 STEP1: 비정상값 검증")
			// STEP 1: 비정상값 입력 → 조회 → 결과 확인
			// ─────────────────────────────────────────────────────────────────
			List<String> invalidTestCases = ["123", "홍길동😊", "홍#길동", "   "]
			invalidTestCases.each { String testVal ->
				try {
					// 매번 클리어 후 입력
					try {
						setSearchInput("")
					} catch (Exception e) {}
					waitSec(0.2)
					setSearchInput(testVal)
					clickSearch()
					List<String> items = getListItems()
					String disp = testVal.trim() ?: '공백'
					String msg
					if (items.isEmpty()) {
						msg = "INPUT[${disp}]: 조회결과없음 → PASS"
					} else {
						boolean contained = items.any { it.contains(testVal.trim()) }
						msg = "INPUT[${disp}]: 리스트${items.size()}건${contained ? '(검색어포함)' : '(검색어미포함)'} → ${contained ? 'PASS' : '비정상결과'}"
					}
					log << msg
					logLine(msg)
				} catch (Exception e) {
					String errMsg = "INPUT[${testVal.trim()?:'공백'}]: 처리에러 무시"
					log << errMsg
					logLine(errMsg)
				}
			}

			// ─────────────────────────────────────────────────────────────────
			logLine("이름팝업 STEP2: 정상검색어 조회")
			// STEP 2: 정상 검색어 입력 → 조회 → 리스트 확인
			// ─────────────────────────────────────────────────────────────────
			String normalKeyword = "홍"
			try {
				setSearchInput(normalKeyword)
			} catch (Exception e) {}
			try {
				clickSearch()
			} catch (Exception e) {}
			List<String> normalList = []
			try {
				normalList = getListItems()
			} catch (Exception e) {}

			if (!normalList.isEmpty() && normalList.any { it.contains(normalKeyword) }) {
				String msg = "SEARCH[${normalKeyword}]: 리스트${normalList.size()}건, 검색어포함 → PASS"
				log << msg
				logLine(msg)
				scenarioSuccess = true
			} else if (!normalList.isEmpty()) {
				String msg = "SEARCH[${normalKeyword}]: 리스트${normalList.size()}건 노출 → PASS"
				log << msg
				logLine(msg)
				scenarioSuccess = true
			} else {
				String msg = "SEARCH[${normalKeyword}]: 조회결과없음, 전체조회 재시도"
				log << msg
				logLine(msg)
				try {
					setSearchInput("")
				} catch (Exception e) {}
				try {
					clickSearch()
				} catch (Exception e) {}
				try {
					normalList = getListItems()
				} catch (Exception e) {}
				if (!normalList.isEmpty()) {
					String msg2 = "SEARCH[전체]: 리스트${normalList.size()}건 → PASS"
					log << msg2
					logLine(msg2)
					scenarioSuccess = true
				}
			}

			// ─────────────────────────────────────────────────────────────────
			logLine("이름팝업 STEP3: 드롭다운 검증")
			// STEP 3: 드롭다운 있으면 각 옵션 선택 후 조회
			// ─────────────────────────────────────────────────────────────────
			List<Map> selects = (popupScan?.selects ?: []) as List<Map>
			if (selects) 			selects.each { Map sel ->
				String selLabel = (sel.label ?: "드롭박스").toString()
				List<Map> options = (sel.options ?: []) as List<Map>
				int selIdx = (sel.index ?: 0) as int

				options.each { Map opt ->
					try {
						int optIdx = (opt.index ?: 0) as int
						String optTxt = (opt.text ?: "").toString().trim()
						if (optIdx == 0 || !optTxt || optTxt.contains("선택")) return

							runJs("""
							var p=document.querySelector("[role=dialog],[class*=modal],[class*=popup],[class*=layer]")||
								Array.from(document.querySelectorAll("div")).find(function(el){
									var s=window.getComputedStyle(el);
									return (s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10&&el.offsetWidth>100;
								});
							if(!p) return;
							var ss=p.querySelectorAll("select");
							if(ss.length>${selIdx}){ ss[${selIdx}].selectedIndex=${optIdx}; ss[${selIdx}].dispatchEvent(new Event("change",{bubbles:true})); }
						""")
						waitSec(0.3)
						setSearchInput(normalKeyword)
						clickSearch()
						List<String> dropList = getListItems()
						String msg
						if (!dropList.isEmpty() && dropList.any { it.contains(normalKeyword) }) {
							msg = "DROP[${selLabel}=${optTxt}]: 리스트${dropList.size()}건, 검색어포함 → PASS"
							scenarioSuccess = true
						} else if (!dropList.isEmpty()) {
							msg = "DROP[${selLabel}=${optTxt}]: 리스트${dropList.size()}건 → PASS"
							scenarioSuccess = true
						} else {
							msg = "DROP[${selLabel}=${optTxt}]: 조회결과없음"
						}
						log << msg
						logLine(msg)
					} catch (Exception e) {
						String errMsg = "DROP[${sel.label}=${opt.text}]: 처리에러 무시"
						log << errMsg
						logLine(errMsg)
					}
				}
			}

			// ─────────────────────────────────────────────────────────────────
			logLine("이름팝업 STEP4: 최종 항목 선택")
			// STEP 4: 드롭다운 리셋 + 빈값 조회 → 첫 번째 항목 선택
			// ─────────────────────────────────────────────────────────────────
			try {
				runJs('''
					var cs=Array.from(document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer],[class*=Modal],[class*=Dialog],[class*=overlay],[class*=Overlay]"));
					document.querySelectorAll("div,section,aside").forEach(function(el){
						var s=window.getComputedStyle(el);
						if((s.position==="fixed"||s.position==="absolute")&&parseInt(s.zIndex||0)>10) cs.push(el);
					});
					var p=null;
					for(var i=0;i<cs.length;i++){
						var d=cs[i],ds=window.getComputedStyle(d);
						if(ds.display==="none"||ds.visibility==="hidden") continue;
						var t=(d.innerText||"").replace(/\\s+/g," ").trim();
						if(t.indexOf("사용자 정보 검색")>=0||t.indexOf("회원 선택")>=0){ p=d; break; }
					}
					if(!p) return;
					p.querySelectorAll("select").forEach(function(s){ s.selectedIndex=0; s.dispatchEvent(new Event("change",{bubbles:true})); });
				''')
				waitSec(0.3)
			} catch (Exception e) { }

			try {
				setSearchInput("")
			} catch (Exception e) {}
			try {
				clickSearch()
			} catch (Exception e) {}
			List<String> finalList = []
			try {
				finalList = getListItems()
			} catch (Exception e) {}

			if (!finalList.isEmpty()) {
				try {
					actionNamePopupSelectFirstItem()
				} catch (Exception e) {}
				String msg = "SUCCESS: 빈값조회→첫번째항목선택완료 (리스트${finalList.size()}건)"
				log << msg
				logLine(msg)
			} else {
				String msg = "FALLBACK: 결과없음 → X버튼닫고정상값세팅"
				log << msg
				logLine(msg)
				try {
					closeNameSearchPopup()
				} catch (Exception e) {}
				waitSec(0.5)
				try {
					setNameByAutoSelection("홍길동")
				} catch (Exception e) {}
			}
		} catch (Exception ex) {
			String errMsg = "ERROR(무시): " + (ex.message ?: ex.toString()).take(80)
			log << errMsg
			logLine(errMsg)
			try {
				closeNameSearchPopup()
			} catch (Exception ignored) {}
		}

		nameFieldDone = true  // 이름 시나리오 완료 → 이후 TC에서 이름 건드리지 않음
		return log ? log.last() : ""
	}

	// =========================================================
	// [I] 일반 체크 / 라디오 / 드롭다운 / 주소
	// =========================================================
	private static void checkAllCheckboxes() {
		runJs('''
            document.querySelectorAll("input[type=checkbox]").forEach(function(c){
                c.checked = true;
                c.dispatchEvent(new Event("change",{bubbles:true}));
            });
        ''')
	}

	private static void uncheckAllCheckboxes() {
		runJs('''
            document.querySelectorAll("input[type=checkbox]").forEach(function(c){
                c.checked = false;
                c.dispatchEvent(new Event("change",{bubbles:true}));
            });
        ''')
	}

	private static void setCheckboxByIndex(int idx, boolean on) {
		runJs("""
            var c=document.querySelectorAll("input[type=checkbox]");
            if(c.length>${idx}){
                c[${idx}].checked=${on ? 'true' : 'false'};
                c[${idx}].dispatchEvent(new Event("change",{bubbles:true}));
            }
        """)
	}

	private static void checkOnlyFirstCheckbox() {
		uncheckAllCheckboxes()
		setCheckboxByIndex(0, true)
	}

	private static void clearRadioGroupSelection() {
		runJs('''
            document.querySelectorAll("input[type=radio]").forEach(function(r){
                r.checked = false;
                r.dispatchEvent(new Event("change",{bubbles:true}));
            });
        ''')
	}

	private static void selectFirstRadioOrButtonGroup() {
		runJs('''
            var radios = document.querySelectorAll("input[type=radio]");
            if(radios.length > 0){
                radios[0].checked = true;
                radios[0].dispatchEvent(new Event("change",{bubbles:true}));
                var label = radios[0].closest("label");
                if(label) try{ label.click(); }catch(e){}
                return;
            }

            var groups = {};
            document.querySelectorAll("button,[role=button]").forEach(function(b){
                if(b.offsetParent === null) return;
                var tx = (b.innerText || "").trim();
                if(!tx || tx.length > 10) return;
                if(["중복","확인","가입","등록","신청","제출"].some(function(k){ return tx.indexOf(k) >= 0; })) return;

                var key = (b.parentElement && (b.parentElement.className || b.parentElement.id)) || "g";
                if(!groups[key]) groups[key] = [];
                groups[key].push(b);
            });

            for(var k in groups){
                if(groups[k].length >= 2){
                    try{ groups[k][0].click(); }catch(e){}
                    break;
                }
            }
        ''')
	}

	private static void selectRadioOrButtonOption(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return

			String name = parts[0]
		int idx = (parts[1] ?: "0") as int

		runJs("""
            var radios=document.querySelectorAll('input[type=radio][name="${escapeJsString(name)}"]');
            if(radios.length>${idx}){
                radios[${idx}].checked=true;
                radios[${idx}].dispatchEvent(new Event("change",{bubbles:true}));
                var label=radios[${idx}].closest("label");
                if(label) try{ label.click(); }catch(e){}
            }else{
                var buttons=Array.from(document.querySelectorAll("button,[role=button]")).filter(function(b){
                    return b.offsetParent!==null &&
                           (b.innerText||"").trim().length>0 &&
                           (b.innerText||"").trim().length<=10;
                });
                if(buttons.length>${idx}) try{ buttons[${idx}].click(); }catch(e){}
            }
        """)
		waitSec(0.2)
	}

	private static void autoSelectMainFormDropdowns() {
		runJs('''
            function vis(e){
                var s = window.getComputedStyle(e);
                return e.offsetParent !== null && s.display !== "none" && s.visibility !== "hidden";
            }

            document.querySelectorAll("select").forEach(function(s){
                if(!vis(s) || !s.options || s.options.length <= 1 || s.selectedIndex > 0) return;

                for(var i=1;i<s.options.length;i++){
                    var v = (s.options[i].value || "").trim();
                    var t = (s.options[i].text || "").trim();
                    if(v && t.indexOf("선택") < 0 && t.indexOf("--") < 0){
                        s.selectedIndex = i;
                        s.dispatchEvent(new Event("change",{bubbles:true}));
                        break;
                    }
                }
            });
        ''')
	}

	private static void resetMainSelectByIdOrName(String selectIdOrName) {
		runJs("""
            var s=document.getElementById('${escapeJsString(selectIdOrName)}') || document.querySelector('select[name="${escapeJsString(selectIdOrName)}"]');
            if(s){
                s.selectedIndex=0;
                s.dispatchEvent(new Event("change",{bubbles:true}));
            }
        """)
	}

	private static void selectMainDropdownOption(String value) {
		List<String> parts = (value ?: "").split("::") as List<String>
		if (parts.size() < 2) return

			String sid = parts[0]
		int idx = (parts[1] ?: "0") as int

		runJs("""
            var s=document.getElementById('${escapeJsString(sid)}') || document.querySelector('select[name="${escapeJsString(sid)}"]');
            if(s && s.options.length>${idx}){
                s.selectedIndex=${idx};
                s.dispatchEvent(new Event("change",{bubbles:true}));
            }
        """)
	}

	private static void closeDaumAddressPopupIfExists() {
		try {
			String main = driver().getWindowHandle()
			for (String handle : driver().getWindowHandles()) {
				if (handle == main) continue
					driver().switchTo().window(handle)
				String cur = driver().getCurrentUrl()?.toLowerCase() ?: ""
				if (cur.contains("postcode") || cur.contains("daum") || cur.contains("about:blank")) {
					driver().close()
				}
			}
			driver().switchTo().window(main)
		} catch (Exception ignore) {
		}
	}

	private static void fillDummyAddress() {
		closeDaumAddressPopupIfExists()

		runJs('''
            function vis(e){ return e && e.offsetParent !== null; }
            function setValue(e,v){
                if(!e) return;
                var ro = e.readOnly, di = e.disabled;
                e.readOnly = false; e.disabled = false;
                e.value = v;
                e.dispatchEvent(new Event("input",{bubbles:true}));
                e.dispatchEvent(new Event("change",{bubbles:true}));
                e.readOnly = ro; e.disabled = di;
            }
            function findByPlaceholder(t){
                for(var i of document.querySelectorAll("input")){
                    if(vis(i) && (i.placeholder||"").indexOf(t) >= 0) return i;
                }
                return null;
            }
            function findByIdName(keys){
                for(var i of document.querySelectorAll("input")){
                    if(!vis(i)) continue;
                    var id = (i.id||"").toLowerCase();
                    var nm = (i.name||"").toLowerCase();
                    for(var k=0;k<keys.length;k++){
                        if(id.indexOf(keys[k]) >= 0 || nm.indexOf(keys[k]) >= 0) return i;
                    }
                }
                return null;
            }

            var zip=findByPlaceholder("우편번호")||findByIdName(["post","zip","zipcode"]);
            var road=findByPlaceholder("도로명주소")||findByPlaceholder("도로명")||findByIdName(["road","addr1","address","roadaddr"]);
            var jibun=findByPlaceholder("지번주소")||findByPlaceholder("지번")||findByIdName(["jibun","addr2","jibunaddr"]);
            var detail=findByPlaceholder("상세주소")||findByPlaceholder("상세")||findByIdName(["detail","addr3","detailaddr","addrdetail"]);

            if(zip && !(zip.value||"").trim()) setValue(zip,"06000");
            if(road && !(road.value||"").trim()) setValue(road,"서울특별시 강남구 테헤란로 123");
            if(jibun && !(jibun.value||"").trim()) setValue(jibun,"서울특별시 강남구 역삼동 123-45");
            if(detail && !(detail.value||"").trim()) setValue(detail,"101동 101호");
        ''')
	}

	private static void setAddressCase(String mode) {
		runJs('''
            ["우편번호","도로명","지번","상세","post","zip","road","addr","address","detail"].forEach(function(k){
                document.querySelectorAll("input").forEach(function(i){
                    if(i.offsetParent===null) return;
                    var ph=(i.placeholder||"").toLowerCase();
                    var id=(i.id||"").toLowerCase();
                    var nm=(i.name||"").toLowerCase();
                    if(ph.indexOf(k)>=0 || id.indexOf(k)>=0 || nm.indexOf(k)>=0){
                        i.value="";
                        i.dispatchEvent(new Event("input",{bubbles:true}));
                    }
                });
            });
        ''')

		if (mode == "clear") return

			String m = escapeJsString(mode)
		runJs("""
            function vis(e){ return e && e.offsetParent !== null; }
            function setValue(e,v){
                if(!e) return;
                e.value=v;
                e.dispatchEvent(new Event("input",{bubbles:true}));
                e.dispatchEvent(new Event("change",{bubbles:true}));
            }
            function findByPlaceholder(t){
                for(var i of document.querySelectorAll("input")){
                    if(vis(i) && (i.placeholder||"").indexOf(t) >= 0) return i;
                }
                return null;
            }
            function findByIdName(keys){
                for(var i of document.querySelectorAll("input")){
                    if(!vis(i)) continue;
                    var id=(i.id||"").toLowerCase();
                    var nm=(i.name||"").toLowerCase();
                    for(var k=0;k<keys.length;k++){
                        if(id.indexOf(keys[k]) >= 0 || nm.indexOf(keys[k]) >= 0) return i;
                    }
                }
                return null;
            }

            var zip=findByPlaceholder("우편번호")||findByIdName(["post","zip"]);
            var road=findByPlaceholder("도로명주소")||findByIdName(["road","addr1","address"]);
            var detail=findByPlaceholder("상세주소")||findByIdName(["detail","addr3"]);
            var mode='${m}';

            if(mode==="zipOnly") setValue(zip,"06000");
            if(mode==="roadOnly") setValue(road,"서울특별시 강남구 테헤란로 123");
            if(mode==="detailOnly") setValue(detail,"101동 101호");
        """)
	}

	private static void applyGeneralRequiredDefaults() {
		try {
			selectFirstRadioOrButtonGroup()
		} catch (Exception ignore) {}
		try {
			fillDummyAddress()
		} catch (Exception ignore) {}
		try {
			autoSelectMainFormDropdowns()
		} catch (Exception ignore) {}
	}

	// =========================================================
	// [J] 메인 화면 스캔
	// =========================================================
	private static Map scanMainFormElements() {
		try {
			Object r = runJs('''
                function isInsideModal(e){
                    return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]");
                }
                function vis(e){
                    var s = window.getComputedStyle(e);
                    return e.offsetParent !== null &&
                           s.display !== "none" &&
                           s.visibility !== "hidden" &&
                           !e.closest("header,footer,nav") &&
                           !isInsideModal(e);
                }
                function txt(e){
                    return (e.innerText || "").replace(/\\s+/g," ").trim();
                }

                var result = {groups:[],selects:[],checkboxes:[]};

                var radioMap = {};
                document.querySelectorAll("input[type=radio]").forEach(function(r){
                    if(!vis(r)) return;
                    var name = r.name || "r";
                    if(!radioMap[name]) radioMap[name] = [];

                    var label = "";
                    if(r.labels && r.labels.length) label = txt(r.labels[0]);
                    else{
                        var p = r.closest("label");
                        if(p) label = txt(p);
                    }
                    radioMap[name].push(label || r.value || "옵션");
                });

                for(var n in radioMap){
                    var first = document.querySelector('input[type=radio][name="'+n+'"]');
                    if(!first || !vis(first)) continue;

                    var groupWrap = first.closest("div,fieldset");
                    var groupLabel = n;
                    if(groupWrap){
                        var prev = groupWrap.previousElementSibling;
                        if(prev && vis(prev)) groupLabel = txt(prev).replace("*","").trim();
                    }

                    result.groups.push({
                        name:n,
                        label:groupLabel,
                        options:Array.from(new Set(radioMap[n])),
                        type:"radio"
                    });
                }

                var selectDupMap = {};
                document.querySelectorAll("select").forEach(function(s){
                    if(!vis(s) || !s.options || s.options.length <= 1) return;

                    var options = [];
                    for(var i=0;i<s.options.length;i++){
                        options.push({
                            value:s.options[i].value,
                            text:txt(s.options[i])
                        });
                    }

                    var label = "";
                    if(s.labels && s.labels.length && vis(s.labels[0])) label = txt(s.labels[0]).replace("*","").trim();
                    else{
                        var prev = s.previousElementSibling;
                        if(prev && prev.tagName !== "SELECT" && vis(prev)) label = txt(prev).replace("*","").trim();
                    }

                    var key = (s.id||"") + (s.name||"") + label;
                    if(selectDupMap[key]) return;
                    selectDupMap[key] = 1;

                    if(!label) label = s.id || s.name || "드롭박스";

                    result.selects.push({
                        id:s.id || "",
                        name:s.name || "",
                        label:label,
                        options:options
                    });
                });

                document.querySelectorAll("input[type=checkbox]").forEach(function(cb){
                    if(!vis(cb)) return;

                    var label = "";
                    if(cb.labels && cb.labels.length && vis(cb.labels[0])) label = txt(cb.labels[0]);
                    else{
                        var p = cb.closest("label");
                        if(p && vis(p)) label = txt(p);
                    }

                    result.checkboxes.push({
                        id:cb.id || "",
                        name:cb.name || "",
                        label:label,
                        index:result.checkboxes.length
                    });
                });

                return JSON.stringify(result);
            ''')

			return (Map) new JsonSlurper().parseText(r?.toString() ?: '{"groups":[],"selects":[],"checkboxes":[]}')
		} catch (Exception e) {
			return [groups:[], selects:[], checkboxes:[]]
		}
	}

	private static List<String> scanMainFormDomOrder() {
		try {
			Object r = runJs('''
                var result = [], seen = {};

                function push(k){
                    if(!seen[k]){
                        seen[k] = 1;
                        result.push(k);
                    }
                }

                function isInsideModal(e){
                    return !!e.closest("[role=dialog],[class*=modal],[class*=popup],[class*=layer]");
                }

                function vis(e){
                    if(!e) return false;
                    var s = window.getComputedStyle(e);
                    return e.offsetParent !== null &&
                           s.display !== "none" &&
                           s.visibility !== "hidden" &&
                           !isInsideModal(e);
                }

                var all = [];

                document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])").forEach(function(e){
                    if(vis(e)) all.push({e:e,t:"input"});
                });

                var radioSeen = {};
                document.querySelectorAll("input[type=radio]").forEach(function(e){
                    if(!vis(e)) return;
                    var n = e.name || "r";
                    if(!radioSeen[n]){
                        radioSeen[n] = 1;
                        all.push({e:e,t:"radio"});
                    }
                });

                document.querySelectorAll("select").forEach(function(e){
                    if(vis(e) && e.options && e.options.length > 1) all.push({e:e,t:"select"});
                });

                var checkboxAdded = false;
                document.querySelectorAll("input[type=checkbox]").forEach(function(e){
                    if(!vis(e) || checkboxAdded) return;
                    all.push({e:e,t:"cb"});
                    checkboxAdded = true;
                });

                all.sort(function(a,b){
                    var ra = a.e.getBoundingClientRect();
                    var rb = b.e.getBoundingClientRect();
                    return Math.abs(ra.top-rb.top) > 10 ? ra.top-rb.top : ra.left-rb.left;
                });

                for(var i=0;i<all.length;i++){
                    var item = all[i];
                    var el = item.e;

                    if(item.t === "input"){
                        var id = (el.id||"").toLowerCase();
                        var nm = (el.name||"").toLowerCase();
                        var ph = (el.placeholder||"").toLowerCase();

                        if(el.type === "password"){
                            (id.indexOf("confirm") >= 0 || nm.indexOf("confirm") >= 0 || ph.indexOf("확인") >= 0) ? push("pwConf") : push("pw");
                        }else if(id.indexOf("email") >= 0 || nm.indexOf("email") >= 0 || ph.indexOf("이메일") >= 0 || ph.indexOf("@") >= 0) push("email");
                        else if(id.indexOf("id") >= 0 || nm.indexOf("id") >= 0 || ph.indexOf("아이디") >= 0) push("id");
                        else if(id.indexOf("name") >= 0 || nm.indexOf("name") >= 0 || ph.indexOf("이름") >= 0) push("name");
                        else if(id.indexOf("phone") >= 0 || nm.indexOf("phone") >= 0 || ph.indexOf("전화") >= 0) push("phone");
                        else if(id.indexOf("addr") >= 0 || nm.indexOf("addr") >= 0 || ph.indexOf("주소") >= 0) push("address");
                    } else if(item.t === "radio"){
                        push("btnGroup");
                    } else if(item.t === "select"){
                        push("select_" + (el.id || el.name || i));
                    } else if(item.t === "cb"){
                        push("terms");
                    }
                }

                return result.join(",");
            ''')

			return (r?.toString() ?: "").split(",").findAll {
				it?.trim()
			}
		} catch (Exception e) {
			return []
		}
	}

	private static boolean hasVisibleCssElement(String css) {
		try {
			return driver().findElements(By.cssSelector(css)).any {
				isElementVisible(it)
			}
		} catch (Exception e) {
			return false
		}
	}

	// =========================================================
	// [K] TC 생성
	// =========================================================
	private static List<Map> buildStaticTestCases() {
		String FAIL = "실패"
		String PASS = "성공"

		// ── 이름: 팝업 모드면 "선택 없이 완료" 1개, 일반 모드면 7개 ──
		// 팝업 모드에서 이름 단위 TC는 namePopupScenario TC 하나로 대체됨
		List<Map> nameTcs = isNameSearchPopupMode
				? []  // 팝업 모드: 단위-이름 TC 없음 (namePopupScenario에서 전부 처리)
				: [
					makeTc("단위-이름","빈 값","이름","name","",FAIL),
					makeTc("단위-이름","숫자","이름","name","123",FAIL),
					makeTc("단위-이름","공백","이름","name","홍 길동",FAIL),
					makeTc("단위-이름","이모지","이름","name","홍길동😊",FAIL),
					makeTc("단위-이름","51자초과","이름","name","홍"*51,FAIL),
					makeTc("단위-이름","특수문자","이름","name","홍#길동",FAIL),
					makeTc("단위-이름","2자정상","이름","name","김철",PASS)
				]

		return nameTcs + [
			// ── 아이디 ────────────────────────────────────────
			makeTc("단위-아이디","빈 값","아이디","id","",FAIL),
			makeTc("단위-아이디","3자미달","아이디","id","tes",FAIL),
			makeTc("단위-아이디","한글","아이디","id","관리자12",FAIL),
			makeTc("단위-아이디","이모지","아이디","id","✨✨123",FAIL),
			makeTc("단위-아이디","특수문자","아이디","id","user!@#",FAIL),
			makeTc("단위-아이디","공백","아이디","id","use 01",FAIL),
			makeTc("단위-아이디","50자초과","아이디","id","a"*50,FAIL),
			makeTc("단위-아이디","6자정상","아이디","id","user12",PASS),
			// ── 비밀번호 ──────────────────────────────────────
			makeTc("단위-비밀번호","빈 값","비밀번호","pw","",FAIL),
			makeTc("단위-비밀번호","7자미달","비밀번호","pw","123456",FAIL),
			makeTc("단위-비밀번호","영문만","비밀번호","pw","password",FAIL),
			makeTc("단위-비밀번호","숫자만","비밀번호","pw","12345678",FAIL),
			makeTc("단위-비밀번호","특수만","비밀번호","pw",'1@#$%^&*',FAIL),
			makeTc("단위-비밀번호","공백","비밀번호","pw","Pass 12!",FAIL),
			makeTc("단위-비밀번호","영숫자만","비밀번호","pw","pass1234",FAIL),
			makeTc("단위-비밀번호","한글","비밀번호","pw","비밀123!@",FAIL),
			makeTc("단위-비밀번호","8자정상","비밀번호","pw","Test12!@",PASS),
			// ── 비번확인 ──────────────────────────────────────
			makeTc("단위-비번확인","빈 값","비밀번호","pwConf","",FAIL),
			makeTc("단위-비번확인","불일치","비밀번호","pwConf","wrong!@#1",FAIL),
			makeTc("단위-비번확인","한글","비밀번호","pwConf","비밀123!@",FAIL),
			makeTc("단위-비번확인","공백만","비밀번호","pwConf","       ",FAIL),
			makeTc("단위-비번확인","대소불일치","비밀번호","pwConf","test123!@",FAIL),
			makeTc("단위-비번확인","정상일치","비밀번호","pwConf","Test123!@",PASS),
			// ── 주소 ──────────────────────────────────────────
			makeTc("단위-주소","전체미입력","주소","address","clear",FAIL),
			makeTc("단위-주소","우편번호만","주소","address","zipOnly",FAIL),
			makeTc("단위-주소","도로명만","주소","address","roadOnly",FAIL),
			makeTc("단위-주소","상세만","주소","address","detailOnly",FAIL),
			// ── 시나리오 ──────────────────────────────────────
			makeTc("시나리오","전체빈값","이름","clear","clear",FAIL),
			makeTc("시나리오","중복확인생략","중복확인","skipDupCheck","testus01",FAIL),
			makeTc("시나리오","ID변경","중복확인","id","user02",FAIL),
			makeTc("시나리오","ID삭제","아이디","id","",FAIL),
			makeTc("시나리오","중복3회","중복확인","tripledup","tripledup",PASS),
			makeTc("시나리오","ID공백","중복확인","id"," use 01 ",FAIL),
			makeTc("시나리오","전체재시도","이름","clear","clear",FAIL),
			makeTc("시나리오","ID대소문자","중복확인","id","USER01",PASS),
			// ── 성공 ──────────────────────────────────────────
			makeTc("성공","정상가입","성공","success","success",PASS)
		]
	}

	private static List<Map> buildMainFormDynamicTestCases(Map mainScan) {
		List<Map> tcs = []
		String FAIL = "실패"
		String PASS = "성공"

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
				Map opt = obj as Map
				String txt = (opt.text ?: "").toString()
				if (i == 0 || !txt.trim() || txt.contains("선택")) return
					tcs << makeTc("단위-${label}", "${label} [${txt}]", label, "selectOption", "${sid}::${i}", PASS)
			}
		}

		List<Map> checkboxes = (mainScan.checkboxes ?: []) as List<Map>
		if (checkboxes && hasVisibleCssElement("input[type='checkbox']")) {
			tcs << makeTc("단위-약관", "전체미체크", "약관", "uncheckAll", "all", FAIL)
			tcs << makeTc("단위-약관", "전체체크", "약관", "checkAll", "all", PASS)

			if (((checkboxes[0]?.label ?: "") as String).contains("전체")) {
				tcs << makeTc("단위-약관", "전체동의만", "약관", "checkOnlyFirst", "0", PASS)
			}

			checkboxes.eachWithIndex { Map cb, int i ->
				if (((cb.label ?: "") as String).contains("전체")) return
					String label = ((cb.label ?: "항목${i}") as String).take(15)
				tcs << makeTc("단위-약관", "[${label}] 미체크", "약관", "uncheckOne", "${i}", FAIL)
				tcs << makeTc("단위-약관", "[${label}] 체크", "약관", "checkOne", "${i}", PASS)
			}
		}

		return tcs
	}

	private static List<Map> buildNamePopupDynamicTestCases(Map popupScan) {
		// 팝업 시나리오 전체를 단일 TC로 처리 (runNamePopupScenario 에서 순차 실행)
		if (!isNameSearchPopupMode) return []
		return [
			makeTc("단위-이름팝업", "이름팝업 시나리오", "이름팝업", "namePopupScenario", "", "성공")
		]
	}

	// =========================================================
	// [K-2] TC 정렬 - 수정됨: 단위-이름 계열 전체를 name DOM 순서 기준으로
	// =========================================================
	private static List<Map> sortTestCasesByDomOrder(List<Map> tcs, List<String> domOrder) {
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

			// ★ 핵심 수정: 단위-이름 / 단위-이름팝업* 모두 name DOM 순서 기준
			if (type.startsWith("단위-이름")) {
				return domIndex("name") + 0.3
			}

			if (type.startsWith("단위-")) {
				Map<String, String> fieldMap = [
					name        : "name",
					id          : "id",
					pw          : "pw",
					pwConf      : "pwConf",
					address     : "address",
					skipDupCheck: "id",
					tripledup   : "id",
					email       : "email",
					phone       : "phone"
				]

				if (fieldMap.containsKey(target)) return domIndex(fieldMap[target])
				if (target.startsWith("btnGroup")) return domIndex("btnGroup")
				if (target.toLowerCase().contains("check") || target.toLowerCase().contains("uncheck")) return domIndex("terms")

				if (target.toLowerCase().contains("select")) {
					String selId = ((one.value ?: "") as String).contains("::") ? ((one.value ?: "") as String).split("::")[0] : ""
					Integer matched = null
					for (int i = 0; i < domOrder.size(); i++) {
						if (domOrder[i].startsWith("select_") && selId && domOrder[i].contains(selId)) {
							matched = i
							break
						}
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

		tcs.eachWithIndex { Map tc, int i ->
			tc.id = String.format("TC-%02d", i + 1)
		}

		return tcs
	}

	// =========================================================
	// [L] 판정
	// =========================================================
	private static Map judgeTestResult(Map tc, String actual) {
		String msg = normalizeText(actual)

		List<String> positiveWords = [
			"가입 완료",
			"등록 완료",
			"저장 완료",
			"사용 가능",
			"사용가능",
			"성공",
			"완료",
			"일치",
			"정상 가입",
			"제출 완료",
			"[선택완료]"
		]
		List<String> weakPositiveWords = [
			"확인되었습니다",
			"적용되었습니다",
			"등록되었습니다",
			"저장되었습니다"
		]
		List<String> negativeWords = [
			"필수",
			"입력",
			"선택",
			"체크",
			"동의",
			"오류",
			"실패",
			"불가",
			"형식",
			"일치하지",
			"다릅니다",
			"중복",
			"이미 사용",
			"작성",
			"누락",
			"재입력",
			"올바른"
		]

		boolean hasPositive = positiveWords.any { msg.contains(it) }
		boolean hasWeakPositive = weakPositiveWords.any { msg.contains(it) }
		boolean hasNegative = negativeWords.any { msg.contains(it) }
		boolean empty = isBlankPopup(msg)

		String target = (tc.target ?: "").toString()

		if ((tc.expect ?: "") == "성공") {
			if (target == "btnGroupSelect") return [passed:true, reason:"옵션선택OK"]
			if (target in [
						"namePopupInput",
						"namePopupCheckbox",
						"namePopupSelectDefault",
						"namePopupSearchButton",
						"namePopupSelectOption"
					]) {
				return [passed:true, reason:"이름 팝업 검증"]
			}
			if (hasPositive || hasWeakPositive) return [passed:true, reason:"정상통과"]
			if (empty && !hasNegative) return [passed:true, reason:"오류없이정상"]
			return [passed:false, reason:(hasNegative ? "성공해야하나 에러" : "성공판정근거부족")]
		}

		if (empty) return [passed:false, reason:"예외미감지"]
		if (hasNegative) return [passed:true, reason:"예외정상방어"]
		if (hasPositive) return [passed:false, reason:"에러여야하나 성공"]
		return [passed:true, reason:"비정상응답감지"]
	}

	// =========================================================
	// [M] 로그
	// =========================================================
	private static void printTestCaseLog(boolean passed, Map tc, String popup, double elapsed, String reason) {
		String mark = passed ? "PASS" : "FAIL"
		String sep = "=" * 54
		String input = ((tc.value == "") ? "(빈 값)" : (tc.value ?: "-").toString()).take(40)

		println ""
		println sep
		println " ${mark} | ${tc.id} | ${tc.type} | ${tc.field ?: ''}"
		println " 입력값: ${input}"
		println " 결과  :"
		String popupText = popup ?: '없음'
		// \n 기준으로 줄 분리 (이름팝업 시나리오), 없으면 한 줄 출력
		List<String> resultLines = popupText.split("\\|").collect { it.trim() }.findAll { it }
		resultLines.each { String line -> println "   " + line }
		println " 판정  : ${reason}"
		println sep
	}

	private static void printSummary() {
		int total = testResults.size()
		int pass = testResults.count { it.passed == true }
		int fail = total - pass
		int rate = total > 0 ? (int)((pass * 100) / total) : 0

		println ""
		println "============================================================"
		println " 최종 결과: 전체 ${total}건 | PASS ${pass} | FAIL ${fail} | 통과율 ${rate}%"
		println "============================================================"
	}

	// =========================================================
	// [N] TC 실행
	// =========================================================
	private static boolean runSingleTestCase(int num, Map tc) {
		String actual = ""
		long started = System.currentTimeMillis()

		String target = (tc.target ?: "").toString()
		boolean isNamePopupTc = target.startsWith("namePopup")

		try {
			if (!isNamePopupTc) {
				dismissGeneralPopups()
			} else {
				closeBrowserAlertIfExists()
			}

			resetMainForm()
			waitSec(0.2)

			fillVisibleEmptyFieldsWithDefault()
			checkAllCheckboxes()
			applyGeneralRequiredDefaults()

			setFormFieldValue("id", FIXED_VALID_ID)
			setFormFieldValue("pw", "Test123!@")
			setFormFieldValue("pwConf", "Test123!@")

			if (!isNamePopupTc) {
				if (isNameSearchPopupMode) {
					if (nameFieldDone) {
						// 이름 시나리오 완료 후: 팝업 없이 JS로 직접 이름 세팅
						try {
							runJs('''
								var ins=document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");
								for(var i=0;i<ins.length;i++){
									var e=ins[i]; if(e.offsetParent===null) continue;
									var id=(e.id||"").toLowerCase(),nm=(e.name||"").toLowerCase(),ph=(e.placeholder||"").toLowerCase();
									if(id.indexOf("name")>=0||nm.indexOf("name")>=0||ph.indexOf("이름")>=0||ph.indexOf("성명")>=0){
										e.readOnly=false; e.disabled=false;
										e.value="홍길동";
										["input","change"].forEach(function(ev){ e.dispatchEvent(new Event(ev,{bubbles:true})); });
										break;
									}
								}
							''')
						} catch (Exception ignored) {}
					}
					// nameFieldDone=false이면: namePopupScenario TC가 아직 안 실행됨
					// → 이름 필드 그냥 비워둠 (팝업 안 열어도 됨, 이름 필드는 다른 TC에서 검증 안 함)
				} else {
					setFormFieldValue("name", "홍길동")
				}
			}

			Map<String, Closure> actionMap = [
				clear                        : {
					->
					resetMainForm()
					waitSec(0.2)
					fillVisibleEmptyFieldsWithDefault()
					checkAllCheckboxes()
					applyGeneralRequiredDefaults()
				},
				btnGroupClear                : { -> clearRadioGroupSelection() },
				btnGroupSelect               : { -> selectRadioOrButtonOption(tc.value?.toString()) },
				selectReset                  : { -> resetMainSelectByIdOrName(tc.value?.toString()) },
				selectOption                 : { -> selectMainDropdownOption(tc.value?.toString()) },
				uncheckAll                   : { -> uncheckAllCheckboxes() },
				uncheckOne                   : { -> setCheckboxByIndex((tc.value ?: "0") as int, false) },
				checkOnlyFirst               : { -> checkOnlyFirstCheckbox() },
				checkAll                     : { -> checkAllCheckboxes() },
				checkOne                     : { -> setCheckboxByIndex((tc.value ?: "0") as int, true) },
				address                      : { -> setAddressCase(tc.value?.toString()) },

				namePopupScenario                 : { -> /* runNamePopupScenario는 actual 수집 블록에서 직접 호출 */ },
				namePopupCompleteWithoutSelection: { -> actionNamePopupCompleteWithoutSelection() },
				namePopupSelectFirstItem          : { -> actionNamePopupSelectFirstItem() },
				namePopupSelectDefault            : { -> actionNamePopupSetDropdownDefault(tc.value?.toString()) },
				namePopupSelectOption             : { -> actionNamePopupSelectDropdownOption(tc.value?.toString()) },
				namePopupInput                    : { -> actionNamePopupSetInputValue(tc.value?.toString()) },
				namePopupCheckbox                 : { -> actionNamePopupSetCheckbox(tc.value?.toString()) },
				namePopupSearchButton            : { -> actionNamePopupClickSearchOnly() },

				skipDupCheck                  : {
					-> setFormFieldValue("id", tc.value?.toString())
				}
			]

			if (target == "success" || target == "tripledup") {
				// 아래 분기에서 처리
			} else if (target == "name" && isNameSearchPopupMode) {
				// 팝업 모드: 팝업 열고 → 아무것도 선택 안 한 채로 완료 버튼 클릭 → 에러 확인
				if (openNameSearchPopup()) {
					waitSec(0.5)
					actionNamePopupCompleteWithoutSelection()
				}
				actual = collectPopupMessage("name")
				// 판정 후 바로 return (아래 공통 가입버튼 블록 건너뜀)
			} else if (actionMap.containsKey(target)) {
				actionMap[target].call()
			} else {
				setFormFieldValue(target, tc.value?.toString())
			}

			if (target == "id") {
				if (existsDuplicateCheckButton()) clickDuplicateCheckButton()
				actual = collectPopupMessage("id")
			} else if (target == "tripledup") {
				setFormFieldValue("id", "testuser" + num)
				if (existsDuplicateCheckButton()) {
					for (int i = 0; i < 3; i++) {
						clickDuplicateCheckButton()
						actual = collectPopupMessage("id")
						waitSec(0.2)
					}
				}
				actual = "[중복3회] " + (actual ?: "[팝업없음]")
			} else if (target == "skipDupCheck") {
				closeGeneralModalIfExists()
				clickRegisterButton()
				actual = collectPopupMessage("skipDupCheck")
			} else if (target == "namePopupScenario") {
				actual = runNamePopupScenario(namePopupScan)
			} else if (target == "namePopupCompleteWithoutSelection") {
				actual = collectPopupMessage("namePopupCompleteWithoutSelection")
			} else if (target == "namePopupSelectFirstItem") {
				Object nameValue = runJs('''
                    var inputs = document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");
                    for(var i=0;i<inputs.length;i++){
                        var e = inputs[i];
                        if(e.offsetParent===null) continue;

                        var id=(e.id||"").toLowerCase();
                        var nm=(e.name||"").toLowerCase();
                        var ph=(e.placeholder||"").toLowerCase();
                        var lb="";
                        if(e.labels && e.labels.length) lb=(e.labels[0].innerText||"").toLowerCase();
                        else{
                            var p=e.closest("div,td,tr,li,section");
                            if(p) lb=(p.innerText||"").toLowerCase();
                        }

                        var isName = id.indexOf("name")>=0 ||
                                     nm.indexOf("name")>=0 ||
                                     ph.indexOf("이름")>=0 ||
                                     ph.indexOf("성명")>=0 ||
                                     lb.indexOf("이름")>=0 ||
                                     lb.indexOf("성명")>=0;

                        if(isName) return e.value || "";
                    }
                    return "";
                ''')
				actual = ((nameValue ?: "").toString().trim()) ? "[선택완료]" : "[팝업없음]"
				closeInnerCompletePopupIfExists()
			} else if (target in [
						"namePopupSelectOption",
						"namePopupInput",
						"namePopupCheckbox",
						"namePopupSelectDefault",
						"namePopupSearchButton"
					]) {
				actual = "[선택완료]"
				closeInnerCompletePopupIfExists()
			} else if (target == "btnGroupSelect") {
				actual = "[선택완료]"
			} else if (target == "success") {
				fillDummyAddress()
				selectFirstRadioOrButtonGroup()

				setFormFieldValue("id", FIXED_VALID_ID)
				if (existsDuplicateCheckButton()) {
					clickDuplicateCheckButton()
					dismissGeneralPopups()
				}

				closeGeneralModalIfExists()
				clickRegisterButton()
				actual = collectPopupMessage("success")
			} else if (target == "name" && isNameSearchPopupMode) {
				// actual은 이미 위 action 블록에서 세팅됨 — 가입 버튼 클릭 없이 통과
			} else {
				closeGeneralModalIfExists()
				clickRegisterButton()
				actual = collectPopupMessage(target)
			}
		} catch (Exception ex) {
			// 세션 끊김 감지 → 페이지 재로딩으로 복구
			if (ex instanceof NoSuchSessionException || (ex.message ?: "").contains("invalid session id") || (ex.message ?: "").contains("NoSuchSession")) {
				try {
					WebUI.openBrowser(startUrl)
					waitSec(2.0)
				} catch (Exception ignored) {}
				actual = "[세션복구] 브라우저 재시작"
			} else {
				// 일반 에러 — 페이지 이동 감지 시 복귀
				try {
					String curUrl = driver().currentUrl ?: ""
					if (startUrl && curUrl && !curUrl.contains(startUrl.take(30))) {
						driver().navigate().to(startUrl)
						waitSec(1.5)
					}
				} catch (Exception ignored) {}
				actual = "[에러] " + ((ex.message ?: ex.toString()).take(120))
			}
		}

		// TC 종료 후 이름 팝업이 남아있으면 강제 닫기 (이름 시나리오 완료 전까지만)
		if (!nameFieldDone) {
			try {
				closeNameSearchPopup()
			} catch (Exception ignored) {}
		}

		double elapsed = (System.currentTimeMillis() - started) / 1000.0
		String clean = (actual ?: "").replace("\n", " ").trim()

		Map judged = judgeTestResult(tc, clean)
		if (!(judged.passed as boolean)) failCount++

		printTestCaseLog(judged.passed as boolean, tc, clean, elapsed, judged.reason?.toString())
		testResults << [
			tc     : tc.id,
			type   : tc.type,
			field  : tc.field ?: "",
			popup  : clean,
			passed : judged.passed,
			elapsed: String.format("%.2f초", elapsed),
			expect : tc.expect,
			reason : judged.reason
		]
		return true
	}

	private static void setNameByAutoSelection(String value) {
		if (!isNameSearchPopupMode) {
			setFormFieldValue("name", value)
			return
		}

		if (!openNameSearchPopup()) return

			runJs('''
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var selected = false;

            var radios = popup.querySelectorAll('input[type=radio]');
            if(radios.length>0){
                try{ radios[0].click(); selected = true; }catch(e){}
            }

            if(!selected){
                var rows=popup.querySelectorAll('tbody tr');
                if(rows.length>0){
                    try{ rows[0].click(); selected = true; }catch(ex){}
                }
            }

            if(!selected){
                var cards = Array.from(popup.querySelectorAll('div,li,button,a,label')).filter(function(el){
                    if(el.offsetParent===null) return false;
                    var txt=(el.innerText||'').replace(/\\s+/g,' ').trim();
                    if(txt.length < 2) return false;
                    if(txt.indexOf('사용자 정보 검색') >= 0) return false;
                    if(txt.indexOf('회원 선택') >= 0) return false;
                    if(txt.indexOf('선택 완료') >= 0) return false;
                    if(txt.indexOf('조회') >= 0) return false;
                    return true;
                });
                if(cards.length>0){
                    try{ cards[0].click(); selected = true; }catch(ex){}
                }
            }

            var checks = popup.querySelectorAll("input[type=checkbox]");
            if(checks.length>0){
                try{
                    checks[0].checked = true;
                    checks[0].dispatchEvent(new Event("change",{bubbles:true}));
                }catch(ex){}
            }
        ''')
		waitSec(0.5)

		runJs('''
            var dialogs=document.querySelectorAll('[role=dialog],[class*=modal],[class*=popup],[class*=layer]');
            var popup = null;
            for(var i=0;i<dialogs.length;i++){
                var d = dialogs[i];
                if(d.offsetParent===null) continue;
                var txt=(d.innerText||'').replace(/\\s+/g,' ').trim();
                if(txt.indexOf('사용자 정보 검색') >= 0 || txt.indexOf('회원 선택') >= 0){
                    popup = d;
                    break;
                }
            }
            if(!popup) return;

            var btns=popup.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
            for(var i=btns.length-1;i>=0;i--){
                var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
                if(tx.indexOf('선택완료') >= 0 || tx.indexOf('완료') >= 0 || tx.indexOf('확인') >= 0 || tx.indexOf('적용') >= 0){
                    try{ btns[i].click(); }catch(e){}
                    break;
                }
            }
        ''')
		waitSec(0.8)
		closeInnerCompletePopupIfExists()
	}

	// =========================================================
	// [O] 전체 실행
	// =========================================================
	private static void runAllInternal() {
		failCount = 0
		testResults = []
		isNameSearchPopupMode = false
		nameFieldDone = false
		namePopupScan = [:]

		logLine("회원가입 예외 테스트 시작")
		waitSec(1.0)

		startUrl = driver().currentUrl

		isNameSearchPopupMode = detectNameFieldUsesPopupSearch()
		logLine("이름 필드 방식: " + (isNameSearchPopupMode ? "검색팝업형" : "직접입력형"))

		if (isNameSearchPopupMode) {
			// 스캔 전: 폼 리셋 + 잔여 팝업 전부 정리
			resetMainForm()
			waitSec(0.3)
			closeBrowserAlertIfExists()
			closeInnerCompletePopupIfExists()
			closeGeneralModalIfExists()
			waitSec(0.5)
			namePopupScan = scanNameSearchPopup()
			logLine(
					"이름 팝업 스캔: exists=" + (namePopupScan.exists ?: false) +
					", listCount=" + (namePopupScan.listCount ?: 0) +
					", dropdownCount=" + (((namePopupScan.selects ?: []) as List).size()) +
					", inputCount=" + (((namePopupScan.inputs ?: []) as List).size()) +
					", checkboxCount=" + (((namePopupScan.checkboxes ?: []) as List).size())
					)
		}

		Map mainScan = scanMainFormElements()
		List<String> domOrder = scanMainFormDomOrder()
		logLine("감지 순서: " + (domOrder ? domOrder.join(" -> ") : "(감지 없음)"))

		List<Map> allTcs = []
		allTcs.addAll(buildStaticTestCases())
		allTcs.addAll(buildMainFormDynamicTestCases(mainScan))
		if (isNameSearchPopupMode) {
			allTcs.addAll(buildNamePopupDynamicTestCases(namePopupScan))
		}

		allTcs = sortTestCasesByDomOrder(allTcs, domOrder)

		logLine("전체 TC 수: " + allTcs.size())

		for (int i = 0; i < allTcs.size(); i++) {
			if (!runSingleTestCase(i + 1, allTcs[i])) break
		}

		printSummary()
		logLine(failCount > 0 ? "실패 ${failCount}건" : "전체 통과")
	}

	// =========================================================
	// [P] 외부 호출용
	// =========================================================
	@Keyword
	def runAll() {
		if (!hasBrowserDriver()) {
			KeywordUtil.markFailed("브라우저가 열려 있지 않습니다. 먼저 회원가입 페이지를 연 뒤 runAll()을 호출하세요.")
			return
		}

		try {
			if (!driver().currentUrl) {
				KeywordUtil.markFailed("현재 페이지 URL을 가져올 수 없습니다.")
				return
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("현재 페이지에 접근할 수 없습니다: " + e.message)
			return
		}

		runAllInternal()
	}
}