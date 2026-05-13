package ca.vanzyl.ck8s.aws.s3.actions;

import ca.vanzyl.ck8s.aws.s3.S3ClientFactory;

import javax.inject.Inject;

@Deprecated
public class TagBucketDeprecatedAction extends TagBucketAction {

    @Inject
    public TagBucketDeprecatedAction(S3ClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public Action action() {
        return Action.TAG_BUCKET_DEPRECATED_ACTION;
    }
}
