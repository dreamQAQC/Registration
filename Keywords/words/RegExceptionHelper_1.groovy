
//import com.kms.katalon.core.annotation.Keyword
//import com.kms.katalon.core.util.KeywordUtil
//import com.kms.katalon.core.webui.driver.DriverFactory
//import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
//
//import org.openqa.selenium.Alert
//import org.openqa.selenium.By
//import org.openqa.selenium.Keys
//import org.openqa.selenium.WebDriver
//import org.openqa.selenium.WebElement
//import org.openqa.selenium.interactions.Actions
//
//import groovy.json.JsonOutput
//import groovy.json.JsonSlurper
//
//class RegExceptionHelper {
//
//	// =========================================================
//	// [A] 전역 상태
//	// =========================================================
//	private static List<Map> allResults = []
//	private static int totalFail = 0
//	private static String initialUrl = ""
//	private static String confirmedId = ""
//
//	private static String fileInputId = ""
//	private static boolean nameIsSearch = false
//	private static String dummyFilePath = ""
//
//	private static final String FIXED_ID = "testfix01"
//	private static final String FIXED_FILE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "reg_exception_dummy"
//
//	// =========================================================
//	// [B] 공용 유틸
//	// =========================================================
//	private static WebDriver d() {
//		return DriverFactory.getWebDriver()
//	}
//
//	private static void sleep(double sec) {
//		WebUI.delay(sec)
//	}
//
//	private static Object js(String script, List args = []) {
//		return WebUI.executeJavaScript(script, args)
//	}
//
//	private static void log(String msg) {
//		KeywordUtil.logInfo(msg)
//		println(msg)
//	}
//
//	private static String norm(String s) {
//		return (s ?: "").replaceAll("\\s+", " ").trim()
//	}
//
//	private static String safe(String s) {
//		return (s ?: "")
//				.replace("\\", "\\\\")
//				.replace("'", "\\'")
//				.replace("\n", " ")
//	}
//
//	private static boolean blank(String s) {
//		String t = (s ?: "").trim()
//		return !t || t == "[팝업없음]"
//	}
//
//	private static boolean hasDriver() {
//		try {
//			return d() != null
//		} catch (Exception e) {
//			return false
//		}
//	}
//
//	private static boolean isVisible(WebElement el) {
//		try {
//			return el != null && el.isDisplayed()
//		} catch (Exception e) {
//			return false
//		}
//	}
//
//	// =========================================================
//	// [C] 버튼 클릭
//	// =========================================================
//	private static void clickEl(WebElement el) {
//		if (el == null) return
//		try {
//			js("arguments[0].scrollIntoView({block:'center'});", [el])
//			sleep(0.2)
//			try {
//				el.click()
//			} catch (Exception e) {
//				js("arguments[0].click();", [el])
//			}
//			sleep(0.4)
//		} catch (Exception ignore) {}
//	}
//
//	private static WebElement findButtonByText(List<String> keywords) {
//		List<WebElement> all = d().findElements(By.xpath("//button | //a | //input[@type='button'] | //input[@type='submit']"))
//		for (WebElement el : all) {
//			if (!isVisible(el)) continue
//			String txt = norm((el.getText() ?: "") + " " + (el.getAttribute("value") ?: "") + " " + (el.getAttribute("title") ?: ""))
//			String compact = txt.replaceAll("\\s+", "")
//			for (String kw : keywords) {
//				if (compact.contains(kw.replaceAll("\\s+", ""))) {
//					return el
//				}
//			}
//		}
//		return null
//	}
//
//	private static void clickBtnByText(String... keywords) {
//		WebElement btn = findButtonByText(keywords as List<String>)
//		if (btn != null) clickEl(btn)
//	}
//
//	private static void clickRegister() {
//		clickBtnByText("가입하기", "회원가입", "등록", "신청", "제출", "완료")
//	}
//
//	private static void clickDupCheck() {
//		clickBtnByText("중복확인", "중복체크", "중복")
//	}
//
//	private static boolean hasDupBtn() {
//		try {
//			List<WebElement> buttons = d().findElements(By.xpath("//button | //a | //input[@type='button']"))
//			return buttons.any { el ->
//				try {
//					isVisible(el) && norm((el.text ?: "") + " " + (el.getAttribute("value") ?: "")).replaceAll("\\s+", "").contains("중복")
//				} catch (Exception e) {
//					false
//				}
//			}
//		} catch (Exception e) {
//			return false
//		}
//	}
//
//	// =========================================================
//	// [D] 팝업 처리
//	// =========================================================
//	private static void closeAlert() {
//		try {
//			Alert a = d().switchTo().alert()
//			a.accept()
//			sleep(0.2)
//		} catch (Exception ignore) {}
//	}
//
//	private static void closeModals() {
//		closeAlert()
//		js("""
//			document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer]").forEach(function(m){
//				var s=window.getComputedStyle(m); 
//				if(s.display==="none") return;
//				var bs=m.querySelectorAll("button,a,[class*=btn]");
//				for(var i=0;i<bs.length;i++){
//					var t=(bs[i].innerText||bs[i].value||"").replace(/\\s+/g,"");
//					if(t.includes("확인")||t==="OK"||t.includes("닫기")){
//						try{bs[i].click();}catch(e){}
//						break;
//					}
//				}
//			});
//			document.querySelectorAll(".modal,.popup,[class*=modal],[class*=overlay],.modal-backdrop")
//				.forEach(function(e){ try{ e.style.display="none"; }catch(ex){} });
//		""")
//		sleep(0.2)
//	}
//
//	private static void dismiss() {
//		sleep(1.0)
//		closeAlert()
//		try {
//			new Actions(d()).sendKeys(Keys.ESCAPE).perform()
//			sleep(0.1)
//		} catch (Exception ignore) {}
//
//		js("""
//			document.querySelectorAll("[role=dialog],[class*=modal],[class*=popup],[class*=layer]").forEach(function(md){
//				if(window.getComputedStyle(md).display==="none") return;
//				var bs=md.querySelectorAll("button,a,[class*=btn]");
//				for(var i=0;i<bs.length;i++){
//					var t=(bs[i].innerText||bs[i].value||"").replace(/\\s+/g,"");
//					if(t.includes("확인")||t==="OK"||t.includes("닫기")){
//						try{bs[i].click();}catch(e){}
//						break;
//					}
//				}
//			});
//		""")
//		sleep(0.3)
//		closeModals()
//	}
//
//	private static String getPopup(String hint = "", boolean wait = true) {
//		if (wait) sleep(1.4)
//
//		try {
//			new Actions(d()).sendKeys(Keys.ESCAPE).perform()
//			sleep(0.1)
//		} catch (Exception ignore) {}
//
//		try {
//			Alert a = d().switchTo().alert()
//			String t = a.getText()
//			a.accept()
//
//			if ((t ?: "").contains("팝업") || (t ?: "").contains("차단")) {
//				try {
//					Alert a2 = d().switchTo().alert()
//					String t2 = a2.getText()
//					a2.accept()
//					return t2
//				} catch (Exception ignore) {}
//			}
//			return t
//		} catch (Exception ignore) {}
//
//		Object r = js("""
//			function vis(e){
//				var s=window.getComputedStyle(e);
//				return e.offsetParent!==null && s.visibility!=="hidden" && s.display!=="none";
//			}
//			function cl(t){ return (t||"").replace(/\\s+/g," ").trim(); }
//
//			var bs=document.querySelectorAll("button,a,[class*=btn]");
//			for(var i=0;i<bs.length;i++){
//				var b=bs[i], bt=cl(b.innerText||b.value||"");
//				if(vis(b) && (bt.includes("확인")||bt==="OK")){
//					var p=b.parentElement;
//					while(p && p.tagName!=="BODY"){
//						var st=window.getComputedStyle(p);
//						if(st.position==="fixed"||st.position==="absolute"||parseInt(st.zIndex)>0){
//							var raw=cl(p.innerText||"");
//							if(raw.includes("팝업을 열 수 없습니다")){
//								try{b.click();}catch(e){}
//								return "[팝업차단]";
//							}
//							var txt=raw.replace(/확인/g,"").trim();
//							try{b.click();}catch(e){}
//							return "M::"+txt;
//						}
//						p=p.parentElement;
//					}
//				}
//			}
//
//			var ins=document.querySelectorAll("input,select,textarea");
//			for(var k=0;k<ins.length;k++){
//				if(ins[k].validity && !ins[k].validity.valid && ins[k].validationMessage){
//					return "H::"+ins[k].validationMessage;
//				}
//			}
//
//			var kws=["필수","선택","입력","확인","주소","아이디","비밀번호","이름","중복","오류","실패","첨부","업로드","동의"];
//			var ns=Array.from(document.querySelectorAll(
//				"[role=alert],[aria-live],.error,.invalid,.tooltip,.toast,.message,.feedback,[class*=error],[class*=invalid],[class*=warn],[class*=message]"
//			)).filter(vis).map(function(e){ return cl(e.innerText||""); })
//			  .filter(function(t){ return t.length>0 && t.length<=200; });
//
//			for(var n=0;n<ns.length;n++){
//				if(kws.some(function(k){ return ns[n].includes(k); })) return "I::"+ns[n];
//			}
//			return "[팝업없음]";
//		""")
//
//		String result = r?.toString() ?: "[팝업없음]"
//
//		if (result == "[팝업차단]") {
//			sleep(0.3)
//			return getPopup(hint, false)
//		}
//
//		["M::", "H::", "I::"].each { prefix ->
//			if (result.startsWith(prefix)) {
//				result = result.substring(3)
//			}
//		}
//		return result ?: "[팝업없음]"
//	}
//
//	// =========================================================
//	// [E] 폼 초기화 / 기본 세팅
//	// =========================================================
//	private static void resetForm() {
//		try {
//			new Actions(d()).sendKeys(Keys.ESCAPE).perform()
//			sleep(0.1)
//		} catch (Exception ignore) {}
//
//		js("""
//			document.querySelectorAll("form").forEach(function(f){
//				try{f.reset();}catch(e){}
//			});
//
//			document.querySelectorAll("input").forEach(function(i){
//				if(i.type==="checkbox"||i.type==="radio") i.checked=false;
//				else if(i.type!=="file") i.value="";
//				i.dispatchEvent(new Event("input",{bubbles:true}));
//				i.dispatchEvent(new Event("change",{bubbles:true}));
//			});
//
//			document.querySelectorAll("select").forEach(function(s){
//				try{
//					s.selectedIndex=0;
//					s.dispatchEvent(new Event("change",{bubbles:true}));
//				}catch(e){}
//			});
//
//			document.querySelectorAll(".modal,.popup,[role=dialog],[class*=modal],[class*=overlay],.modal-backdrop")
//				.forEach(function(m){ try{m.style.display="none";}catch(e){} });
//		""")
//	}
//
//	private static void fillEmpty() {
//		js("""
//			document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])")
//				.forEach(function(e){
//					if(e.offsetParent===null) return;
//					var id=(e.id||"").toLowerCase();
//					if(id.includes("sample")||id.includes("address")||id.includes("post")) return;
//					if(e.value && e.value.length>0) return;
//
//					e.value = (e.type==="password") ? "Temp123!@" : "dummyData12";
//					e.dispatchEvent(new Event("input",{bubbles:true}));
//					e.dispatchEvent(new Event("change",{bubbles:true}));
//				});
//		""")
//	}
//
//	private static void setField(String ftype, String value) {
//		js("""
//			var t='${safe(ftype)}', v='${safe(value ?: "")}', tgt=null;
//			document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])")
//				.forEach(function(e){
//					if(tgt || e.offsetParent===null) return;
//					var id=(e.id||"").toLowerCase(),
//						nm=(e.name||"").toLowerCase(),
//						ph=(e.placeholder||"").toLowerCase(),
//						lb="";
//					if(e.labels && e.labels.length) lb=e.labels[0].innerText.toLowerCase();
//					else{
//						var p=e.closest("div,tr,li,td");
//						if(p) lb=(p.innerText||"").toLowerCase();
//					}
//
//					if(t==="pwConf" && e.type==="password" && (id.includes("confirm")||nm.includes("confirm")||ph.includes("확인")||ph.includes("재입력"))) tgt=e;
//					else if(t==="pw" && e.type==="password" && !id.includes("confirm") && !nm.includes("confirm") && !ph.includes("확인")) tgt=e;
//					else if(t==="id" && e.type!=="password" && (id.includes("id")||nm.includes("id")||ph.includes("아이디")||lb.includes("아이디"))) tgt=e;
//					else if(t==="name" && e.type!=="password" && (id.includes("name")||nm.includes("name")||ph.includes("이름")||ph.includes("성명")||lb.includes("이름"))) tgt=e;
//					else if(t==="email" && (id.includes("email")||nm.includes("email")||ph.includes("이메일")||ph.includes("@"))) tgt=e;
//					else if(t==="phone" && (id.includes("phone")||nm.includes("phone")||ph.includes("전화")||ph.includes("휴대"))) tgt=e;
//				});
//
//			if(tgt){
//				tgt.value=v;
//				["input","change","blur"].forEach(function(ev){
//					tgt.dispatchEvent(new Event(ev,{bubbles:true}));
//				});
//			}
//		""")
//	}
//
//	// =========================================================
//	// [F] 이름 필드 처리
//	// =========================================================
//	private static boolean detectNameSearchMode() {
//		Object result = js("""
//			var inputs = document.querySelectorAll(
//				"input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox]):not([type=file])"
//			);
//			for(var i=0; i<inputs.length; i++){
//				var e = inputs[i];
//				if(e.offsetParent===null) continue;
//
//				var id=(e.id||"").toLowerCase(),
//					nm=(e.name||"").toLowerCase(),
//					ph=(e.placeholder||"").toLowerCase(),
//					lb="";
//				if(e.labels && e.labels.length) lb=e.labels[0].innerText.toLowerCase();
//
//				var isName = id.includes("name") || nm.includes("name") ||
//							 ph.includes("이름") || ph.includes("성명") ||
//							 lb.includes("이름") || lb.includes("성명") ||
//							 lb.includes("검색 클릭") || lb.includes("(검색");
//
//				if(!isName) continue;
//
//				var btn = null;
//				var sib = e.nextElementSibling;
//				while(sib){
//					if((sib.matches('button,[role=button],input[type=button]')) && sib.offsetParent!==null){ btn=sib; break; }
//					var inner = sib.querySelector('button,[role=button],input[type=button]');
//					if(inner && inner.offsetParent!==null){ btn=inner; break; }
//					sib = sib.nextElementSibling;
//				}
//
//				if(!btn){
//					sib = e.previousElementSibling;
//					while(sib){
//						if((sib.matches('button,[role=button],input[type=button]')) && sib.offsetParent!==null){ btn=sib; break; }
//						var inner2 = sib.querySelector('button,[role=button],input[type=button]');
//						if(inner2 && inner2.offsetParent!==null){ btn=inner2; break; }
//						sib = sib.previousElementSibling;
//					}
//				}
//				return btn ? "true" : "false";
//			}
//			return "false";
//		""")
//		return "true".equalsIgnoreCase(result?.toString())
//	}
//
//	private static void setName(String value = "홍길동") {
//		if (!nameIsSearch) {
//			setField("name", value)
//			return
//		}
//
//		// 검색팝업 방식이면 기본 세팅 시 실제 선택까지 진행
//		if (openNameModal()) {
//			Object selected = js("""
//				var m=document.querySelector('[role=dialog],[class*=modal],[class*=Modal],[class*=popup],[class*=Popup],[class*=layer],[class*=Layer],[class*=Dialog]');
//				if(!m) m=document;
//
//				var rs=m.querySelectorAll('input[type=radio]');
//				if(rs.length>0){ rs[0].click(); return 'radio:'+rs.length; }
//
//				var rows=m.querySelectorAll('tbody tr');
//				if(rows.length>0){ try{rows[0].click();}catch(ex){} return 'tr:'+rows.length; }
//
//				var cs=m.querySelectorAll('input[type=checkbox]');
//				if(cs.length>0){ cs[0].click(); return 'checkbox:'+cs.length; }
//
//				return 'none';
//			""")
//			log("   모달 항목 선택: ${selected}")
//			sleep(0.5)
//
//			js("""
//				var m=document.querySelector('[role=dialog],[class*=modal],[class*=Modal],[class*=popup],[class*=layer],[class*=Dialog]');
//				if(!m) m=document;
//				var btns=m.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
//				for(var i=btns.length-1;i>=0;i--){
//					var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
//					if(tx.includes('선택완료')||tx.includes('완료')||tx.includes('확인')||tx.includes('적용')){
//						try{btns[i].click();}catch(ex){}
//						break;
//					}
//				}
//			""")
//			sleep(0.8)
//			log("   선택완료 클릭")
//		}
//	}
//
//	private static boolean openNameModal() {
//		Object btn = js("""
//			var inputs = document.querySelectorAll(
//				"input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox]):not([type=file])"
//			);
//			for(var i=0; i<inputs.length; i++){
//				var e=inputs[i];
//				if(e.offsetParent===null) continue;
//
//				var id=(e.id||"").toLowerCase(),
//					nm=(e.name||"").toLowerCase(),
//					ph=(e.placeholder||"").toLowerCase(),
//					lb="";
//				if(e.labels&&e.labels.length) lb=e.labels[0].innerText.toLowerCase();
//
//				var isName=id.includes("name")||nm.includes("name")||
//						   ph.includes("이름")||ph.includes("성명")||
//						   lb.includes("이름")||lb.includes("성명")||
//						   lb.includes("검색 클릭")||lb.includes("(검색");
//				if(!isName) continue;
//
//				var btn=null;
//				var sib=e.nextElementSibling;
//				while(sib){
//					if(sib.matches('button,[role=button],input[type=button]')&&sib.offsetParent!==null){btn=sib;break;}
//					var inner=sib.querySelector('button,[role=button],input[type=button]');
//					if(inner&&inner.offsetParent!==null){btn=inner;break;}
//					sib=sib.nextElementSibling;
//				}
//				if(!btn){
//					sib=e.previousElementSibling;
//					while(sib){
//						if(sib.matches('button,[role=button],input[type=button]')&&sib.offsetParent!==null){btn=sib;break;}
//						var inner2=sib.querySelector('button,[role=button],input[type=button]');
//						if(inner2&&inner2.offsetParent!==null){btn=inner2;break;}
//						sib=sib.previousElementSibling;
//					}
//				}
//				return btn;
//			}
//			return null;
//		""")
//
//		if (!(btn instanceof WebElement)) return false
//
//		try {
//			((WebElement) btn).click()
//		} catch (Exception e) {
//			js("arguments[0].click();", [btn])
//		}
//		sleep(2.0)
//		return true
//	}
//
//	private static void closeNameModal() {
//		try {
//			js("""
//				var modal=document.querySelector('[role=dialog],[class*=modal],[class*=Modal],[class*=popup],[class*=Dialog],[class*=layer]');
//				if(!modal) return;
//
//				var closes=modal.querySelectorAll('[class*=close],[class*=Close],[class*=dismiss],[aria-label*=닫기]');
//				if(closes.length>0){
//					try{closes[0].click();}catch(e){}
//					return;
//				}
//
//				var btns=modal.querySelectorAll('button,input[type=button],input[type=submit]');
//				for(var i=0;i<btns.length;i++){
//					var tx=(btns[i].innerText||btns[i].value||'').trim();
//					if(tx==='×'||tx==='✕'||tx==='X'||tx==='닫기'){
//						try{btns[i].click();}catch(e){}
//						return;
//					}
//				}
//			""")
//		} catch (Exception ignore) {}
//
//		try {
//			d().findElement(By.tagName("body")).sendKeys(Keys.ESCAPE)
//		} catch (Exception ignore) {}
//		sleep(0.5)
//	}
//
//	// =========================================================
//	// [G] 체크박스 / 라디오 / 버튼그룹
//	// =========================================================
//	private static void checkAll() {
//		js("""
//			document.querySelectorAll("input[type=checkbox]").forEach(function(c){
//				c.checked=true;
//				c.dispatchEvent(new Event("change",{bubbles:true}));
//			});
//		""")
//	}
//
//	private static void uncheckAll() {
//		js("""
//			document.querySelectorAll("input[type=checkbox]").forEach(function(c){
//				c.checked=false;
//				c.dispatchEvent(new Event("change",{bubbles:true}));
//			});
//		""")
//	}
//
//	private static void checkByIdx(int i, boolean on = true) {
//		js("""
//			var c=document.querySelectorAll("input[type=checkbox]");
//			if(c.length>${i}){
//				c[${i}].checked=${on ? 'true' : 'false'};
//				c[${i}].dispatchEvent(new Event("change",{bubbles:true}));
//			}
//		""")
//	}
//
//	private static void checkOnlyFirst() {
//		uncheckAll()
//		checkByIdx(0, true)
//	}
//
//	private static void clearBtnGroup() {
//		js("""
//			document.querySelectorAll("input[type=radio]").forEach(function(r){
//				r.checked=false;
//				r.dispatchEvent(new Event("change",{bubbles:true}));
//			});
//			document.querySelectorAll("button,[role=button],label").forEach(function(el){
//				if(el.offsetParent===null) return;
//				try{el.classList.remove("active","selected","checked","on");}catch(e){}
//			});
//		""")
//	}
//
//	private static void selectFirstBtn() {
//		js("""
//			var rs=document.querySelectorAll("input[type=radio]");
//			if(rs.length>0){
//				rs[0].checked=true;
//				rs[0].dispatchEvent(new Event("change",{bubbles:true}));
//				var l=rs[0].closest("label");
//				if(l) try{l.click();}catch(e){}
//				return;
//			}
//
//			var groups={};
//			document.querySelectorAll("button,[role=button]").forEach(function(b){
//				if(b.offsetParent===null) return;
//				var tx=(b.innerText||"").trim();
//				if(!tx || tx.length>10) return;
//				if(["중복","확인","가입","등록","신청","제출"].some(function(k){ return tx.includes(k); })) return;
//				var k=(b.parentElement && (b.parentElement.className||b.parentElement.id)) || "g";
//				if(!groups[k]) groups[k]=[];
//				groups[k].push(b);
//			});
//
//			for(var k in groups){
//				if(groups[k].length>=2){
//					try{groups[k][0].click();}catch(e){}
//					break;
//				}
//			}
//		""")
//	}
//
//	private static void selectBtnOption(String val) {
//		List<String> p = (val ?: "").split("::") as List<String>
//		if (p.size() < 2) return
//
//		String name = p[0]
//		int idx = (p[1] ?: "0") as int
//
//		js("""
//			var rs=document.querySelectorAll('input[type=radio][name="${safe(name)}"]');
//			if(rs.length>${idx}){
//				rs[${idx}].checked=true;
//				rs[${idx}].dispatchEvent(new Event("change",{bubbles:true}));
//				var l=rs[${idx}].closest("label");
//				if(l) try{l.click();}catch(e){}
//			}else{
//				var bs=Array.from(document.querySelectorAll("button,[role=button]")).filter(function(b){
//					return b.offsetParent!==null &&
//						   (b.innerText||"").trim().length>0 &&
//						   (b.innerText||"").trim().length<=10;
//				});
//				if(bs.length>${idx}) try{bs[${idx}].click();}catch(e){}
//			}
//		""")
//		sleep(0.2)
//	}
//
//	// =========================================================
//	// [H] 드롭박스
//	// =========================================================
//	private static void autoSelectDropdowns() {
//		js("""
//			function vis(e){
//				var s=window.getComputedStyle(e);
//				return e.offsetParent!==null && s.display!=="none" && s.visibility!=="hidden";
//			}
//			document.querySelectorAll("select").forEach(function(s){
//				if(!vis(s) || !s.options || s.options.length<=1 || s.selectedIndex>0) return;
//				for(var i=1;i<s.options.length;i++){
//					var v=(s.options[i].value||"").trim(),
//						t=(s.options[i].text||"").trim();
//					if(v && !t.includes("선택") && !t.includes("--")){
//						s.selectedIndex=i;
//						s.dispatchEvent(new Event("change",{bubbles:true}));
//						break;
//					}
//				}
//			});
//		""")
//	}
//
//	private static void resetSelect(String sid) {
//		js("""
//			var s=document.getElementById('${safe(sid)}') || document.querySelector('select[name="${safe(sid)}"]');
//			if(s){
//				s.selectedIndex=0;
//				s.dispatchEvent(new Event("change",{bubbles:true}));
//			}
//		""")
//	}
//
//	private static void selectOption(String val) {
//		List<String> p = (val ?: "").split("::") as List<String>
//		if (p.size() < 2) return
//
//		String sid = p[0]
//		int idx = (p[1] ?: "0") as int
//
//		js("""
//			var s=document.getElementById('${safe(sid)}') || document.querySelector('select[name="${safe(sid)}"]');
//			if(s && s.options.length>${idx}){
//				s.selectedIndex=${idx};
//				s.dispatchEvent(new Event("change",{bubbles:true}));
//			}
//		""")
//	}
//
//	// =========================================================
//	// [I] 파일 첨부
//	// =========================================================
//	private static void clearFile() {
//		try {
//			d().findElements(By.cssSelector("input[type='file']")).each { WebElement f ->
//				try {
//					js("""
//						var o=arguments[0];
//						try{o.value='';}catch(e){}
//						var n=o.cloneNode(true);
//						o.parentNode.replaceChild(n,o);
//					""", [f])
//				} catch (Exception ignore) {}
//			}
//		} catch (Exception ignore) {}
//
//		js("""
//			document.querySelectorAll("input[type=file]").forEach(function(f){
//				try{f.value="";}catch(e){}
//				try{f.dispatchEvent(new Event("change",{bubbles:true}));}catch(e){}
//			});
//			document.querySelectorAll("[class*=delete],[class*=remove],[class*=clear]").forEach(function(b){
//				var p=b.closest("[class*=file],[class*=upload]");
//				if(p) try{b.click();}catch(e){}
//			});
//		""")
//		sleep(0.2)
//	}
//
//	private static String makeDummyFile(boolean forceImage = false) {
//		File dir = new File(FIXED_FILE_DIR)
//		if (!dir.exists()) dir.mkdirs()
//
//		List<String> imgExts = [".png", ".jpg", ".gif", ".bmp"]
//		List<String> allExts = imgExts + [".pdf", ".txt", ".zip"]
//		String ext = forceImage ? imgExts[new Random().nextInt(imgExts.size())] : allExts[new Random().nextInt(allExts.size())]
//
//		File file = new File(dir, "test_auto${ext}")
//
//		switch (ext) {
//			case ".png":
//				file.bytes = [
//					0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D,
//					0x49,0x48,0x44,0x52,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,
//					0x08,0x02,0x00,0x00,0x00,0x90,0x77,0x53,0xDE,0x00,0x00,0x00,
//					0x0C,0x49,0x44,0x41,0x54,0x08,0xD7,0x63,0xF8,0xFF,0xFF,0x3F,
//					0x00,0x05,0xFE,0x02,0xFE,0xA7,0x35,0x81,0x84,0x00,0x00,0x00,
//					0x00,0x49,0x45,0x4E,0x44,0xAE,0x42,0x60,0x82
//				].collect { it as byte } as byte[]
//				break
//			case ".jpg":
//				file.bytes = [
//					0xFF,0xD8,0xFF,0xE0,0x00,0x10,0x4A,0x46,0x49,0x46,
//					0x00,0x01,0x01,0x00,0x00,0x01,0x00,0x01,0x00,0x00,0xFF,0xD9
//				].collect { it as byte } as byte[]
//				break
//			case ".gif":
//				file.bytes = [
//					0x47,0x49,0x46,0x38,0x39,0x61,0x01,0x00,0x01,0x00,
//					0x00,0xFF,0x00,0x2C,0x00,0x00,0x00,0x00,0x01,0x00,
//					0x01,0x00,0x00,0x02,0x00,0x3B
//				].collect { it as byte } as byte[]
//				break
//			case ".pdf":
//				file.text = "%PDF-1.0\n1 0 obj<</Type/Catalog>>endobj\nxref\n0 0\ntrailer<</Root 1 0 R>>\n%%EOF", "UTF-8"
//				break
//			default:
//				file.text = "dummy test file for automation", "UTF-8"
//				break
//		}
//
//		log("📎 더미 파일 생성: ${file.absolutePath}")
//		return file.absolutePath
//	}
//
//	private static void scanFileInput() {
//		try {
//			Object result = js("""
//				var found=[];
//				document.querySelectorAll('input[type=file]').forEach(function(e){
//					found.push({
//						id: e.id || '',
//						name: e.name || '',
//						accept: e.getAttribute('accept') || '',
//						cls: e.className || ''
//					});
//				});
//				return JSON.stringify(found);
//			""")
//
//			List<Map> inputs = (List<Map>) new JsonSlurper().parseText(result?.toString() ?: "[]")
//			if (!inputs) {
//				log("⚠️ file input 없음")
//				return
//			}
//
//			log("🔍 file input 감지 (${inputs.size()}개):")
//			inputs.each { Map inp ->
//				log("   id=${inp.id ?: '없음'}  accept=${inp.accept ?: '없음'}  class=${(inp.cls ?: '').toString().take(30)}")
//			}
//
//			Map best = inputs.find { (it.id ?: "").toString().trim() } ?: inputs[0]
//			fileInputId = (best.id ?: best.name ?: "").toString()
//			String accept = (best.accept ?: "").toString().toLowerCase()
//
//			boolean needImage = accept.contains("image")
//			dummyFilePath = makeDummyFile(needImage)
//
//			log("✅ file input id='${fileInputId}' accept='${accept}'")
//			log("📎 더미 파일 생성: ${dummyFilePath}")
//		} catch (Exception e) {
//			log("⚠️ scanFileInput 실패: ${e.message}")
//		}
//	}
//
//	private static void attachFile() {
//		try {
//			File f = (dummyFilePath ? new File(dummyFilePath) : null)
//			String path = (f != null && f.exists()) ? f.absolutePath : makeDummyFile(false)
//			String name = new File(path).name
//
//			log("📂 첨부 파일: ${path}")
//
//			List<WebElement> fileInputs = d().findElements(By.cssSelector("input[type='file']"))
//			for (WebElement fi : fileInputs) {
//				try {
//					js("""
//						var e=arguments[0];
//						e.classList.remove('hidden','sr-only','d-none','invisible','visually-hidden');
//						e.style.removeProperty('display');
//						e.style.removeProperty('visibility');
//						e.style.removeProperty('opacity');
//						e.removeAttribute('hidden');
//						e.removeAttribute('disabled');
//						e.readOnly=false;
//					""", [fi])
//
//					sleep(0.2)
//					fi.sendKeys(path)
//					sleep(0.8)
//
//					js("""
//						arguments[0].dispatchEvent(new Event('change',{bubbles:true}));
//						arguments[0].dispatchEvent(new Event('input',{bubbles:true}));
//					""", [fi])
//
//					sleep(0.3)
//					log("✅ [send_keys] 성공: ${name}")
//					return
//				} catch (Exception e) {
//					log("   send_keys 실패: ${e.message}")
//				}
//			}
//
//			log("⚠️ 파일 첨부 실패 — input[type=file] sendKeys 방식 실패")
//		} catch (Exception e) {
//			log("⚠️ attachFile 실패: ${e.message}")
//		}
//	}
//
//	// =========================================================
//	// [J] 주소
//	// =========================================================
//	private static void closeDaumPopup() {
//		try {
//			String main = d().getWindowHandle()
//			for (String handle : d().getWindowHandles()) {
//				if (handle == main) continue
//				d().switchTo().window(handle)
//				String cur = d().getCurrentUrl()?.toLowerCase()
//				if (cur.contains("postcode") || cur.contains("daum") || cur.contains("about:blank")) {
//					d().close()
//					log("   Daum 우편번호 팝업 닫음")
//				}
//			}
//			d().switchTo().window(main)
//		} catch (Exception ignore) {}
//	}
//
//	private static void setAddrDummy() {
//		closeDaumPopup()
//		js("""
//			function vis(e){ return e && e.offsetParent!==null; }
//			function sv(e,v){
//				if(!e) return;
//				var ro=e.readOnly, di=e.disabled;
//				e.readOnly=false; e.disabled=false;
//				e.value=v;
//				e.dispatchEvent(new Event("input",{bubbles:true}));
//				e.dispatchEvent(new Event("change",{bubbles:true}));
//				e.readOnly=ro; e.disabled=di;
//			}
//			function fp(t){
//				for(var i of document.querySelectorAll("input")){
//					if(vis(i) && (i.placeholder||"").includes(t)) return i;
//				}
//				return null;
//			}
//			function fn(ks){
//				for(var i of document.querySelectorAll("input")){
//					if(!vis(i)) continue;
//					var id=(i.id||"").toLowerCase(), nm=(i.name||"").toLowerCase();
//					for(var k of ks) if(id.includes(k)||nm.includes(k)) return i;
//				}
//				return null;
//			}
//			var z=fp("우편번호")||fn(["post","zip","zipcode"]);
//			var r=fp("도로명주소")||fp("도로명")||fn(["road","addr1","address","roadaddr"]);
//			var j=fp("지번주소")||fp("지번")||fn(["jibun","addr2","jibunaddr"]);
//			var d=fp("상세주소")||fp("상세")||fn(["detail","addr3","detailaddr","addrdetail"]);
//
//			if(z && !(z.value||"").trim()) sv(z,"06000");
//			if(r && !(r.value||"").trim()) sv(r,"서울특별시 강남구 테헤란로 123");
//			if(j && !(j.value||"").trim()) sv(j,"서울특별시 강남구 역삼동 123-45");
//			if(d && !(d.value||"").trim()) sv(d,"101동 101호");
//		""")
//	}
//
//	private static void setAddrCase(String mode) {
//		js("""
//			["우편번호","도로명","지번","상세","post","zip","road","addr","address","detail"].forEach(function(k){
//				document.querySelectorAll("input").forEach(function(i){
//					if(i.offsetParent===null) return;
//					var ph=(i.placeholder||"").toLowerCase(),
//						id=(i.id||"").toLowerCase(),
//						nm=(i.name||"").toLowerCase();
//					if(ph.includes(k)||id.includes(k)||nm.includes(k)){
//						i.value="";
//						i.dispatchEvent(new Event("input",{bubbles:true}));
//					}
//				});
//			});
//		""")
//
//		if (mode == "clear") return
//
//		js("""
//			function vis(e){ return e&&e.offsetParent!==null; }
//			function sv(e,v){
//				if(!e) return;
//				e.value=v;
//				e.dispatchEvent(new Event("input",{bubbles:true}));
//				e.dispatchEvent(new Event("change",{bubbles:true}));
//			}
//			function fp(t){
//				for(var i of document.querySelectorAll("input")){
//					if(vis(i) && (i.placeholder||"").includes(t)) return i;
//				}
//				return null;
//			}
//			function fn(ks){
//				for(var i of document.querySelectorAll("input")){
//					if(!vis(i)) continue;
//					var id=(i.id||"").toLowerCase(), nm=(i.name||"").toLowerCase();
//					for(var k of ks) if(id.includes(k)||nm.includes(k)) return i;
//				}
//				return null;
//			}
//
//			var z=fp("우편번호")||fn(["post","zip"]);
//			var r=fp("도로명주소")||fn(["road","addr1","address"]);
//			var d=fp("상세주소")||fn(["detail","addr3"]);
//			var m='${safe(mode)}';
//
//			if(m==="zipOnly") sv(z,"06000");
//			if(m==="roadOnly") sv(r,"서울특별시 강남구 테헤란로 123");
//			if(m==="detailOnly") sv(d,"101동 101호");
//		""")
//	}
//
//	// =========================================================
//	// [K] 필수값 세팅
//	// =========================================================
//	private static void setRequired() {
//		[ this.&selectFirstBtn, this.&setAddrDummy, this.&autoSelectDropdowns ].each { Closure c ->
//			try { c.call() } catch (Exception ignore) {}
//		}
//	}
//
//	private static void backIfNeeded() {
//		try {
//			if (initialUrl && d().getCurrentUrl() != initialUrl) {
//				d().get(initialUrl)
//				sleep(2.0)
//			}
//		} catch (Exception ignore) {}
//	}
//
//	// =========================================================
//	// [L] 테스트 케이스 빌드
//	// =========================================================
//	private static Map tc(String typ, String desc, String e, String tgt, String val, String exp) {
//		return [
//			id    : "TC-00",
//			type  : typ,
//			desc  : desc,
//			e     : e,
//			target: tgt,
//			value : val,
//			expect: exp
//		]
//	}
//
//	private static List<Map> buildStaticTcs() {
//		String F = "실패"
//		String S = "성공"
//
//		return [
//			// 이름
//			tc("단위-이름","빈 값","이름","name","",F),
//			tc("단위-이름","숫자","이름","name","123",F),
//			tc("단위-이름","공백","이름","name","홍 길동",F),
//			tc("단위-이름","이모지","이름","name","홍길동😊",F),
//			tc("단위-이름","51자초과","이름","name","홍"*51,F),
//			tc("단위-이름","특수문자","이름","name","홍#길동",F),
//			tc("단위-이름","2자정상","이름","name","김철",S),
//
//			// 아이디
//			tc("단위-아이디","빈 값","아이디","id","",F),
//			tc("단위-아이디","3자미달","아이디","id","tes",F),
//			tc("단위-아이디","한글","아이디","id","관리자12",F),
//			tc("단위-아이디","이모지","아이디","id","✨✨123",F),
//			tc("단위-아이디","특수문자","아이디","id","user!@#",F),
//			tc("단위-아이디","공백","아이디","id","use 01",F),
//			tc("단위-아이디","50자초과","아이디","id","a"*50,F),
//			tc("단위-아이디","6자정상","아이디","id","user12",S),
//
//			// 비밀번호
//			tc("단위-비밀번호","빈 값","비밀번호","pw","",F),
//			tc("단위-비밀번호","7자미달","비밀번호","pw","123456",F),
//			tc("단위-비밀번호","영문만","비밀번호","pw","password",F),
//			tc("단위-비밀번호","숫자만","비밀번호","pw","12345678",F),
//			tc("단위-비밀번호","특수만","비밀번호","pw","!@#$%^&*",F),
//			tc("단위-비밀번호","공백","비밀번호","pw","Pass 12!",F),
//			tc("단위-비밀번호","영숫자만","비밀번호","pw","pass1234",F),
//			tc("단위-비밀번호","한글","비밀번호","pw","비밀123!@",F),
//			tc("단위-비밀번호","8자정상","비밀번호","pw","Test12!@",S),
//
//			// 비밀번호 확인
//			tc("단위-비번확인","빈 값","비밀번호","pwConf","",F),
//			tc("단위-비번확인","불일치","비밀번호","pwConf","wrong!@#1",F),
//			tc("단위-비번확인","한글","비밀번호","pwConf","비밀123!@",F),
//			tc("단위-비번확인","공백만","비밀번호","pwConf","       ",F),
//			tc("단위-비번확인","대소불일치","비밀번호","pwConf","test123!@",F),
//			tc("단위-비번확인","정상일치","비밀번호","pwConf","Test123!@",S),
//
//			// 주소
//			tc("단위-주소","전체미입력","주소","address","clear",F),
//			tc("단위-주소","우편번호만","주소","address","zipOnly",F),
//			tc("단위-주소","도로명만","주소","address","roadOnly",F),
//			tc("단위-주소","상세만","주소","address","detailOnly",F),
//
//			// 시나리오
//			tc("시나리오","전체빈값","이름","clear","clear",F),
//			tc("시나리오","중복확인생략","중복확인","skipDupCheck","testus01",F),
//			tc("시나리오","ID변경","중복확인","id","user02",F),
//			tc("시나리오","ID삭제","아이디","id","",F),
//			tc("시나리오","중복3회","중복확인","tripledup","tripledup",S),
//			tc("시나리오","ID공백","중복확인","id"," use 01 ",F),
//			tc("시나리오","전체재시도","이름","clear","clear",F),
//			tc("시나리오","ID대소문자","중복확인","id","USER01",S),
//
//			// 성공
//			tc("성공","정상가입","성공","success","success",S)
//		]
//	}
//
//	private static Map scanForm() {
//		try {
//			Object r = js("""
//				function vis(e){
//					var s=window.getComputedStyle(e);
//					return e.offsetParent!==null &&
//						   s.display!=="none" &&
//						   s.visibility!=="hidden" &&
//						   !e.closest("header,footer,nav");
//				}
//				function tx(e){ return (e.innerText||"").replace(/\\s+/g," ").trim(); }
//
//				var R={groups:[],selects:[],checkboxes:[],hasFile:false};
//
//				var rn={};
//				document.querySelectorAll("input[type=radio]").forEach(function(r){
//					if(!vis(r)) return;
//					var n=r.name||"r";
//					if(!rn[n]) rn[n]=[];
//					var l="";
//					if(r.labels&&r.labels.length) l=tx(r.labels[0]);
//					else {
//						var p=r.closest("label");
//						if(p) l=tx(p);
//					}
//					rn[n].push(l||r.value||"옵션");
//				});
//
//				for(var n in rn){
//					var ge=document.querySelector('input[type=radio][name="'+n+'"]');
//					if(!ge||!vis(ge)) continue;
//					var c=ge.closest("div,fieldset"), gl=n;
//					if(c){
//						var pv=c.previousElementSibling;
//						if(pv&&vis(pv)) gl=tx(pv).replace("*","").trim();
//					}
//					R.groups.push({name:n,label:gl,options:Array.from(new Set(rn[n])),type:"radio"});
//				}
//
//				var ss={};
//				document.querySelectorAll("select").forEach(function(s){
//					if(!vis(s)||!s.options||s.options.length<=1) return;
//
//					var opts=[];
//					for(var i=0;i<s.options.length;i++){
//						opts.push({value:s.options[i].value,text:tx(s.options[i])});
//					}
//
//					var l="";
//					if(s.labels&&s.labels.length&&vis(s.labels[0])) l=tx(s.labels[0]).replace("*","").trim();
//					else{
//						var pv=s.previousElementSibling;
//						if(pv&&pv.tagName!=="SELECT"&&vis(pv)) l=tx(pv).replace("*","").trim();
//					}
//
//					var key=(s.id||"")+(s.name||"")+l;
//					if(ss[key]) return;
//					ss[key]=1;
//
//					if(!l) l=s.id||s.name||"드롭박스";
//					R.selects.push({id:s.id||"",name:s.name||"",label:l,options:opts});
//				});
//
//				document.querySelectorAll("input[type=checkbox]").forEach(function(cb){
//					if(!vis(cb)) return;
//					var l="";
//					if(cb.labels&&cb.labels.length&&vis(cb.labels[0])) l=tx(cb.labels[0]);
//					else{
//						var p=cb.closest("label");
//						if(p&&vis(p)) l=tx(p);
//					}
//					R.checkboxes.push({id:cb.id||"",name:cb.name||"",label:l,index:R.checkboxes.length});
//				});
//
//				R.hasFile = Array.from(document.querySelectorAll("input[type=file]")).some(vis);
//				return JSON.stringify(R);
//			""")
//
//			return (Map) new JsonSlurper().parseText(r?.toString() ?: "{}")
//		} catch (Exception e) {
//			return [groups:[], selects:[], checkboxes:[], hasFile:false]
//		}
//	}
//
//	private static boolean hasVis(String css) {
//		try {
//			return d().findElements(By.cssSelector(css)).any { isVisible(it) }
//		} catch (Exception e) {
//			return false
//		}
//	}
//
//	private static List<Map> buildDynamicTcs(Map scan) {
//		List<Map> tcs = []
//		String F = "실패"
//		String S = "성공"
//
//		(scan.groups ?: []).each { Map g ->
//			String lb = (g.label ?: "선택항목").toString()
//			List opts = (g.options ?: []) as List
//			if (opts.size() < 2) return
//
//			tcs << tc("단위-${lb}", "${lb} 미선택", lb, "btnGroupClear", g.name?.toString(), F)
//			opts.eachWithIndex { Object o, int i ->
//				tcs << tc("단위-${lb}", "${lb} [${o}]", lb, "btnGroupSelect", "${g.name}::${i}", S)
//			}
//		}
//
//		(scan.selects ?: []).each { Map sel ->
//			String lb = (sel.label ?: "드롭박스").toString()
//			String sid = (sel.id ?: sel.name ?: "").toString()
//			if (!sid) return
//
//			tcs << tc("단위-${lb}", "${lb} 미선택", lb, "selectReset", sid, F)
//
//			List opts = (sel.options ?: []) as List
//			opts.eachWithIndex { Object obj, int i ->
//				Map o = obj as Map
//				String txt = (o.text ?: "").toString()
//				if (i == 0 || !txt.trim() || txt.contains("선택")) return
//				tcs << tc("단위-${lb}", "${lb} [${txt}]", lb, "selectOption", "${sid}::${i}", S)
//			}
//		}
//
//		if ((scan.hasFile ?: false) && hasVis("input[type='file']")) {
//			tcs << tc("단위-파일", "미첨부", "파일", "fileClear", "clear", F)
//			tcs << tc("단위-파일", "첨부", "파일", "fileAttach", "attach", S)
//		}
//
//		List<Map> cbs = (scan.checkboxes ?: []) as List<Map>
//		if (cbs && hasVis("input[type='checkbox']")) {
//			tcs << tc("단위-약관", "전체미체크", "약관", "uncheckAll", "all", F)
//			tcs << tc("단위-약관", "전체체크", "약관", "checkAllBox", "all", S)
//
//			if (((cbs[0]?.label ?: "") as String).contains("전체")) {
//				tcs << tc("단위-약관", "전체동의만", "약관", "checkOnlyFirst", "0", S)
//			}
//
//			cbs.eachWithIndex { Map cb, int i ->
//				if (((cb.label ?: "") as String).contains("전체")) return
//				String lbl = ((cb.label ?: "항목${i}") as String).take(15)
//				tcs << tc("단위-약관", "[${lbl}] 미체크", "약관", "uncheckOne", "${i}", F)
//				tcs << tc("단위-약관", "[${lbl}] 체크", "약관", "checkOne", "${i}", S)
//			}
//		}
//
//		if (nameIsSearch) {
//			tcs << [id:"TC-00", type:"단위-이름검색", desc:"이름 미선택 후 선택완료", e:"이름", target:"nameModalNoSelect", value:"", expect:"실패"]
//			tcs << [id:"TC-00", type:"단위-이름검색", desc:"이름 선택 후 선택완료", e:"이름", target:"nameModalSelect", value:"first", expect:"성공"]
//		}
//
//		return tcs
//	}
//
//	// =========================================================
//	// [M] DOM 순서
//	// =========================================================
//	private static List<String> getDomOrder() {
//		try {
//			Object r = js("""
//				var o=[], s={};
//				function p(k){ if(!s[k]){ s[k]=1; o.push(k); } }
//				function vis(e){
//					if(!e) return false;
//					var st=window.getComputedStyle(e);
//					return e.offsetParent!==null && st.display!=="none" && st.visibility!=="hidden";
//				}
//
//				var all=[];
//
//				document.querySelectorAll("input:not([type=hidden]):not([type=radio]):not([type=checkbox]):not([type=button]):not([type=submit]):not([type=file])")
//					.forEach(function(e){
//						if(vis(e)) all.push({e:e,t:"input"});
//					});
//
//				var rg={};
//				document.querySelectorAll("input[type=radio]").forEach(function(e){
//					if(!vis(e)) return;
//					var n=e.name||"r";
//					if(!rg[n]){
//						rg[n]=1;
//						all.push({e:e,t:"radio"});
//					}
//				});
//
//				document.querySelectorAll("input[type=file]").forEach(function(e){
//					var pr=e.closest("div,label");
//					all.push({e:(pr&&vis(pr))?pr:e,t:"file"});
//				});
//
//				document.querySelectorAll("select").forEach(function(e){
//					if(vis(e)&&e.options&&e.options.length>1) all.push({e:e,t:"select"});
//				});
//
//				var cbf=false;
//				document.querySelectorAll("input[type=checkbox]").forEach(function(e){
//					if(!vis(e)||cbf) return;
//					all.push({e:e,t:"cb"});
//					cbf=true;
//				});
//
//				all.sort(function(a,b){
//					var ra=a.e.getBoundingClientRect(), rb=b.e.getBoundingClientRect();
//					return Math.abs(ra.top-rb.top)>10 ? ra.top-rb.top : ra.left-rb.left;
//				});
//
//				for(var i=0;i<all.length;i++){
//					var it=all[i], el=it.e;
//					if(it.t==="input"){
//						var id=(el.id||"").toLowerCase(),
//							nm=(el.name||"").toLowerCase(),
//							ph=(el.placeholder||"").toLowerCase();
//
//						if(el.type==="password"){
//							(id.includes("confirm")||nm.includes("confirm")||ph.includes("확인")) ? p("pwConf") : p("pw");
//						}else if(id.includes("email")||nm.includes("email")||ph.includes("이메일")||ph.includes("@")) p("email");
//						else if(id.includes("id")||nm.includes("id")||ph.includes("아이디")) p("id");
//						else if(id.includes("name")||nm.includes("name")||ph.includes("이름")) p("name");
//						else if(id.includes("phone")||nm.includes("phone")||ph.includes("전화")) p("phone");
//						else if(id.includes("addr")||nm.includes("addr")||ph.includes("주소")) p("address");
//					}else if(it.t==="radio") p("btnGroup");
//					else if(it.t==="file") p("file");
//					else if(it.t==="select") p("select_"+(el.id||el.name||i));
//					else if(it.t==="cb") p("terms");
//				}
//				return o.join(",");
//			""")
//
//			return (r?.toString() ?: "").split(",").findAll { it?.trim() }
//		} catch (Exception e) {
//			return []
//		}
//	}
//
//	private static List<Map> sortTcs(List<Map> list, List<String> domOrder) {
//		def domIdx = { String key ->
//			for (int i = 0; i < domOrder.size(); i++) {
//				String x = domOrder[i]
//				if (x == key || x.startsWith(key)) return i
//			}
//			return 100
//		}
//
//		list.sort { Map t ->
//			String target = (t.target ?: "").toString()
//			String typ = (t.type ?: "").toString()
//
//			if (typ.startsWith("단위-") || typ in ["이름모달", "단위-이름검색"]) {
//				if (target.contains("nameModal") || typ in ["이름모달", "단위-이름검색"]) {
//					return domIdx("name") + 0.5
//				}
//
//				Map<String, String> fieldMap = [
//					"id":"id",
//					"tripledup":"id",
//					"skipDupCheck":"id",
//					"pw":"pw",
//					"pwConf":"pwConf",
//					"name":"name",
//					"address":"address",
//					"email":"email",
//					"phone":"phone"
//				]
//
//				if (fieldMap.containsKey(target)) return domIdx(fieldMap[target])
//				if (target.startsWith("btnGroup")) return domIdx("btnGroup")
//				if (target in ["fileClear", "fileAttach"]) return domIdx("file")
//				if (target.toLowerCase().contains("check") || target.toLowerCase().contains("uncheck")) return domIdx("terms")
//
//				if (target.toLowerCase().contains("select")) {
//					String selId = ((t.value ?: "") as String).contains("::") ? ((t.value ?: "") as String).split("::")[0] : ""
//					Integer matched = null
//					for (int i = 0; i < domOrder.size(); i++) {
//						if (domOrder[i].startsWith("select_") && selId && domOrder[i].contains(selId)) {
//							matched = i
//							break
//						}
//					}
//					if (matched != null) return matched
//					String firstSelect = domOrder.find { it.startsWith("select_") } ?: "select_"
//					return domIdx(firstSelect)
//				}
//
//				return 50
//			}
//
//			if (typ == "시나리오") return 60
//			if (typ == "성공") return 70
//			return 100
//		}
//
//		list.eachWithIndex { Map tc, int i ->
//			tc.id = String.format("TC-%02d", i + 1)
//		}
//		return list
//	}
//
//	// =========================================================
//	// [N] 판정
//	// =========================================================
//	private static Map judge(Map tc, String actual) {
//		String msg = norm(actual)
//
//		List<String> POS  = ["가입 완료","등록 완료","저장 완료","사용 가능","사용가능","성공","완료","일치","정상 가입","제출 완료","[선택완료]"]
//		List<String> WPOS = ["확인되었습니다","적용되었습니다","등록되었습니다","저장되었습니다"]
//		List<String> NEG  = ["필수","입력","선택","체크","동의","오류","실패","불가","형식","일치하지","다릅니다","중복","이미 사용","첨부","업로드","작성","누락","재입력","올바른"]
//
//		boolean pos = POS.any { msg.contains(it) }
//		boolean wpos = WPOS.any { msg.contains(it) }
//		boolean neg = NEG.any { msg.contains(it) }
//		boolean emp = blank(msg)
//
//		String t = (tc.target ?: "").toString()
//
//		if (t == "fileClear") {
//			return [p: (!emp && neg), r: (!emp && neg) ? "미첨부 예외정상" : (emp ? "파일 결과 미감지" : "미첨부인데 성공")]
//		}
//
//		if (t == "fileAttach") {
//			boolean pass = (pos || wpos) || (emp && !neg)
//			return [p: pass, r: (pos || wpos) ? "첨부 정상" : (emp ? "오류없음" : "에러발생")]
//		}
//
//		if (t == "actionBtnNoClick") return [p:true, r:"미클릭 상태 확인"]
//		if (t == "actionBtnClick") {
//			if (pos || wpos) return [p:true, r:"클릭 반응 확인"]
//			if (!emp && !neg) return [p:true, r:"클릭 후 응답"]
//			return [p:false, r:"클릭 반응 미감지"]
//		}
//
//		if ((tc.expect ?: "") == "성공") {
//			if (t == "btnGroupSelect") return [p:true, r:"옵션선택OK"]
//			if (pos || wpos) return [p:true, r:"정상통과"]
//			if (emp && !neg) return [p:true, r:"오류없이정상"]
//			return [p:false, r: neg ? "성공해야하나 에러" : "성공판정근거부족"]
//		}
//
//		if (emp) return [p:false, r:"예외미감지"]
//		if (neg) return [p:true, r:"예외정상방어"]
//		if (pos) return [p:false, r:"에러여야하나 성공"]
//		return [p:true, r:"비정상응답감지"]
//	}
//
//	// =========================================================
//	// [O] 이름 모달 보조
//	// =========================================================
//	private static void modalNoSelect() {
//		if (!openNameModal()) return
//		js("""
//			var m=document.querySelector('[role=dialog],[class*=modal],[class*=Modal],[class*=popup],[class*=Dialog],[class*=layer]');
//			if(!m) m=document;
//			var btns=m.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
//			for(var i=btns.length-1;i>=0;i--){
//				var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
//				if(tx.includes('선택완료')||tx.includes('완료')||tx.includes('확인')){
//					try{btns[i].click();}catch(e){}
//					break;
//				}
//			}
//		""")
//		sleep(0.8)
//	}
//
//	private static void modalSelectFirst() {
//		if (!openNameModal()) return
//		js("""
//			var m=document.querySelector('[role=dialog],[class*=modal],[class*=Modal],[class*=popup],[class*=Dialog],[class*=layer]');
//			if(!m) m=document;
//			var rs=m.querySelectorAll('input[type=radio]');
//			if(rs.length>0){ rs[0].click(); }
//			else{
//				var rows=m.querySelectorAll('tbody tr');
//				if(rows.length>0) try{rows[0].click();}catch(e){}
//			}
//		""")
//		sleep(0.3)
//
//		js("""
//			var m=document.querySelector('[role=dialog],[class*=modal],[class*=Modal],[class*=popup],[class*=Dialog],[class*=layer]');
//			if(!m) m=document;
//			var btns=m.querySelectorAll('button,[role=button],input[type=button],input[type=submit]');
//			for(var i=btns.length-1;i>=0;i--){
//				var tx=(btns[i].innerText||btns[i].value||'').replace(/\\s+/g,'');
//				if(tx.includes('선택완료')||tx.includes('완료')||tx.includes('확인')){
//					try{btns[i].click();}catch(e){}
//					break;
//				}
//			}
//		""")
//		sleep(0.8)
//	}
//
//	// =========================================================
//	// [P] TC 1건 실행
//	// =========================================================
//	private static boolean runTc(int num, Map tc) {
//		String actual = ""
//		long start = System.currentTimeMillis()
//
//		if (nameIsSearch && (tc.type ?: "") == "단위-이름") {
//			log("⏭️  ${tc.id} [${tc.type}] 이름 검색팝업 방식 → 스킵")
//			allResults << [
//				tc     : tc.id,
//				type   : tc.type,
//				field  : tc.e ?: "",
//				popup  : "[스킵-검색팝업]",
//				passed : true,
//				elapsed: "0.00초",
//				expect : tc.expect,
//				reason : "검색팝업방식-직접입력불가"
//			]
//			return true
//		}
//
//		try {
//			dismiss()
//			backIfNeeded()
//			resetForm()
//			sleep(0.2)
//
//			fillEmpty()
//			checkAll()
//			setRequired()
//			setName("홍길동")
//			setField("pw", "Test123!@")
//			setField("pwConf", "Test123!@")
//
//			boolean isId = (tc.target in ["id", "tripledup", "skipDupCheck"])
//
//			setField("id", isId ? "testuser${num}" : confirmedId)
//			sleep(0.2)
//
//			if (!isId && hasDupBtn() && confirmedId) {
//				clickDupCheck()
//				dismiss()
//				sleep(0.2)
//			}
//
//			String t = (tc.target ?: "").toString()
//
//			Map<String, Closure> actions = [
//				clear          : { ->
//					resetForm()
//					sleep(0.2)
//					fillEmpty()
//					checkAll()
//					setRequired()
//				},
//				skipDupCheck   : { -> setField("id", tc.value?.toString()) },
//				btnGroupClear  : { -> clearBtnGroup() },
//				btnGroupSelect : { -> selectBtnOption(tc.value?.toString()) },
//				actionBtnNoClick: { -> },
//				selectReset    : { -> resetSelect(tc.value?.toString()) },
//				selectOption   : { -> selectOption(tc.value?.toString()) },
//				fileClear      : { -> clearFile() },
//				fileAttach     : { -> attachFile() },
//				nameModalNoSelect: { -> modalNoSelect() },
//				nameModalSelect: { -> modalSelectFirst() },
//				uncheckAll     : { -> uncheckAll() },
//				uncheckOne     : { -> checkByIdx((tc.value ?: "0") as int, false) },
//				checkOnlyFirst : { -> checkOnlyFirst() },
//				checkAllBox    : { -> checkAll() },
//				checkOne       : { -> checkByIdx((tc.value ?: "0") as int, true) },
//				address        : { -> setAddrCase(tc.value?.toString()) }
//			]
//
//			if (t == "actionBtnNoClick") {
//				actual = "[버튼미클릭]"
//			} else if (t in ["success", "tripledup"]) {
//				// 아래 로직에서 별도 처리
//			} else if (actions.containsKey(t)) {
//				actions[t].call()
//			} else if (!isId) {
//				setField(t, tc.value?.toString())
//			}
//
//			if (t == "fileClear") {
//				clearFile()
//				sleep(0.2)
//			}
//
//			if (t == "id") {
//				if (hasDupBtn()) clickDupCheck()
//				actual = getPopup("id")
//				setRequired()
//			} else if (t == "tripledup") {
//				if (hasDupBtn()) {
//					for (int i = 0; i < 3; i++) {
//						clickDupCheck()
//						actual = getPopup("id")
//						sleep(0.2)
//					}
//				}
//				actual = "[중복3회] " + (actual ?: "[팝업없음]")
//				setRequired()
//			} else if (t == "btnGroupSelect") {
//				if (blank(actual)) actual = "[선택완료]"
//			} else if (t == "actionBtnNoClick") {
//				// 그대로 둠
//			} else if (t == "success") {
//				setAddrDummy()
//				clearBtnGroup()
//				selectFirstBtn()
//
//				if (hasDupBtn()) {
//					setField("id", confirmedId)
//					sleep(0.2)
//					clickDupCheck()
//					dismiss()
//				}
//				closeModals()
//				clickRegister()
//				actual = getPopup("success")
//			} else if (t in ["fileClear", "fileAttach"]) {
//				closeModals()
//				clickRegister()
//				actual = getPopup("file") ?: "[팝업없음]"
//			} else {
//				closeModals()
//				clickRegister()
//				actual = getPopup(t)
//			}
//		} catch (Exception ex) {
//			actual = "[에러] " + ((ex.message ?: ex.toString()).take(120))
//			if (actual.contains("invalid session")) {
//				logTc(false, tc, actual, 0, "세션끊김")
//				return false
//			}
//		}
//
//		double elapsed = (System.currentTimeMillis() - start) / 1000.0
//		String clean = (actual ?: "").replace("\n", " ").trim()
//
//		Map v = judge(tc, clean)
//		if (!(v.p as boolean)) totalFail++
//
//		logTc(v.p as boolean, tc, clean, elapsed, v.r?.toString())
//		allResults << [
//			tc     : tc.id,
//			type   : tc.type,
//			field  : tc.e ?: "",
//			popup  : clean,
//			passed : v.p,
//			elapsed: String.format("%.2f초", elapsed),
//			expect : tc.expect,
//			reason : v.r
//		]
//		return true
//	}
//
//	// =========================================================
//	// [Q] 아이디 확정
//	// =========================================================
//	private static void confirmDupOnce() {
//		confirmedId = FIXED_ID
//		log("🔒 확정 ID 고정: ${confirmedId}")
//
//		if (!hasDupBtn()) return
//
//		try {
//			dismiss()
//			backIfNeeded()
//			resetForm()
//			sleep(0.2)
//
//			fillEmpty()
//			checkAll()
//			setRequired()
//			setName("홍길동")
//
//			setField("id", confirmedId)
//			setField("pw", "Test123!@")
//			setField("pwConf", "Test123!@")
//			sleep(0.3)
//
//			clickDupCheck()
//			String popup = getPopup("dup_confirm")
//			log("   ↳ 중복확인 결과: ${popup}")
//			dismiss()
//		} catch (Exception e) {
//			log("⚠️ confirmDupOnce: ${e.message}")
//		}
//	}
//
//	// =========================================================
//	// [R] 로그 / 요약
//	// =========================================================
//	private static void logTc(boolean ok, Map tc, String popup, double elapsed, String reason) {
//		String icon = ok ? "✅  PASS" : "❌  FAIL"
//		String sep = "═" * 62
//		String inp = ((tc.value == "") ? "(빈 값)" : (tc.value ?: "-").toString()).take(40)
//
//		println ""
//		println "╔${sep}╗"
//		println "║  ${icon.padRight(60)}║"
//		println "╠${sep}╣"
//		println "║  🆔 ${((tc.id ?: '') + ' | ' + (tc.type ?: '') + ' | ' + (tc.e ?: '')).padRight(58)}║"
//		println "║  ⌨️  ${inp.padRight(56)}║"
//		println "║  📝 ${(popup ?: '없음').take(45).padRight(56)}║"
//		println "║  💬 ${(reason ?: '').take(56).padRight(56)}║"
//		println "╚${sep}╝"
//	}
//
//	private static void printSummary() {
//		int t = allResults.size()
//		int p = allResults.count { it.passed == true }
//		int f = t - p
//		int rate = t > 0 ? (int) ((p * 100) / t) : 0
//
//		println ""
//		println "=" * 64
//		println " 🏁 최종 결과: 전체 ${t}건 | ✅ ${p} | ❌ ${f} | 통과율 ${rate}%"
//		println "=" * 64
//	}
//
//	// =========================================================
//	// [S] 메인 실행
//	// =========================================================
//	private static void runAllInternal() {
//		totalFail = 0
//		allResults = []
//		confirmedId = ""
//		fileInputId = ""
//		dummyFilePath = ""
//		nameIsSearch = false
//
//		log("🚀 회원가입 예외 테스트 시작")
//		log("═" * 70)
//		sleep(1.0)
//
//		initialUrl = d().currentUrl
//		nameIsSearch = detectNameSearchMode()
//		log("🔍 이름 필드 방식: " + (nameIsSearch ? "검색팝업형" : "직접입력형"))
//
//		List<String> domOrder = getDomOrder()
//		log("🔍 감지 순서: " + (domOrder ? domOrder.join(" ➔ ") : "(감지 없음)"))
//
//		Map scan = scanForm()
//		scanFileInput()
//
//		List<Map> allTcs = sortTcs(buildStaticTcs() + buildDynamicTcs(scan), domOrder)
//
//		List<Map> idTcs = allTcs.findAll { it.target in ["id", "tripledup", "skipDupCheck"] }
//		List<Map> rest  = allTcs.findAll { !(it.target in ["id", "tripledup", "skipDupCheck"]) }
//
//		log("")
//		log("📋 아이디 TC ${idTcs.size()}개")
//		for (int i = 0; i < idTcs.size(); i++) {
//			if (!runTc(i + 1, idTcs[i])) {
//				printSummary()
//				return
//			}
//		}
//
//		log("")
//		log("🔒 아이디 검증 완료 → 중복확인 & ID 고정")
//		confirmDupOnce()
//
//		log("")
//		log("📋 나머지 TC ${rest.size()}개")
//		for (int i = 0; i < rest.size(); i++) {
//			if (!runTc(idTcs.size() + i + 1, rest[i])) break
//		}
//
//		printSummary()
//		log(totalFail > 0 ? "🚨 실패 ${totalFail}건" : "✅ 전체 통과!")
//	}
//
//	// =========================================================
//	// [T] 외부 호출용 Keyword
//	// =========================================================
//	@Keyword
//	def runAll() {
//		if (!hasDriver()) {
//			KeywordUtil.markFailed("브라우저가 열려 있지 않습니다. 먼저 회원가입 페이지를 연 뒤 runAll()을 호출하세요.")
//			return
//		}
//
//		try {
//			if (!d().currentUrl) {
//				KeywordUtil.markFailed("현재 페이지 URL을 가져올 수 없습니다.")
//				return
//			}
//		} catch (Exception e) {
//			KeywordUtil.markFailed("현재 페이지에 접근할 수 없습니다: ${e.message}")
//			return
//		}
//
//		runAllInternal()
//	}
//}