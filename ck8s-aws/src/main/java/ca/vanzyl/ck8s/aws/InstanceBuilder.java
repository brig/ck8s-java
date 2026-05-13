package ca.vanzyl.ck8s.aws;

// snippet-start:[ec2.java2.create_instance.import]

import static java.lang.String.format;
import static java.nio.file.Paths.get;
import static software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE;
import static software.amazon.awssdk.services.ec2.model.ResourceType.SECURITY_GROUP;

import com.google.common.collect.ImmutableList;

import java.nio.file.Files;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressResponse;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;

/*

[ ] userdata needs to be operating system specific
[ ] put tailscale on the node
[ ] automatically get instance id and script to log in
[ ] fix provisio error before user exists. i think it's $HOME not available during cloud init
[ ] find desired subnet in vpc
[ ] need a system of built in defaults but able to override locally: directory layout, configuration, os, packages
[ ] real git credentials helper on linux instead of cheaty askpass setup
[ ] adding non-root users
[x] template/source all sensitive information from local config
[x] cleanly delete everything
[x] find default vpc
[x] find security group accurately only creating the Name tag and not name
[x] script to generate github access token
[x] script to retrieve secrets
[x] git credentials
[x] aws credentials
[x] docker configuration: config.json -> { "credsStore": "ecr-login" }

 What worked
 aws sso login and docker pull 179392497636.dkr.ecr.us-west-2.amazonaws.com/access-ui:k8s-latest
 What doesn't work
 the instance profile will only work across accounts with trust policies setup between accounts

 */

public class InstanceBuilder
{

    private final InstanceBuilderRequest request;
    private Region region = Region.US_EAST_2;

    public InstanceBuilder(InstanceBuilderRequest request)
    {
        this.request = request;
    }

    public static void main(String[] args)
    {
        InstanceBuilder instanceBuilder = new InstanceBuilder(ImmutableInstanceBuilderRequest.builder()
                .region("us-east-2")
                .account("sandbox")
                .instanceProfile("engineering-fuji-admin")
                .sshKeyPair("sre")
                .tags(Map.of("name", "jvz"))
                .build());

        //instanceBuilder.showParameters();
        instanceBuilder.build();
    }

    public String build()
    {
        String name = request.tags().get("name");

        try (Ec2Client ec2 = AwsUtils.ec2Client(request.region(), request.account())) {
            VpcResult vpcResult = new DescribeDefaultVpc(region.id()).describeVpc();

            EbsBlockDevice ebsBlockDevice = EbsBlockDevice.builder()
                    .volumeSize(request.volumeSize())
                    .deleteOnTermination(true)
                    .volumeType(request.volumeType())
                    .build();

            BlockDeviceMapping blockDeviceMapping = BlockDeviceMapping.builder()
                    .deviceName("/dev/sda1")
                    .ebs(ebsBlockDevice)
                    .build();

            IamInstanceProfileSpecification instanceProfile = IamInstanceProfileSpecification.builder()
                    .name(request.instanceProfile())
                    .build();

            Collection<String> sgs = securityGroup(ec2, name, vpcResult.vpcId());
            System.out.println(sgs);

            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                    .instanceType(InstanceType.T2_MICRO)
                    .iamInstanceProfile(instanceProfile)
                    .imageId(amiId(request.architecture()))
                    .userData(userData())
                    .securityGroupIds(sgs)
                    .subnetId(vpcResult.publiclyAccessibleSubnets().get(0).id())
                    .blockDeviceMappings(blockDeviceMapping)
                    .tagSpecifications(tags(name, INSTANCE))
                    .keyName(request.sshKeyPair())
                    .maxCount(1)
                    .minCount(1)
                    .build();

            return ec2.runInstances(runRequest).instances().get(0).instanceId();
        }
    }

    private String userData()
    {
        try {
            String userData = Files.readString(get("/Users/jason.vanzyl/js/ck8s-system/ck8s-all/ck8s-java/ck8s-aws/target/userdata.bash"));
            return Base64.getEncoder().encodeToString(userData.getBytes());
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot load userdata", e);
        }
    }

    private TagSpecification tags(String name, ResourceType resourceType)
    {
        // This seems to only be making the capitalized version
        return TagSpecification.builder()
                .tags(
                        Tag.builder()
                                .key("name")
                                .value(name)
                                .key("Name")
                                .value(name)
                                .build())
                .resourceType(resourceType)
                .build();
    }

    private Collection<String> securityGroup(Ec2Client ec2, String name, String vpcId)
    {
        // Can't look up a security group by name directly
        System.out.println("Security group!!!");
        Filter filter = Filter.builder().name("tag:Name").values(name).build();
        DescribeSecurityGroupsResponse describeSecurityGroupsResponse = ec2.describeSecurityGroups(DescribeSecurityGroupsRequest.builder()
                .filters(filter)
                .build());

        if (describeSecurityGroupsResponse.securityGroups().size() == 0) {
            System.out.format("Security group %s doesn't exist. Creating.", name);
            CreateSecurityGroupRequest securityGroupRequest = CreateSecurityGroupRequest.builder()
                    .groupName(name)
                    .description(name)
                    .vpcId(vpcId)
                    .tagSpecifications(tags(name, SECURITY_GROUP))
                    .build();

            CreateSecurityGroupResponse resp = ec2.createSecurityGroup(securityGroupRequest);

            IpRange ipRange = IpRange.builder()
                    .cidrIp("0.0.0.0/0").build();

            IpPermission ipPerm2 = IpPermission.builder()
                    .ipProtocol("tcp")
                    .toPort(22)
                    .fromPort(22)
                    .ipRanges(ipRange)
                    .build();

            AuthorizeSecurityGroupIngressRequest authRequest =
                    AuthorizeSecurityGroupIngressRequest.builder()
                            .groupId(resp.groupId())
                            .ipPermissions(ipPerm2)
                            .build();

            AuthorizeSecurityGroupIngressResponse authResponse =
                    ec2.authorizeSecurityGroupIngress(authRequest);

            return ImmutableList.of(resp.groupId());
        }

        return describeSecurityGroupsResponse.securityGroups()
                .stream()
                .map(g -> g.groupId())
                .collect(Collectors.toList());
    }

    public String amiId(String architecture)
    {
        SsmClient ssmClient = SsmClient.builder().region(region).build();

        GetParameterRequest parameterRequest = GetParameterRequest
                .builder()
                .name(format("/aws/service/canonical/ubuntu/server/22.04/stable/current/%s/hvm/ebs-gp2/ami-id", architecture))
                .build();

        GetParameterResponse response = ssmClient.getParameter(parameterRequest);

        return response.parameter().value();
    }

    public void showParameters()
    {
        SsmClient ssmClient = SsmClient.builder().region(region).build();
        GetParametersByPathRequest request = GetParametersByPathRequest
                .builder()
                .withDecryption(true)
                .recursive(true)
                .path("/aws/service/canonical/ubuntu/server/22.04/stable/current")
                .build();
        GetParametersByPathResponse r = ssmClient.getParametersByPath(request);
        r.parameters().forEach(p -> System.out.println(p.name() + " -> " + p.value()));
    }
}