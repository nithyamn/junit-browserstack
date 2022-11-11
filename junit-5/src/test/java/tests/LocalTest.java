package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import runners.WebDriverTest;
import utils.MarkSessionStatus;


public class LocalTest {

    @WebDriverTest
    void localTest(WebDriver driver) {
        MarkSessionStatus sessionStatus = new MarkSessionStatus(driver);

        try {
            driver.get("http://bs-local.com:45454/");
            String validateContent = driver.getTitle();
            if (validateContent.contains("BrowserStack Local")) {
                sessionStatus.markTestStatus("passed", "Local content validated!");
            } else {
                sessionStatus.markTestStatus("failed", "Local content not validated!");
            }
        } catch (Exception e) {
            sessionStatus.markTestStatus("failed", "There was some issue!");
            System.out.println(e.getMessage());
        }
        driver.quit();
    }
}
