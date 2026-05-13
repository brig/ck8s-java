package ca.vanzyl.ck8s.aws.s3.state;

import ca.vanzyl.ck8s.state.Entity;
import org.immutables.value.Value;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

@Value.Style(jdkOnly = true)
@Value.Immutable
public interface S3Bucket extends Entity {

    String bucketName();

    @Nullable
    PublicAccessBlockConfiguration publicAccessBlock();

    @Value.Default
    default Map<String, String> tags() {
        return Map.of();
    }

    static ImmutableS3Bucket.Builder builder() {
        return ImmutableS3Bucket.builder();
    }

    @Override
    default String entityName() {
        return bucketName();
    }

    @Override
    default void dump(OutputStream out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println("name: " + bucketName());
        if (publicAccessBlock() != null) {
            writer.println("publicAccessBlock: " + publicAccessBlock());
        }
        if (!tags().isEmpty()) {
            writer.println("tags:");
            tags().forEach((k, v) -> writer.append("  ").append(k).append(": ").append(v).append("\n"));
        }
        writer.flush();
    }
}
