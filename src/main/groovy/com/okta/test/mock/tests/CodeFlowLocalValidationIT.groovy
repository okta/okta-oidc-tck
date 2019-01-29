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
import io.restassured.builder.ResponseSpecBuilder
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import io.restassured.specification.ResponseSpecification
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.testng.annotations.Test

import java.nio.charset.StandardCharsets

import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.urlMatcher
import static com.okta.test.mock.scenarios.Scenario.CODE_FLOW_LOCAL_VALIDATION
import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.is
import static org.hamcrest.text.MatchesPattern.matchesPattern

@Scenario(CODE_FLOW_LOCAL_VALIDATION)
class CodeFlowLocalValidationIT extends ApplicationTestRunner {

    private def redirectUriPath = Config.Global.codeFlowRedirectPath

    @Test
    void redirectToLogin() {

        given()
            .redirects()
                .follow(false)
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}/${protectedPath}")
        .then()
            .statusCode(302)
            .header("Location", is("http://localhost:${applicationPort}${redirectUriPath}".toString()))
    }

    @Test
    ExtractableResponse redirectToRemoteLogin() {

        def locationMatcher = urlMatcher("${baseUrl}/oauth2/default/v1/authorize",
                singleQueryValue("client_id", "OOICU812"),
                singleQueryValue("redirect_uri", "http://localhost:${applicationPort}${redirectUriPath}"),
                singleQueryValue("response_type", "code"),
                singleQueryValue("scope", "offline_access"),
                singleQueryValue("state", matchesPattern(".{6,}")))

        given()
            .redirects()
                .follow(false)
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}/${protectedPath}")
        .then()
            .statusCode(302)
            .header("Location", locationMatcher)
    }

    @Test
    void followRedirect() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}${redirectUriPath}")
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
            .header("Location", Matchers.equalTo("http://localhost:${applicationPort}/".toString()))
        .extract()

        given()
            .accept(ContentType.JSON)
            .cookies(response2.cookies())
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/")
        .then()
            .body(Matchers.equalTo("The message of the day is boring: joe.coder@example.com"))
    }

    @Test
    void wrongStateTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = redirectUrl.substring(redirectUrl.lastIndexOf('=')+1) + "wrong"
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
            .spec(statusCodeMatcher(401))
        .extract()
    }

    @Test
    void noAuthCodeTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = redirectUrl.substring(redirectUrl.lastIndexOf('=')+1)
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .spec(statusCodeMatcher(500))
    }

    @Test
    void invalidSignatureAccessTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = redirectUrl.substring(redirectUrl.lastIndexOf('=')+1)
        String code = "TEST_CODE_invalidSignatureAccessTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .spec(statusCodeMatcher(401))
    }

    @Test
    void wrongKeyIdAccessTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = redirectUrl.substring(redirectUrl.lastIndexOf('=')+1)
        String code = "TEST_CODE_wrongKeyIdAccessTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .spec(statusCodeMatcher(401))
    }

    @Test (enabled = false)
    void wrongScopeAccessTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = redirectUrl.substring(redirectUrl.lastIndexOf('=')+1)
        String code = "TEST_CODE_wrongScopeAccessTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        ExtractableResponse response2 = given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
               .follow(false)
        .when()
            .get(requestUrl)
        .then().log().everything()
            .statusCode(302)
            .header("Location", Matchers.equalTo("http://localhost:${applicationPort}/".toString()))
        .extract()

        given()
            .accept(ContentType.JSON)
            .cookies(response2.cookies())
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/")
        .then().log().everything()
            .spec(statusCodeMatcher(403))
    }

    @Test
    void wrongAudienceAccessTokenJwtTest() {
        ExtractableResponse response = redirectToRemoteLogin()
        String redirectUrl = response.header("Location")
        String state = redirectUrl.substring(redirectUrl.lastIndexOf('=')+1)
        String code = "TEST_CODE_wrongAudienceAccessTokenJwt"
        String requestUrl = "http://localhost:${applicationPort}${redirectUriPath}?code=${code}&state=${state}"

        given()
            .accept(ContentType.JSON)
            .cookies(response.cookies())
            .redirects()
                .follow(false)
        .when()
            .get(requestUrl)
        .then()
            .spec(statusCodeMatcher(401))
    }

    protected Matcher<?> loginPageMatcher() {
        return Matchers.equalTo("<html>fake_login_page<html/>")
    }

    private shouldErrorsRedirectToLogin() {
        return Boolean.getBoolean("okta.tck.redirectOnError") || System.getenv().getOrDefault("OKTA_TCK_REDIRECT_ON_ERROR", "false").toBoolean()
    }

    protected ResponseSpecification statusCodeMatcher(int statusCode) {
        if (shouldErrorsRedirectToLogin()) {
            return new ResponseSpecBuilder()
                    .expectStatusCode(302)
                    .expectHeader("Location", is("http://localhost:${applicationPort}${redirectUriPath}".toString()))
                    .build()
        } else {
            return new ResponseSpecBuilder()
                    .expectStatusCode(statusCode)
                    .build()
        }
    }

    @Override
    String getProtectedPath() {
        return System.getProperty("redirect.path", super.getProtectedPath())
    }
}