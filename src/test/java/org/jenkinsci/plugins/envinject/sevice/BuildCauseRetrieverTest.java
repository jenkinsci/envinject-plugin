package org.jenkinsci.plugins.envinject.sevice;

import hudson.model.AbstractBuild;
import hudson.model.CauseAction;
import org.jenkinsci.plugins.envinject.EnvInjectPluginAction;
import org.jenkinsci.plugins.envinject.service.BuildCauseRetriever;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BuildCauseRetrieverTest {

    private AbstractBuild build;

    private BuildCauseRetriever buildCause;

    @Before
    public void setUp() {
        build = mock(AbstractBuild.class);
        buildCause = new BuildCauseRetriever();
    }

    @Test
    public void abstractBuildWithNullCauseActionReturnsUnknown() throws Exception {
        when(build.getAction(CauseAction.class)).thenReturn(null);
        Map<String, String> envVars = buildCause.getTriggeredCause(build);
        assertTrue(envVars.get("BUILD_CAUSE").equals("UNKNOWN"));
    }

}
