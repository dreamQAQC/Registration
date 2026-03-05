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
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keyss




WebUI.openBrowser('')
WebUI.navigateToUrl('file:///C:/Users/ssr0128/Desktop/page/Registration.html')

// 최초 1회 수동 입력 (사용자 코드 유지)
WebUI.setText(findTestObject('Object Repository/my/Page_-/input_ID ( )_adminId'), 'ssr0128')
WebUI.setEncryptedText(findTestObject('Object Repository/my/Page_-/input__password'), '0j7gx7IaHoqVvtnDzehB9w==')
WebUI.setEncryptedText(findTestObject('Object Repository/my/Page_-/input__passwordConfirm'), '0j7gx7IaHoqVvtnDzehB9w==')
WebUI.setText(findTestObject('Object Repository/my/Page_-/input__adminName'), '정민호')
WebUI.setText(findTestObject('Object Repository/my/Page_-/input__adminEmail'), 'sssr0123@ad.com')
WebUI.selectOptionByValue(findTestObject('Object Repository/my/Page_-/select_QA'), 'super', true)
WebUI.click(findTestObject('Object Repository/my/Page_-/input_SMS_marketing'))


// 2. 가공하거나 조건에 따라 데이터를 조절한 뒤 실행 가능
CustomKeywords.'words.RegExceptionHelper.runAll'()


