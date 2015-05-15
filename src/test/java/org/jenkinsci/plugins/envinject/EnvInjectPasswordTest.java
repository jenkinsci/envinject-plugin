package org.jenkinsci.plugins.envinject;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.Secret;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import junit.framework.Assert;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.TestExtension;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordTest extends HudsonTestCase {

    private static final String PWD_KEY = "PASS_KEY";
    private static final String PWD_VALUE = "PASS_VALUE";

    private FreeStyleProject project;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        project = createFreeStyleProject();
    }

    public void testEnvInjectPasswordWrapper() throws Exception {

        EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry(PWD_KEY, PWD_VALUE)
        });

        project.getBuildWrappersList().add(passwordWrapper);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        checkEnvInjectResult(build);
    }

    public void testEnvInjectJobParameterPassword() throws Exception {

        List<ParameterValue> parameterValues = new ArrayList<ParameterValue>();
        parameterValues.add(new PasswordParameterValue(PWD_KEY, PWD_VALUE));
        ParametersAction parametersAction = new ParametersAction(parameterValues);

        @SuppressWarnings("deprecation")
        FreeStyleBuild build = project.scheduleBuild2(0, new Cause.UserCause(), parametersAction).get();
        Assert.assertEquals(Result.SUCCESS, build.getResult());

        checkEnvInjectResult(build);
    }

    @Bug(28409)
    public void testFileHandlesLeak() throws Exception {
        final EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
        passwordWrapper.setPasswordEntries(new EnvInjectPasswordEntry[]{
                new EnvInjectPasswordEntry(PWD_KEY, PWD_VALUE)
        });
        final FileLeakBuildWrapper fileLeakDetector = new FileLeakBuildWrapper();

        project.getBuildWrappersList().add(fileLeakDetector);
        project.getBuildWrappersList().add(passwordWrapper);
        
        @SuppressWarnings("deprecation")  
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertBuildStatusSuccess(build);
        
        Assert.assertTrue("Nested output stream has not been closed", fileLeakDetector.getLastOutputStream().isClosed());    
    } 
    
    private void checkEnvInjectResult(FreeStyleBuild build) {
        EnvInjectAction action = build.getAction(EnvInjectAction.class);
        Map<String, String> envVars = action.getEnvMap();
        //The value must be encrypted in the envVars
        Assert.assertEquals(Secret.fromString(PWD_VALUE).getEncryptedValue(), envVars.get(PWD_KEY));
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
