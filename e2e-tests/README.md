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
The sub-directories for each scenario contain a protractor configuration file (conf.js) which runs the specs for each scenario.

E.g To run the Okta-Hosted Login tests, run the following command 

```bash
protractor okta-oidc-tck/e2e-tests/okta-hosted-login/conf.js
```

The sample apps are expected to clone this tck repo locally to invoke the tests.

It is the responsibility of the sample apps that run these tests to install the required dependencies & setup the environment before running the tests.
