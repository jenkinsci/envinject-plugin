/*
 * The MIT License
 *
 * Copyright 2014 Sony Mobile Communications AB. All rights reserved.
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

package org.jenkinsci.plugins.envinject.migration;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Tests the migrations.
 *
 * @author Robert Sandell
 */
public class EnvInjectMigrationBuildWrapperTest extends HudsonTestCase {

    /**
     * Tests that an old project containing both a set-env setting and a envInject wrapper
     * doesn't get overwritten in the migration.
     */
    @LocalData
    @Bug(22169)
    public void testSetEnvAndEnvInject() {
        FreeStyleProject project = (FreeStyleProject) jenkins.getItem("Experimental_SetEnvMigration");
        assertNotNull(project);
        EnvInjectBuildWrapper wrapper = project.getBuildWrappersList().get(EnvInjectBuildWrapper.class);
        String content = wrapper.getInfo().getPropertiesContent();

        assertStringContains(content, "ONE=one");
        assertStringContains(content, "HELLO=world");
        assertStringContains(content, "ME=you");

        EnvInjectJobProperty property = project.getProperty(EnvInjectJobProperty.class);
        assertStringContains(property.getInfo().getPropertiesContent(), "ZERO=0");
    }
}
