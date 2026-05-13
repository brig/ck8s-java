package ca.vanzyl.concord.k8s;

import ca.vanzyl.concord.k8s.model.K8sClusterDeployment;
import ca.vanzyl.concord.k8s.model.Metadata;
import ca.vanzyl.concord.k8s.model.NodeGroup;
import ca.vanzyl.concord.k8s.model.TemplateRenderer;
import ca.vanzyl.concord.k8s.model.TemplateRendererOptions;
import ca.vanzyl.concord.k8s.model.applications.ApplicationDeployment;
import ca.vanzyl.concord.k8s.model.applications.HiveApplicationDeployment;
import ca.vanzyl.concord.k8s.model.applications.S3Settings;
import ca.vanzyl.concord.k8s.model.aws.Aws;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static ca.vanzyl.concord.k8s.model.TemplateRenderer.version;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.readLines;
import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TemplateRendererTest
{

    public static final TemplateRenderer REQUEST_RENDERER = new TemplateRenderer(
            "../ck8s-server/src/main/resources/imports");

    public static final File RENDERED_EXPERIMENTATION_TEMPLATE = new File(
            "src/test/resources/rendered_template/experimentation.yaml");

    @Test
    public void testClusterRequestRendering()
            throws IOException
    {
        ApplicationDeployment trino = ApplicationDeployment.builder()
                .subdomain("fakedomain")
                .version("344.0.1")
                .build();
        HiveApplicationDeployment hiveApplication = HiveApplicationDeployment.builder()
                .s3(S3Settings.builder().region("{{region}}").build())
                .version("344.0.1")
                .build();

        Aws aws = Aws.builder()
                .region("us-east-2")
                .hostedZoneId("Z1T2PRVUBFACTP")
                .cidr("10.10.0.0/24")
                .secretRef("test")
                .build();

        assertThat(aws.region()).isEqualTo("us-east-2");
        assertThat(aws.instanceType()).isEqualTo("m5.xlarge");
        assertThat(aws.hostedZoneId()).isEqualTo("Z1T2PRVUBFACTP");

        K8sClusterDeployment clusterRequest = K8sClusterDeployment
                .builder()
                .deploymentId(UUID.fromString("4a84cca2-0737-11eb-a36e-f305d545ac42"))
                .metadata(Metadata.builder()
                        .profile("myco")
                        .debug(true)
                        .organization("myco")
                        .project("concord-k8s-builder")
                        .name("test-cluster") //WARNING -> Do not remove. This checks inter variable resolution
                        .build())
                .account("experimentation")
                .user("test")
                .domain("test.domain.com")
                .provider("aws")
                .aws(aws)
                .applications(ImmutableMap.of("myhive", hiveApplication,
                        "mypresto", trino))
                .flow("show-variables")
                .build();

        String clusterRequestYaml = REQUEST_RENDERER.render(clusterRequest, TemplateRendererOptions.defaults());
        assertThat(clusterRequest.metadata().name()).isEqualTo("test-cluster");
        String renderedExperimentationTemplate = join("\n",
                readLines(RENDERED_EXPERIMENTATION_TEMPLATE, UTF_8)).trim().replace("@VERSION@", version());
        assertEquals(renderedExperimentationTemplate, clusterRequestYaml.trim());
    }

    @Test
    public void testCusterRequestRenderingWithoutApplications()
            throws IOException
    {
        File clusterRequestWithoutApplications = new File(
                "src/test/resources/rendered_template/cluster-request-without-applications.yaml");

        Aws aws = Aws.builder()
                .region("us-east-2")
                .hostedZoneId("Z1T2PRVUBFACTP")
                .cidr("10.10.0.0/24")
                .secretRef("test")
                .build();

        assertThat(aws.region()).isEqualTo("us-east-2");
        assertThat(aws.instanceType()).isEqualTo("m5.xlarge");
        assertThat(aws.hostedZoneId()).isEqualTo("Z1T2PRVUBFACTP");

        NodeGroup nodeGroup = NodeGroup.builder()
                .desiredCapacity(3)
                .minSize(1)
                .maxSize(8)
                .build();

        K8sClusterDeployment clusterRequest = K8sClusterDeployment
                .builder()
                .deploymentId(UUID.fromString("4a84cca2-0737-11eb-a36e-f305d545ac42"))
                .metadata(Metadata.builder()
                        .debug(true)
                        .profile("myco")
                        .organization("myco")
                        .project("concord-k8s-builder")
                        .name("test-cluster") //WARNING -> Do not remove. This checks inter variable resolution
                        .build())
                .user("test")
                .account("experimentation")
                .domain("test.domain.com")
                .provider("aws")
                .aws(aws)
                .nodegroup(nodeGroup)
                .flow("show-variables")
                .build();

        String clusterRequestYaml = REQUEST_RENDERER.render(clusterRequest, TemplateRendererOptions.defaults());
        assertThat(clusterRequest.metadata().name()).isEqualTo("test-cluster");
        String renderedExperimentationTemplate = join("\n",
                readLines(clusterRequestWithoutApplications, UTF_8)).trim().replace("@VERSION@", version());
        assertEquals(renderedExperimentationTemplate, clusterRequestYaml.trim());
    }
}
