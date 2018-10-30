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

import io.restassured.response.Response
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.is

class TckMatchers {

    static Matcher<Response> responseCode(int code) {
        return new TypeSafeMatcher<Response>() {
            @Override
            protected boolean matchesSafely(Response response) {
                return is(response.statusCode()).matches(code)
            }

            @Override
            void describeTo(Description description) {
                description.appendText(" Status Code ").appendValue(code)
            }
        }
    }

    static Matcher<Response> header(String name, Matcher<String> value) {
        return new TypeSafeMatcher<Response>() {
            @Override
            protected boolean matchesSafely(Response response) {
                return value.matches(response.header(name))
            }

            @Override
            void describeTo(Description description) {
                description.appendText(" Header '").appendValue(name).appendText("' value: ").appendDescriptionOf(value)
            }
        }
    }

    static Matcher<Response> bodyMatcher(Matcher<String> body) {

        new TypeSafeMatcher<Response>() {
            @Override
            protected boolean matchesSafely(Response response) {
                return body.matches(response.body().asString())
            }

            @Override
            void describeTo(Description description) {
                description.appendText(" body: ").appendDescriptionOf(body)
            }
        }
    }

    static Matcher<Response> redirect(Matcher<String> location) {
        allOf(
            header("Location", location),
            responseCode(302)
        )
    }
}