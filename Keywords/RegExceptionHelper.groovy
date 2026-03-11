/*
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║             RegExceptionHelper.groovy  ← 메인 파일              ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  TC 실행 / 판정 / 로그 출력 / 외부 진입점(@Keyword) 담당        ║
 * ║  RegExceptionHelperBase 를 extends 해서 서브 기능 사용           ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  [수정 대상 안내]                                                ║
 * ║                                                                  ║
 * ║  ✏️  이 파일을 수정하는 경우                                     ║
 * ║  ─────────────────────────────────────────────────────────────  ║
 * ║  • PASS/FAIL 판정 기준 수정                                     ║
 * ║      → [L] judgeTestResult()                                    ║
 * ║        positiveWords / negativeWords 리스트 편집                ║
 * ║                                                                  ║
 * ║  • 로그/콘솔 출력 형식 수정                                     ║
 * ║      → [M] printTestCaseLog()                                   ║
 * ║      → [M] printSummary()                                       ║
 * ║                                                                  ║
 * ║  • TC 실행 순서/흐름 수정                                       ║
 * ║      → [N] runSingleTestCase()                                  ║
 * ║        (액션맵, actual 수집 블록, 에러 복구 로직 포함)          ║
 * ║                                                                  ║
 * ║  • 전체 실행 시작 흐름 수정                                     ║
 * ║      → [O] runAllInternal()                                     ║
 * ║        (스캔 순서, TC 빌드 순서, TC 정렬 등)                   ║
 * ║                                                                  ║
 * ║  • 외부 호출 조건/사전 체크 수정                                ║
 * ║      → [P] runAll()  ← Katalon 테스트에서 호출하는 메서드      ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import org.openqa.selenium.By
import org.openqa.selenium.NoSuchSessionException

/**
 * RegExceptionHelper (메인)
 * ─────────────────────────
 * 외부에서 호출하는 진입점 클래스.
 * 실제 로직(유틸·팝업·폼·스캔·TC생성)은 RegExceptionHelperBase 에 위임합니다.
 *
 * 섹션 목록
 *   [L] 판정
 *   [M] 로그 출력
 *   [N] 단일 TC 실행
 *   [O] 전체 실행 (내부)
 *   [P] 외부 호출용 @Keyword
 */
class RegExceptionHelper extends RegExceptionHelperBase {

	// =========================================================
	// [L] 판정
	// =========================================================
	private static Map judgeTestResult(Map tc, String actual) {
		String msg = normalizeText(actual)

		List<String> positiveWords = ["가입 완료", "등록 완료", "저장 완료", "사용 가능", "사용가능", "성공", "완료", "일치", "정상 가입", "제출 완료", "[선택완료]"]
		List<String> weakPositiveWords = ["확인되었습니다", "적용되었습니다", "등록되었습니다", "저장되었습니다"]
		List<String> negativeWords = ["필수", "입력", "선택", "체크", "동의", "오류", "실패", "불가", "형식", "일치하지", "다릅니다", "중복", "이미 사용", "작성", "누락", "재입력", "올바른"]

		boolean hasPositive     = positiveWords.any { msg.contains(it) }
		boolean hasWeakPositive = weakPositiveWords.any { msg.contains(it) }
		boolean hasNegative     = negativeWords.any { msg.contains(it) }
		boolean empty           = isBlankPopup(msg)

		String target = (tc.target ?: "").toString()

		if ((tc.expect ?: "") == "성공") {
			if (target == "btnGroupSelect") return [passed: true, reason: "옵션선택OK"]
			if (target in ["namePopupInput", "namePopupCheckbox", "namePopupSelectDefault", "namePopupSearchButton", "namePopupSelectOption"])
				return [passed: true, reason: "이름 팝업 검증"]
			if (hasPositive || hasWeakPositive) return [passed: true, reason: "정상통과"]
			if (empty && !hasNegative)          return [passed: true, reason: "오류없이정상"]
			return [passed: false, reason: (hasNegative ? "성공해야하나 에러" : "성공판정근거부족")]
		}

		if (empty)       return [passed: false, reason: "예외미감지"]
		if (hasNegative) return [passed: true,  reason: "예외정상방어"]
		if (hasPositive) return [passed: false, reason: "에러여야하나 성공"]
		return [passed: true, reason: "비정상응답감지"]
	}

