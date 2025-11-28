/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.envinject;

import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class EnvInjectBuilderTest {

    private JenkinsRule j;

    private FreeStyleProject p;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        p = j.jenkins.createProject(FreeStyleProject.class, "project");
    }

    @Test
    void setVarsInBuildStep() throws Exception {
        p.setScm(new SingleFileSCM("vars.properties", "FILE_VAR=fvalue"));
        p.getBuildersList().add(new EnvInjectBuilder("vars.properties", "TEXT_VAR=tvalue"));

        assertEquals("tvalue", buildEnvVars().get("TEXT_VAR"));
        assertEquals("fvalue", buildEnvVars().get("FILE_VAR"));
    }

    @Test
    void override() throws Exception {
        p.getBuildersList().add(new EnvInjectBuilder(null, "VAR=old"));
        p.getBuildersList().add(new EnvInjectBuilder(null, "VAR=new"));

        assertEquals("new", buildEnvVars().get("VAR"));
    }

    private EnvVars buildEnvVars() throws Exception {
        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);

        p.scheduleBuild2(0).get();
        final EnvVars envVars = capture.getEnvVars();
        return envVars;
    }
}
