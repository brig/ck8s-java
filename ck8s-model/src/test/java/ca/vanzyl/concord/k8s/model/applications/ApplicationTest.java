package ca.vanzyl.concord.k8s.model.applications;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ApplicationTest
{

    @Test
    public void testCatalogsToYaml()
            throws IOException
    {
        Map<String, String> fakeConnectorProerties = new HashMap<>();
        fakeConnectorProerties.put("connector.name", "fakeConnector");
        fakeConnectorProerties.put("fake.prop.1", "1");
        fakeConnectorProerties.put("fake.prop.2", "2");
        Map<String, Map<String, String>> catalogs = new HashMap<>();
        catalogs.put("hive", fakeConnectorProerties);

        ApplicationDeployment trino = ApplicationDeployment
                .builder()
                .subdomain("fakedomain")
                .version("344.0.1")
                .catalogs(catalogs)
                .build();

        Assert.assertEquals(
                "\n" +
                        "  hive: |-\n" +
                        "    connector.name=fakeConnector\n" +
                        "    fake.prop.1=1\n" +
                        "    fake.prop.2=2\n" +
                        "  ",
                trino.catalogsToYaml());
    }

    @Test
    public void testCatalogsToYamlWithSinglePropertyInConnector()
            throws IOException
    {
        Map<String, String> fakeConnectorProerties = new HashMap<>();
        fakeConnectorProerties.put("connector.name", "tcph");
        Map<String, Map<String, String>> catalogs = new HashMap<>();
        catalogs.put("tcph2", fakeConnectorProerties);

        ApplicationDeployment trino = ApplicationDeployment
                .builder()
                .subdomain("fakedomain")
                .version("344.0.1")
                .catalogs(catalogs)
                .build();

        String catalogsYaml = trino.catalogsToYaml();

        Assert.assertEquals(
                "\n" +
                        "  tcph2: |-\n" +
                        "    connector.name=tcph\n" +
                        "  ",
                catalogsYaml);
    }

    @Test
    public void testEtcFilesToYaml()
            throws IOException
    {
        Map<String, String> etcFiles = new HashMap<>();
        String accessControlFileContents = "access-control.name=file\n" +
                "security.config-file=etc/rules.json\n";
        etcFiles.put("access-control.properties", accessControlFileContents);

        String jsonFileContents = String.join("\n", "{",
                "  \"catalogs\": [",
                "    {",
                "      \"user\": \"admin\",",
                "      \"catalog\": \"(mysql|system)\",",
                "      \"allow\": \"all\"",
                "    },",
                "    {",
                "      \"group\": \"finance|human_resources\",",
                "      \"catalog\": \"postgres\",",
                "      \"allow\": true",
                "    },",
                "    {",
                "      \"catalog\": \"hive\",",
                "      \"allow\": \"all\"",
                "    },",
                "    {",
                "      \"user\": \"alice\",",
                "      \"catalog\": \"postgresql\",",
                "      \"allow\": \"read-only\"",
                "    },",
                "    {",
                "      \"catalog\": \"system\",",
                "      \"allow\": \"none\"",
                "    }",
                "  ]",
                "}");
        etcFiles.put("rules.json", jsonFileContents);

        ApplicationDeployment trino = ApplicationDeployment
                .builder()
                .type("trino")
                .version("344.0.1")
                .subdomain("fakedomain")
                .etcFiles(etcFiles)
                .build();

        Assert.assertEquals(
                "\n" +
                        "      access-control.properties: |\n" +
                        "        access-control.name=file\n" +
                        "        security.config-file=etc/rules.json\n" +
                        "      rules.json: |\n" +
                        "        {\n" +
                        "          \"catalogs\": [\n" +
                        "            {\n" +
                        "              \"user\": \"admin\",\n" +
                        "              \"catalog\": \"(mysql|system)\",\n" +
                        "              \"allow\": \"all\"\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"group\": \"finance|human_resources\",\n" +
                        "              \"catalog\": \"postgres\",\n" +
                        "              \"allow\": true\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"catalog\": \"hive\",\n" +
                        "              \"allow\": \"all\"\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"user\": \"alice\",\n" +
                        "              \"catalog\": \"postgresql\",\n" +
                        "              \"allow\": \"read-only\"\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"catalog\": \"system\",\n" +
                        "              \"allow\": \"none\"\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ",
                trino.etcFilesToYaml());
    }

    @Test
    public void testEtcFilesToYamlSingleLineContent()
            throws IOException
    {
        Map<String, String> etcFiles = new HashMap<>();
        String file1Contents = "lineWithoutNewNewline";
        etcFiles.put("file1", file1Contents);

        String file2Content = "lineWithNewLine\n";
        etcFiles.put("file2", file2Content);

        ApplicationDeployment trino = ApplicationDeployment
                .builder()
                .type("trino")
                .subdomain("fakedomain")
                .version("344.0.1")
                .etcFiles(etcFiles)
                .build();

        Assert.assertEquals(
                "\n" +
                        "      file2: |\n" +
                        "        lineWithNewLine\n" +
                        "      file1: |\n" +
                        "        lineWithoutNewNewline\n" +
                        "      ",
                trino.etcFilesToYaml());
    }
}
