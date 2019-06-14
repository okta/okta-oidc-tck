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
package com.okta.test.mock.application

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.Assert

import java.util.concurrent.TimeUnit

class CliApplicationUnderTest implements ApplicationUnderTest {

    private final Logger logger = LoggerFactory.getLogger(CliApplicationUnderTest)

    private Process process

    @Override
    void start() {
        String[] command = [testScenario.command] + testScenario.args

        def processBuilder = new ProcessBuilder(command)

        def useLogFileEnv = System.getenv("TCK_USE_LOG_FILE")
        boolean useLogFile = useLogFileEnv == null ? true : Boolean.parseBoolean(useLogFileEnv)

        println("useLogFile: ${useLogFileEnv} - ${useLogFile}")

        if (useLogFile) {
            File logFile = new File("target", "${testScenario.command}-${new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))}" )
            logger.info("Starting process: ${command}, logging to file: ${logFile}")

            processBuilder.redirectErrorStream(true)
            processBuilder.redirectOutput(logFile)
        } else {
            logger.info("Starting process: ${command}")

            processBuilder.inheritIO()
        }

        // working directory (defaults to ".")
        processBuilder.directory(new File(testScenario.workingDirectory))

        // add any new env vars
        processBuilder.environment().putAll(testScenario.env)

        // start the application
        process = processBuilder.start()

        if (process.waitFor(5, TimeUnit.SECONDS)) {
            Assert.fail("Process did not start: ${testScenario.command}: exit status: ${process.exitValue()}")
        }
    }

    @Override
    int stop() {
        process.destroy()
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            if (process.destroyForcibly().waitFor(5, TimeUnit.SECONDS)) {
                Assert.fail("Failed to stop process: ${testScenario.command}")
            }
            Assert.fail("Process did not exit cleanly: ${testScenario.command}")
        }
        return process.exitValue()
    }
}