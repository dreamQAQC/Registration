import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject

import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import com.kms.katalon.core.webui.driver.DriverFactory as DriverFactory
import internal.GlobalVariable as GlobalVariable

import org.openqa.selenium.Keys as Keys
import com.kms.katalon.core.testobject.ConditionType as ConditionType


WebUI.openBrowser('')
WebUI.navigateToUrl('http://localhost:8080/join3.html')

WebUI.setText(findTestObject('Object Repository/join3/Page_/input__userId'), 'ssr0128')
WebUI.setEncryptedText(findTestObject('Object Repository/join3/Page_/input__password'), '0j7gx7IaHoqVvtnDzehB9w==')
WebUI.setEncryptedText(findTestObject('Object Repository/join3/Page_/input__passwordConfirm'), '0j7gx7IaHoqVvtnDzehB9w==')
WebUI.setText(findTestObject('Object Repository/join3/Page_/input__userName'), '정민호')

// 성별(남성) 클릭
WebUI.click(findTestObject('Object Repository/join3/Page_/div_'))

// 파일 업로드
TestObject fileInput = new TestObject('fileInput')
fileInput.addProperty('id', ConditionType.EQUALS, 'profileImg')
WebUI.uploadFile(fileInput, 'C:\\Users\\ssr0128\\테스트용이미지.png')


// =========================
// ✅ 주소검색 (여기만 교체)
// =========================
// 기존처럼 ObjectRepository로 postcode 창 title/결과 클릭 하지 말고,
// Helper의 "범용" 처리 1줄로 끝냅니다.


// 상세 주소 및 나머지 입력
WebUI.setText(findTestObject('Object Repository/join3/Page_/input_(        286)_sample4_detailAddress'), 'aaaa')
WebUI.selectOptionByValue(findTestObject('Object Repository/join3/Page_/select_SKT                        KT       _9a0272'), 'skt', true)
WebUI.selectOptionByValue(findTestObject('Object Repository/join3/Page_/select_naver.com                        gma_f566ea'), 'naver.com', true)
WebUI.selectOptionByValue(findTestObject('Object Repository/join3/Page_/select_'), 'student', true)
WebUI.click(findTestObject('Object Repository/join3/Page_/label_'))

// 마지막에 예외 TC 전체 실행
CustomKeywords.'words.RegExceptionHelper.runAll'()