	// =========================================================
	// [M] 로그 출력
	// =========================================================
	private static void printTestCaseLog(boolean passed, Map tc, String popup, double elapsed, String reason) {
		String mark = passed ? "PASS" : "FAIL"
		String sep  = "=" * 54
		String input = ((tc.value == "") ? "(빈 값)" : (tc.value ?: "-").toString()).take(40)

		println ""
		println sep
		println " ${mark} | ${tc.id} | ${tc.type} | ${tc.field ?: ''}"
		println " 입력값: ${input}"
		println " 결과  :"
		List<String> resultLines = (popup ?: "없음").split("\\|").collect { it.trim() }.findAll { it }
		resultLines.each { String line -> println "   " + line }
		println " 판정  : ${reason}"
		println sep
	}

	private static void printSummary() {
		int total = RegExceptionHelperBase.testResults.size()
		int pass  = RegExceptionHelperBase.testResults.count { it.passed == true }
		int fail  = total - pass
		int rate  = total > 0 ? (int) ((pass * 100) / total) : 0

		// 검증 항목 종류 수집
		List<String> fieldTypes = RegExceptionHelperBase.testResults.collect { (it.field ?: "").toString() }.unique().findAll { it }

		println ""
		println "============================================================"
		println " 페이지: ${RegExceptionHelperBase.startUrl}"
		println " 검증항목: ${fieldTypes.join(', ')}"
		println " 총 ${total}건 검증 | PASS ${pass}건 | FAIL ${fail}건 | 통과율 ${rate}%"
		println "============================================================"

		if (fail > 0) {
			println ""
			println " ■ 부적합 내역 (${fail}건)"
			println " ─────────────────────────────────────────────────────"

			RegExceptionHelperBase.testResults.findAll { !(it.passed as boolean) }.each { Map r ->
				String tcId    = (r.tc    ?: "").toString()
				String type    = (r.type  ?: "").toString()
				String field   = (r.field ?: "").toString()
				String popup   = (r.popup ?: "").toString()
				String reason  = (r.reason ?: "").toString()
				String expect  = (r.expect ?: "").toString()

				// 한 줄 요약 생성
				String summary = buildFailSummary(type, field, popup, reason, expect, r)
				println "  [${tcId}] ${summary}"
			}
			println " ─────────────────────────────────────────────────────"
		}

		println "============================================================"

		// FAIL 건 있으면 Katalon 로그에 빨간색 표시
		if (fail > 0) {
			KeywordUtil.markFailed(
				"검증 실패 ${fail}건 발생 — 부적합 내역을 콘솔 로그에서 확인하세요. (전체 ${total}건 중 ${fail}건 FAIL)"
			)
		}
	}

	// FAIL 한 줄 요약 문구 생성
	private static String buildFailSummary(String type, String field, String popup, String reason, String expect, Map r) {
		String inputVal = (r.value ?: "").toString()
		String inputDisp = (!inputVal.trim() ? "빈 값" : inputVal.take(30))

		// 이름팝업 서브TC
		if (type == "이름팝업 검증") {
			return "${field} — 입력[${inputDisp}] → ${popup.take(60)}"
		}

		// 공통 패턴
		switch (type) {
			case "단위-아이디":
				return "아이디 필드에 [${inputDisp}] 입력 가능 (차단 필요) — ${popup.take(50)}"
			case "단위-비밀번호":
				return "비밀번호 필드에 [${inputDisp}] 입력 가능 (차단 필요) — ${popup.take(50)}"
			case "단위-비번확인":
				return "비밀번호 확인 [${inputDisp}] 불일치 미감지 — ${popup.take(50)}"
			case "단위-이름":
				return "이름 필드에 [${inputDisp}] 입력 가능 (차단 필요) — ${popup.take(50)}"
			case "단위-주소":
				return "주소 부분 입력[${inputDisp}] 시 가입 차단 안 됨 — ${popup.take(50)}"
			case "단위-약관":
				return "약관 미동의 상태로 가입 가능 — ${popup.take(50)}"
			default:
				if (type.startsWith("단위-")) {
					String label = type.replace("단위-", "")
					return "${label} [${inputDisp}] → ${reason} — ${popup.take(50)}"
				}
				if (type == "시나리오") {
					return "시나리오[${field}] — ${reason} (입력: ${inputDisp})"
				}
				return "${field} [${inputDisp}] → ${reason}"
		}
	}


	private static void ensureDuplicateCheckReadyForNonIdCase() {
		if (!existsDuplicateCheckButton()) return
		try {
			clickDuplicateCheckButton()
			String msg = collectPopupMessage("id", true)
			if (!isBlankPopup(msg)) logLine("사전 ID 조회 결과: " + msg)
		} catch (Exception ignore) {}
	}

