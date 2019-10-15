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
import com.okta.test.mock.matchers.TckMatchers
import com.okta.test.mock.scenarios.NonceHolder
import com.okta.test.mock.wiremock.TestUtils
import io.restassured.filter.Filter
import io.restassured.filter.cookie.CookieFilter
import io.restassured.filter.session.SessionFilter
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.testng.annotations.Test

import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.urlMatcher
import static io.restassured.RestAssured.given
import static io.restassured.RestAssured.post
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.anyOf
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.either
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isOneOf
import static org.hamcrest.Matchers.not
import static org.hamcrest.Matchers.nullValue
import static org.hamcrest.text.MatchesPattern.matchesPattern
import static com.okta.test.mock.scenarios.Scenario.OIDC_CODE_FLOW_LOCAL_VALIDATION
import static com.okta.test.mock.wiremock.TestUtils.followRedirectUntilLocation

@Scenario(OIDC_CODE_FLOW_LOCAL_VALIDATION)
class OIDCCodeFlowLocalValidationIT extends ApplicationTestRunner {

    private def redirectUriPath = Config.Global.codeFlowRedirectPath

    @Test
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
        doLogin()
    }

    @Test
    void rpInitiatedLogoutTest() {
        Filter filter = new CookieFilter()
        doLogin(filter)

        // make sure these cookies are good (it's easy to mix up the cookies between the mock server and the app server)
         given()
            .redirects()
                .follow(false)
            .accept(ContentType.JSON)
            .filter(filter)
        .when()
            .get("http://localhost:${applicationPort}/")
        .then()
            .body(containsString("Welcome home"))
        .extract()

        // then trigger a logout: POST /logout
        given()
            .redirects()
                .follow(false)
            .filter(filter)
        .when()
            .post("http://localhost:${applicationPort}/logout")
        .then()
            .statusCode(302) // logout should redirect somewhere

        // TODO: there should probably be a back channel request in here, but for now this is what Spring supports

         given()
            .redirects()
                .follow(false)
            .accept(ContentType.JSON)
            .filter(filter)
        .when()
            .get("http://localhost:${applicationPort}/")
        .then()
            .statusCode(isOneOf(302, 401)) // work around for Spring Reactive returning a 401 with a bad cookie
    }

    @Test
    void wrongStateTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl) + "wrong"
        String code = "TEST_CODE"
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void noAuthCodeTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void invalidSignatureIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_invalidSignatureIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void wrongKeyIdIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_wrongKeyIdIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void issuedInFutureIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_issuedInFutureIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void expiredIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_expiredIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void wrongAudienceIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_wrongAudienceIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void invalidIssuerIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_invalidIssuerIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    @Test
    void inactiveTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_invalidNotBeforeIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .extract())
    }

    @Test
    void unsignedIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_unsignedIdTokenJwt"
        setNonce(redirectUrl, code)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        assertAccessDenied(
            given()
                .accept(ContentType.JSON)
                .cookies(response.cookies())
                .redirects()
                    .follow(false)
            .when()
                .get(requestUrl)
            .then()
                .extract())
    }

    ExtractableResponse doLogin(Filter filter = new CookieFilter()) {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE"
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

    private String getState(String redirectUrl) {
        return getQueryParamValue(redirectUrl, "state")
    }

    private String getNonce(String redirectUrl) {
        return getQueryParamValue(redirectUrl, "nonce")
    }

    private void setNonce(String redirectUrl, String key) {
        String nonce = getNonce(redirectUrl)
        NonceHolder.setNonce(key, nonce)
    }

    private String getQueryParamValue(String redirectUrl, String paramName) {
        def value = TestUtils.parseQuery(new URL(redirectUrl).query).get(paramName)
        return value != null ? value[0] : null
    }

    protected Matcher<?> loginPageMatcher() {
        return Matchers.equalTo("<html>fake_login_page<html/>")
    }

    def loginPageLocationMatcher() {
        return urlMatcher("${baseUrl}/oauth2/default/v1/authorize",
                singleQueryValue("client_id", "OOICU812"),
                singleQueryValue("redirect_uri", "http://localhost:${applicationPort}${redirectUriPath}"),
                singleQueryValue("response_type", "code"),
                singleQueryValue("scope", "profile email openid"),
                singleQueryValue("state", matchesPattern(".{6,}")))
    }

    def errorPageMatcher() {
        return containsString("Invalid credentials")
    }

    @Override
    String getProtectedPath() {
        return Config.Global.getConfigProperty("redirect.path", super.getProtectedPath())
    }

    /*
     * Different frameworks handle error conditions differently, some 401 or 403, others redirect to an error
     * page (with ending with a 200 status), and other just redirect back to the login page.
     */
    private void assertAccessDenied(ExtractableResponse response,
                                    Matcher<Response> responseMatcher = anyOf(TckMatchers.responseCode(401),
                                                                              TckMatchers.responseCode(403),
                                                                              TckMatchers.redirect(loginPageLocationMatcher()),
                                                                              TckMatchers.bodyMatcher(errorPageMatcher()))) {
        followRedirectUntilLocation(response, responseMatcher)
    }
}