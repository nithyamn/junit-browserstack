package com.browserstack;

import com.browserstack.local.Local;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.openqa.selenium.remote.SessionId;

@RunWith(Parallelized.class)
public class BrowserStackJUnitTest {
    public WebDriver driver;
    private Local bsLocal;
    public static String username, accessKey;

    private static JSONObject config;

    @Parameter(value = 0)
    public int taskID;

    @Parameters
    public static Iterable<? extends Object> data() throws Exception {
        List<Integer> taskIDs = new ArrayList<Integer>();

        if (System.getProperty("config") != null) {
            JSONParser parser = new JSONParser();
            config = (JSONObject) parser
                    .parse(new FileReader("src/test/resources/conf/" + System.getProperty("config")));
            int envs = ((JSONArray) config.get("environments")).size();

            for (int i = 0; i < envs; i++) {
                taskIDs.add(i);
            }
        }

        return taskIDs;
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        JSONArray envs = (JSONArray) config.get("environments");

        DesiredCapabilities capabilities = new DesiredCapabilities();

        Map<String, Object> envCapabilities = (Map<String, Object>) envs.get(taskID);
        Iterator<Map.Entry<String, Object>> it = envCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
            capabilities.setCapability(pair.getKey().toString(), pair.getValue());
        }

        Map<String, Object> commonCapabilities = (Map<String, Object>) config.get("capabilities");
        it = commonCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
            Object envData = capabilities.getCapability(pair.getKey().toString());
            Object resultData = pair.getValue();
            if (envData != null && envData.getClass() == JSONObject.class) {
                ((JSONObject) resultData).putAll((JSONObject) envData);
            }
            capabilities.setCapability(pair.getKey().toString(), resultData);
        }

        username = System.getenv("BROWSERSTACK_USERNAME");
        if (username == null) {
            username = (String) config.get("user");
        }

        accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (accessKey == null) {
            accessKey = (String) config.get("key");
        }

        this.checkAndStartBrowserStackLocal(capabilities, accessKey);

        driver = new RemoteWebDriver(
                new URL("https://" + username + ":" + accessKey + "@" + config.get("server") + "/wd/hub"), capabilities);
    }

    public void checkAndStartBrowserStackLocal (DesiredCapabilities capabilities, String accessKey) throws Exception {
        if (bsLocal != null) {
            return;
        }
        if (capabilities.getCapability("bstack:options") != null
                && ((JSONObject) capabilities.getCapability("bstack:options")).get("local") != null
                && ((Boolean) ((JSONObject) capabilities.getCapability("bstack:options")).get("local")) == true) {
            bsLocal = new Local();
            Map<String, String> options = new HashMap<String, String>();
            options.put("key", accessKey);
            bsLocal.start(options);
        }
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
        if (bsLocal != null) {
            bsLocal.stop();
        }
    }

    public static void mark(SessionId sessionID, String status, String reason) throws URISyntaxException, UnsupportedEncodingException, IOException {
        URI uri = new URI("https://"+username+":"+accessKey+"@api.browserstack.com/automate/sessions/"+sessionID+".json");
        HttpPut putRequest = new HttpPut(uri);

        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add((new BasicNameValuePair("status", status)));
        nameValuePairs.add((new BasicNameValuePair("reason", reason)));
        putRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        HttpClientBuilder.create().build().execute(putRequest);
    }
}