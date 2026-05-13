package ca.vanzyl.ck8s.cloudformation;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

public final class Serializer {

    public static String serialize(CloudFormation cloudFormation) {
        DumpSettings settings = DumpSettings.builder()
                .setDefaultFlowStyle(org.snakeyaml.engine.v2.common.FlowStyle.BLOCK)
                .setIndent(2)
                .build();

        var yaml = new Dump(settings, new CloudFormationRepresenter(settings));

        return yaml.dumpToString(cloudFormation);
    }

    public static class CloudFormationRepresenter extends StandardRepresenter {

        public CloudFormationRepresenter(DumpSettings settings) {
            super(settings);
            this.parentClassRepresenters.put(Set.class, data -> {
                @SuppressWarnings("unchecked")
                var set = (Collection<Object>) data;
                return representSequence(Tag.SEQ, new ArrayList<>(set), settings.getDefaultFlowStyle());
            });
            this.representers.put(Resource.class, data -> {
                var resource = (Resource)data;
                return switch (resource.type()) {
                    case PLAIN -> super.representData(resource.value());
                    case SUB -> super.representScalar(new Tag("!Sub"), normalizeParameters(resource.value()), ScalarStyle.DOUBLE_QUOTED);
                };
            }

            );
            this.representers.put(Statement.class, data -> {
                var s = (Statement) data;
                var map = new LinkedHashMap<>();
                map.put("Effect", s.effect());
                map.put("Action", s.actions().stream().sorted().toList());
                map.put("Resource", s.resources().stream().sorted().toList());

                return super.representData(map);
            });
            this.representers.put(CloudFormation.class, data -> {
                var cf = (CloudFormation) data;
                var map = new LinkedHashMap<>();
                map.put("AWSTemplateFormatVersion", "2010-09-09");
//                map.put("Resources", cf.getResources());

                return super.representData(cf.statements());
            });
        }
    }

    private static String normalizeParameters(String str) {
        return str.replace("#{", "${");
    }

    private Serializer() {
    }
}
