# junit-browserstack
[JUnit](http://junit.org/junit4/) Integration with BrowserStack.

![BrowserStack Logo](https://d98b8t1nnulk5.cloudfront.net/production/images/layout/logo-header.png?1469004780) 

![JUnit](http://junit.org/junit4/images/junit-logo.png)

## Setup
* Clone the repo
* Install dependencies `mvn install`
* Update `browserstack.yml` files inside the root directory with your [BrowserStack Username and Access Key](https://www.browserstack.com/accounts/settings). 

## Running your tests with Maven
* To run a parallel test, run `mvn test -P sample`
* To run local tests, set `browserStackLocal: true` in `browserstack.yml` and  run `mvn test -P local`

## Running your tests with Gradle
* To run a test, run `gradle sampleTest`
* To run local tests, set `browserStackLocal: true` in `browserstack.yml` and  run `gradle localTest`
* Note: Currently, the SDK setup with gradle only allows parallel execution on a single platform combination based on the no.of tests configured. `m * n` parallelization is not suppported yet

 Understand how many parallel sessions you need by using our [Parallel Test Calculator](https://www.browserstack.com/automate/parallel-calculator?ref=github)

## Notes
* You can view your test results on the [BrowserStack Automate dashboard](https://www.browserstack.com/automate)
* To test on a different set of browsers, check out our [platform configurator](https://www.browserstack.com/automate/java#setting-os-and-browser)
* You can export the environment variables for the Username and Access Key of your BrowserStack account. 

  * For Unix-like or Mac machines:
  ```
  export BROWSERSTACK_USERNAME=<browserstack-username> &&
  export BROWSERSTACK_ACCESS_KEY=<browserstack-access-key>
  ```

  * For Windows:
  ```
  set BROWSERSTACK_USERNAME=<browserstack-username>
  set BROWSERSTACK_ACCESS_KEY=<browserstack-access-key>
  ```
