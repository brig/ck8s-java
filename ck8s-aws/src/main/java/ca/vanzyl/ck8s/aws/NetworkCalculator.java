package ca.vanzyl.ck8s.aws;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;

// https://erikberg.com/notes/networks.html
// http://www.faqs.org/rfcs/rfc1878.html
// https://www.site24x7.com/tools/ipv4-subnetcalculator.html

// percentage of IPs to allocate
// IPs to allocate per subnet
// percentage to allocate to public subnets
// percentage to allocate to private subnets

// 10.254.  0.0/19 --> 10.254. 31.255/19
// 10.254. 32.0/19 --> 10.254. 63.255/19
// 10.254. 64.0/19 --> 10.254. 95.255/19
// 10.254. 96.0/19 --> 10.254.127.255/19
// 10.254.128.0/19 --> 10.254.159.255/19
// 10.254.160.0/19 --> 10.254.191.255/19

public class NetworkCalculator
{
    private final String cidrBlock;
    private final int numberOfSubnets;
    private final List<Subnet> subnets;

    public NetworkCalculator(VpcRequest vpcRequest, ImmutableVpcResult.Builder vpcResultBuilder)
    {
        this.cidrBlock = vpcResultBuilder.build().cidrBlock();
        this.numberOfSubnets = vpcRequest.availabilityZones() * vpcRequest.subnetsPerAvailabilityZone();
        this.subnets = new ArrayList<>();
        calc();
    }

    public long numberOfHostsAvailable(int cidr)
    {
        // 2^(32-cidr) - 2
        return (long) Math.pow(2, (32 - cidr));
    }

    public void calc()
    {
        String[] arr = cidrBlock.split("\\/");
        //
        // String IP into 4 octets: "10.254.0.0" --> [10, 254, 0, 0]
        //
        int[] octets = stream(arr[0].split("\\.")).mapToInt(Integer::parseInt).toArray();
        int networkBits = parseInt(arr[1]);
        //
        // Networks formula for subnetting: This formula is 2^x, where x is the number of 1s added to the subnet
        // mask from the previous subnet mask when converted to binary.
        //
        int mask = 0;
        for (int i = 1; i <= 32; i++) {
            if (Math.pow(2, i) >= numberOfSubnets) {
                mask = i;
                break;
            }
        }
        mask += networkBits;

        int ipsPerSubnet = (int) Math.pow(2, 32 - mask);
        System.out.println("ipsPerSubnet = " + ipsPerSubnet);
        int[] toAdd = new int[2];
        if (ipsPerSubnet > 256) {
            toAdd[0] = (ipsPerSubnet - 1) / 256;
            System.out.println("toAdd[0] = " + toAdd[0]);
            toAdd[1] = (ipsPerSubnet - 1) % 256;
            System.out.println("toAdd[1] = " + toAdd[1]);
        }

        System.out.println("No of addresses per subnet " + ipsPerSubnet);
        System.out.println("The subnets are:");
        for (int i = 0; i < numberOfSubnets; i++) {
            System.out.print(octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3] + "/" + mask);
            System.out.print(" --> ");
            octets[3] += toAdd[1];
            octets[2] += toAdd[0];
            Subnet subnet = ImmutableSubnet.builder()
                    .cidrBlock(octets[0] + "." + octets[1] + "." + octets[2] + "." + octets[3] + "/" + mask)
                    .availabilityZoneId("needs-settings")
                    .build();
            subnets.add(subnet);
            System.out.print(subnet);
            System.out.println();
            if (octets[3] == 255) {
                octets[2] += 1;
                octets[3] = 0;
            }
            else {
                octets[3] += 1;
            }
        }

        System.out.println("Total subnets are " + numberOfSubnets);
        System.out.println("Total IPs used     : " + ipsPerSubnet * numberOfSubnets);
        System.out.println("Total IPs available: " + numberOfHostsAvailable(networkBits));
    }

    public List<Subnet> subnets()
    {
        return subnets;
    }
}