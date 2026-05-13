package ca.vanzyl.ck8s.aws;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class DescribeVpcs
{

    private final Ec2Client ec2;

    public DescribeVpcs()
    {
        ec2 = AwsUtils.ec2Client("us-east-1");
    }

    public static void main(String[] args)
            throws Exception
    {
        new DescribeVpcs().describeVpc("vpc-0e2f8c39b2233e20e");
    }

    public void describeVpc(String vpcId)
    {

        DescribeVpcsRequest request = DescribeVpcsRequest
                .builder()
                .vpcIds(vpcId)
                .build();

        Vpc vpc = ec2.describeVpcs(request).vpcs().get(0);

        System.out.printf(
                "Found VPC with id %s, " +
                        "vpc state %s " +
                        "and tennancy %s %n default vpc: %s %n",
                vpc.vpcId(),
                vpc.stateAsString(),
                vpc.instanceTenancyAsString(), vpc.isDefault());

        System.out.println(vpc.isDefault());

        DescribeSubnetsRequest subnetsRequest = DescribeSubnetsRequest
                .builder()
                .filters(Filter.builder()
                        .name("vpc-id")
                        .values(vpc.vpcId())
                        .build())
                .build();
        DescribeSubnetsResponse subnetsResponse = ec2.describeSubnets(subnetsRequest);
        subnetsResponse.subnets().forEach(s -> System.out.println(s.subnetId() + ": " + s.cidrBlock()));
    }

    public String find(String region)
    {
        DescribeVpcsRequest request = DescribeVpcsRequest.builder().build();
        Ec2Client client = AwsUtils.ec2Client(region);
        DescribeVpcsResponse result = client.describeVpcs(request);
        List<String> vpcIps = result.vpcs().stream().map(s -> cidrIp(s.cidrBlock())).collect(Collectors.toList());
        for (int i = 1; i < 100; i++) {
            String freeCidrIp = String.format("10.%s.0.0", i);
            if (!vpcIps.contains(freeCidrIp)) {
                return freeCidrIp + "/16";
            }
        }
        return null;
    }

    private String cidrIp(String cidrBlock)
    {
        return cidrBlock.substring(0, cidrBlock.indexOf("/"));
    }
}