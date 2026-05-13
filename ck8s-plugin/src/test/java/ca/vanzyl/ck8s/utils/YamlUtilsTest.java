package ca.vanzyl.ck8s.utils;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class YamlUtilsTest
{

    @Test
    public void validateMapToYamlConversion()
    {
        YamlUtils utils = new YamlUtils();
        Map<String, String> map = ImmutableMap.of(
                "fe_common.cost_center", "intel_eng-dev",
                "fe_common.env_type", "dev",
                "fe_common.env", "dev",
                "fe_common.product", "intel-eks");

        assertThat(utils.nindentYaml(map, 0)).isEqualTo(
                "\n" +
                        "fe_common.cost_center: \"intel_eng-dev\"\n" +
                        "fe_common.env_type: \"dev\"\n" +
                        "fe_common.env: \"dev\"\n" +
                        "fe_common.product: \"intel-eks\"");

        assertThat(utils.nindentYaml(map, 2)).isEqualTo(
                "\n" +
                        "  fe_common.cost_center: \"intel_eng-dev\"\n" +
                        "  fe_common.env_type: \"dev\"\n" +
                        "  fe_common.env: \"dev\"\n" +
                        "  fe_common.product: \"intel-eks\"");

        assertThat(utils.indentYaml(map, 2)).isEqualTo(
                "  fe_common.cost_center: \"intel_eng-dev\"\n" +
                        "  fe_common.env_type: \"dev\"\n" +
                        "  fe_common.env: \"dev\"\n" +
                        "  fe_common.product: \"intel-eks\"");

        assertThat(utils.toCsv(map))
                .isEqualTo("fe_common.cost_center=intel_eng-dev,fe_common.env_type=dev,fe_common.env=dev,fe_common.product=intel-eks");
    }
}
