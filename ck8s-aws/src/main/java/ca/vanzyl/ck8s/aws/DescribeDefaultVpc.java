package ca.vanzyl.ck8s.aws;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Vpc;

public class DescribeDefaultVpc
{

    private final Ec2Client ec2;
    private final String region;

    public DescribeDefaultVpc(String region)
    {
        this.region = region;
        this.ec2 = AwsUtils.ec2Client("us-east-2", "sandbox");
    }

    public static void main(String[] args)
            throws Exception
    {
        new DescribeDefaultVpc("us-east-2").describeVpc();
    }

    public VpcResult describeVpc()
    {

        DescribeVpcsRequest request = DescribeVpcsRequest
                .builder()
                .build();

        Vpc defaultVpc = ec2.describeVpcs(request).vpcs().stream()
                .filter(vpc -> vpc.isDefault())
                .findFirst().get();

        ImmutableVpcResult.Builder vpcResult = ImmutableVpcResult.builder()
                .region(region)
                .cidrBlock(defaultVpc.cidrBlock())
                .vpcId(defaultVpc.vpcId());

        DescribeSubnetsRequest subnetsRequest = DescribeSubnetsRequest
                .builder()
                .filters(Filter.builder()
                        .name("vpc-id")
                        .values(defaultVpc.vpcId())
                        .build())
                .build();

        ec2.describeSubnets(subnetsRequest).subnets().forEach(s -> {
            if (s.mapPublicIpOnLaunch()) {
                ImmutableSubnet subnet = ImmutableSubnet.builder()
                        .id(s.subnetId())
                        .availabilityZoneId(s.availabilityZoneId())
                        .cidrBlock(s.cidrBlock())
                        .publiclyAccessible(true)
                        .build();
                vpcResult.addPubliclyAccessibleSubnets(subnet);
            }
            System.out.println("public: " + s.mapPublicIpOnLaunch());
            System.out.println(s.subnetId() + ": " + s.cidrBlock());
        });

        return vpcResult.build();
    }

    // This is not very sophisticated, need to look at the routes
    private boolean isPublic(software.amazon.awssdk.services.ec2.model.Subnet subnet)
    {
        return subnet.mapPublicIpOnLaunch();
    }
}