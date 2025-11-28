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
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributor;
import hudson.model.FreeStyleProject;
import hudson.model.InvisibleAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import hudson.tasks.BuildWrapper;
import jenkins.model.RunAction2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class EnvInjectActionTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    @Issue("JENKINS-26583")
    void doNotOverrideWrapperEnvVar() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        assertValueInjected(p);
    }

    @Test
    @Disabled("TODO: Fails, create a follow-up issue for that")
    void doNotOverrideContributorEnvVar() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();

        p.getBuildersList().add(new ContributingBuilder("DISPLAY", "BUILD_VAL"));

        assertValueInjected(p);
    }

    @Test
    @Disabled("TODO: Fails, create a follow-up issue for that")
    void doNotOverrideWithBuildStep() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();
        p.getBuildersList().add(new EnvInjectBuilder(null, "IRRELEVANT_VAR=true"));

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        assertValueInjected(p);
    }

    @Test
    void doNotOverrideWithBuildWrapper() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();
        final EnvInjectBuildWrapper wrapper = new EnvInjectBuildWrapper(new EnvInjectJobPropertyInfo(
                null, "IRRELEVANT_VAR=true", null, null, false, null));
        p.getBuildWrappersList().add(wrapper);

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        assertValueInjected(p);
    }

    @Test
    void doNotOverrideWithPasswordWrapper() throws Exception {
        FreeStyleProject p = setupProjectWithDefaultEnvValue();
        final EnvInjectPasswordWrapper wrapper = new EnvInjectPasswordWrapper();
        wrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry("IRRELEVANT", "value")
        });
        p.getBuildWrappersList().add(wrapper);

        p.getBuildWrappersList().add(new ContributingWrapper("DISPLAY", "BUILD_VAL"));

        assertValueInjected(p);
    }

    private void assertValueInjected(FreeStyleProject p) throws Exception {
        CaptureEnvironmentBuilder capture = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(capture);

        p.scheduleBuild2(0).get();
        assertEquals("BUILD_VAL", capture.getEnvVars().get("DISPLAY"));
    }

    private FreeStyleProject setupProjectWithDefaultEnvValue() throws Exception {
        DumbSlave agent = slaveContributing("DISPLAY", "SLAVE_VAL");
        FreeStyleProject p = j.jenkins.createProject(FreeStyleProject.class, "project");
        p.setAssignedNode(agent);
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
        ) {
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
        ) {
            // Start serving envvar from EnvironmentContributor
            build.addAction(new ContributorAction(key, value));
            return true;
        }
    }

    public static class ContributorAction extends InvisibleAction implements RunAction2 {
        private String value = null;
        private String key = null;

        public ContributorAction(String k, String v) {
            value = v;
            key = k;
        }

        @Override
        public void onAttached(Run<?, ?> r) {
            // Do not care
        }

        @Override
        public void onLoad(Run<?, ?> r) {
            // Do not care
        }
    }

    @TestExtension
    public static class Contributor extends EnvironmentContributor {

        @Override
        public void buildEnvironmentFor(Run r, EnvVars envs, TaskListener listener) {
            ContributorAction a = r.getAction(ContributorAction.class);
            if (a != null) {
                envs.put(a.key, a.value);
            }
        }
    }
}
