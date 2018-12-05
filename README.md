Okta OAuth Test Suite
=====================

This is the start of an OAuth test suite based on [WireMock](http://wiremock.org/).

The goal of this project is to support _mostly_ black box testing against Okta's OAuth endpoints (specifically for testing error conditions, like a JWT token with an invalid signature)

This suite is runnable from a self contained jar (so you can integrate it with a non-java build tool)

You will need these things:
- The projects uberjar (this will be published in the near future, but for now see the build section below)
- testRunner.yml - See section below
- testng.xml - See section below (this will go away too, as we can programmatically configure TestNG)

## Available Scenarios:

- code-flow-local-validation - Code Flow with local access token validation
- code-flow-remote-validation - Code Flow with remote access token validation
- custom-code-flow-local-validation - Code Flow with local access token validation using sign-in widget hosted login page
- custom-code-flow-remote-validation - Code Flow with remote access token validation using sign-in widget hosted login page
- implicit-flow-local-validation - Implicit Flow with local access token validation
- implicit-flow-remote-validation - Implicit Flow with remote access token validation
- oidc-code-flow-local-validation - Open ID Connect Code Flow with local access token validation

## testRunner.yml

This is the file that defines how the test scenarios are run.

Example first:

```yml
scenarios:
  implicit-flow-local-validation:
    enabled: true
    disabledTests:
      - disabledTest1
      - disabledTest2
    ports:
      applicationPort: 8080
      mockPort: 9090
    command: node
    args:
    - test/integration-test/resource-server.js
    env:
      ISSUER: https://localhost:9999/oauth2/default
      CLIENT_ID: OOICU812
      CLIENT_SECRET: VERY_SECRET
      NODE_EXTRA_CA_CERTS: ./tck-keystore.pem
```

- `scenarios` - The top level scenarios defines how the individual scenarios are run
- `ports` - Optional, if not defined the properties will be set to an available ephemeral port
- `command` - The script or bin to execute
- `args` - each args gets a new line
- `enabled` - Optional, if this scenario doesn't apply, you can set this to false (default set to true) to not run the entire test class
- `disabledTests` - Tests in a scenario you want to disable. Each disabled test in a scenario gets a new line
- `env` - Environment variables to be set in the context of application that's run using `command` option

**Note:** The args will be interpolated with the two ports. The equivalent command line for the above block would be:
```bash
 export ISSUER=https://localhost:9999/oauth2/default
 export CLIENT_ID=OOICU812
 export CLIENT_SECRET=VERY_SECRET
 export NODE_EXTRA_CA_CERTS=./tck-keystore.pem
 node test/integration-test/resource-server.js
```

## testng.xml

Needed temporarily which allows customization of which tests to run. You will to need to understand the structure of classes and test in this project to configure one. See the [TestNG doc](http://testng.org/doc/documentation-main.html#testng-xml) for more info.

## Logging

Each forked process gets an individual log file in the format of `target/'${command}'-${date}`.

## Build this project!

This project can be build from this directory with a standard `mvn install`. This will create an uberjar located `target/okta-oidc-tck-${target}-shaded.jar`.


## Run it already !

```java
java -Dconfig=${path-to-testRunner.yml} -jar okta-oidc-tck-${version}-shaded.jar -d test-report-directory path-to-your-testng.xml
```

Test it out with okta-oidc-js project (https://github.com/okta/okta-oidc-js):
```bash
cd packages/oidc-middleware
yarn install

TCK_VERSION=0.4.0-SNAPSHOT
TCK_JAR_URL="https://oss.sonatype.org/service/local/artifact/maven/redirect?r=public&g=com.okta.oidc.tck&a=okta-oidc-tck&v=${TCK_VERSION}&e=jar&c=shaded"
TCK_FILE="./okta-oidc-tck-${TCK_VERSION}-shaded.jar"
TCK_PEM="./tck-keystore.pem"

ls "${TCK_FILE}" || curl "${TCK_JAR_URL}" -L -o "${TCK_FILE}"
unzip -p "${TCK_FILE}" BOOT-INF/classes/tck-keystore.pem > "${TCK_PEM}"

java -Dconfig=test/integration-test/resources/testRunner.yml -jar ${TCK_FILE} test/integration-test/resources/testng.xml
```

## Run Other Java tests?

Of course, bug @bdemers for details.
