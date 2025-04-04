package org.jenkinsci.plugins.envinject;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterValue;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.Secret;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gregory Boissinot
 */
@WithJenkins
class EnvInjectPasswordTest {

    private static final String PWD_KEY = "PASS_KEY";
    private static final String PWD_VALUE = "PASS_VALUE";

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void testEnvInjectPasswordWrapper() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry(PWD_KEY, PWD_VALUE)
        });

        project.getBuildWrappersList().add(passwordWrapper);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        checkEnvInjectResult(build);
    }

    @Test
    void testEnvInjectJobParameterPassword() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition(PWD_KEY, "")));

        List<ParameterValue> parameterValues = new ArrayList<>();
        parameterValues.add(new PasswordParameterValue(PWD_KEY, PWD_VALUE));
        ParametersAction parametersAction = new ParametersAction(parameterValues);

        @SuppressWarnings("deprecation")
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), parametersAction).get();
        jenkins.assertBuildStatusSuccess(build);

        checkEnvInjectResult(build);
    }

    @Test
    @Issue("JENKINS-28409")
    void testFileHandlesLeak() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        final EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry(PWD_KEY, PWD_VALUE)
        });
        final FileLeakBuildWrapper fileLeakDetector = new FileLeakBuildWrapper();

        project.getBuildWrappersList().add(fileLeakDetector);
        project.getBuildWrappersList().add(passwordWrapper);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatusSuccess(build);

        assertTrue(fileLeakDetector.getLastOutputStream().isClosed(), "Nested output stream has not been closed");
    }

    /**
     * Test that
     * {@link EnvInjectPasswordWrapper#decorateLogger(AbstractBuild, OutputStream)}
     * does not write to the output stream. The written message would bypass
     * the other build wrappers, which have not yet wrapped the OutputStream.
     *
     * @throws Exception
     */
    @Test
    @Issue("JENKINS-30028")
    void testDoNotWriteDuringDecorateLogger() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = new FreeStyleBuild(project);

        EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setInjectGlobalPasswords(true);
        passwordWrapper.setMaskPasswordParameters(true);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        passwordWrapper.decorateLogger(build, outputStream);

        assertThat(outputStream.toByteArray(), is(new byte[0]));
    }

    private void checkEnvInjectResult(FreeStyleBuild build) {
        EnvInjectAction action = build.getAction(EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();

        // The value must be encrypted in the envVars
        assertEquals(Secret.fromString(PWD_VALUE), Secret.decrypt(envVars.get(PWD_KEY)));
    }

    /**
     * A wrapper for an {@link OutputStream}, which raises the flag when a
     * stream gets closed externally.
     *
     * @since 1.91.3
     */
    public static class FileLeakDetectorStream extends FilterOutputStream {

        private boolean closed;

        public FileLeakDetectorStream(OutputStream out) {
            super(out);
            this.closed = false;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            super.close();
            closed = true;
        }
    }

    /**
     * A class, which decorates loggers by {@link FileLeakDetectorStream}.
     *
     * @since 1.91.3
     */
    public static class FileLeakBuildWrapper extends BuildWrapper {

        private FileLeakDetectorStream lastOutputStream = null;

        public FileLeakDetectorStream getLastOutputStream() {
            return lastOutputStream;
        }

        @Override
        public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws Run.RunnerAbortedException {
            lastOutputStream = new FileLeakDetectorStream(logger);
            return lastOutputStream;
        }

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) {
            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    //Do nothing
                }
            };
        }

        @TestExtension
        public static class DescriptorImpl extends BuildWrapperDescriptor {

            @Override
            public String getDisplayName() {
                return "N/A";
            }

            @Override
            public boolean isApplicable(AbstractProject<?, ?> item) {
                return true;
            }
        }
    }
}
