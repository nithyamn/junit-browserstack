package com.browserstack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

public class BrowserStackJUnitTest {
    public WebDriver driver;

    @Before
    public void setUp() throws Exception {
        MutableCapabilities capabilities = new MutableCapabilities();
        this.driver = new RemoteWebDriver(
                new URL("https://hub.browserstack.com/wd/hub"), capabilities);
    }

    public static void mark(WebDriver remoteDriver, String status, String reason) {
        final JavascriptExecutor jse = (JavascriptExecutor) remoteDriver;
        JSONObject executorObject = new JSONObject();
        JSONObject argumentsObject = new JSONObject();
        argumentsObject.put("status", status);
        argumentsObject.put("reason", reason);
        executorObject.put("action", "setSessionStatus");
        executorObject.put("arguments", argumentsObject);
        jse.executeScript(String.format("browserstack_executor: %s", executorObject));
    }

    @After
    public void tearDown() {
        this.driver.quit();
    }
}
