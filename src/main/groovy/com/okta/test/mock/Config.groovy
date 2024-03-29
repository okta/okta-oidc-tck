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
package com.okta.test.mock

class Config {
    String implementation = "com.okta.test.mock.application.CliApplicationUnderTest"
    Map<String, TestScenario> scenarios = new LinkedHashMap<>()

    static class Global {
        static String getConfigProperty(String key, String defaultValue = null) {
            return System.getProperty(key, defaultValue)
        }

        static String getCodeFlowRedirectPath() {
            return getConfigProperty("codeFlow.redirectPath", "/authorization-code/callback")
        }

        static String getEnrollmentFlowCallbackPath() {
            return getConfigProperty("enrollmentFlow.CallbackPath", "/enrollment/callback")
        }
    }
}

class TestScenario {
    List<String> disabledTests = new ArrayList<>()
    Boolean enabled
    String command
    /**
     * A protected path that will force a redirect or an access error.
     */
    String protectedPath = "/"
    String workingDirectory = "."
    Map<String, String> env = new HashMap<>()
    List<String> args = new ArrayList<>()
    Map<String, Integer> ports = new HashMap<>()
}
