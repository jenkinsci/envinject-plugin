package org.jenkinsci.plugins.envinject;

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
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.service.EnvInjectGlobalPasswordRetriever;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectGlobalPasswordWrapper extends BuildWrapper {

    private static final Logger LOGGER = Logger.getLogger(EnvInjectGlobalPasswordWrapper.class.getName());

    @DataBoundConstructor
    public EnvInjectGlobalPasswordWrapper() {
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

        List<String> passwords = new ArrayList<String>();
        try {
            //Put Clear passwords
            EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
            EnvInjectGlobalPasswordEntry[] passwordEntries = globalPasswordRetriever.getGlobalPasswords();
            if (passwordEntries != null) {
                for (EnvInjectGlobalPasswordEntry globalPasswordEntry : passwordEntries) {
                    passwords.add(globalPasswordEntry.getValue().getPlainText());
                }
            }

        } catch (EnvInjectException ee) {
            throw new IOException(ee);
        }

        return new MaskPasswordsOutputStream(logger, passwords);
    }

    @Override
    public void makeBuildVariables(AbstractBuild build, Map<String, String> variables) {

        try {
            //Put Clear passwords
            EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
            EnvInjectGlobalPasswordEntry[] passwordEntries = globalPasswordRetriever.getGlobalPasswords();
            if (passwordEntries != null) {
                for (EnvInjectGlobalPasswordEntry globalPasswordEntry : passwordEntries) {
                    variables.put(globalPasswordEntry.getName(),
                            globalPasswordEntry.getValue().getPlainText());
                }
            }

        } catch (EnvInjectException ee) {
            LOGGER.log(Level.SEVERE, "Can't inject global password", ee);
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.envinject_wrapper_globalPasswords_displayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/envinject/help-buildWrapperGlobalPasswords.html";
        }
    }


}
