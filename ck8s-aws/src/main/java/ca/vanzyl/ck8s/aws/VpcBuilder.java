package ca.vanzyl.ck8s.aws;

import ca.vanzyl.concord.k8s.ImmutablesYamlMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.InetAddresses.*;
import static java.lang.String.format;

// https://docs.aws.amazon.com/eks/latest/userguide/network_reqs.html

public class VpcBuilder
{

    private final VpcRequest vpcRequest;

    private final Ec2Client awsClient;

    public VpcBuilder(VpcRequest vpcRequest)
    {
        this.vpcRequest = vpcRequest;
        this.awsClient = Ec2Client.builder()
                .region(Region.of(vpcRequest.region()))
                .build();
    }

    /**
     * Calculate subnet CIDR within VPC of given CIDR.
     *
     * @param vpcCidr CIDR of VPC, e.g. 10.1.0.0/16
     * @param subnetBits additional bits to extend subnet prefix over vpc prefix
     * @param subnetNumber index of subsequent subnet to create, starting from zero
     * @return CIDR of subnet
     */
    public static String vpcSubnet(String vpcCidr, Number subnetBits, Number subnetNumber)
    {
        String[] address_mask = vpcCidr.split("/");
        String vpcAddress = address_mask[0];
        int vpcBits = Integer.parseInt(address_mask[1]);

        int subnetBitsInt = subnetBits.intValue();
        checkArgument(subnetBitsInt > 0, "subnetBits must be positive");
        checkArgument(subnetNumber.intValue() >= 0, "subnetNumber cannot be negative");
        checkArgument(subnetBitsInt + vpcBits < 30, "resulting prefix of subnet must be less than 30 bits");
        checkArgument(subnetNumber.intValue() < (1 << subnetBitsInt), "subnetNumber larger than number of subnets for given subnetBits");

        subnetBitsInt += vpcBits;
        int address = coerceToInteger(forString(vpcAddress));
        int subnetAddress = address | (subnetNumber.intValue() << (32 - subnetBitsInt));
        return format("%s/%d", toAddrString(fromInteger(subnetAddress)), subnetBitsInt);
    }

    // - 10.0.0.0    - 10.255.255.255
    // - 172.16.0.0  - 172.31.255.255
    // - 192.168.0.0 - 192.168.255.255

    public static void main(String[] args)
            throws Exception
    {
        VpcBuilder vpcBuilder = new VpcBuilder(ImmutableVpcRequest
                .builder()
                .dryRun(true)
                .region("us-east-2")
                .availabilityZoneIds(List.of("us-east-2a", "us-east-2b"))
                .tags(Map.of("Name", "foobar"))
                .cidrBlock("172.16.x.0/16")
                .build()
        );

        VpcResult result = vpcBuilder.build();
        System.out.println(new ImmutablesYamlMapper().pretty(result));
    }

