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

import com.okta.test.mock.Config
import com.okta.test.mock.application.ApplicationTestRunner
import com.okta.test.mock.matchers.TckMatchers
import io.restassured.filter.Filter
import io.restassured.filter.cookie.CookieFilter
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import org.hamcrest.Matcher
import org.hamcrest.Matchers

import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.urlMatcher
import static com.okta.test.mock.wiremock.TestUtils.followRedirectUntilLocation
import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.text.MatchesPattern.matchesPattern

abstract class BaseValidationIT extends ApplicationTestRunner implements UrlParser {

    protected Matcher<?> loginPageMatcher() {
        return Matchers.equalTo("<html>fake_login_page<html/>")
    }

    String getRedirectUriPath() {
        return Config.Global.codeFlowRedirectPath
    }

    Matcher loginPageLocationMatcher(String scope="profile email openid") {
        return urlMatcher("${baseUrl}/oauth2/default/v1/authorize",
                singleQueryValue("client_id", "OOICU812"),
                singleQueryValue("redirect_uri", "http://localhost:${applicationPort}${redirectUriPath}"),
                singleQueryValue("response_type", "code"),
                singleQueryValue("scope", scope),
                singleQueryValue("state", matchesPattern(".{6,}")))
    }

    ExtractableResponse redirectToRemoteLogin() {

        return given()
                .redirects()
                .follow(false)
                .accept(ContentType.JSON)
            .when()
                .get("http://localhost:${applicationPort}${protectedPath}")
            .then()
                .statusCode(302)
                .header("Location", loginPageLocationMatcher())
            .extract()
    }

    ExtractableResponse doLogin(Filter filter = new CookieFilter(), String code = "TEST_CODE") {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        ExtractableResponse initialResponse = given()
                .filter(filter)
                .redirects()
                .follow(false)
                .accept(ContentType.JSON)
                .cookies(response.cookies())
            .when()
                .get(requestUrl)
            .then()
                .extract()

        return followRedirectUntilLocation(initialResponse,
                allOf(TckMatchers.responseCode(200),
                        TckMatchers.bodyMatcher(containsString("Welcome home"))),
                3,
                "http://localhost:${applicationPort}",
                filter)
    }

    @Override
    String getProtectedPath() {
        return System.getProperty("redirect.path", super.getProtectedPath())
    }
}
