package runners;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import utils.SetupLocalTesting;

import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

public class BstackRunner implements TestTemplateInvocationContextProvider {
    public WebDriver driver;
    public DesiredCapabilities capabilities;
    public String username, accesskey, server;
    private JSONObject mainConfig;
    private JSONObject browserConfig;
    private JSONObject profileConfig;
    private JSONObject testConfig;
    private JSONObject platformConfig;
    private JSONObject commonCapsConfig;
    private HashMap<String, String> allCapsMap;
    private HashMap<String, String> commonCapsMap;
    private HashMap<String, String> bstackOptions;
    private HashMap<String, String> bstackOptionsCommonCaps;
    private HashMap<String, String> bstackOptionsPlatform;


    public BstackRunner() {
        this.username = setupCredentials()[0];
        this.accesskey = setupCredentials()[1];
        this.server = setupCredentials()[2];
    }

    public String[] setupCredentials() {
        try {
            JSONParser parse = new JSONParser();
            mainConfig = (JSONObject) parse.parse(new FileReader(System.getProperty("user.dir") + "/src/test/resources/caps.json"));
            server = (String) mainConfig.get("server");
            username = System.getenv("BROWSERSTACK_USERNAME");
            if (username == null) {
                username = (String) mainConfig.get("userName");
            }
            accesskey = System.getenv("BROWSERSTACK_ACCESS_KEY");
            if (accesskey == null) {
                accesskey = (String) mainConfig.get("accessKey");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return (new String[]{username, accesskey, server});
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        List<TestTemplateInvocationContext> desiredCapsInvocationContexts = new ArrayList<>();

        //picks the test profile based on the maven command executed - single, local, parallel
        String profile = System.getProperty("config");

        try {
            testConfig = (JSONObject) mainConfig.get("tests");
            profileConfig = (JSONObject) testConfig.get(profile);
            platformConfig = (JSONObject) profileConfig.get("platform");
            commonCapsConfig = (JSONObject) profileConfig.get("common_caps");
            commonCapsMap = (HashMap<String, String>) commonCapsConfig;

            Iterator platformIterator = platformConfig.keySet().iterator();
            while (platformIterator.hasNext()) {

                capabilities = new DesiredCapabilities();
                Iterator commonCapsIterator = commonCapsMap.entrySet().iterator();
                while (commonCapsIterator.hasNext()) {
                    Map.Entry capsName = (Map.Entry) commonCapsIterator.next();
                    if ("bstack:options".equals(capsName.getKey().toString())) {
                        bstackOptionsCommonCaps = (HashMap<String, String>) commonCapsConfig.get("bstack:options");
                    } else {
                        capabilities.setCapability((String) capsName.getKey(), capsName.getValue());
                    }
                }

                final String platformType = (String) platformIterator.next();
                browserConfig = (JSONObject) platformConfig.get(platformType);
                allCapsMap = (HashMap<String, String>) browserConfig;

                Iterator finalCapsIterator = allCapsMap.entrySet().iterator();
                while (finalCapsIterator.hasNext()) {
                    Map.Entry platformName = (Map.Entry) finalCapsIterator.next();
                    if ("bstack:options".equals(platformName.getKey().toString())) {
                        bstackOptionsPlatform = (HashMap<String, String>) browserConfig.get("bstack:options");
                    } else {
                        capabilities.setCapability((String) platformName.getKey(), platformName.getValue());
                    }
                }
                //Initializing local testing connection
                if (bstackOptionsCommonCaps.containsKey("local")) {
                    HashMap<String, String> localOptions = new HashMap<>();
                    localOptions.put("key", accesskey);
                    //Add more local options here, e.g. forceLocal, localIdentifier, etc.
                    SetupLocalTesting.createInstance(localOptions);
                }
                bstackOptions = new HashMap<>();
                bstackOptions.putAll(bstackOptionsCommonCaps);
                bstackOptions.putAll(bstackOptionsPlatform);
                capabilities.setCapability("bstack:options", bstackOptions);

                desiredCapsInvocationContexts.add(invocationContext(capabilities));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return desiredCapsInvocationContexts.stream();
    }

    private TestTemplateInvocationContext invocationContext(DesiredCapabilities caps) {
        return new TestTemplateInvocationContext() {

            @Override
            public List<Extension> getAdditionalExtensions() {

                return Collections.singletonList(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext,
                                                     ExtensionContext extensionContext) {
                        return parameterContext.getParameter().getType().equals(WebDriver.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext,
                                                   ExtensionContext extensionContext) {
                        try {
                            driver = new RemoteWebDriver(new URL("https://" + username + ":" + accesskey + "@" + server + "/wd/hub"), caps);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        return driver;
                    }
                });
            }
        };
    }
}
