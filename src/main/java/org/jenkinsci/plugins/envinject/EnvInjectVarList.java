package org.jenkinsci.plugins.envinject;

import hudson.model.Api;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Gregory Boissinot
 */
@ExportedBean(defaultVisibility = 99)
public class EnvInjectVarList implements Serializable {

    private Map<String, String> envVars = new TreeMap<String, String>();

    public EnvInjectVarList(Map<String, String> envMap) {
        this.envVars.putAll(envMap);
    }

    @SuppressWarnings("unused")
    @Exported
    public Map<String, String> getEnvMap() {
        return envVars;
    }

    @SuppressWarnings("unused")
    public Api getApi() {
        return new Api(this);
    }

    @SuppressWarnings("unused")
    public void doExport(StaplerRequest request, StaplerResponse response) throws IOException {

        String path = request.getPathInfo();
        if (path != null) {
            doExportWithPath(path, request, response);
            return;
        }

        doExportHeaders(request, response);
    }


    private void doExportWithPath(String path, StaplerRequest request, StaplerResponse response) throws IOException {

        if (path.endsWith("text")) {
            writeTextResponse(response);
            return;
        }

        if (path.endsWith("xml")) {
            writeXmlResponse(response);
            return;
        }

        if (path.endsWith("json")) {
            writeJsonResponse(response);
            return;
        }

        doExportHeaders(request, response);
    }

    private void doExportHeaders(StaplerRequest request, StaplerResponse response) throws IOException {

        String acceptHeader = request.getHeader("Accept");

        if (acceptHeader == null) {
            writeTextResponse(response);
            return;
        }

        if (acceptHeader.contains("application/xml")) {
            writeXmlResponse(response);
            return;
        }

        if (acceptHeader.contains("application/json")) {
            writeJsonResponse(response);
            return;
        }

        writeTextResponse(response);
        return;
    }


    private void writeTextResponse(StaplerResponse response) throws IOException {
        response.setContentType("plain/text");
        StringWriter stringWriter = new StringWriter();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            stringWriter.write(String.format("%s%s%s\n", entry.getKey(), "=", entry.getValue()));
        }
        response.getOutputStream().write(stringWriter.toString().getBytes());
    }

    private void writeXmlResponse(StaplerResponse response) throws IOException {
        response.setContentType("application/xml");
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write("<envVars>".getBytes());
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            outputStream.write(String.format("<envVar name=\"%s\" value=\"%s\"/>", entry.getKey(), entry.getValue()).getBytes());
        }
        outputStream.write("</envVars>".getBytes());
    }

    private void writeJsonResponse(StaplerResponse response) throws IOException {
        response.setContentType("application/json");
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write("{\"envVars\": { \"envVar\":[".getBytes());
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            sb.append(String.format(", {\"name\":\"%s\", \"value\":\"%s\"}", entry.getKey(), entry.getValue()));
        }
        sb.delete(0, 1);
        outputStream.write(sb.toString().getBytes());
        outputStream.write("]}}".getBytes());
    }

}
