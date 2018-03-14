End to End tests
================

This section contains the Happy path E2E tests for various flows run using Okta sample apps

## Overview

This directory contains protractor based E2E tests that will be used to test the Okta sample applications
The tests are divided into the following sections -

#### Okta-Hosted Login
This tests the E2E flow for user login that uses the hosted login page on your Okta org

#### Custom Login Page
This tests the E2E flow for user login that uses the Okta Sign-In Widget on a custom login page

#### Resource Server
This tests the E2E flow for authenticating requests (using access tokens issued by Okta) against a resource server

## How to run the tests

> **NOTE:** It is the responsibility of the sample apps that run these tests to install the required dependencies & setup the environment before running the tests.

The protractor tests have the following npm dependencies -

```json
"forever-monitor": "^1.7.1",
"node-cmd": "^3.0.0",
"jasmine-reporters": "^2.2.0",
"platform": "^1.3.5",
"protractor": "^5.1.0",
"wait-on": "^2.0.2"
```
Depending on the sample app & scenario you're testing you will need to set the following environment variables:
- ISSUER
- CLIENT_ID
- USERNAME (USER_NAME for Windows)
- PASSWORD

The sub-directories for each scenario contain a protractor configuration file (conf.js) which runs the specs for each scenario.

E.g To run the Okta-Hosted Login tests, run the following command

```bash
protractor okta-oidc-tck/e2e-tests/okta-hosted-login/conf.js
```

The sample apps are expected to clone this tck repo locally to invoke the tests.
