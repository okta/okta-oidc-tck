/*
 * Copyright 2017 Okta, Inc.
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
import com.okta.test.mock.Scenario
import com.okta.test.mock.application.ApplicationTestRunner
import com.okta.test.mock.wiremock.TestUtils
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.testng.annotations.Test

import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.urlMatcher
import static com.okta.test.mock.scenarios.Scenario.PKCE_CODE_FLOW_REMOTE_VALIDATION
import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.isOneOf
import static org.hamcrest.text.MatchesPattern.matchesPattern

@Scenario(PKCE_CODE_FLOW_REMOTE_VALIDATION)
class PkceCodeFlowRemoteValidationIT extends ApplicationTestRunner {

    private def redirectUriPath = Config.Global.codeFlowRedirectPath

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

    @Test
    void followRedirect() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}${protectedPath}")
        .then()
            .statusCode(200)
            .body(loginPageMatcher())
    }

    @Test
    void respondWithCode() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = TestUtils.parseQuery(new URL(redirectUrl).query).get("state").get(0)
        String code = "TEST_CODE"
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        ExtractableResponse response2 = given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(302)
            .header("Location", isOneOf("/", "http://localhost:${applicationPort}/".toString()))
        .extract()

        given()
            .accept(ContentType.JSON)
            .cookies(response2.cookies())
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/")
        .then()
            .body(Matchers.containsString("Welcome home"))

    }

    protected Matcher<?> loginPageMatcher() {
        return Matchers.equalTo("<html>fake_login_page<html/>")
    }

    protected Matcher loginPageLocationMatcher() {
        return urlMatcher("${baseUrl}/oauth2/default/v1/authorize",
                singleQueryValue("client_id", "OOICU812"),
                singleQueryValue("redirect_uri", "http://localhost:${applicationPort}${redirectUriPath}"),
                singleQueryValue("response_type", "code"),
                singleQueryValue("scope", "offline_access"),
                singleQueryValue("state", matchesPattern(".{6,}")))
    }

    @Override
    String getProtectedPath() {
        return System.getProperty("redirect.path", super.getProtectedPath())
    }
}