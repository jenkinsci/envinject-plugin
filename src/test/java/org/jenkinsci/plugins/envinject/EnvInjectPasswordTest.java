package org.jenkinsci.plugins.envinject;

import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.Secret;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.hamcrest.Matchers;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.junit.Assert;
import org.junit.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private static final String PWD_KEY = "PASS_KEY";
    private static final String PWD_VALUE = "PASS_VALUE";

    @Test
    public void testEnvInjectPasswordWrapper() throws Exception {
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
    public void testEnvInjectJobParameterPassword() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        List<ParameterValue> parameterValues = new ArrayList<ParameterValue>();
        parameterValues.add(new PasswordParameterValue(PWD_KEY, PWD_VALUE));
        ParametersAction parametersAction = new ParametersAction(parameterValues);

        @SuppressWarnings("deprecation")
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), parametersAction).get();
        jenkins.assertBuildStatusSuccess(build);

        checkEnvInjectResult(build);
    }

    @Test
    @Bug(28409)
    public void testFileHandlesLeak() throws Exception {
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
        
        assertTrue("Nested output stream has not been closed", fileLeakDetector.getLastOutputStream().isClosed());
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
    public void testDoNotWriteDuringDecorateLogger() throws Exception {
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
        assertEquals(Secret.fromString(PWD_VALUE).getEncryptedValue(), envVars.get(PWD_KEY));
    }    
    
    /**
     * A wrapper for an {@link OutputStream}, which raises the flag when a
     * stream gets closed externally.
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
     * @since 1.91.3
     */
    public static class FileLeakBuildWrapper extends BuildWrapper {

        private FileLeakDetectorStream lastOutputStream = null;

        public FileLeakDetectorStream getLastOutputStream() {
            return lastOutputStream;
        }
            
        @Override
        public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException, Run.RunnerAbortedException {
            lastOutputStream = new FileLeakDetectorStream(logger);
            return lastOutputStream;
        }  

        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
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
