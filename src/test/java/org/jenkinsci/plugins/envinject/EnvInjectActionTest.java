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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributor;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.slaves.DumbSlave;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Shell;

import java.io.IOException;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;

public class EnvInjectActionTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test public void doNotOverrideWrapperEnvVar() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        validate(p);
    }

    @Test public void doNotOverrideContributorEnvVar() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();

        p.getBuildersList().add(new ContributingBuilder("DISPLAY", "BUILD_VAL"));

        validate(p);
    }

    @Test public void doNotOverrideWithBuildStep() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();
        p.getBuildersList().add(new EnvInjectBuilder(null, "IRRELEVANT_VAR=true"));

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        validate(p);
    }

    @Test public void doNotOverrideWithBuildWrapper() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();
        final EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper();
        p.getBuildWrappersList().add(wrapper);
        wrapper.setInfo(new EnvInjectJobPropertyInfo(
                null, "IRRELEVANT_VAR=true", null, null, null, false));

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        validate(p);
    }

    @Test public void doNotOverrideWithPasswordWrapper() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();
        final EnvInjectPasswordWrapper wrapper = new EnvInjectPasswordWrapper();
        wrapper.setPasswordEntries(new EnvInjectPasswordEntry[] {
                new EnvInjectPasswordEntry("IRRELEVANT", "value")
        });
        p.getBuildWrappersList().add(wrapper);

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        validate(p);
    }

    @SuppressWarnings("deprecation")
    private void validate(FreeStyleProject p) throws Exception {
        p.getBuildersList().add(new Shell("echo actual=$DISPLAY"));
        FreeStyleBuild build = p.scheduleBuild2(0).get();
        assertEquals("BUILD_VAL", build.getEnvironment().get("DISPLAY"));
        assertTrue(build.getLog(), build.getLog().contains("actual=BUILD_VAL"));
    }

    private FreeStyleProject setupProjectWithDefaultEnvValue()throws Exception, IOException {
        DumbSlave slave = slaveContributing("DISPLAY", "SLAVE_VAL");
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "project");
        p.setAssignedNode(slave);
        return p;
    }

    private DumbSlave slaveContributing(String key, String value) throws Exception {
        return j.createOnlineSlave(null, new EnvVars(key, value));
    }

    private static final class ContributingWrapper extends BuildWrapper {
        private final String value;
        private final String key;

        private ContributingWrapper(String key, String value) {
            this.value = value;
            this.key = key;
        }

        @Override
        public Environment setUp(
                AbstractBuild build, Launcher launcher, BuildListener listener
        ) throws IOException, InterruptedException {
            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    env.put(key, value);
                }
            };
        }

        @Extension
        public static class Descriptor extends hudson.model.Descriptor<BuildWrapper> {
            @Override
            public String getDisplayName() {
                return null;
            }
        }
    }

    private static final class ContributingBuilder extends TestBuilder {
        private final String value;
        private final String key;

        private ContributingBuilder(String key, String value) {
            this.value = value;
            this.key = key;
        }

        @Override
        public boolean perform(
                AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener
        ) throws InterruptedException, IOException {
            // Start serving envvar from EnvironmentContributor
            ContributingExtension.values(key, value);
            return true;
        }
    }

    @TestExtension
    public static final class ContributingExtension extends EnvironmentContributor {
        private static String value = null;
        private static String key = null;

        private static void values(String k, String v) {
            value = v;
            key = k;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void buildEnvironmentFor(
                Run r, EnvVars envs, TaskListener listener
        ) throws IOException, InterruptedException {
            if (key != null && value != null) {
                envs.put(key, value);
            }
        }
    }
}
