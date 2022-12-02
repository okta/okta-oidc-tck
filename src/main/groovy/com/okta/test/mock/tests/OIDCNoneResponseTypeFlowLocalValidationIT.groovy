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
import com.okta.test.mock.matchers.TckMatchers
import io.restassured.filter.Filter
import io.restassured.filter.cookie.CookieFilter
import io.restassured.http.ContentType
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import org.hamcrest.Matcher
import org.testng.annotations.Test

import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.urlMatcher
import static com.okta.test.mock.scenarios.Scenario.OIDC_CODE_FLOW_LOCAL_VALIDATION
import static com.okta.test.mock.scenarios.Scenario.OIDC_FLOW_NONE_RESPONSE_TYPE_VALIDATION
import static com.okta.test.mock.wiremock.TestUtils.followRedirectUntilLocation
import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.*
import static org.hamcrest.text.MatchesPattern.matchesPattern

@Scenario(OIDC_FLOW_NONE_RESPONSE_TYPE_VALIDATION)
class OIDCNoneResponseTypeFlowLocalValidationIT extends BaseValidationIT {

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

    def errorPageMatcher() {
        return containsString("Invalid credentials")
    }

    Matcher loginPageLocationMatcher() {
        return urlMatcher("${baseUrl}/oauth2/default/v1/authorize",
                singleQueryValue("client_id", "OOICU812"),
                singleQueryValue("redirect_uri", "http://localhost:${applicationPort}${redirectUriPath}"),
                singleQueryValue("response_type", "none"),
                singleQueryValue("state", matchesPattern(".{6,}")))
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
        followRedirectUntilLocation(response, responseMatcher, 3, "http://localhost:${applicationPort}")
    }
}