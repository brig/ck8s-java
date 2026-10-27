ARG MAVEN_VERSION
FROM ghcr.io/concord-workflow/ck8s/ck8s-common-base-image:latest

COPY --chown=concord:concord target/dist/ /opt/concord/server/
COPY --chown=concord:concord --chmod=755 target/dist/start.sh /opt/concord/server/start.sh

ARG MAVEN_VERSION
ADD --chown=concord:concord target/ck8s-server-${MAVEN_VERSION} /opt/concord/server/ext

ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini
ENTRYPOINT ["/tini", "--"]

EXPOSE 8001

USER concord
ENV BASE_RESOURCE_PATH /opt/concord/console/
ENV CONCORD_JAVA_OPTS "-XX:+UseContainerSupport"
CMD ["bash", "/opt/concord/server/start.sh"]
