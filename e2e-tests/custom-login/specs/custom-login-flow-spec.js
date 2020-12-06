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

'use strict';

const LoginHomePage = require('../../page-objects/shared/login-home-page');
const CustomSignInPage = require('../../page-objects/custom-signin-page');
const AuthenticatedHomePage = require('../../page-objects/shared/authenticated-home-page');
const ProfilePage = require('../../page-objects/shared/profile-page');
const MessagesPage = require('../../page-objects/messages-page');
const url = require('url');

describe('Custom Login Flow', () => {
  const loginHomePage = new LoginHomePage();
  const customSignInPage = new CustomSignInPage();
  const authenticatedHomePage = new AuthenticatedHomePage();
  const profile = new ProfilePage();
  const messagesPage = new MessagesPage();
  const appRoot = `http://localhost:${browser.params.appPort}`;

  beforeEach(() => {
    browser.ignoreSynchronization = true;
    if (process.env.DEFAULT_TIMEOUT_INTERVAL) {
      console.log(`Setting default timeout interval to ${process.env.DEFAULT_TIMEOUT_INTERVAL}`)
      jasmine.DEFAULT_TIMEOUT_INTERVAL = process.env.DEFAULT_TIMEOUT_INTERVAL;
    }
  });

  afterAll(() => {
    browser.driver.close().then(() => {
      browser.driver.quit();
    });
  });

  it('can login with Okta as the IDP using custom signin page', async () => {
    console.log("Loading the app login page");
    browser.get(appRoot);
    loginHomePage.waitForPageLoad();

    console.log("Loaded the app login page");

    loginHomePage.clickLoginButton();
    customSignInPage.waitForPageLoad();


    console.log("Loaded the custom login page");

    // Verify that current domain hasn't changed to okta-hosted login, rather a local custom login page
    const urlProperties = url.parse(process.env.ISSUER);
    expect(browser.getCurrentUrl()).not.toContain(urlProperties.host);
    expect(browser.getCurrentUrl()).toContain(appRoot);

    console.log("Expectations met");
    console.log("Waiting to login");

    console.log(browser.params.login.username);
    console.log(browser.params.login.password);

    await customSignInPage.login(browser.params.login.username, browser.params.login.password);
    authenticatedHomePage.waitForPageLoad();

    console.log("Loaded authenticatedHomePage");
    // authenticatedHomePage.waitForWelcomeTextToLoad();
    // expect(authenticatedHomePage.getUIText()).toContain('Welcome');
  });

  it('can access user profile', async () => {
    authenticatedHomePage.viewProfile();
    profile.waitForPageLoad();
    expect(profile.getEmailClaim()).toBe(browser.params.login.email);
  });

  xit('can access resource server messages after login', async () => {
    // If it's not implicit flow, don't test messages resource server
    if (process.env.TEST_TYPE !== 'implicit') {
      return;
    }
    authenticatedHomePage.viewMessages();
    messagesPage.waitForPageLoad();
    expect(messagesPage.getMessage()).toBeTruthy();
  });

  xit('can log the user out', async () => {
    browser.get(appRoot);
    authenticatedHomePage.waitForPageLoad();
    authenticatedHomePage.logout();
    loginHomePage.waitForPageLoad();
  });
});
