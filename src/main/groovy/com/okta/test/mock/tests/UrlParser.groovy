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