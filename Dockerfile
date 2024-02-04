FROM eclipse-temurin:21 AS soulfire-builder

# Get soulfire data
COPY --chown=root:root . /soulfire

# Build soulfire
WORKDIR /soulfire
RUN --mount=type=cache,target=/root/.gradle,sharing=locked --mount=type=cache,target=/soulfire/.gradle,sharing=locked --mount=type=cache,target=/soulfire/work,sharing=locked \
    ./gradlew build --stacktrace

FROM eclipse-temurin:21-jdk-alpine AS jre-no-javac-builder

# Install necessery dependencies
RUN apk add --no-progress --no-cache binutils tzdata

ARG JAVA_MODULES="java.base,java.compiler,java.instrument,java.logging,java.management,java.net.http,java.sql,java.desktop,java.security.sasl,java.naming,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.incubator.vector,jdk.jfr,jdk.zipfs,jdk.security.auth,jdk.unsupported,jdk.management"

# Create a custom Java runtime for soulfire Server
RUN jlink \
        --add-modules $JAVA_MODULES \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=2 \
        --output /soulfire/java

FROM alpine:latest AS soulfire-runner

# Setup groups and install dumb init
RUN addgroup --gid 1001 soulfire && \
    adduser --home /soulfire --uid 1001 -S -G soulfire soulfire && \
    apk add --update --no-progress --no-cache dumb-init libstdc++

# Setting up Java
ENV JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:$PATH"

# Copy over JRE
COPY --from=jre-no-javac-builder --chown=soulfire:soulfire /soulfire/java $JAVA_HOME
COPY --from=soulfire-builder --chown=soulfire:soulfire /soulfire/build/libs/SoulFire-*.jar /soulfire/soulfire.jar

# Use the soulfire's home directory as our work directory
WORKDIR /soulfire

# Switch from root to soulfire
USER soulfire

# Start the process using dumb-init
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/soulfire/java/bin/java", "-jar", "/soulfire/soulfire.jar"]
