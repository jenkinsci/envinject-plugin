package org.jenkinsci.plugins.envinject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Api;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * @author Gregory Boissinot
 */
@ExportedBean(defaultVisibility = 99)
public class EnvInjectVarList implements Serializable {

    @NonNull
    private final Map<String, String> envVars = new TreeMap<String, String>();
    
    /**
     * Empty variables list, which should be returned if the variables are hidden
     * due to the security settings.
     */
    public static final EnvInjectVarList HIDDEN = new Hidden();

    public EnvInjectVarList(Map<String, String> envMap) {
        if (envMap != null) {
            this.envVars.putAll(envMap);
        }
    }

    @Exported
    public Map<String, String> getEnvMap() {
        return envVars;
    }

    public Api getApi() {
        return new Api(this);
    }

    @RequirePOST
    public void doExport(@NonNull StaplerRequest request, @NonNull StaplerResponse response) throws IOException {

        String path = request.getPathInfo();
        if (path != null) {
            doExportWithPath(path, request, response);
            return;
        }

        doExportHeaders(request, response);
    }


    private void doExportWithPath(@NonNull String path, @NonNull StaplerRequest request, @NonNull StaplerResponse response) throws IOException {

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

    private void doExportHeaders(@NonNull StaplerRequest request, @NonNull StaplerResponse response) throws IOException {

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
    }


    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "TODO needs triage")
    private void writeTextResponse(@NonNull StaplerResponse response) throws IOException {
        response.setContentType("plain/text");
        StringWriter stringWriter = new StringWriter();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            stringWriter.write(String.format("%s%s%s%n", entry.getKey(), "=", entry.getValue()));
        }
        response.getOutputStream().write(stringWriter.toString().getBytes());
    }

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "TODO needs triage")
    private void writeXmlResponse(@NonNull StaplerResponse response) throws IOException {
        response.setContentType("application/xml");
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write("<envVars>".getBytes());
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            outputStream.write(String.format("<envVar name=\"%s\" value=\"%s\"/>", escapeXml(entry.getKey()), escapeXml(entry.getValue())).getBytes());
        }
        outputStream.write("</envVars>".getBytes());
    }

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "TODO needs triage")
    private void writeJsonResponse(@NonNull StaplerResponse response) throws IOException {
        response.setContentType("application/json");
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write("{\"envVars\": { \"envVar\":[".getBytes());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            sb.append(String.format(", {\"name\":\"%s\", \"value\":\"%s\"}", escapeJson(entry.getKey()), escapeJson(entry.getValue())));
        }
        sb.delete(0, 1);
        outputStream.write(sb.toString().getBytes());
        outputStream.write("]}}".getBytes());
    }

    private String escapeXml(String xml) {
        return xml.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String escapeJson(String json) {
        return json.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    
    //TODO: Throw errors in responses?
    /**
     * Implements an {@link EnvInjectVarList}, which does not provide any variables.
     */
    private static class Hidden extends EnvInjectVarList {
        private static final long serialVersionUID = 1L;

        public Hidden() {
            super(Collections.<String,String>emptyMap());
        }  
    }

}
