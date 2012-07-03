package org.jenkinsci.plugins.envinject.service;

import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.maven.MavenModuleSet;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

/**
 * @author Gregory Boissinot
 */
public class BuildWrapperService implements Serializable {

    @SuppressWarnings("unchecked")
    public void addBuildWrapper(AbstractBuild build, BuildWrapper buildWrapper) throws EnvInjectException {

        if (buildWrapper == null) {
            throw new NullPointerException("A build wrapper object is required.");
        }

        try {
            DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersProject = getBuildWrapperDescriptorDescribableList(build);
            wrappersProject.add(buildWrapper);

        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        }
    }

    @SuppressWarnings("unchecked")
    public void removeBuildWrappers(AbstractBuild build, Class<? extends BuildWrapper>... wrappersClass) throws EnvInjectException {

        if (wrappersClass == null) {
            throw new NullPointerException("A class wrappers is required.");
        }

        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersProject = getBuildWrapperDescriptorDescribableList(build);
        Iterator<BuildWrapper> iterator = wrappersProject.iterator();
        while (iterator.hasNext()) {
            BuildWrapper buildWrapper = iterator.next();
            for (Class<? extends BuildWrapper> wrapperClass : wrappersClass) {
                if ((wrapperClass.getName()).equals(buildWrapper.getClass().getName())) {
                    try {
                        wrappersProject.remove(buildWrapper);
                    } catch (IOException ioe) {
                        throw new EnvInjectException(ioe);
                    }
                }
            }
        }
    }

    private DescribableList<BuildWrapper, Descriptor<BuildWrapper>> getBuildWrapperDescriptorDescribableList(AbstractBuild build) throws EnvInjectException {
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersProject;
        if (build instanceof MatrixRun) {
            MatrixProject project = ((MatrixRun) build).getParentBuild().getProject();
            return project.getBuildWrappersList();
        } else {
            AbstractProject abstractProject = build.getProject();
            if (abstractProject instanceof FreeStyleProject) {
                Project project = (Project) abstractProject;
                return project.getBuildWrappersList();
            } else if (abstractProject instanceof MavenModuleSet) {
                MavenModuleSet moduleSet = (MavenModuleSet) abstractProject;
                return moduleSet.getBuildWrappersList();
            } else if (Hudson.getInstance().getPlugin("ivy") != null && abstractProject instanceof hudson.ivy.IvyModuleSet) {
                hudson.ivy.IvyModuleSet ivyModuleSet = (hudson.ivy.IvyModuleSet) abstractProject;
                return ivyModuleSet.getBuildWrappersList();
            } else {
                throw new EnvInjectException(String.format("Job type %s is not supported", abstractProject));
            }
        }
    }

}