	// =========================================================
	// [N] 단일 TC 실행
	// =========================================================
	private static boolean runSingleTestCase(int num, Map tc) {
		String actual  = ""
		long   started = System.currentTimeMillis()

		String  target          = (tc.target ?: "").toString()
		boolean isNamePopupTc   = target.startsWith("namePopup")

		try {
			if (!isNamePopupTc) dismissGeneralPopups()
			else                closeBrowserAlertIfExists()

			resetMainForm()
			waitSec(0.2)

			fillVisibleEmptyFieldsWithDefault()
			checkAllCheckboxes()
			applyGeneralRequiredDefaults()

			setFormFieldValue("id",     RegExceptionHelperBase.FIXED_VALID_ID)
			setFormFieldValue("pw",     "Test123!@")
			setFormFieldValue("pwConf", "Test123!@")
			boolean needsDupReady = !(target in ["id", "tripledup", "skipDupCheck"]) && !isNamePopupTc
			if (needsDupReady) ensureDuplicateCheckReadyForNonIdCase()

			// 이름 필드 세팅 (팝업 여부에 따라 분기)
			if (!isNamePopupTc) {
				if (RegExceptionHelperBase.isNameSearchPopupMode) {
					if (RegExceptionHelperBase.nameFieldDone) {
						try {
							runJs('''
								var ins=document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");
								for(var i=0;i<ins.length;i++){
									var e=ins[i]; if(e.offsetParent===null) continue;
									var id=(e.id||"").toLowerCase(),nm=(e.name||"").toLowerCase(),ph=(e.placeholder||"").toLowerCase();
									if(id.indexOf("name")>=0||nm.indexOf("name")>=0||ph.indexOf("이름")>=0||ph.indexOf("성명")>=0){
										e.readOnly=false; e.disabled=false; e.value="홍길동";
										["input","change"].forEach(function(ev){ e.dispatchEvent(new Event(ev,{bubbles:true})); }); break;
									}
								}
							''')
						} catch (Exception ignored) {}
					}
					// nameFieldDone=false: namePopupScenario TC 아직 실행 전 → 이름 필드 비워둠
				} else {
					setFormFieldValue("name", "홍길동")
				}
			}

			// 액션 맵
			Map<String, Closure> actionMap = [
				clear                        : { -> resetMainForm(); waitSec(0.2); fillVisibleEmptyFieldsWithDefault(); checkAllCheckboxes(); applyGeneralRequiredDefaults() },
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
				namePopupScenario            : { -> /* actual 수집 블록에서 직접 호출 */ },
				namePopupCompleteWithoutSelection: { -> actionNamePopupCompleteWithoutSelection() },
				namePopupSelectFirstItem         : { -> actionNamePopupSelectFirstItem() },
				namePopupSelectDefault           : { -> actionNamePopupSetDropdownDefault(tc.value?.toString()) },
				namePopupSelectOption            : { -> actionNamePopupSelectDropdownOption(tc.value?.toString()) },
				namePopupInput                   : { -> actionNamePopupSetInputValue(tc.value?.toString()) },
				namePopupCheckbox                : { -> actionNamePopupSetCheckbox(tc.value?.toString()) },
				namePopupSearchButton            : { -> actionNamePopupClickSearchOnly() },
				skipDupCheck                 : { -> setFormFieldValue("id", tc.value?.toString()) }
			]

			// 액션 실행
			if (target == "success" || target == "tripledup") {
				// 아래 actual 수집 블록에서 처리
			} else if (target == "name" && RegExceptionHelperBase.isNameSearchPopupMode) {
				if (openNameSearchPopup()) { waitSec(0.5); actionNamePopupCompleteWithoutSelection() }
				actual = collectPopupMessage("name")
			} else if (target.startsWith("fileEmpty::") || target.startsWith("fileValid::") ||
			           target.startsWith("fileInvalid::") || target.startsWith("fileOversize::")) {
				// 파일 첨부 TC — actual 수집 블록에서 처리
			} else if (actionMap.containsKey(target)) {
				actionMap[target].call()
			} else {
				setFormFieldValue(target, tc.value?.toString())
			}

			// actual 수집
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
				actual = runNamePopupScenario(RegExceptionHelperBase.namePopupScan)
			} else if (target == "namePopupCompleteWithoutSelection") {
				actual = collectPopupMessage("namePopupCompleteWithoutSelection")
			} else if (target == "namePopupSelectFirstItem") {
				Object nameValue = runJs('''
					var inputs=document.querySelectorAll("input:not([type=hidden]):not([type=password]):not([type=radio]):not([type=checkbox])");
					for(var i=0;i<inputs.length;i++){
						var e=inputs[i]; if(e.offsetParent===null) continue;
						var id=(e.id||"").toLowerCase(),nm=(e.name||"").toLowerCase(),ph=(e.placeholder||"").toLowerCase(),lb="";
						if(e.labels&&e.labels.length) lb=(e.labels[0].innerText||"").toLowerCase();
						else{ var p=e.closest("div,td,tr,li,section"); if(p) lb=(p.innerText||"").toLowerCase(); }
						var isName=id.indexOf("name")>=0||nm.indexOf("name")>=0||ph.indexOf("이름")>=0||ph.indexOf("성명")>=0||lb.indexOf("이름")>=0||lb.indexOf("성명")>=0;
						if(isName) return e.value||"";
					}
					return "";
				''')
				actual = ((nameValue ?: "").toString().trim()) ? "[선택완료]" : "[팝업없음]"
				closeInnerCompletePopupIfExists()
			} else if (target in ["namePopupSelectOption", "namePopupInput", "namePopupCheckbox", "namePopupSelectDefault", "namePopupSearchButton"]) {
				actual = "[선택완료]"
				closeInnerCompletePopupIfExists()
			} else if (target == "btnGroupSelect") {
				actual = "[선택완료]"
			} else if (target.startsWith("fileEmpty::") || target.startsWith("fileValid::") ||
			           target.startsWith("fileInvalid::") || target.startsWith("fileOversize::")) {
				// 파일 첨부 TC 처리
				List<String> tparts = target.split("::") as List<String>
				String fileAction = tparts[0]
				int fileIdx = (tparts.size() > 1 ? tparts[1] : "0") as int
				String ext = (tc.value ?: "pdf").toString()

				try {
					List<org.openqa.selenium.WebElement> fileInputEls = driver().findElements(
						By.cssSelector("input[type=file]")
					).findAll { it != null }

					if (!fileInputEls || fileIdx >= fileInputEls.size()) {
						actual = "[input[type=file] 없음]"
					} else if (fileAction == "fileEmpty") {
						// 파일 미첨부 → 그냥 제출
						closeGeneralModalIfExists()
						clickRegisterButton()
						actual = collectPopupMessage("file")
					} else {
						// 더미 파일 생성 및 주입
						double sizeMb = (fileAction == "fileOversize") ? 10.0 : 0
						String fname = "${fileAction}_${num}.${ext}"
						File dummy = createDummyFile(fname, sizeMb)
						try {
							injectFileToInput(fileInputEls[fileIdx], dummy.absolutePath)
							closeGeneralModalIfExists()
							clickRegisterButton()
							actual = collectPopupMessage("file")
						} finally {
							try { if (dummy.exists()) dummy.delete() } catch (Exception ignore) {}
						}
					}
				} catch (Exception fe) {
					actual = "[파일TC오류] ${fe.message?.take(50) ?: ''}"
				}
			} else if (target == "success") {
				fillDummyAddress()
				selectFirstRadioOrButtonGroup()
				setFormFieldValue("id", RegExceptionHelperBase.FIXED_VALID_ID)
				if (existsDuplicateCheckButton()) { clickDuplicateCheckButton(); dismissGeneralPopups() }
				closeGeneralModalIfExists()
				clickRegisterButton()
				actual = collectPopupMessage("success")
			} else if (target == "name" && RegExceptionHelperBase.isNameSearchPopupMode) {
				// actual은 위 action 블록에서 이미 세팅됨
			} else {
				closeGeneralModalIfExists()
				clickRegisterButton()
				actual = collectPopupMessage(target)
			}
		} catch (Exception ex) {
			if (ex instanceof NoSuchSessionException || (ex.message ?: "").contains("invalid session id") || (ex.message ?: "").contains("NoSuchSession")) {
				try { WebUI.openBrowser(RegExceptionHelperBase.startUrl); waitSec(2.0) } catch (Exception ignored) {}
				actual = "[세션복구] 브라우저 재시작"
			} else {
				try {
					String curUrl = driver().currentUrl ?: ""
					if (RegExceptionHelperBase.startUrl && curUrl && !curUrl.contains(RegExceptionHelperBase.startUrl.take(30))) { driver().navigate().to(RegExceptionHelperBase.startUrl); waitSec(1.5) }
				} catch (Exception ignored) {}
				actual = "[에러] " + ((ex.message ?: ex.toString()).take(120))
			}
		}

