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
import com.github.tomakehurst.wiremock.http.Request
import com.okta.test.mock.Config
import com.okta.test.mock.wiremock.TestUtils
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.apache.commons.codec.binary.Base64

import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Function
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
    String accessTokenWithGroupsJwt
    JwtBuilder idTokenJwtBuilder
    JwtBuilder idTokenWithGroupsJwtBuilder
    JwtBuilder wrongKeyIdIdTokenJwtBuilder
    JwtBuilder wrongAudienceIdTokenJwtBuilder
    JwtBuilder issuedInFutureIdTokenJwtBuilder
    JwtBuilder expiredIdTokenJwtBuilder
    JwtBuilder invalidSignatureIdTokenJwtBuilder
    JwtBuilder invalidIssuerIdTokenJwtBuilder
    JwtBuilder invalidNotBeforeIdTokenJwtBuilder
    JwtBuilder unsignedIdTokenJwtBuilder
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

        RSAPublicKey rsaPublicKey = keyPair.getPublic()

        pubKeyE = Base64.encodeBase64URLSafeString(TestUtils.toIntegerBytes(rsaPublicKey.getPublicExponent()))
        pubKeyN = Base64.encodeBase64URLSafeString(TestUtils.toIntegerBytes(rsaPublicKey.getModulus()))

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
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        accessTokenWithGroupsJwt =  Jwts.builder()
                .setSubject("joe.coder@example.com")
                .setAudience("api://default")
                .claim("scp", ["profile", "openid", "email"])
                .claim("groups", ["Everyone", "Test-Group"])
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())
                .compact()

        idTokenJwtBuilder =  Jwts.builder()
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
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        idTokenWithGroupsJwtBuilder =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .claim("groups", ["Everyone", "Test-Group"])
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now))
                .setNotBefore(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        wrongKeyIdIdTokenJwtBuilder =  Jwts.builder()
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
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        wrongAudienceIdTokenJwtBuilder =  Jwts.builder()
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
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        issuedInFutureIdTokenJwtBuilder =  Jwts.builder()
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
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        expiredIdTokenJwtBuilder =  Jwts.builder()
                .setSubject("00uid4BxXw6I6TV4m0g3")
                .claim("name", "Joe Coder")
                .claim("email", "joe.coder@example.com")
                .claim("preferred_username", "jod.coder@example.com")
                .setAudience("OOICU812")
                .setIssuer(issuerUrl)
                .setIssuedAt(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .setNotBefore(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .setExpiration(Date.from(now.minus(30, ChronoUnit.MINUTES)))

                .setHeader(Jwts.jwsHeader()
                .setKeyId('TEST_PUB_KEY_ID'))
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        invalidSignatureIdTokenJwtBuilder =  Jwts.builder()
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
                .signWith(SignatureAlgorithm.RS256, invalidKeyPair.getPrivate())

        invalidIssuerIdTokenJwtBuilder =  Jwts.builder()
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
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        invalidNotBeforeIdTokenJwtBuilder =  Jwts.builder()
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
                .signWith(SignatureAlgorithm.RS256, keyPair.getPrivate())

        unsignedIdTokenJwtBuilder =  Jwts.builder()
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
                idTokenJwt: idTokenJwtBuilder.compact()
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
                get(urlPathEqualTo("/oauth2/default/v1/authorize"))
                        .withQueryParam("client_id", matching(clientId))
                        .withQueryParam("redirect_uri", matching(Pattern.quote("http://localhost:")+ "\\d+${redirectUriPath}"))
                        .withQueryParam("response_type", matching("none"))
                        .withQueryParam("state", matching(".{6,}"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody("<html>no_token_requested<html/>")))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped) +".*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json;charset=UTF-8")
                                .withBodyFile("token.json")
                                .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE", idTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_GROUPS&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped) +".*"))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json;charset=UTF-8")
                                .withBodyFile("token.json")
                                .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_GROUPS", idTokenWithGroupsJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_wrongKeyIdIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_wrongKeyIdIdTokenJwt", wrongKeyIdIdTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_invalidSignatureIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_invalidSignatureIdTokenJwt", invalidSignatureIdTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_invalidIssuerIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_invalidIssuerIdTokenJwt", invalidIssuerIdTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_issuedInFutureIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_issuedInFutureIdTokenJwt", issuedInFutureIdTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_expiredIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_expiredIdTokenJwt", expiredIdTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_wrongAudienceIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_wrongAudienceIdTokenJwt", wrongAudienceIdTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_invalidNotBeforeIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_invalidNotBeforeIdTokenJwt", invalidNotBeforeIdTokenJwtBuilder))))

        wireMockServer.stubFor(
                post(urlPathEqualTo("/oauth2/default/v1/token"))
                        .withHeader("Authorization", equalTo(authHeader))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("code=TEST_CODE_unsignedIdTokenJwt&"))
                        .withRequestBody(matching(".*"+Pattern.quote("redirect_uri=http%3A%2F%2Flocalhost%3A") + "\\d+" +Pattern.quote(redirectUriPathEscaped)  +".*"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("token.json")
                        .withTransformer("gstring-template", "idTokenJwt", new JwtWithNonce("TEST_CODE_unsignedIdTokenJwt", unsignedIdTokenJwtBuilder))))

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

        wireMockServer.stubFor(
                get(urlPathEqualTo("/oauth2/default/v1/userinfo"))
                        .withHeader("Authorization", containing("Bearer ${accessTokenWithGroupsJwt}"))
                        .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBodyFile("userinfo-groups.json")))
    }

    private class JwtWithNonce implements Function<Request, String> {

        private final String nonceKey
        private final JwtBuilder jwtBuilder

        JwtWithNonce(String nonceKey, JwtBuilder jwtBuilder) {
            this.nonceKey = nonceKey
            this.jwtBuilder = jwtBuilder
        }

        @Override
        String apply(Request request) {
            String nonce = NonceHolder.getNonce(nonceKey)
            if (nonce != null) {
                jwtBuilder.claim("nonce", nonce)
            }
            return jwtBuilder.compact()
        }
    }
}