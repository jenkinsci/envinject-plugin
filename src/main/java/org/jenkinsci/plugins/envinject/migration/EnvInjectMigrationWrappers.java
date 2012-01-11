package org.jenkinsci.plugins.envinject.migration;


import hudson.Extension;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.TopLevelItem;
import hudson.model.listeners.ItemListener;
import hudson.plugins.envfile.EnvFileBuildWrapper;
import hudson.plugins.setenv.SetEnvBuildWrapper;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.plugins.envinject.EnvInjectBuildWrapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gregory Boissinot
 */
@Extension
public class EnvInjectMigrationWrappers extends ItemListener {

    private static final Logger LOGGER = Logger.getLogger(EnvInjectMigrationWrappers.class.getName());

    private boolean containAPluginToMigrate(Class<? extends BuildWrapper> wrapperClass) {
        return EnvFileBuildWrapper.class.isAssignableFrom(wrapperClass)
                || SetEnvBuildWrapper.class.isAssignableFrom(wrapperClass);
    }

    @Override
    public void onLoaded() {
        List<TopLevelItem> items = Hudson.getInstance().getItems();
        for (TopLevelItem item : items) {
            try {
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
                            addOrModifyEnvInjectBuildWrapper(buildableItemWithBuildWrappers, oldWrapper.getEnvInjectBuildWrapper());

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

    private void addOrModifyEnvInjectBuildWrapper(BuildableItemWithBuildWrappers buildableItemWithBuildWrappers, EnvInjectBuildWrapper envInjectBuildWrapper) throws EnvInjectException {

        //Iterate through all wrappers and remove the envInjectWrapper if exists: only one is authorized and the new wins
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> wrappersList = buildableItemWithBuildWrappers.getBuildWrappersList();
        Iterator<BuildWrapper> buildWrapperIterator = wrappersList.iterator();
        while (buildWrapperIterator.hasNext()) {
            BuildWrapper buildWrapper = buildWrapperIterator.next();
            if (buildWrapper.getClass().isAssignableFrom(EnvInjectBuildWrapper.class)) {
                buildWrapperIterator.remove();
            }
        }

        //Add the new envInjectBuildWrapper
        try {
            wrappersList.add(envInjectBuildWrapper);
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        }

    }

}
