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
package com.okta.test.mock.scenarios

import com.github.tomakehurst.wiremock.WireMockServer
import com.okta.test.mock.Config

import java.util.regex.Pattern

import static com.github.tomakehurst.wiremock.client.WireMock.*

class OIDCNoneResponseTypeValidationScenarioDefinition implements ScenarioDefinition {

    String clientId = "OOICU812"

    @Override
    void configureHttpMock(WireMockServer wireMockServer, String baseUrl) {

        def redirectUriPath = Config.Global.codeFlowRedirectPath

        wireMockServer.stubFor(
                get("/oauth2/default/.well-known/openid-configuration")
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("discovery.json")
                        .withTransformers("gstring-template")))

        wireMockServer.stubFor(
                get(urlPathEqualTo("/oauth2/default/v1/authorize"))
                        .withQueryParam("client_id", matching(clientId))
                        .withQueryParam("redirect_uri", matching(Pattern.quote("http://localhost:")+ "\\d+${redirectUriPath}"))
                        .withQueryParam("response_type", matching("none"))
                        .withQueryParam("state", matching(".{6,}"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("<html>fake_login_page<html/>")))
    }

}