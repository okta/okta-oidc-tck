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
const daemonUtil = require('../tools/daemon-util');
const commonConfig = require('../tools/common-config');

// Start the resource server only for implicit flow
if (process.env.TEST_TYPE === 'implicit') {
  module.exports.config = commonConfig.configure(Promise.all([
    daemonUtil.startOktaHostedLoginServer(),
    daemonUtil.startResourceServer()
  ]));
} else {
  module.exports.config = commonConfig.configure(Promise.all([
    daemonUtil.startOktaHostedLoginServer()
  ]));
}
