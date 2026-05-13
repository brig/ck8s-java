ARG MAVEN_VERSION
ARG BASE_IMAGE_VERSION=latest

FROM ghcr.io/concord-workflow/ck8s/ck8s-agent-base-image:${BASE_IMAGE_VERSION}

USER root
COPY --chown=concord:concord target/deps/repository /home/concord/.m2/repository
ADD --chown=concord:concord target/agent.tar.gz /opt/concord/agent/
RUN chown -R concord:concord /opt/concord
RUN rm -rf /opt/concord/agent/runner/runner-v1.jar
COPY --chown=concord:concord runner/runner-v2.jar /opt/concord/agent/runner/runner-v2.jar
COPY --chown=concord:concord target/default-dependencies /opt/concord/conf/agent/

USER concord
CMD ["bash", "/opt/concord/agent/start.sh"]
