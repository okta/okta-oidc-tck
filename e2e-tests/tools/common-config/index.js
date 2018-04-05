/*
 * Copyright 2017 Okta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* global jasmine */
const path = require('path');
// Env vars on new lines in the form of NAME=VALUE in testenv file at root of project
const dotenv = require('dotenv');
dotenv.config({path: path.join(require('os').homedir(), '.okta', 'testenv')});
dotenv.config({path: path.join(__dirname, '..', '..', '..', '..', 'testenv')});

const jasmineReporters = require('jasmine-reporters');

const commonConfig = module.exports = {};

commonConfig.configure = function (promises) {
  const config = {
    // Set the following env vars to match your test environment
    // Note the USERNAME should be of the form "username@email.com"
    params: {
      login: {
        // In windows, USERNAME is a built-in env var, which we don't want to change
        username: process.env.USER_NAME || process.env.USERNAME,
        password: process.env.PASSWORD,
        email: process.env.USER_NAME || process.env.USERNAME,
      },
      // App servers start on port 8080 but configurable using env var
      appPort: process.env.PORT || 8080,
      appTimeOut: process.env.TIMEOUT || 1000
    },
    framework: 'jasmine2',
    beforeLaunch() {
      return promises;
    },
    onPrepare() {
      jasmine.getEnv().addReporter(new jasmineReporters.JUnitXmlReporter({
        savePath: 'build2/reports/junit',
        filePrefix: 'results',
      }));
    },
    afterLaunch() {
      promises.then((childProcesses) => {
        childProcesses.forEach(child => child.stop());
      });
      return new Promise(resolve => setTimeout(() => resolve(), browser.params.appTimeOut));
    },
    specs: ['specs/*.js'],
    restartBrowserBetweenTests: false,
    capabilities: {
      browserName: 'chrome',
    }
  };

  if (process.env.SAUCE_USERNAME) {
    console.log('-- Using SauceLabs --');
    config.sauceUser = process.env.SAUCE_USERNAME;
    config.sauceKey = process.env.SAUCE_ACCESS_KEY;
    config.capabilities.tunnelIdentifier = process.env.TRAVIS_JOB_NUMBER;
    config.capabilities.build = process.env.TRAVIS_BUILD_NUMBER;
    config.capabilities.screenResolution = '1600x1200';
    config.capabilities.extendedDebugging = true;
  }
  // Run Chrome Headless
  else if (process.env.CHROME_HEADLESS || process.env.TRAVIS) {
    console.log('-- Using Chrome Headless --');
    config.capabilities.chromeOptions = {
      args: ['--headless','--disable-gpu','--window-size=1600x1200']
    };

    // work around for chrome crashes on Travis-CI
    if (process.env.TRAVIS) {
      config.capabilities.chromeOptions.args.push('--no-sandbox');
    }
  }
  // otherwise just launch the browser locally
  else {
    console.log('-- Using Chrome --');
  }
  return config;
};
