package ca.vanzyl.ck8s.aws.s3.state;

public final class S3ChangeType {

    public static final String BUCKET_TYPE = "aws:s3:bucket";
    public static final String BUCKET_TAG_TYPE = "aws:s3:bucket:tag";

    public static final String PUBLIC_ACCESS_BLOCK_TYPE = "aws:s3:bucket:public-access-block";

    public static String bucketId(String bucketName) {
        return String.format("%s:%s", BUCKET_TYPE, bucketName);
    }

    public static String bucketTagId(String bucketName, String tagKey) {
        return bucketId(bucketName) + ":tag:" + tagKey;
    }

    public static String publicAccessBlockId(String bucketName) {
        return bucketId(bucketName) + ":public-access-block";
    }

    private S3ChangeType() {
    }
}
