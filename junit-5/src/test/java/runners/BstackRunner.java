package runners;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

public class BstackRunner implements TestTemplateInvocationContextProvider {
    public WebDriver driver;
    public String userName, accessKey;
    private static Map<String, Object> browserstackYamlMap;
    public static final String USER_DIR = "user.dir";


    public BstackRunner() {
        this.browserstackYamlMap = setupCredsAndServer();
    }

    public Map<String, Object> setupCredsAndServer() {

        File file = new File(getUserDir() + "/browserstack.yml");
        browserstackYamlMap = convertYamlFileToMap(file, new HashMap<>());
        return browserstackYamlMap;
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext extensionContext) {
        List<TestTemplateInvocationContext> desiredCapsInvocationContexts = new ArrayList<>();
        ArrayList<LinkedHashMap<String, Object>> browserStackPlatforms = (ArrayList<LinkedHashMap<String, Object>>) browserstackYamlMap.get("platforms");
        int platformSize = browserStackPlatforms.size();
        for (int i = 0; i < platformSize; i++) {
            MutableCapabilities capabilities = new MutableCapabilities();
            HashMap<String, Object> bStackOptions = new HashMap<>();
            int platform = i;
            browserstackYamlMap.forEach((key, value) -> {
                if (key.equalsIgnoreCase("userName")) {
                    userName = System.getenv("BROWSERSTACK_USERNAME") != null ? System.getenv("BROWSERSTACK_USERNAME") : (String) value;
                } else if (key.equalsIgnoreCase("accessKey")) {
                    accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY") != null ? System.getenv("BROWSERSTACK_ACCESS_KEY") : (String) value;
                } else if (key.equalsIgnoreCase("platforms")) {
                    browserStackPlatforms.get(platform).forEach((k, v) -> {
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
            desiredCapsInvocationContexts.add(invocationContext(capabilities, i));
        }
        return desiredCapsInvocationContexts.stream();
    }

    private String getUserDir() {
        return System.getProperty(USER_DIR);
    }

    private Map<String, Object> convertYamlFileToMap(File yamlFile, Map<String, Object> map) {
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

    private TestTemplateInvocationContext invocationContext(MutableCapabilities caps, int i) {
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
                            Thread.currentThread().setName("Junit5@"+i);
                            driver = new RemoteWebDriver(new URL("https://" + userName + ":" + accessKey + "@hub.browserstack.com/wd/hub"), caps);
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
