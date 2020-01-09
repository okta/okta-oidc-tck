/*
 * Copyright 2020-Present Okta, Inc.
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
package com.okta.test.mock.tests

import com.okta.test.mock.scenarios.NonceHolder
import com.okta.test.mock.wiremock.TestUtils

trait UrlParser {

    String getState(String redirectUrl) {
        return getQueryParamValue(redirectUrl, "state")
    }

    String getNonce(String redirectUrl) {
        return getQueryParamValue(redirectUrl, "nonce")
    }

    void setNonce(String redirectUrl, String key) {
        String nonce = getNonce(redirectUrl)
        NonceHolder.setNonce(key, nonce)
    }

    String getQueryParamValue(String redirectUrl, String paramName) {
        def value = TestUtils.parseQuery(new URL(redirectUrl).query).get(paramName)
        return value != null ? value[0] : null
    }
}