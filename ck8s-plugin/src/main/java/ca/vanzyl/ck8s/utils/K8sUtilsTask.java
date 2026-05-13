package ca.vanzyl.ck8s.utils;

import com.walmartlabs.concord.runtime.v2.sdk.Task;

import javax.inject.Named;
import java.io.Serializable;

@Named("k8sUtils")
public class K8sUtilsTask implements Task {

    public static ImageDefinition parseImage(String image) {
        ImageDefinition imageDefinition = new ImageDefinition();

        String[] registryAndRest = image.split("/", 2);
        if (registryAndRest.length == 2 && registryAndRest[0].contains(".")) {
            imageDefinition.setRegistry(registryAndRest[0]);
            image = registryAndRest[1];
        }

        String[] repoAndTag = image.split(":");
        imageDefinition.setRepository(repoAndTag[0]);
        if (repoAndTag.length > 1) {
            imageDefinition.setTag(repoAndTag[1]);
        } else {
            imageDefinition.setTag("latest");
        }
        return imageDefinition;
    }

    public static class ImageDefinition implements Serializable {

        private String registry;
        private String repository;
        private String tag;

        // Getters and Setters
        public String getRegistry() {
            return registry;
        }

        public void setRegistry(String registry) {
            this.registry = registry;
        }

        public String getRepository() {
            return repository;
        }

        public void setRepository(String repository) {
            this.repository = repository;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return "ImageDefinition{" +
                   "registry='" + registry + '\'' +
                   ", repository='" + repository + '\'' +
                   ", tag='" + tag + '\'' +
                   '}';
        }
    }
}
