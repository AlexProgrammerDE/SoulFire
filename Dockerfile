FROM eclipse-temurin:21-jdk AS soulfire-builder

# Get soulfire data
COPY --chown=root:root . /soulfire

# Install git
RUN apt-get update && apt-get install -y git

# Build soulfire
WORKDIR /soulfire
RUN --mount=type=cache,target=/root/.gradle,sharing=locked --mount=type=cache,target=/soulfire/.gradle,sharing=locked --mount=type=cache,target=/soulfire/work,sharing=locked \
    ./gradlew :dedicated:build --stacktrace

FROM eclipse-temurin:21-jre-alpine AS soulfire-runner

# Setup groups and install dumb init
RUN addgroup --gid 1001 soulfire && \
    adduser --home /soulfire --uid 1001 -S -G soulfire soulfire && \
    apk add --update --no-progress --no-cache dumb-init libstdc++

# Copy over JAR
COPY --from=soulfire-builder --chown=soulfire:soulfire /soulfire/dedicated/build/libs/SoulFireDedicated-*.jar /soulfire/soulfire.jar

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
