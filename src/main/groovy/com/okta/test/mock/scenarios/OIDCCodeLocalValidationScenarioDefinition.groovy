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
import com.okta.test.mock.wiremock.TestUtils
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.apache.commons.codec.binary.Base64

import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.containing
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo

class OIDCCodeLocalValidationScenarioDefinition implements ScenarioDefinition {

    final Map<String, String> bindingMap = new HashMap<>()
    String pubKeyE
    String pubKeyN
    String accessTokenJwt
    String idTokenJwt
    String wrongKeyIdIdTokenJwt
    String wrongAudienceIdTokenJwt
    String issuedInFutureIdTokenJwt
    String expiredIdTokenJwt
    String invalidSignatureIdTokenJwt
    String invalidIssuerIdTokenJwt
    String invalidNotBeforeIdTokenJwt
    String unsignedIdTokenJwt
    String clientId = "OOICU812"
    String clientSecret = "VERY_SECRET"
    String authHeader = "Basic " + "${clientId}:${clientSecret}".bytes.encodeBase64().toString()

    Map getBindingMap() {
        // late binding
        return bindingMap
    }

    void configureBindings(String issuerUrl) {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(4096)
        KeyPair invalidKeyPair = keyPairGenerator.generateKeyPair()
        KeyPair keyPair = keyPairGenerator.generateKeyPair()

        pubKeyE = Base64.encodeBase64URLSafeString(TestUtils.toIntegerBytes(keyPair.publicKey.getPublicExponent()))
        pubKeyN = Base64.encodeBase64URLSafeString(TestUtils.toIntegerBytes(keyPair.publicKey.getModulus()))

        Instant now = Instant.now()
        accessTokenJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://default")
                .claim("scp", ["profile", "openid", "email"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        idTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        wrongKeyIdIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('WRONG_TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        wrongAudienceIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("WRONG_OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        issuedInFutureIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        expiredIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        invalidSignatureIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, invalidKeyPair.privateKey)
                .compact()

        invalidIssuerIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer("${issuerUrl}_INVALID")
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        invalidNotBeforeIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.privateKey)
                .compact()

        unsignedIdTokenJwt =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .compact()
    }

    @Override
    void configureHttpMock(WireMockServer wireMockServer, String baseUrl) {

        def redirectUriPath = Config.Global.codeFlowRedirectPath
        def redirectUriPathEscaped = URLEncoder.encode(redirectUriPath, StandardCharsets.UTF_8.toString())

        configureBindings("${baseUrl}/oauth2/default")

        bindingMap.putAll([
                accessTokenJwt: accessTokenJwt,
                pubKeyE: pubKeyE,
                pubKeyN: pubKeyN,
                idTokenJwt: idTokenJwt
        ])

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
                        .withQueryParam("response_type", matching("code"))
                        .withQueryParam("scope", matching("profile email openid"))
                        .withQueryParam("state", matching(".{6,}"))
                        .willReturn(aResponse()
                        .withBody("<html>fake_login_page<html/>")))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped) +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", idTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_wrongKeyIdIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", wrongKeyIdIdTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_invalidSignatureIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", invalidSignatureIdTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_invalidIssuerIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", invalidIssuerIdTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_issuedInFutureIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", issuedInFutureIdTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_expiredIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", expiredIdTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_wrongAudienceIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", wrongAudienceIdTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_invalidNotBeforeIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", invalidNotBeforeIdTokenJwt)))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_unsignedIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", unsignedIdTokenJwt)))

        wireMockServer.stubFor(
                get(urlPathEqualTo("/oauth2/default/v1/keys"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("keys.json")
                        .withTransformers("gstring-template")))

        wireMockServer.stubFor(
                get(urlPathEqualTo("/oauth2/default/v1/userinfo"))
                        .withHeader("Authorization", containing("Bearer ${accessTokenJwt}"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("userinfo.json")))
    }
}