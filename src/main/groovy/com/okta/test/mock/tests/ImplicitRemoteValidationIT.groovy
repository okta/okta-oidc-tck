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
import org.testng.annotations.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.startsWith
import static com.okta.test.mock.scenarios.Scenario.IMPLICIT_FLOW_REMOTE_VALIDATION

@Scenario(IMPLICIT_FLOW_REMOTE_VALIDATION)
class ImplicitRemoteValidationIT extends ApplicationTestRunner {
    @Test
    void noToken401() {
        given()
            .redirects()
            .follow(false)
            .accept(ContentType.JSON)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(401)
            .header("WWW-Authenticate", startsWith("Bearer realm="))
    }

    @Test
    void scopeAccessTest() {
        given()
            .header("Authorization", "Bearer some.random.jwt")
            .redirects()
                .follow(false)
        .when()
            .get("http://localhost:${applicationPort}/api/messages")
        .then()
            .statusCode(403)
    }
}