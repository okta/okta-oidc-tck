/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.test.mock.matchers

import org.testng.annotations.Test

import static com.okta.test.mock.matchers.UrlMatcher.singleQueryValue
import static com.okta.test.mock.matchers.UrlMatcher.urlMatcher

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.text.MatchesPattern.matchesPattern

class UrlMatcherTest {

    @Test
    void noQueryTest() {
        assertThat "http://example.com/foo", urlMatcher("http://example.com/foo")
    }

    @Test
    void emptyQueryTest() {
        assertThat "http://example.com/foo?", urlMatcher("http://example.com/foo")
    }

    @Test
    void singleValueQueryTest() {
        assertThat "http://example.com/foo?one=two&three=four",
                urlMatcher("http://example.com/foo",
                    singleQueryValue("one", "two"))
    }

    @Test
    void complexTest() {
        assertThat "http://test.example.com:1234/oauth2/default/v1/authorize" +
                        "?redirect_uri=http://localhost:123456/authorization-code/callback" +
                        "&client_id=OOICU812" +
                        "&response_type=code" +
                        "&scope=profile%20email%20openid" +
                        "&state=somerandomestategoeshere",

            urlMatcher("http://test.example.com:1234/oauth2/default/v1/authorize",
                singleQueryValue("client_id", "OOICU812"),
                singleQueryValue("redirect_uri", "http://localhost:123456/authorization-code/callback"),
                singleQueryValue("response_type", "code"),
                singleQueryValue("scope", "profile email openid"),
                singleQueryValue("state", matchesPattern(".{6,}")))
    }

    @Test
    void anotherComplexTest() {
        def locationMatcher = urlMatcher("https://localhost:58987/oauth2/default/v1/authorize",
                singleQueryValue("client_id", "OOICU812"),
                singleQueryValue("redirect_uri", "http://localhost:58985/authorization-code/callback"),
                singleQueryValue("response_type", "code"),
                singleQueryValue("scope", "profile email openid"),
                singleQueryValue("state", matchesPattern(".{6,}")))

        assertThat " https://localhost:58987/oauth2/default/v1/authorize?response_type=code&client_id=OOICU812&scope=profile+email+openid&state=KMXBPpLm_VfBcU-DWFYsXWHsKwDxeaw4LLK11s5p4wo%3D&redirect_uri=http%3A%2F%2Flocalhost%3A58985%2Fauthorization-code%2Fcallback",
            locationMatcher
    }

    @Test
    void emptyQueryParamTest() {
        assertThat "http://localhost:59383/login?error", urlMatcher("http://localhost:59383/login?error")
    }
}