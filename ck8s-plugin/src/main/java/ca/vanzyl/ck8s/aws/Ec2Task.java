package ca.vanzyl.ck8s.aws;

import ca.vanzyl.ck8s.common.Mapper;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.Task;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.runtime.v2.sdk.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

import static ca.vanzyl.ck8s.aws.AwsTaskUtils.assertRegion;

@Named("ck8sAwsEc2")
public class Ec2Task implements Task {

    private final static Logger log = LoggerFactory.getLogger(Ec2Task.class);

    private final Context context;
    private final CredentialsProvider credentialsProvider;

    @Inject
    public Ec2Task(Context context, CredentialsProvider credentialsProvider) {
        this.context = context;
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public TaskResult execute(Variables input) throws Exception {
        var action = input.assertString("action");
        if ("describe-launch-templates".equals(action)) {
            return describeLaunchTemplates(input);
        } else if ("create-launch-template".equals(action)) {
            return createLaunchTemplate(input);
        } else if ("delete-launch-template".equals(action)) {
            return deleteLaunchTemplate(input);
        } else if ("describe-launch-template-versions".equals(action)) {
            return describeLaunchTemplateVersions(input);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private TaskResult describeLaunchTemplates(Variables input) throws Exception {
        var name =  input.getString("name");

        try (var client = Ec2Client.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build()) {

            var request = DescribeLaunchTemplatesRequest.builder();
            if (name != null) {
                request = request.launchTemplateNames(name);
            }
            var response = client.describeLaunchTemplates(request.build());
            return TaskResult.success()
                    .value("templates", AwsTaskUtils.serializeList(response.launchTemplates()));
        } catch (Ec2Exception e) {
            if ("InvalidLaunchTemplateName.NotFoundException".equals(e.awsErrorDetails().errorCode())) {
                return TaskResult.success()
                        .value("templates", List.of());
            }

            throw e;
        }
    }

    private TaskResult describeLaunchTemplateVersions(Variables input) throws Exception {
        var launchTemplateId =  input.getString("launchTemplateId");
        var versions =  input.getString("versions");

        try (var client = Ec2Client.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build()) {

            var request = DescribeLaunchTemplateVersionsRequest.builder();
            if (launchTemplateId != null) {
                request = request.launchTemplateId(launchTemplateId);
            }
            if (versions != null) {
                request = request.versions(versions);
            }

            var response = client.describeLaunchTemplateVersions(request.build());
            return TaskResult.success()
                    .value("launchTemplateVersions", AwsTaskUtils.serializeList(response.launchTemplateVersions()));
        } catch (Ec2Exception e) {
            if ("InvalidLaunchTemplateId.NotFound".equals(e.awsErrorDetails().errorCode())) {
                return TaskResult.success()
                        .value("launchTemplateVersions", List.of());
            }

            throw e;
        }
    }

    private TaskResult createLaunchTemplate(Variables input) throws Exception {
        var name = input.assertString("name");
        Map<String, Object> data = input.assertMap("data");

        try (var client = Ec2Client.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build()) {

            var request = CreateLaunchTemplateRequest.builder()
                    .launchTemplateName(name)
                    .launchTemplateData(AwsTaskUtils.deserialize(data, RequestLaunchTemplateData.serializableBuilderClass()).build())
                    .build();

            if (context.processConfiguration().debug()) {
                log.info("Launch template data:\n{}", Mapper.yaml().writeAsString(data));
                log.info("Launch template request: {}", request);
            }

            var response = client.createLaunchTemplate(request);
            return TaskResult.success()
                    .value("response", AwsTaskUtils.serialize(response));
        }
    }

    private TaskResult deleteLaunchTemplate(Variables input) throws Exception {
        var name = input.assertString("name");

        try (var client = Ec2Client.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build()) {

            var request = DeleteLaunchTemplateRequest.builder()
                    .launchTemplateName(name)
                    .build();

            var response = client.deleteLaunchTemplate(request);
            return TaskResult.success()
                    .value("response", AwsTaskUtils.serialize(response));
        }
    }
}
