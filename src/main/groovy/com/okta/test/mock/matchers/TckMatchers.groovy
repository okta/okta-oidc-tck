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