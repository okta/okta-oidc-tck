/*!
 * Copyright (c) 2015-2018, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License, Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 */

/* eslint import/no-unresolved:0, import/no-extraneous-dependencies:0, no-console:0 */
/* global jasmine */
const jasmineReporters = require('jasmine-reporters');
const daemonUtil = require('../tools/daemon-util');

const promises = Promise.all([
  daemonUtil.startOktaHostedLoginServer(),
  daemonUtil.startResourceServer()
]);

const config = {
  // Set the following env vars to match your test environment
  // Note the USERNAME should be of the form "username@email.com"
  params: {
    login: {
      username: process.env.USERNAME,
      password: process.env.PASSWORD
    },
    appRoot: 'http://localhost:8080'
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
    return new Promise(resolve => setTimeout(() => resolve(), 1000));
  },
  specs: ['specs/*.js'],
  restartBrowserBetweenTests: false,
  capabilities: {},
};

// Run Headless chrome in Travis, else Chrome
if (process.env.TRAVIS) {
  console.log('-- Using Chrome Headless --');
  config.capabilities = {
    'browserName': 'chrome',
    chromeOptions: {
      args: ['--headless','--disable-gpu','--window-size=1600x1200']
    }
  }
} else {
  config.capabilities.browserName = 'chrome';
}

module.exports.config = config;