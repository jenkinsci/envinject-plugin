package org.jenkinsci.plugins.envinject;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.Plugin;
import hudson.model.Hudson;
import hudson.plugins.envfile.EnvFileBuildWrapper;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectPlugin extends Plugin {
    @Override
    public void start() throws Exception {
        super.start();
        Hudson.XSTREAM.registerConverter(new EnvInjectXStreamConverter());

        this.load();

    }

    private class EnvInjectXStreamConverter implements Converter {

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

            EnvInjectPlugin plugin = (EnvInjectPlugin) source;
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            Object object = context.currentObject();
            if (object != null && object instanceof EnvInjectPlugin) {
                EnvInjectPlugin injectPlugin = (EnvInjectPlugin) object;

            }

            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean canConvert(Class type) {
            return EnvFileBuildWrapper.class.isAssignableFrom(type);
        }
    }
}
