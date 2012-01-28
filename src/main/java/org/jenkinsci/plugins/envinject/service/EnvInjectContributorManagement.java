package org.jenkinsci.plugins.envinject.service;

import hudson.DescriptorExtensionList;
import org.jenkinsci.plugins.envinject.model.EnvInjectJobPropertyContributor;
import org.jenkinsci.plugins.envinject.model.EnvInjectJobPropertyContributorDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectContributorManagement {

    public boolean isEnvInjectContributionActivated() {
        DescriptorExtensionList<EnvInjectJobPropertyContributor, EnvInjectJobPropertyContributorDescriptor>
                descriptors = EnvInjectJobPropertyContributor.all();
        return descriptors.size() != 0;
    }

    public EnvInjectJobPropertyContributor[] getNewContributorsInstance() throws org.jenkinsci.lib.envinject.EnvInjectException {

        List<EnvInjectJobPropertyContributor> result = new ArrayList<EnvInjectJobPropertyContributor>();

        DescriptorExtensionList<EnvInjectJobPropertyContributor, EnvInjectJobPropertyContributorDescriptor>
                descriptors = EnvInjectJobPropertyContributor.all();

        for (EnvInjectJobPropertyContributorDescriptor descriptor : descriptors) {
            Class<? extends EnvInjectJobPropertyContributor> classJobProperty = descriptor.clazz;
            try {
                EnvInjectJobPropertyContributor contributor = classJobProperty.newInstance();
                Method initMethod = classJobProperty.getMethod("init");
                initMethod.invoke(contributor);
                result.add(contributor);
            } catch (InstantiationException ie) {
                throw new org.jenkinsci.lib.envinject.EnvInjectException(ie);
            } catch (IllegalAccessException ie) {
                throw new org.jenkinsci.lib.envinject.EnvInjectException(ie);
            } catch (NoSuchMethodException ne) {
                throw new org.jenkinsci.lib.envinject.EnvInjectException(ne);
            } catch (InvocationTargetException ie) {
                throw new org.jenkinsci.lib.envinject.EnvInjectException(ie);
            }
        }
        return result.toArray(new EnvInjectJobPropertyContributor[result.size()]);
    }
}
