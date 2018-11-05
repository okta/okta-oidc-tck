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
package com.okta.test.mock.wiremock

import io.restassured.config.HttpClientConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.http.ContentType
import io.restassured.matcher.ResponseAwareMatcher
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import org.apache.http.client.params.ClientPNames
import org.apache.http.client.params.CookiePolicy
import org.hamcrest.Matcher

import java.nio.charset.StandardCharsets

import static io.restassured.RestAssured.given
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.notNullValue

class TestUtils {
    static byte[] toIntegerBytes(final BigInteger bigInt) {
        int bitlen = bigInt.bitLength();
        // round bitlen
        bitlen = ((bitlen + 7) >> 3) << 3;
        final byte[] bigBytes = bigInt.toByteArray();

        if (((bigInt.bitLength() % 8) != 0) && (((bigInt.bitLength() / 8) + 1) == (bitlen / 8))) {
            return bigBytes;
        }
        // set up params for copying everything but sign bit
        int startSrc = 0;
        int len = bigBytes.length;

        // if bigInt is exactly byte-aligned, just skip signbit in copy
        if ((bigInt.bitLength() % 8) == 0) {
            startSrc = 1;
            len--;
        }
        final int startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
        final byte[] resizedBytes = new byte[bitlen / 8];
        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
        return resizedBytes;
    }

    static ExtractableResponse followRedirectUntilLocation(ExtractableResponse response,
                                                           Matcher<Response> responseMatcher,
                                                           int maxRedirects = 3) {

        if (responseMatcher.matches(response)) {
            return response
        }

        for (int ii=0; ii<maxRedirects; ii++) {

            def location = response.header("Location")
            println("redirecting to:  ${location}")

            assertThat "Location is null, could not match or follow redirect", location, notNullValue()

            def tempResponse = given()
                .redirects()
                    .follow(false)
                .accept(ContentType.JSON)
            .when()
                .cookies(response.cookies())
                .get(location)
            .then()
            .extract()

            response = tempResponse
            if (responseMatcher.matches(response)) {
                return response
            }
        }
        assertThat "Exceded max redirect follow of '${maxRedirects}' last location failure: ", response.header("Location"), responseMatcher
    }

    static Map<String, List<String>> parseQuery(String query) {

        if (query == null || query.empty) {
            return null
        }

        // convert the query portion into an map with multiple values i.e. "?foo=bar1&foo=bar2" turns into "foo": ["bar1" "bar2"]
        Map<String, List<String>> result = new HashMap<>()
        Arrays.stream(query.split('&'))
        .map { it.split('=', 2) }
        .map { it.eachWithIndex { def entry, int i -> it[i] = URLDecoder.decode(entry, StandardCharsets.UTF_8.toString())}}
        .forEach {
            List<String> values = result.getOrDefault(it[0], new ArrayList<String>())
            if(it.length == 2) {
                // typical value for query param, i.e. ?foo=bar
                values.add(it[1])
            } else {
                // no value for query param, i.e. ?foo
                values.add(null)
            }

            result.put(it[0], values)
        }
        return result
    }

    static RestAssuredConfig browserCompatibleRedirects() {
        def config = RestAssuredConfig.newConfig()
                .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY))
        config.getRedirectConfig().followRedirects(true)
        return config
    }
}
