package ca.vanzyl.ck8s.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.sdk.DryRunReady;
import com.walmartlabs.concord.runtime.v2.sdk.FileService;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.WorkingDirectory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Named("ck8sResource")
@DryRunReady
public class Ck8sResourceTask implements Task {

    private final Path workDir;
    private final FileService fileService;

    @Inject
    public Ck8sResourceTask(WorkingDirectory workingDirectory, FileService fileService) {
        this.workDir = workingDirectory.getValue();
        this.fileService = fileService;
    }

    public String writeAsYaml(Object content) throws IOException {
        Path tmpFile = fileService.createTempFile("ck8s_resource", "yaml");
        try (OutputStream out = Files.newOutputStream(tmpFile)) {
            createYamlObjectMapper().writeValue(out, content);
        }
        return workDir.relativize(tmpFile.toAbsolutePath()).toString();
    }

    public String writeAsProperties(Map<String, Object> map) throws IOException {
        Properties properties = new Properties();
        map.forEach((key, value) -> properties.setProperty(key, String.valueOf(value)));

        Path dst = fileService.createTempFile("properties", ".tmp");
        try (OutputStream out = Files.newOutputStream(dst)) {
            store(properties, out);
        }

        return workDir.relativize(dst.toAbsolutePath()).toString();
    }

    public Object asJson(String path) throws IOException {
        try (InputStream in = Files.newInputStream(Paths.get( path))) {
            return createObjectMapper().readValue(in, Object.class);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        return om;
    }

    private static ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true);
        yamlFactory.disable(YAMLGenerator.Feature.SPLIT_LINES);
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        var om = new ObjectMapper(yamlFactory);
        om.registerModule(new JavaTimeModule());
        return om;
    }

    private void store(Properties properties, OutputStream out) throws IOException {
        store0(properties, new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.ISO_8859_1)), true);
    }

    private void store0(Properties properties, BufferedWriter bw, boolean escUnicode) throws IOException {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Collection<Map.Entry<String, String>> entries = (Set<Map.Entry<String, String>>) (Set) properties.entrySet();
            entries = new ArrayList<>(entries);

        ((List<Map.Entry<String, String>>) entries).sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, String> e : entries) {
            String key = e.getKey();
            String val = e.getValue();
            key = saveConvert(key, true, escUnicode);
            val = saveConvert(val, false, escUnicode);
            bw.write(key + "=" + val);
            bw.newLine();
        }
        bw.flush();
    }

    private String saveConvert(String theString,
                               boolean escapeSpace,
                               boolean escapeUnicode) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder outBuffer = new StringBuilder(bufLen);
        HexFormat hex = HexFormat.of().withUpperCase();
        for (int x = 0; x < len; x++) {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch (aChar) {
                case ' ':
                    if (x == 0 || escapeSpace)
                        outBuffer.append('\\');
                    outBuffer.append(' ');
                    break;
                case '\t':
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n':
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r':
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f':
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                    break;
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
                        outBuffer.append("\\u");
                        outBuffer.append(hex.toHexDigits(aChar));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }
}