		// TC 종료 후 이름 팝업 잔여분 닫기
		if (!RegExceptionHelperBase.nameFieldDone) { try { closeNameSearchPopup() } catch (Exception ignored) {} }

		double elapsed = (System.currentTimeMillis() - started) / 1000.0
		String clean   = sanitizePopupText((actual ?: "").replace("\n", " ").trim())

		Map judged = judgeTestResult(tc, clean)
		if (!(judged.passed as boolean)) RegExceptionHelperBase.failCount++

		printTestCaseLog(judged.passed as boolean, tc, clean, elapsed, judged.reason?.toString())
		RegExceptionHelperBase.testResults << [
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

	// =========================================================
	// [O] 전체 실행 (내부)
	// =========================================================
	private static void runAllInternal() {
		RegExceptionHelperBase.failCount             = 0
		RegExceptionHelperBase.testResults           = []
		RegExceptionHelperBase.isNameSearchPopupMode = false
		RegExceptionHelperBase.nameFieldDone         = false
		RegExceptionHelperBase.namePopupScan         = [:]

		// 팝업 내부 서브TC 결과를 testResults 에 누적하는 콜백 등록
		RegExceptionHelperBase.popupSubTcCallback = { String subId, String label, String inputVal, String result, boolean passed, String reason ->
			if (!passed) RegExceptionHelperBase.failCount++
			RegExceptionHelperBase.testResults << [
				tc     : subId,
				type   : "이름팝업 검증",
				field  : label,
				value  : inputVal,
				popup  : result,
				passed : passed,
				elapsed: "-",
				expect : passed ? "성공" : "실패",
				reason : reason
			]
		}

		logLine("회원가입 예외 테스트 시작")
		waitSec(1.0)

		RegExceptionHelperBase.startUrl = driver().currentUrl

		RegExceptionHelperBase.isNameSearchPopupMode = detectNameFieldUsesPopupSearch()
		logLine("이름 필드 방식: " + (RegExceptionHelperBase.isNameSearchPopupMode ? "검색팝업형" : "직접입력형"))

		if (RegExceptionHelperBase.isNameSearchPopupMode) {
			resetMainForm(); waitSec(0.3)
			closeBrowserAlertIfExists()
			closeInnerCompletePopupIfExists()
			closeGeneralModalIfExists(); waitSec(0.5)
			RegExceptionHelperBase.namePopupScan = scanNameSearchPopup()
			logLine(
				"이름 팝업 스캔: exists=" + (RegExceptionHelperBase.namePopupScan.exists ?: false) +
				", listCount="     + (RegExceptionHelperBase.namePopupScan.listCount ?: 0) +
				", dropdownCount=" + (((RegExceptionHelperBase.namePopupScan.selects   ?: []) as List).size()) +
				", inputCount="    + (((RegExceptionHelperBase.namePopupScan.inputs    ?: []) as List).size()) +
				", checkboxCount=" + (((RegExceptionHelperBase.namePopupScan.checkboxes ?: []) as List).size())
			)
		}

		Map mainScan        = scanMainFormElements()
		List<String> domOrder = scanMainFormDomOrder()
		logLine("감지 순서: " + (domOrder ? domOrder.join(" -> ") : "(감지 없음)"))

		List<Map> fileInputs = (mainScan.fileInputs ?: []) as List<Map>
		if (fileInputs) {
			logLine("파일첨부 필드 감지: ${fileInputs.size()}개 → " + fileInputs.collect { it.label ?: "첨부파일" }.join(", "))
		} else {
			logLine("파일첨부 필드: 없음")
		}

		List<Map> allTcs = []
		allTcs.addAll(buildStaticTestCases())
		allTcs.addAll(buildMainFormDynamicTestCases(mainScan))
		if (RegExceptionHelperBase.isNameSearchPopupMode) allTcs.addAll(buildNamePopupDynamicTestCases(RegExceptionHelperBase.namePopupScan))

		allTcs = sortTestCasesByDomOrder(allTcs, domOrder)
		logLine("전체 TC 수: " + allTcs.size())

		for (int i = 0; i < allTcs.size(); i++) {
			if (!runSingleTestCase(i + 1, allTcs[i])) break
		}

		printSummary()
		deleteTempFiles()
		logLine(RegExceptionHelperBase.failCount > 0 ? "실패 ${RegExceptionHelperBase.failCount}건" : "전체 통과")
	}

	// =========================================================
	// [P] 외부 호출용 @Keyword
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
