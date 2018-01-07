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
const ProfilePage = require('../../page-objects/shared/profile-page');

describe('Okta Hosted Login Flow', () => {
  const loginHomePage = new LoginHomePage();
  const oktaSignInPage = new OktaSignInPage();
  const authenticatedHomePage = new AuthenticatedHomePage();
  const profile = new ProfilePage();

  beforeEach(() => {
    browser.ignoreSynchronization = true;
  });
 
  it('can login with Okta as the IDP', async () => {
    let username, password;

    // You can pass username, password for the tests either through env vars or protractor configuration
    // Env var takes precedence. If not set, the value in conf.js will be used
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
  });

  it('can access user profile', async () => {
    let email;

    if (process.env.email) {
      email = process.env.email;
    } else {
      email = browser.params.login.email;
    }

    authenticatedHomePage.viewProfile();
    profile.waitForPageLoad();
    expect(profile.containsClaim(email)).toBe(true);
  });

  it('can log the user out', async () => {
    authenticatedHomePage.logout();
    loginHomePage.waitForPageLoad();
  });
});