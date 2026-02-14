FROM eclipse-temurin:25-jre AS soulfire-runner

ARG VERSION

# Setup groups and install dumb init
RUN groupadd --gid 1001 soulfire && \
    useradd --home-dir /soulfire --uid 1001 --gid soulfire --create-home soulfire && \
    apt-get update && apt-get install -y --no-install-recommends dumb-init && \
    rm -rf /var/lib/apt/lists/* && \
    chmod 755 /soulfire

# Download JAR from GitHub releases
ADD --chown=soulfire:soulfire https://github.com/AlexProgrammerDE/SoulFire/releases/download/${VERSION}/SoulFireDedicated-${VERSION}.jar /soulfire/soulfire.jar
RUN chmod 644 /soulfire/soulfire.jar

# Use the soulfire's home directory as our work directory
WORKDIR /soulfire/data

# Copy over the start script
COPY --chmod=755 start.sh /soulfire/start.sh

# Switch from root to soulfire
USER soulfire

EXPOSE 38765/tcp

# Start the process using dumb-init
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/bin/bash", "/soulfire/start.sh"]
