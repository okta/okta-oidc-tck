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
    browser.get(browser.params.appRoot);
    loginHomePage.waitForPageLoad();

    loginHomePage.clickLoginButton();
    oktaSignInPage.waitForPageLoad();

    // Verify that curent domain has changed to okta-hosted login page
    expect(oktaSignInPage.urlContains('okta')).toBe(true);
    expect(oktaSignInPage.urlContains(browser.params.appRoot)).toBe(false);

    await oktaSignInPage.login(browser.params.login.username, browser.params.login.password);
    authenticatedHomePage.waitForPageLoad();
  });

  it('can access user profile', async () => {
    authenticatedHomePage.viewProfile();
    profile.waitForPageLoad();
    expect(profile.getEmailClaim()).toBe(browser.params.login.email);
  });

  it('can log the user out', async () => {
    authenticatedHomePage.logout();
    loginHomePage.waitForPageLoad();
  });
});