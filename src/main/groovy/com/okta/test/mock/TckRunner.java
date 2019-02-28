package com.okta.test.mock;

import com.beust.jcommander.JCommander;
import io.jsonwebtoken.lang.Collections;
import org.testng.CommandLineArgs;
import org.testng.TestNG;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TckRunner {

    public static void main(String[] args) throws Exception {

        // parse the command line args and set the defaults if needed
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        CommandLineArgs cla = new CommandLineArgs();
        new JCommander(cla, null, args);

        // set the default output directory
        if (cla.outputDirectory == null) {
            argList.add("-d");
            argList.add("target/cli-test-output");
        }

        // copy the testng.xml from the classpath to a temp file and configure TestNG to use that
        if (Collections.isEmpty(cla.suiteFiles)) {
            URL defaultTestNg = TckRunner.class.getClassLoader().getResource("testng.xml");
            Path dest = Files.createTempFile("testng-", ".xml");
            Files.copy(defaultTestNg.openStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            argList.add(dest.toUri().toString());
        }

        // now call TestNG normally
        TestNG.main(argList.toArray(new String[0]));
    }
}