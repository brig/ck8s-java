package ca.vanzyl.concord.k8s;

import static ca.vanzyl.concord.k8s.VpcUtils.vpcSubnet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class VpcUtilsTest
{

    @org.junit.Test
    public void testFirstSubnet()
    {
        assertEquals(
                "10.23.0.0/22",
                vpcSubnet("10.23.0.0/16", 6, 0));
    }

    @org.junit.Test
    public void testSecondSubnet()
    {
        assertEquals(
                "10.23.4.0/22",
                vpcSubnet("10.23.0.0/16", 6, 1));
    }

    @org.junit.Test
    public void testLastSubnet()
    {
        assertEquals(
                "10.23.252.0/22",
                vpcSubnet("10.23.0.0/16", 6, 63));
    }

    @org.junit.Test
    public void testSubnetBitsTooSmall()
    {
        assertThrows(IllegalArgumentException.class, () -> vpcSubnet("10.23.0.0/22", 0, 1));
    }

    @org.junit.Test
    public void testSubnetNumberTooSmall()
    {
        assertThrows(IllegalArgumentException.class, () -> vpcSubnet("10.23.0.0/22", 4, -1));
    }

    @org.junit.Test
    public void testSubnetBitsTooLarge()
    {
        assertThrows(IllegalArgumentException.class, () -> vpcSubnet("10.23.0.0/22", 10, 0));
    }

    @org.junit.Test
    public void testNoCidrPrefix()
    {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> vpcSubnet("10.23.0.0", 4, 0));
    }

    @org.junit.Test
    public void testSubnetNumberTooLarge()
    {
        assertThrows(IllegalArgumentException.class, () -> vpcSubnet("10.23.0.0/22", 4, 16));
    }

    @org.junit.Test
    public void testIncorrectCidr()
    {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> vpcSubnet("this is not and address", 4, 1));
    }
}
