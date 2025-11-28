package org.jenkinsci.plugins.envinject.service;

import hudson.model.AbstractBuild;
import hudson.model.CauseAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class BuildCauseRetrieverTest {

    private AbstractBuild build;

    private BuildCauseRetriever buildCause;

    @BeforeEach
    void setUp() {
        build = mock(AbstractBuild.class);
        buildCause = new BuildCauseRetriever();
    }

    @Test
    void abstractBuildWithNullCauseActionReturnsUnknown() {
        when(build.getAction(CauseAction.class)).thenReturn(null);
        Map<String, String> envVars = buildCause.getTriggeredCause(build);
        assertEquals("UNKNOWN", envVars.get("BUILD_CAUSE"));
    }

}