    public VpcResult build()
    {
        ImmutableVpcResult.Builder vpcResultBuilder = ImmutableVpcResult.builder()
                .region(vpcRequest.region())
                .tags(this.vpcRequest.tags());

        String availableCidrBlock = findAvailableCidrBlock(vpcRequest.cidrBlock());
        vpcResultBuilder.cidrBlock(availableCidrBlock);

        // Vpc
        CreateVpcRequest createVpcRequest = CreateVpcRequest.builder()
                .cidrBlock(availableCidrBlock)
                .tagSpecifications(tags(ResourceType.VPC.toString()))
                .build();
        if (!vpcRequest.dryRun()) {
            CreateVpcResponse createVpcResponse = awsClient.createVpc(createVpcRequest);
            vpcResultBuilder.vpcId(createVpcResponse.vpc().vpcId());
        }
        else {
            vpcResultBuilder.vpcId("vpcId");
        }

        List<String> availabilityZoneIds;
        if (vpcRequest.availabilityZoneIds() != null) {
            availabilityZoneIds = vpcRequest.availabilityZoneIds();
        }
        else {
            // A request for a specific number of availability zones has been made, so we need to select
            // the availability zones to use. We can be more sophisticated later but for now we'll just
            // select the first two: a + b.
            availabilityZoneIds = List.of(vpcRequest.region() + "a", vpcRequest.region() + "b");
        }

        NetworkCalculator networkCalculator = new NetworkCalculator(vpcRequest, vpcResultBuilder);
        for (Subnet subnet : networkCalculator.subnets()) {
            CreateSubnetRequest subnetRequest = CreateSubnetRequest
                    .builder()
                    .vpcId(vpcResultBuilder.build().vpcId())
                    .availabilityZoneId(subnet.availabilityZoneId())
                    .cidrBlock(subnet.cidrBlock())
                    .tagSpecifications(tags(ResourceType.SUBNET.toString()))
                    .build();
            vpcResultBuilder.addSubnets(subnet);
            if (!vpcRequest.dryRun()) {
                CreateSubnetResponse subnetResponse = awsClient.createSubnet(subnetRequest);
            }
        }

    /*

    // Get AZs to select from or take from configuration


    // Subnets

    // Nat Gateway: allows instances with no public IPs to access the internet.
    CreateNatGatewayRequest natGatewayRequest = CreateNatGatewayRequest
        .builder()
        .subnetId("")
        .build();

    // Internet Gateway: allows instances with public IPs to access the internet.
    CreateInternetGatewayRequest internetGatewayRequest = CreateInternetGatewayRequest
        .builder()
        .build();

    // Routes
    CreateRouteRequest routeRequest = CreateRouteRequest
        .builder()
        .build();

    // Transit Gateways
    CreateTransitGatewayRequest transitGatewayRequest = CreateTransitGatewayRequest
        .builder()
        .build();

    // S3 Endpoint
    CreateVpcEndpointRequest vpcEndpointRequest = CreateVpcEndpointRequest
        .builder()
        .build();

     */

        return vpcResultBuilder.build();
    }

    public String findAvailableCidrBlock()
    {
        return findAvailableCidrBlock("176.16.x.0/16");
    }

    public String findAvailableCidrBlock(String cidrBlockPattern)
    {
        // "176.16.x.0/16" --> "176.16.%s.0/16"
        String cidrBlockFormat = cidrBlockPattern.replace("x", "%s");
        DescribeVpcsRequest request = DescribeVpcsRequest.builder().build();
        DescribeVpcsResponse response = awsClient.describeVpcs(request);
        List<String> vpcIps = response.vpcs().stream()
                .map(vpc -> ipOfCidrBlockFor(vpc.cidrBlock()))
                .collect(Collectors.toList());
        for (int i = 1; i < 100; i++) {
            // "176.16.%s.0/16" --> "176.16.%s.0"
            String cidrIpFormat = ipOfCidrBlockFor(cidrBlockFormat);
            // "176.16.%s.0" --> 176.16.0.0
            String freeCidrIp = format(cidrIpFormat, i);
            if (!vpcIps.contains(freeCidrIp)) {
                return format(cidrBlockFormat, i);
            }
        }
        return null;
    }

    private String ipOfCidrBlockFor(String cidrBlock)
    {
        return cidrBlock.substring(0, cidrBlock.indexOf("/"));
    }

  /*

  https://github.com/belavina/VLSMCalculator

  vpc-0e2f8c39b2233e20e: 10.254.0.0/16

  subnet-035dacf94282c62c6: 10.254.32.0/20
  subnet-03cb5ee4fa5d88ab2: 10.254.96.0/20
  subnet-088381501e60742b9: 10.254.0.0/19
  subnet-0aedbd0a828115474: 10.254.128.0/19
  subnet-048ec97e6b530a0ec: 10.254.64.0/19
  subnet-001d70636529485b9: 10.254.160.0/20

   */

    protected TagSpecification tags(String resourceType)
    {
        return TagSpecification.builder()
                .resourceType(resourceType)
                .tags(vpcRequest.tags().entrySet().stream()
                        .map(e -> Tag.builder()
                                .key(e.getKey())
                                .value(e.getValue())
                                .build())
                        .collect(Collectors.toList())).build();
    }
}
