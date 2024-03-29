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
import org.hamcrest.Matcher
import org.testng.annotations.Test

import static com.okta.test.mock.scenarios.Scenario.IMPLICIT_FLOW_LOCAL_VALIDATION
import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.hasItems
import static org.hamcrest.Matchers.startsWith

@Scenario(IMPLICIT_FLOW_LOCAL_VALIDATION)
class ImplicitLocalValidationIT extends ApplicationTestRunner {
    @Test
    void noToken401Test() {
        given()
            .redirects()
            .follow(false)
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(401)
            .header("WWW-Authenticate", authenticateHeader())
    }

    @Test
    void accessKeyNonTrustedKeyTest() {
        given()
            .header("Authorization", "Bearer ${IMPLICIT_FLOW_LOCAL_VALIDATION.definition.invalidAccessTokenJwt}")
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(401)
            .header("WWW-Authenticate", authenticateHeader())
    }

    @Test
    void nonJWTAccessKeyTest() {
        given()
            .header("Authorization", "Bearer not-a-jwt")
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(401)
            .header("WWW-Authenticate", authenticateHeader())
    }

    @Test
    void wrongAudienceAccessTokenTest() {
        given()
            .header("Authorization", "Bearer ${IMPLICIT_FLOW_LOCAL_VALIDATION.definition.wrongAudienceAccessTokenJwt}")
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(401)
            .header("WWW-Authenticate", authenticateHeader())
    }

    @Test
    void scopeAccessTokenTest() {
        given()
            .header("Authorization", "Bearer ${IMPLICIT_FLOW_LOCAL_VALIDATION.definition.accessTokenJwt}")
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(200)
            .body("messages.text", hasItems("I am a robot.", "Hello, world!"))
    }

    @Test
    void wrongScopeAccessTokenTest() {
        given()
            .header("Authorization", "Bearer ${IMPLICIT_FLOW_LOCAL_VALIDATION.definition.wrongScopeAccessTokenJwt}")
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(403)
    }

    @Test
    void inactiveAccessTokenTest() {
        given()
            .header("Authorization", "Bearer ${IMPLICIT_FLOW_LOCAL_VALIDATION.definition.inactiveAccessTokenJwt}")
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(401)
    }

    private Matcher authenticateHeader() {
        return startsWith("Bearer")
    }
}
