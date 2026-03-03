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
import org.openqa.selenium.Keys as Keys

WebUI.openBrowser('')

WebUI.navigateToUrl('http://10.20.110.62:22005/login/?yn_reissue_token=N')

WebUI.setText(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/input_LOGIN_manager_id'), 'ssr0128')

WebUI.setEncryptedText(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/input_LOGIN_manager_pw'), '0j7gx7IaHoqVvtnDzehB9w==')

WebUI.click(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/button_LOGIN'))

WebUI.click(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/a_'))

WebUI.click(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/a__1'))

WebUI.click(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/button_'))

WebUI.setText(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/input_ID_manager_id'), 'ssr0122')

WebUI.setEncryptedText(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/input_LOGIN_manager_pw'), '0j7gx7IaHoqVvtnDzehB9w==')

WebUI.setEncryptedText(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/input__check_manager_pw'), '0j7gx7IaHoqVvtnDzehB9w==')

WebUI.setText(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/input__manager_name'), '정민호')

WebUI.selectOptionByValue(findTestObject('Object Repository/sdp_verification/Page_MagicZTNA/select_'), '10', true)

CustomKeywords.'words.RegExceptionHelper.runAll'()
