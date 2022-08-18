package runners;

import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
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
    public DesiredCapabilities capabilities;
    public String username, accesskey;
    private static Map<String, Object> browserstackYamlMap;
    public static final String USER_DIR = "user.dir";


    public BstackRunner() {
        this.browserstackYamlMap = setupCredsAndServer();
    }

    public Map<String, Object> setupCredsAndServer() {

        File file = new File(getUserDir() + "/browserstack.yml");
        browserstackYamlMap = convertYamlFileToMap(file, new HashMap<>());
        username = System.getenv("BROWSERSTACK_USERNAME");
        if (username == null) {
            username = browserstackYamlMap.get("userName").toString();
        }
        accesskey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (accesskey == null) {
            accesskey = browserstackYamlMap.get("accessKey").toString();
        }
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
            capabilities = new DesiredCapabilities();
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

    private TestTemplateInvocationContext invocationContext(DesiredCapabilities caps, int i) {
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
                            driver = new RemoteWebDriver(new URL("https://" + username + ":" + accesskey + "@hub.browserstack.com/wd/hub"), caps);
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
