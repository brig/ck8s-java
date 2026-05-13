package ca.vanzyl.ck8s.aws.s3.state;

import ca.vanzyl.ck8s.preview.Change;
import ca.vanzyl.ck8s.state.EntityChangeProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ca.vanzyl.ck8s.preview.ChangeUtils.mapChanges;

public class S3BucketChangesProducer implements EntityChangeProducer<S3BucketKey, S3Bucket> {

    @Override
    public Class<S3Bucket> entityType() {
        return S3Bucket.class;
    }

    @Override
    public List<Change> produce(S3BucketKey key, S3Bucket prev, S3Bucket current, Instant lastModified) {
        if (prev == null && current == null) {
            return List.of();
        }

        if (current == null) {
            return List.of(Change.delete(S3ChangeType.bucketId(prev.bucketName()))
                    .type(S3ChangeType.BUCKET_TYPE)
                    .metadata(Change.Metadata.builder().name(prev.bucketName()).build())
                    .timestamp(lastModified)
                    .build());
        }

        var action = Change.Action.UPDATE;
        if (prev == null) {
            action = Change.Action.CREATE;
            prev = S3Bucket.builder().bucketName(current.bucketName()).build();
        }

        var diff = diff(action, prev, current);
        if (diff.isEmpty()) {
            return List.of();
        }

        var changes = new ArrayList<Change>();
        changes.add(Change.builder()
                .id(S3ChangeType.bucketId(current.bucketName()))
                .action(action)
                .type(S3ChangeType.BUCKET_TYPE)
                .metadata(Change.Metadata.builder().name(current.bucketName()).build())
                .timestamp(lastModified)
                .build());
        changes.addAll(diff);
        return changes;
    }

    private static List<Change> diff(Change.Action action, S3Bucket stateBucket, S3Bucket bucket) {
        List<Change> result = new ArrayList<>();

        var bucketId = S3ChangeType.bucketId(bucket.bucketName());

        var stateAccessBlock = stateBucket.publicAccessBlock();
        var bucketAccessBlock = bucket.publicAccessBlock();
        if (!Objects.equals(stateAccessBlock, bucketAccessBlock)) {
            result.add(Change.builder()
                    .action(action)
                    .id(S3ChangeType.publicAccessBlockId(bucket.bucketName()))
                    .parentId(bucketId)
                    .type(S3ChangeType.PUBLIC_ACCESS_BLOCK_TYPE)
                    .metadata(Change.Metadata.builder().name("").build())
                    .build());
        }

        result.addAll(mapChanges(bucketId, S3ChangeType.BUCKET_TAG_TYPE,
                stateBucket.tags(), bucket.tags(),
                key -> S3ChangeType.bucketTagId(bucketId, key)));

        return result;
    }
}
