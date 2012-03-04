package org.jenkinsci.plugins.envinject.buildwrapper;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.envinject.EnvInjectPasswordEntry;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordWrapper extends BuildWrapper {

    private List<EnvInjectPasswordEntry> passwords;

    @DataBoundConstructor
    public EnvInjectPasswordWrapper(List<EnvInjectPasswordEntry> passwords) {
        this.passwords = passwords;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new Environment() {
        };
    }

    /**
     * Class took from the mask-passwords plugin
     */
    class MaskPasswordsOutputStream extends LineTransformationOutputStream {

        private final static String MASKED_PASSWORD = "********";

        private final OutputStream logger;
        private final Pattern passwordsAsPattern;

        MaskPasswordsOutputStream(OutputStream logger, Collection<String> passwords) {

            this.logger = logger;

            if (passwords != null && passwords.size() > 0) {
                // passwords are aggregated into a regex which is compiled as a pattern
                // for efficiency
                StringBuilder regex = new StringBuilder().append('(');

                int nbMaskedPasswords = 0;
                for (String password : passwords) {
                    if (StringUtils.isNotEmpty(password)) { // we must not handle empty passwords
                        regex.append(Pattern.quote(password));
                        regex.append('|');
                        nbMaskedPasswords++;
                    }
                }
                if (nbMaskedPasswords++ >= 1) { // is there at least one password to mask?
                    regex.deleteCharAt(regex.length() - 1); // removes the last unuseful pipe
                    regex.append(')');
                    passwordsAsPattern = Pattern.compile(regex.toString());
                } else { // no passwords to hide
                    passwordsAsPattern = null;
                }
            } else { // no passwords to hide
                passwordsAsPattern = null;
            }
        }

        @Override
        protected void eol(byte[] bytes, int len) throws IOException {
            String line = new String(bytes, 0, len);
            if (passwordsAsPattern != null) {
                line = passwordsAsPattern.matcher(line).replaceAll(MASKED_PASSWORD);
            }
            logger.write(line.getBytes());
        }
    }

    @Override
    public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException, Run.RunnerAbortedException {
        List<String> passwords2decorate = new ArrayList<String>();
        if (passwords != null) {
            for (EnvInjectPasswordEntry passwordEntry : passwords) {
                passwords2decorate.add(passwordEntry.getValue().getPlainText());
            }
        }
        return new MaskPasswordsOutputStream(logger, passwords2decorate);
    }

    @Override
    public void makeBuildVariables(AbstractBuild build, Map<String, String> variables) {
        if (passwords != null) {
            for (EnvInjectPasswordEntry passwordEntry : passwords) {
                variables.put(passwordEntry.getName(), passwordEntry.getValue().getPlainText());
            }
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return false;
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }


}
