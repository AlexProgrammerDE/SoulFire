FROM azul/zulu-openjdk-alpine:25.0.2-jre-headless AS soulfire-runner

ARG VERSION

# Setup groups and install dumb init
RUN addgroup --gid 1001 soulfire && \
    adduser --home /soulfire --uid 1001 -S -G soulfire soulfire && \
    apk add --update --no-progress --no-cache dumb-init libstdc++

# Download JAR from GitHub releases
ADD --chown=soulfire:soulfire https://github.com/AlexProgrammerDE/SoulFire/releases/download/${VERSION}/SoulFireDedicated-${VERSION}.jar /soulfire/soulfire.jar

# Use the soulfire's home directory as our work directory
WORKDIR /soulfire/data

# Copy over the start script
COPY start.sh /soulfire/start.sh

# Make executable
RUN chmod +x /soulfire/start.sh

# Switch from root to soulfire
USER soulfire

EXPOSE 38765/tcp

# Start the process using dumb-init
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/bin/sh", "/soulfire/start.sh"]
