package org.jenkinsci.plugins.envinject;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Launcher;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.PasswordParameterValue;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.EnvInjectLogger;
import org.jenkinsci.plugins.envinject.service.EnvInjectGlobalPasswordRetriever;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPasswordWrapper extends BuildWrapper {

    private static final Function<EnvInjectPasswordEntry, String> PASSWORD_ENTRY_TO_NAME = new Function<EnvInjectPasswordEntry, String> ()  {
        @Override
        public String apply(EnvInjectPasswordEntry envInjectPasswordEntry) {
            if (envInjectPasswordEntry == null) {
                throw new NullPointerException("Received null EnvInject password entry");
            }
            return envInjectPasswordEntry.getName();
        }
    };

    private static final Function<EnvInjectPasswordEntry, String> PASSWORD_ENTRY_TO_VALUE = new Function<EnvInjectPasswordEntry, String> ()  {
        @Override
        public String apply(EnvInjectPasswordEntry envInjectPasswordEntry) {
            if (envInjectPasswordEntry == null) {
                throw new NullPointerException("Received null EnvInject password entry");
            }
            return envInjectPasswordEntry.getValue().getPlainText();
        }
    };

    private boolean injectGlobalPasswords;
    private boolean maskPasswordParameters;
    
    @CheckForNull
    private EnvInjectPasswordEntry[] passwordEntries;

    @DataBoundConstructor
    public EnvInjectPasswordWrapper() {
    }

    public boolean isInjectGlobalPasswords() {
        return injectGlobalPasswords;
    }

    public boolean isMaskPasswordParameters() {
        return maskPasswordParameters;
    }
    
    @DataBoundSetter
    public void setInjectGlobalPasswords(boolean injectGlobalPasswords) {
        this.injectGlobalPasswords = injectGlobalPasswords;
    }

    @DataBoundSetter
    public void setMaskPasswordParameters(boolean maskPasswordParameters) {
        this.maskPasswordParameters = maskPasswordParameters;
    }
    
    /**
     * Retrieves raw array of password entries.
     * @return Array of password entries or {@code null} if the array is not specified
     * @deprecated Use {@link #getPasswordEntryList()} instead
     */
    @CheckForNull
    @Deprecated @Restricted(NoExternalUse.class)
    public EnvInjectPasswordEntry[] getPasswordEntries() {
        return passwordEntries == null ? null : Arrays.copyOf(passwordEntries, passwordEntries.length);
    }
    
    /**
     * Provides a read-only list of password entry sets.
     * @return List of password entries.
     * @since TODO
     */
    @NonNull
    public List<EnvInjectPasswordEntry> getPasswordEntryList() {
        if (passwordEntries == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(passwordEntries));
    }

    @DataBoundSetter
    public void setPasswordEntries(@CheckForNull EnvInjectPasswordEntry[] passwordEntries) {
        this.passwordEntries = passwordEntries;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        EnvInjectLogger logger = new EnvInjectLogger(listener);
        if (isInjectGlobalPasswords()) {
            logger.info("Inject global passwords.");
        }
        if (isMaskPasswordParameters()) {
            logger.info("Mask passwords that will be passed as build parameters.");
        }

        return new Environment() {
        };
    }

    /**
     * Returns a listing of passwords: globals (if active) and locals (job passwords)
     *
     * @return Listing of {@link EnvInjectPasswordEntry}
     * @throws EnvInjectException Operation error
     */
    @NonNull
    private List<EnvInjectPasswordEntry> getEnvInjectPasswordEntries() throws EnvInjectException {

        List<EnvInjectPasswordEntry> passwordList = new ArrayList<EnvInjectPasswordEntry>();

        // Process global passwords (provided by EnvInject)
        if (isInjectGlobalPasswords()) {
            EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
            EnvInjectGlobalPasswordEntry[] passwordEntries = globalPasswordRetriever.getGlobalPasswords();
            if (passwordEntries != null) {
                passwordList.addAll(Arrays.asList(passwordEntries));
            }
        }

        // Process local passwords (provided by EnvInject)
        passwordList.addAll(getPasswordEntryList());

        return passwordList;
    }

    @Override
    public OutputStream decorateLogger(AbstractBuild build, OutputStream outputStream) throws IOException, InterruptedException, Run.RunnerAbortedException {
        try {
            // Decorate passwords provided by EnvInject Plugin (globals and locals)
            List<String> passwords2decorate = Lists.newArrayList(Lists.transform(getEnvInjectPasswordEntries(), PASSWORD_ENTRY_TO_VALUE));

            // Decorate passwords passed as build parameters
            if (isMaskPasswordParameters()) {
                ParametersAction parametersAction = build.getAction(ParametersAction.class);
                if (parametersAction != null) {
                    List<ParameterValue> parameters = parametersAction.getParameters();
                    if (parameters != null) {
                        for (ParameterValue parameter : parameters) {
                            if (parameter instanceof PasswordParameterValue) {
                                PasswordParameterValue passwordParameterValue = ((PasswordParameterValue) parameter);
                                passwords2decorate.add(passwordParameterValue.getValue().getPlainText());
                            }
                        }
                    }
                }
            }
            
            return new EnvInjectPasswordsOutputStream(outputStream, passwords2decorate);

        } catch (EnvInjectException ee) {
            throw new Run.RunnerAbortedException();
        }
    }

    @Override
    public void makeSensitiveBuildVariables(AbstractBuild build, Set<String> sensitiveVariables) {
        try {
            sensitiveVariables.addAll(Lists.transform(getEnvInjectPasswordEntries(), PASSWORD_ENTRY_TO_NAME));
        } catch (EnvInjectException e) {
            // still better than showing secret password
            throw new RuntimeException(e);
        }
    }

    /**
     * Class took from the mask-passwords plugin
     */
    static class EnvInjectPasswordsOutputStream extends LineTransformationOutputStream {

        @NonNull
        private final OutputStream logger;
        @CheckForNull
        private final Pattern passwordsAsPattern;

        EnvInjectPasswordsOutputStream(@NonNull OutputStream logger, @CheckForNull Collection<String> passwords) {

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
                if (nbMaskedPasswords >= 1) { // is there at least one password to mask?
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
        @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "TODO needs triage")
        protected void eol(byte[] bytes, int len) throws IOException {
            String line = new String(bytes, 0, len);
            if (passwordsAsPattern != null) {
                line = passwordsAsPattern.matcher(line).replaceAll(EnvInjectPlugin.DEFAULT_MASK);
            }
            logger.write(line.getBytes());
        }

        @Override
        public void close() throws IOException {
            super.close();
            logger.close();
        }       
    }

    @Override
    public void makeBuildVariables(AbstractBuild build, Map<String, String> variables) {
        try {
            List<EnvInjectPasswordEntry> passwordList = new ArrayList<EnvInjectPasswordEntry>();

            // Process global passwords (provided by EnvInject)
            if (isInjectGlobalPasswords()) {
                EnvInjectGlobalPasswordRetriever globalPasswordRetriever = new EnvInjectGlobalPasswordRetriever();
                EnvInjectGlobalPasswordEntry[] passwordEntries = globalPasswordRetriever.getGlobalPasswords();
                if (passwordEntries != null) {
                    passwordList.addAll(Arrays.asList(passwordEntries));
                }
            }

            // Process local passwords (provided by EnvInject)
            passwordList.addAll(getPasswordEntryList());

            // Finally, the passwords has been injected.
            for (EnvInjectPasswordEntry passwordEntry : passwordList) {
                variables.put(passwordEntry.getName(), passwordEntry.getValue().getPlainText());
            }

        } catch (EnvInjectException ee) {
            throw new Run.RunnerAbortedException();
        }
    }


    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.EnvInjectPasswordWrapper_DisplayName();
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {

            EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
            passwordWrapper.setInjectGlobalPasswords(formData.getBoolean("injectGlobalPasswords"));
            passwordWrapper.setMaskPasswordParameters(formData.getBoolean("maskPasswordParameters"));

            // Inject passwords to the build as environment variables (locals, no globals)
            List<EnvInjectPasswordEntry> passwordEntries = req.bindJSONToList(EnvInjectPasswordEntry.class, formData.get("passwordEntry"));
            passwordWrapper.setPasswordEntries(passwordEntries.toArray(new EnvInjectPasswordEntry[passwordEntries.size()]));

            return passwordWrapper;
        }
    }

}
