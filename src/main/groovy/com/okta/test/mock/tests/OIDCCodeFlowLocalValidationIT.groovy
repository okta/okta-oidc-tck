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

import com.okta.test.mock.Scenario
import com.okta.test.mock.application.ApplicationTestRunner
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.testng.annotations.Test
import java.util.regex.Pattern

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.is
import static org.hamcrest.text.MatchesPattern.matchesPattern
import static com.okta.test.mock.scenarios.Scenario.OIDC_CODE_FLOW_LOCAL_VALIDATION

@Scenario(OIDC_CODE_FLOW_LOCAL_VALIDATION)
class OIDCCodeFlowLocalValidationIT extends ApplicationTestRunner {
    @Test
    ExtractableResponse redirectToRemoteLogin() {
        String expectedRedirect = Pattern.quote(
                "http://localhost:${doGetMockPort()}/oauth2/default/v1/authorize" +
                "?scope=profile%20email%20openid" +
                "&response_type=code" +
                "&redirect_uri=http%3A%2F%2Flocalhost%3A${applicationPort}%2Fauthorization-code%2Fcallback" +
                "&state=")+".{36}" +
                "&client_id=OOICU812"

        return given()
            .redirects()
                .follow(false)
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}/login")
        .then()
            .statusCode(302)
            .header("Location", matchesPattern(expectedRedirect))
        .extract()
    }

    @Test
    void followRedirect() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}/login")
        .then()
            .statusCode(200)
            .body(loginPageMatcher())
    }

    @Test
    void respondWithCode() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(true)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(200)
            .body(Matchers.containsString("Welcome home"))
        .extract()
    }

    @Test
    void wrongStateTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl) + "wrong"
        String code = "TEST_CODE"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
        .extract()
    }

    @Test
    void noAuthCodeTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
        .extract()
    }

    @Test
    void invalidSignatureIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_invalidSignatureIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    @Test
    void wrongKeyIdIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_wrongKeyIdIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    @Test
    void issuedInFutureIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_issuedInFutureIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    @Test
    void expiredIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_expiredIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    @Test
    void wrongAudienceIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_wrongAudienceIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    @Test
    void invalidIssuerIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_invalidIssuerIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    @Test
    void inactiveTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_invalidNotBeforeIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    @Test
    void unsignedIdTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = getState(redirectUrl)
        String code = "TEST_CODE_unsignedIdTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}/authorization-code/callback?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .statusCode(401)
    }

    private String getState(String redirectUrl) {
        String state
        String[] params = redirectUrl.split("&")
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            if (name.equals("state")) {
                state = value
                break
            }   
        }
        return state
    }

    protected Matcher<?> loginPageMatcher() {
        return Matchers.equalTo("<html>fake_login_page<html/>")
    }
}