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
package com.okta.test.mock.scenarios;

import com.github.tomakehurst.wiremock.WireMockServer
import com.okta.test.mock.wiremock.TestUtils
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.apache.commons.codec.binary.Base64

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

class ImplicitLocalValidationScenarioDefinition implements ScenarioDefinition {

    final Map<String, String> bindingMap = new HashMap<>()
    String pubKeyE
    String pubKeyN
    String accessTokenJwt
    String wrongScopeAccessTokenJwt
    String invalidAccessTokenJwt
    String wrongAudienceAccessTokenJwt
    String inactiveAccessTokenJwt
    String expiredAccessTokenJwt
    String idTokenjwt

    ImplicitLocalValidationScenarioDefinition() {
    }

    Map getBindingMap() {
        // late binding
        return bindingMap
    }

    private void configureTokens(String issuer) {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(4096)
        KeyPair invalidKeyPair = keyPairGenerator.generateKeyPair()

        KeyPair keyPair = keyPairGenerator.generateKeyPair()

        RSAPublicKey rsaPublicKey = keyPair.getPublic()

        pubKeyE = Base64.encodeBase64URLSafeString(TestUtils.toIntegerBytes(rsaPublicKey.getPublicExponent()))
        pubKeyN = Base64.encodeBase64URLSafeString(TestUtils.toIntegerBytes(rsaPublicKey.getModulus()))

        Instant now = Instant.now()
        accessTokenJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://default")
                .setIssuer(issuer)
                .claim("scp", ["profile", "openid", "email"])
                .claim("groups", ["Everyone", "Test-Group"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        inactiveAccessTokenJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://default")
                .setIssuer(issuer)
                .claim("scp", ["profile", "openid", "email"])
                .claim("groups", ["Everyone", "Test-Group"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        expiredAccessTokenJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://default")
                .setIssuer(issuer)
                .claim("scp", ["profile", "openid", "email"])
                .claim("groups", ["Everyone", "Test-Group"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        wrongScopeAccessTokenJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://default")
                .setIssuer(issuer)
                .claim("scp", ["profile", "openid"])
                .claim("groups", ["Everyone", "Test-Group"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        invalidAccessTokenJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://default")
                .setIssuer(issuer)
                .claim("scp", ["profile", "openid", "email"])
                .claim("groups", ["Everyone", "Test-Group"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, invalidKeyPair.getPrivate())
                .compact()

        wrongAudienceAccessTokenJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://something-else")
                .setIssuer(issuer)
                .claim("scp", ["profile", "openid", "email"])
                .claim("groups", ["Everyone", "Test-Group"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        idTokenjwt =  Jwts.builder()
                .setSubject("a_subject_id")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("api://default")
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        bindingMap.putAll([
                accessTokenJwt: accessTokenJwt,
                pubKeyE: pubKeyE,
                pubKeyN: pubKeyN,
                idTokenjwt: idTokenjwt
        ])
    }

    @Override
    void configureHttpMock(WireMockServer wireMockServer, String baseUrl) {

        configureTokens("${baseUrl}/oauth2/default")

        wireMockServer.stubFor(
                get("/oauth2/default/.well-known/openid-configuration")
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("discovery.json")
                        .withTransformers("gstring-template")))

        wireMockServer.stubFor(
                get(urlPathEqualTo("/oauth2/default/v1/keys"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("keys.json")
                        .withTransformers("gstring-template")))
        }
}