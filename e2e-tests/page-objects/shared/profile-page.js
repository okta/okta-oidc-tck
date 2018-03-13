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

const util = require('./util');

class ProfilePage {

  constructor() {
    this.$emailClaim = $('#claim-email');
    this.$familyNameClaim = $('#claim-family_name');
    this.$givenNameClaim = $('#claim-given_name');
    this.$localeClaim = $('#claim-locale');
    this.$nameClaim = $('#claim-name');
    this.$preferredUsernameClaim = $('#claim-preferred_username');
    this.$subClaim = $('#claim-sub');
    this.$zoneInfoClaim = $('#claim-zoneinfo');
  }

  waitForPageLoad() {
    return util.wait(this.$emailClaim);
  }

  logout() {
    return this.$logoutLink.click();
  }

  getEmailClaim() {
    return this.$emailClaim.getText();
  }

  getFamilyNameClaim() {
    return this.$familyNameClaim.getText();
  }

  getGivenNameClaim() {
    return this.$givenNameClaim.getText();
  }

  getLocaleClaim() {
    return this.$localeClaim.getText();
  }

  getNameClaim() {
    return this.$nameClaim.getText();
  }

  getPreferredUsernameClaim() {
    return this.$preferredUsernameClaim.getText();
  }

  getSubClaim() {
    return this.$subClaim.getText();
  }

  getZoneInfoClaim() {
    return this.$zoneInfoClaim.getText();
  }
}

module.exports = ProfilePage;
