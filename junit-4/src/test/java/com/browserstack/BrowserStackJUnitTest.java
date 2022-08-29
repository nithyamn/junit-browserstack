package com.browserstack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

@RunWith(Parallelized.class)
public class BrowserStackJUnitTest {
    public static String userName, accessKey;
    private static Map<String, Object> browserstackYamlMap;
    public WebDriver driver;
    @Parameter(value = 0)
    public int taskID;
    public static final String USER_DIR = "user.dir";

    @Parameters
    public static Iterable<? extends Object> data() {
        List<Integer> taskIDs = new ArrayList<Integer>();
        if (browserstackYamlMap == null) {
            File file = new File(getUserDir() + "/browserstack.yml");
            browserstackYamlMap = convertYamlFileToMap(file, new HashMap<>());
        }
        if (browserstackYamlMap != null) {
            ArrayList<LinkedHashMap<String, Object>> browserStackPlatforms = (ArrayList<LinkedHashMap<String, Object>>) browserstackYamlMap.get("platforms");
            int envs = browserStackPlatforms.size();
            for (int i = 0; i < envs; i++) {
                taskIDs.add(i);
            }
        }
        return taskIDs;
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

    @Before
    public void setUp() throws Exception {
        if (browserstackYamlMap == null) {
            File file = new File(getUserDir() + "/browserstack.yml");
            browserstackYamlMap = convertYamlFileToMap(file, new HashMap<>());
        }
        Thread.currentThread().setName(this.getClass().getName() + "@" + taskID);
        MutableCapabilities capabilities = new MutableCapabilities();
        HashMap<String, Object> bStackOptions = new HashMap<>();
        browserstackYamlMap.forEach((key, value) -> {
            if (key.equalsIgnoreCase("userName")) {
                userName = System.getenv("BROWSERSTACK_USERNAME") != null ? System.getenv("BROWSERSTACK_USERNAME") : (String) value;
            } else if (key.equalsIgnoreCase("accessKey")) {
                accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY") != null ? System.getenv("BROWSERSTACK_ACCESS_KEY") : (String) value;
            } else if (key.equalsIgnoreCase("platforms")) {
                ArrayList<LinkedHashMap<String, Object>> platformsArrayList = (ArrayList<LinkedHashMap<String, Object>>) value;
                platformsArrayList.get(taskID).forEach((k, v) -> {
                    if (k.equalsIgnoreCase("browserName") || k.equalsIgnoreCase("browserVersion")) {
                        capabilities.setCapability(k, v.toString());
                    } else {
                        bStackOptions.put(k, v.toString());
                    }
                });
            } else if (key.equalsIgnoreCase("browserstackLocal") ||
                    key.equalsIgnoreCase("local")) {
                bStackOptions.put("local", value);
            } else if (key.equalsIgnoreCase("browserStackLocalOptions") ||
                    key.equalsIgnoreCase("localOptions")) {
                if (value instanceof LinkedHashMap) {
                    ArrayList<LinkedHashMap<String, Object>> localOptionsArrayList = (ArrayList<LinkedHashMap<String, Object>>) value;
                    localOptionsArrayList.forEach(localOptionsMap -> {
                        if (((Boolean) browserstackYamlMap.get("browserstackLocal") || (Boolean) browserstackYamlMap.get("local"))
                                && localOptionsMap.containsKey("localIdentifier")) {
                            bStackOptions.put("localIdentifier", localOptionsMap.get("localIdentifier").toString());
                        }
                    });
                } else if (value instanceof HashMap) {
                    HashMap<String, ?> localOptionsHashMap = (HashMap<String, ?>) new ObjectMapper().convertValue(value, HashMap.class);
                    if (((Boolean) browserstackYamlMap.get("browserstackLocal") || (Boolean) browserstackYamlMap.get("local"))
                            && localOptionsHashMap.containsKey("localIdentifier")) {
                        bStackOptions.put("localIdentifier", localOptionsHashMap.get("localIdentifier").toString());
                    }
                }
            } else {
                bStackOptions.put(key, value);
            }
        });
        capabilities.setCapability("bstack:options", bStackOptions);
        this.driver = new RemoteWebDriver(
                new URL("https://" + userName + ":" + accessKey + "@hub.browserstack.com/wd/hub"), capabilities);
    }

    private static String getUserDir() {
        return System.getProperty(USER_DIR);
    }

    private static Map<String, Object> convertYamlFileToMap(File yamlFile, Map<String, Object> map) {
        try {
            InputStream inputStream = Files.newInputStream(yamlFile.toPath());
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            map.putAll(config);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Malformed browserstack.yml file - %s.", e));
        }
        return map;
    }

    @After
    public void tearDown() {
        this.driver.quit();
    }
}
