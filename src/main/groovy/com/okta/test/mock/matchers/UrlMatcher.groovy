package com.okta.test.mock.matchers

import com.okta.test.mock.wiremock.TestUtils
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.hamcrest.collection.IsMapContaining

import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.nullValue

class UrlMatcher extends TypeSafeDiagnosingMatcher<String> {

    private final URL expectedUrl
    private final Matcher protocolMatcher
    private final Matcher hostMatcher
    private final Matcher portMatcher
    private final Matcher pathMatcher
    private final List<Matcher> queryMatchers


    UrlMatcher(URL expected, List<Matcher> queryMatchers) {
        this.expectedUrl = expected

        this.protocolMatcher = Matchers.is(expectedUrl.protocol)
        this.hostMatcher = Matchers.is(expectedUrl.host)
        this.portMatcher = Matchers.is(expectedUrl.port)
        this.pathMatcher = Matchers.is(expectedUrl.path)
        this.queryMatchers = queryMatchers
    }

    @Override
    protected boolean matchesSafely(String actual, Description mismatchDescription) {
        try {
            mismatchDescription.appendText(" was ").appendValue(actual)

            URL actualUrl = new URL(actual)
            def queryMap = TestUtils.parseQuery(actualUrl.query)

            mismatchDescription.appendText("\nCodeFlowRemoteValidationIT  protocol was:  ").appendValue(actualUrl.protocol)
                               .appendText("\n  host was:      ").appendValue(actualUrl.host)
                               .appendText("\n  port was:      ").appendValue(actualUrl.port)
                               .appendText("\n  path was:      ").appendValue(actualUrl.path)
                               .appendText("\n  query was:     ").appendValue(actualUrl.query)

            boolean matches = true

            if (!protocolMatcher.matches(actualUrl.protocol)) {
                protocolMatcher.describeTo(mismatchDescription.appendText("\n    "))
                matches = false
            }

            if (!hostMatcher.matches(actualUrl.host)) {
                hostMatcher.describeTo(mismatchDescription.appendText("\n    "))
                matches = false
            }

            if (!portMatcher.matches(actualUrl.port)) {
                portMatcher.describeTo(mismatchDescription.appendText("\n    "))
                matches = false
            }

            if (!pathMatcher.matches(actualUrl.path)) {
                pathMatcher.describeTo(mismatchDescription.appendText("\n    "))
                matches = false
            }

            queryMatchers.forEach {
                if (!it.matches(queryMap)) {
                    it.describeTo(mismatchDescription.appendText("\n     "))
                    mismatchDescription.appendValue(queryMap)
                    matches = false
                }
            }

            return matches

        } catch (MalformedURLException e) {
            mismatchDescription.appendText("failed to parse url: '" + actual + "', reason: "+ e.getMessage())
            // let fail
        }

        return false
    }

    @Override
    void describeTo(Description description) {
        description.appendValue(expectedUrl)
                .appendText("\n  protocol: ").appendDescriptionOf(protocolMatcher)
                .appendText("\n  host:     ").appendDescriptionOf(hostMatcher)
                .appendText("\n  port:     ").appendDescriptionOf(portMatcher)
                .appendText("\n  path:     ").appendDescriptionOf(pathMatcher)
                .appendText("\n  query:    ")
        queryMatchers.stream().forEach {
            description.appendText("\n    entry: ").appendDescriptionOf(it)
        }
    }

    private static List<Matcher> createQueryMatchers(String query) {
        if (query == null) {
            return Collections.emptyList()
        }

        return TestUtils.parseQuery(query).entrySet().stream()
                .map { IsMapContaining.hasEntry(is(it.key), is(it.value)) }
                .collect(Collectors.toList())
    }

    static urlMatcher(String url) {
        URL expected = new URL(url)
        return new UrlMatcher(expected, createQueryMatchers(expected.query))
    }

    static urlMatcher(String url, Matcher ... queryMatchers) {
        return urlMatcher(url, Arrays.asList(queryMatchers))
    }

    static urlMatcher(String url, List<Matcher> queryMatchers) {
        URL expected = new URL(url)
        return new UrlMatcher(expected, queryMatchers)
    }

    static singleQueryValue(String key, Matcher<String> matcher) {
        return IsMapContaining.hasEntry(is(key), hasItem(matcher))
    }

    static singleQueryValue(String key, String value) {
        return singleQueryValue(key, is(value))
    }
}
