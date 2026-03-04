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

WebUI.navigateToUrl('file:///C:/Users/ssr0128/Desktop/page/Join.html')

WebUI.click(findTestObject('Object Repository/join2/Page_Premium Sign Up/label_ID'))

WebUI.click(findTestObject('Object Repository/join2/Page_Premium Sign Up/p_'))

WebUI.click(findTestObject('Object Repository/join2/Page_Premium Sign Up/input__userName'))

WebUI.setText(findTestObject('Object Repository/join2/Page_Premium Sign Up/input__userId'), 'ssr123')

WebUI.setText(findTestObject('Object Repository/join2/Page_Premium Sign Up/input__userName'), '정민호')

WebUI.setEncryptedText(findTestObject('Object Repository/join2/Page_Premium Sign Up/input__userPw'), '0j7gx7IaHoqVvtnDzehB9w==')

WebUI.setEncryptedText(findTestObject('Object Repository/join2/Page_Premium Sign Up/input__userPwConfirm'), '0j7gx7IaHoqVvtnDzehB9w==')


WebUI.click(findTestObject('Object Repository/join2/Page_Premium Sign Up/input_checkbox'))


CustomKeywords.'words.RegExceptionHelper.runAll'()
