package org.jenkinsci.plugins.envinject.migration;


import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.ItemListener;
import hudson.plugins.envfile.EnvFileBuildWrapper;
import hudson.plugins.setenv.SetEnvBuildWrapper;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectJobProperty;
import org.jenkinsci.plugins.envinject.EnvInjectPasswordEntry;
import org.jenkinsci.plugins.envinject.EnvInjectPasswordWrapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectMigrationListener extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(EnvInjectMigrationListener.class.getName());

    private boolean containAPluginToMigrate(Class<? extends BuildWrapper> wrapperClass) {
        return EnvFileBuildWrapper.class.isAssignableFrom(wrapperClass)
                || SetEnvBuildWrapper.class.isAssignableFrom(wrapperClass);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onLoaded() {
        List<TopLevelItem> items = Hudson.getInstance().getItems();
        for (TopLevelItem item : items) {
            try {

                if (item instanceof Job) {
                    Job job = (Job) item;
                    Map<JobPropertyDescriptor, JobProperty> propertyMap = job.getProperties();
                    for (JobProperty jobProperty : propertyMap.values()) {
                        if (jobProperty.getClass() == EnvInjectJobProperty.class) {
                            EnvInjectJobProperty envInjectJobProperty = (EnvInjectJobProperty) jobProperty;
                            if (envInjectJobProperty.isOn()) {
                                EnvInjectPasswordWrapper passwordWrapper = new EnvInjectPasswordWrapper();
                                boolean isInjectGlobalPasswords = envInjectJobProperty.isInjectGlobalPasswords();
                                EnvInjectPasswordEntry[] passwordEntries = envInjectJobProperty.getPasswordEntries();
                                if (isInjectGlobalPasswords || (passwordEntries != null && passwordEntries.length != 0)) {
                                    passwordWrapper.setInjectGlobalPasswords(isInjectGlobalPasswords);
                                    passwordWrapper.setPasswordEntries(passwordEntries);
                                    if (item instanceof BuildableItemWithBuildWrappers) {
                                        BuildableItemWithBuildWrappers buildableItemWithBuildWrappers = (BuildableItemWithBuildWrappers) item;
                                        addOrModifyEnvInjectBuildWrapper(buildableItemWithBuildWrappers.getBuildWrappersList(), passwordWrapper);
                                        buildableItemWithBuildWrappers.save();
                                    }
                                }
                            }
                        }
                    }
                }

                if (item instanceof BuildableItemWithBuildWrappers) {
                    BuildableItemWithBuildWrappers buildableItemWithBuildWrappers = (BuildableItemWithBuildWrappers) item;
                    DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersList = buildableItemWithBuildWrappers.getBuildWrappersList();
                    Iterator<BuildWrapper> buildWrapperIterator = wrappersList.iterator();
                    while (buildWrapperIterator.hasNext()) {
                        BuildWrapper buildWrapper = buildWrapperIterator.next();
                        if (containAPluginToMigrate(buildWrapper.getClass())) {

                            //Get real wrapper object
                            EnvInjectMigrationBuildWrapper oldWrapper = (EnvInjectMigrationBuildWrapper) buildWrapper;

                            //Remove old wrapper
                            buildWrapperIterator.remove();

                            //Add new wrapper
                            addOrModifyEnvInjectBuildWrapper(buildableItemWithBuildWrappers.getBuildWrappersList(), oldWrapper.getEnvInjectBuildWrapper());

                            //Save the job with the new elements (the config.xml is overridden)
                            buildableItemWithBuildWrappers.save();
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Can't migrate old plugins to EnvInject plugin for the item %s", item.getName());
                e.printStackTrace();
            } catch (EnvInjectException e) {
                LOGGER.log(Level.SEVERE, "Can't migrate old plugins to EnvInject plugin for the item %s", item.getName());
                e.printStackTrace();
            }
        }
    }

    private void addOrModifyEnvInjectBuildWrapper(DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappers, BuildWrapper wrapper) throws EnvInjectException {

        //Iterate through all wrappers and remove the envInjectWrapper if exists: only one is authorized and the new wins
        Iterator<BuildWrapper> buildWrapperIterator = wrappers.iterator();
        while (buildWrapperIterator.hasNext()) {
            BuildWrapper buildWrapper = buildWrapperIterator.next();
            if (buildWrapper.getClass().isAssignableFrom(wrapper.getClass())) {
                buildWrapperIterator.remove();
            }
        }

        //Add the new envInjectBuildWrapper
        try {
            wrappers.add(wrapper);
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        }

    }

}
