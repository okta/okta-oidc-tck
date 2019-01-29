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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ClasspathFileSource
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import com.okta.test.mock.scenarios.Scenario
import groovy.text.StreamingTemplateEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.AfterClass

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static java.lang.Thread.currentThread

abstract class HttpMock {

    final private Logger logger = LoggerFactory.getLogger(HttpMock)

    protected WireMockServer wireMockServer
    private int port
    private int httpsPort
    private String scenario

    void setScenario(String scenario) {
        this.scenario = scenario
    }

    Map getBaseBindingMap() {
        return Collections.unmodifiableMap([
            baseUrl: getBaseUrl()
        ])
    }

    void startMockServer() {
        if (wireMockServer == null) {

            def keyStorePassword = "password"

            try {
                def outKeyStoreFile = File.createTempFile("testing-keystore", "jks").toPath()
                def keyStoreResource = currentThread().contextClassLoader.getResource("tck-keystore.jks")
                Files.copy(keyStoreResource.openStream(), outKeyStoreFile, StandardCopyOption.REPLACE_EXISTING)
                def keyStorePath = outKeyStoreFile.toFile().absolutePath
                System.setProperty("javax.net.ssl.trustStore", keyStorePath)

                def definition = Scenario.fromId(scenario).definition

                // WireMock has a bug when running with a more complex classloader (like a nested uber jar)
                // to work around this we will just attempt to find a resource and check its path.
                def stubsURL = currentThread().getContextClassLoader().getResource("stubs")
                def stubsPath = stubsURL.toString().contains("BOOT-INF/classes") ? "BOOT-INF/classes/stubs" : "stubs"

                wireMockServer = new WireMockServer(wireMockConfig()
                        .notifier(new Slf4jNotifier(true))
                        .port(getMockPort())
                        .httpsPort(getMockHttpsPort())
                        .keystorePath(keyStorePath)
                        .keystorePassword(keyStorePassword)
                        .fileSource(new ClasspathFileSource(stubsPath))
                        .extensions(new GStringTransformer(definition.bindingMap, baseBindingMap))
                )

                definition.configureHttpMock(wireMockServer, getBaseUrl())
                wireMockServer.start()
                WireMock.configureFor("https", "localhost", wireMockServer.httpsPort())

            } catch (e) {
                e.printStackTrace()
                throw e
            }
        }
    }

    @AfterClass
    void stopMockServer() {
        if (wireMockServer != null) {
            wireMockServer.stop()
        }
    }

    /**
     * Calls {@code doGetMockHttpsPort} and caches the result, allowing the implementation to NOT worry about the
     * cache/init logic.
     * @return the port the mock server will respond to over https
     */
    int getMockHttpsPort() {
        if (httpsPort == 0) {
            httpsPort = doGetMockHttpsPort()
        }
        return httpsPort
    }

    /**
     * Calls {@code doGetMockPort()} and caches the result, allowing the implementation to NOT worry about the
     * cache/init logic.
     * @return the port the mock server will respond to over http
     */
    int getMockPort() {
        if (port == 0) {
            port = doGetMockPort()
        }
        return port
    }

    /**
     * Called by {@code getMockPort()} to allow late init and caching of the actual returned value. Implementing classes
     * may return a static or random port.
     * @return the port the mock server will respond to over http
     */
    abstract int doGetMockPort()

    /**
     * Called by {@code getMockHttpsPort()} to allow late init and caching of the actual returned value. Implementing classes
     * may return a static or random port.
     * @return the port the mock server will respond to over https
     */
    abstract int doGetMockHttpsPort()

    String getBaseUrl() {
        return Boolean.getBoolean("okta.testing.disableHttpsCheck") \
            ? "http://localhost:${getMockPort()}"
            : "https://localhost:${getMockHttpsPort()}"
    }
}

class GStringTransformer extends ResponseTransformer {

    private final List<Map> bindings

    GStringTransformer(Map... bindings) {
        this.bindings = bindings
    }

    @Override
    boolean applyGlobally() {
        return false
    }

    @Override
    Response transform(Request request, Response response, FileSource files, Parameters parameters) {

        Map params = new HashMap()
        // merge bindings
        bindings.forEach {params += it}
        // override binds with params if any
        if (parameters != null) params += parameters

        return Response.Builder
                .like(response)
                .body(new StreamingTemplateEngine().createTemplate(response.bodyAsString).make(params).toString())
                .build()
    }

    @Override
    String getName() {
        return "gstring-template"
    }
}