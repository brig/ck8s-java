package ca.vanzyl.ck8s.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.walmartlabs.concord.runtime.v2.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static ca.vanzyl.ck8s.aws.AwsTaskUtils.assertRegion;
import static java.util.Objects.requireNonNull;

@Named("ck8sAwsEcr")
@DryRunReady
public class EcrTask implements Task {

    private static final Logger log = LoggerFactory.getLogger(EcrTask.class);

    private final Context context;
    private final CredentialsProvider credentialsProvider;
    private final ObjectMapper objectMapper;

    private final boolean dryRunMode;

    @Inject
    public EcrTask(Context context, CredentialsProvider credentialsProvider, ObjectMapper objectMapper) {
        this.context = context;
        this.dryRunMode = context.processConfiguration().dryRun();
        this.credentialsProvider = credentialsProvider;
        this.objectMapper = requireNonNull(objectMapper).copy()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public TaskResult execute(Variables input) {
        var action = input.assertString("action");
        if ("describe-images".equals(action)) {
            return describeImages(input);
        } else if ("delete-images".equals(action)) {
            return deleteImage(input);
        } else if ("tag-image".equals(action)) {
            return tagImage(input);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private TaskResult describeImages(Variables input) {
        var region = assertRegion(input);
        var repositoryName = input.assertString("repositoryName");
        var maxResults = input.getInt("maxResults", 100);
        var debug = input.getBoolean("debug", context.processConfiguration().debug());
        var imageTagPattern = input.getString("imageTagPattern");
        var imageTag = input.getString("imageTag");
        var registryId = input.getString("registryId");

        if (debug) {
            log.info("Using region={}, maxResults={}, imageTagPattern={}, imageTag={}", region, maxResults, imageTagPattern, imageTag);
        }

        try (var client = EcrClient.builder()
                .credentialsProvider(credentialsProvider.get(input))
                .region(region)
                .build()) {

            if (debug) {
                log.info("Describing images in repository '{}'", repositoryName);
            }

            var request = DescribeImagesRequest.builder()
                    .registryId(registryId)
                    .repositoryName(repositoryName);

            if (imageTag != null) {
                request = request.imageIds(ImageIdentifier.builder().imageTag(imageTag).build());
            } else {
                request = request.maxResults(maxResults);
            }

            var data = client.describeImagesPaginator(request.build()).stream()
                    .flatMap(response -> response.imageDetails().stream())
                    .filter(image -> matchTag(image, imageTagPattern))
                    .map(ImageDetail::toBuilder)
                    .map(b -> (Map<?, ?>) objectMapper.convertValue(b, Map.class))
                    .toList();

            if (debug) {
                log.info("Done: {}", data.size());
            }

            return TaskResult.success()
                    .values(Map.of("imageDetails", data));
        }
    }

    private TaskResult deleteImage(Variables input) {
        var repositoryName = input.assertString("repositoryName");
        var imageIds = assertImageIds(input);
        var debug = input.getBoolean("debug", context.processConfiguration().debug());

        if (dryRunMode) {
            log.info("Running in dry-run mode: Skipping deleting images  '{}' in repository '{}'", imageIds, repositoryName);
            return TaskResult.success()
                    .value("dryRunMode", true);
        }

        try (var client = EcrClient.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build()) {

            List<ImageFailure> failures = new ArrayList<>();
            for (var ids : partitions(imageIds, 100)) {
                var request = BatchDeleteImageRequest.builder()
                        .repositoryName(repositoryName)
                        .imageIds(ids)
                        .build();

                var response = client.batchDeleteImage(request);
                if (response.hasFailures()) {
                    failures.addAll(response.failures());
                }

                if (debug) {
                    log.info("Processed {}/{}, failures: {}", ids.size(), imageIds.size(), failures.size());
                }
            }

            if (!failures.isEmpty()) {
                return TaskResult.fail("Failures in response")
                        .values(Map.of("failures", serialize(failures)));
            }

            return TaskResult.success();
        }
    }

    private TaskResult tagImage(Variables input) {
        String repositoryName = input.assertString("repositoryName");
        String imageVersion = input.assertString("version");
        String newTag = input.assertString("tag");

        try (var client = EcrClient.builder()
                .region(assertRegion(input))
                .credentialsProvider(credentialsProvider.get(input))
                .build()) {

            if (!imageExists(client, repositoryName, imageVersion)) {
                return TaskResult.fail("Image with tag '" + imageVersion + "' does not exist in repository '" + repositoryName + "'");
            }

            String manifest = getImageManifest(client, repositoryName, imageVersion);

            if (dryRunMode) {
                log.info("Running in dry-run mode: Skipping tagging image with tag '{}' in repository '{}'", imageVersion, repositoryName);
                return TaskResult.success()
                        .value("dryRunMode", true);
            }

            client.putImage(
                    PutImageRequest.builder()
                            .repositoryName(repositoryName)
                            .imageTag(newTag)
                            .imageManifest(manifest)
                            .build()
            );

            log.info("Successfully tagged image with new tag: {}", newTag);
        } catch (RepositoryNotFoundException e) {
            log.error("Error tagging image '{}' with new tag '{}' in repository '{}': repository not found", imageVersion, newTag, repositoryName);
            return TaskResult.fail(e);
        } catch (ImageAlreadyExistsException e) {
            log.error("Error tagging image '{}' with new tag '{}' in repository '{}': image already exists", imageVersion, newTag, repositoryName);
            return TaskResult.fail(e);
        } catch (Exception e) {
            log.error("Error tagging image '{}' with new tag '{}' in repository '{}'", imageVersion, newTag, repositoryName, e);
            return TaskResult.fail(e);
        }

        return TaskResult.success();
    }

    private static boolean imageExists(EcrClient ecrClient, String repositoryName, String imageVersion) {
        try {
            DescribeImagesResponse response = ecrClient.describeImages(
                    DescribeImagesRequest.builder()
                            .repositoryName(repositoryName)
                            .imageIds(ImageIdentifier.builder().imageTag(imageVersion).build())
                            .build()
            );
            return !response.imageDetails().isEmpty();
        } catch (ImageNotFoundException e) {
            return false;
        }
    }

    private static String getImageManifest(EcrClient ecrClient, String repositoryName, String imageVersion) {
        BatchGetImageResponse response = ecrClient.batchGetImage(
                BatchGetImageRequest.builder()
                        .repositoryName(repositoryName)
                        .imageIds(ImageIdentifier.builder().imageTag(imageVersion).build())
                        .build()
        );

        if (response.images().isEmpty()) {
            throw new UserDefinedException("Failed to retrieve image manifest for '" + imageVersion + "' in '" + repositoryName + "' repository");
        }

        return response.images().get(0).imageManifest();
    }

    private static List<Map<String, Object>> serialize(List<ImageFailure> failures) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var failure : failures) {
            result.add(Map.of("imageId", serialize(failure.imageId()),
                    "failureCode", failure.failureCode(),
                    "failureReason", failure.failureReason()));
        }

        return result;
    }

    private static String serialize(ImageIdentifier imageIdentifier) {
        if (imageIdentifier == null) {
            return null;
        }
        if (imageIdentifier.imageTag() != null) {
            return imageIdentifier.imageTag();
        }
        return imageIdentifier.imageDigest();
    }

    private static List<ImageIdentifier> assertImageIds(Variables input) {
        String imageTag = input.getString("imageTag");
        if (imageTag != null) {
            return List.of(ImageIdentifier.builder().imageTag(imageTag).build());
        }

        List<String> imageTags = input.getList("imageTags", List.of());
        if (!imageTags.isEmpty()) {
            return imageTags.stream().map(i -> ImageIdentifier.builder().imageTag(i).build()).toList();
        }

        throw new IllegalArgumentException("Missing 'imageTags' or 'imageTags' in the input variable");
    }

    private static <T> List<List<T>> partitions(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(list.size(), i + size)))
            );
        }
        return parts;
    }

    private static boolean matchTag(ImageDetail image, String imageTagPattern) {
        if (imageTagPattern == null) {
            return true;
        }

        if (!image.hasImageTags()) {
            return false;
        }

        Pattern compiledPattern = Pattern.compile(imageTagPattern, Pattern.CASE_INSENSITIVE);
        return image.imageTags().stream().anyMatch(t -> compiledPattern.matcher(t).matches());
    }
}
