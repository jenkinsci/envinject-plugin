package org.jenkinsci.plugins.envinject.util;

import hudson.model.FreeStyleProject;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.htmlunit.html.HtmlForm;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test utilities for the plugin.
 * @author Oleg Nenashev
 */
public class TestUtils {
    
    /**
     * Saves configuration as a specific user.
     * The method presumes that all data has been already put to the instance of the project.
     * @param jenkins Test Rule
     * @param project Project to save.
     * @param userId User ID
     */
    public static void saveConfigurationAs(@NonNull JenkinsRule jenkins, @NonNull FreeStyleProject project, @NonNull String userId) throws Exception {
        JenkinsRule.WebClient w = jenkins.createWebClient().login(userId);
        final HtmlForm formByName = w.getPage(project, "configure").getFormByName("config");
        // Workaround for SECURITY-2450 in Script Security 1172.v35f6a_0b_8207e and newer
        formByName.getInputsByName("oldScript").forEach(input -> input.setValue("different value to force behavior when modified"));
        jenkins.submit(formByName);
    }
}
