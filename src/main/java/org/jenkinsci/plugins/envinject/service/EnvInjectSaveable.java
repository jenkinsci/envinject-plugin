package org.jenkinsci.plugins.envinject.service;

import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * @author Gregory Boissinot
 */
@Deprecated
public class EnvInjectSaveable {

    private static final String ENVINJECT_TXT_FILENAME = "injectedEnvVars.txt";
    private static final String TOKEN = "=";

    public Map<String, String> getEnvironment(File rootDir) throws EnvInjectException {
        FileReader fileReader = null;
        try {
            File f = new File(rootDir, ENVINJECT_TXT_FILENAME);
            if (!f.exists()) {
                return null;
            }
            fileReader = new FileReader(f);
            Map<String, String> result = new HashMap<String, String>();
            fromTxt(fileReader, result);
            return result;
        } catch (FileNotFoundException fne) {
            throw new EnvInjectException(fne);
        } finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException ioe) {
                    throw new EnvInjectException(ioe);
                }
            }
        }
    }

    public void saveEnvironment(File rootDir, Map<String, String> envMap) throws EnvInjectException {
        FileWriter fileWriter = null;
        try {
            File f = new File(rootDir, ENVINJECT_TXT_FILENAME);
            fileWriter = new FileWriter(f);
            Map<String, String> map2Write = new TreeMap<String, String>();
            map2Write.putAll(envMap);
            toTxt(map2Write, fileWriter);
        } catch (FileNotFoundException fne) {
            throw new EnvInjectException(fne);
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException ioe) {
                    throw new EnvInjectException(ioe);
                }
            }
        }
    }

    private void fromTxt(FileReader fileReader, Map<String, String> result) throws EnvInjectException {
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, TOKEN);
                int tokens = tokenizer.countTokens();
                if (tokens == 2) {
                    result.put(String.valueOf(tokenizer.nextElement()), String.valueOf(tokenizer.nextElement()));
                }
            }
        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ioe) {
                    throw new EnvInjectException(ioe);
                }
            }
        }
    }

    private void toTxt(Map<String, String> envMap, FileWriter fw) throws IOException {
        for (Map.Entry<String, String> entry : envMap.entrySet()) {
            fw.write(String.format("%s%s%s\n", entry.getKey(), TOKEN, entry.getValue()));
        }
    }
}
