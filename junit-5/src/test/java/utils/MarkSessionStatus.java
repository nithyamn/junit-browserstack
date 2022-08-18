package utils;

import org.json.simple.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import runners.BstackRunner;

public class MarkSessionStatus extends BstackRunner {
    WebDriver webDriver;

    public MarkSessionStatus(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public void markTestStatus(String status, String reason) {
        final JavascriptExecutor jse = (JavascriptExecutor) webDriver;
        JSONObject executorObject = new JSONObject();
        JSONObject argumentsObject = new JSONObject();
        argumentsObject.put("status", status);
        argumentsObject.put("reason", reason);
        executorObject.put("action", "setSessionStatus");
        executorObject.put("arguments", argumentsObject);
        jse.executeScript(String.format("browserstack_executor: %s", executorObject));
    }
}
