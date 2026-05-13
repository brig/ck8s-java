package ca.vanzyl.concord.k8s;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.InetAddresses.coerceToInteger;
import static com.google.common.net.InetAddresses.forString;
import static com.google.common.net.InetAddresses.fromInteger;
import static com.google.common.net.InetAddresses.toAddrString;
import static java.lang.String.format;

public class VpcUtils
{

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
        checkArgument(subnetNumber.intValue() < (1 << subnetBitsInt),
                "subnetNumber larger than number of subnets for given subnetBits");

        subnetBitsInt += vpcBits;
        int address = coerceToInteger(forString(vpcAddress));
        int subnetAddress = address | (subnetNumber.intValue() << (32 - subnetBitsInt));
        return format("%s/%d",
                toAddrString(fromInteger(subnetAddress)),
                subnetBitsInt);
    }
}
