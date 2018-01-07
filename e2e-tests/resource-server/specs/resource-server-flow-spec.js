/*!
 * Copyright (c) 2015-2016, Okta, Inc. and/or its affiliates. All rights reserved.
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
const OktaSignInPage = require('../../page-objects/okta-signin-page');
const AuthenticatedHomePage = require('../../page-objects/shared/authenticated-home-page');
const MessagesPage = require('../../page-objects/messages-page');

describe('Resource server Flow', () => {
  const loginHomePage = new LoginHomePage();
  const oktaSignInPage = new OktaSignInPage();
  const authenticatedHomePage = new AuthenticatedHomePage();
  const messagesPage = new MessagesPage();

  beforeEach(() => {
    browser.ignoreSynchronization = true;
  });
 
  it('can access resource server messages after login', async () => {
    let username, password;

    // You can pass username, password for the tests either through env vars or protractor configuration
    // Env vars takes precedence. If not set, the value in conf.js will be used
    if (process.env.username) {
      username = process.env.username;
    } else {
      username = browser.params.login.username;
    }

    if (process.env.password) {
      password = process.env.password;
    } else {
      password = browser.params.login.password;
    }

    browser.get('http://localhost:8080/');
    loginHomePage.waitForPageLoad();

    loginHomePage.clickLoginButton();
    oktaSignInPage.waitForPageLoad();

    await oktaSignInPage.login(username, password);
    authenticatedHomePage.waitForPageLoad();

    authenticatedHomePage.viewMessages();
    messagesPage.waitForPageLoad();
    expect(messagesPage.getMessage()).toBeTruthy();
  });
});