# syntax=docker/dockerfile:1
# Builds argonaut-vector as linux/amd64 so the linux-x86_64 native library in
# ai.djl.huggingface:tokenizers:0.36.0 is resolved instead of the absent osx-x86_64 variant.

FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

# Full reactor pom set — Maven needs to parse all declared <modules> even when
# building a subset. Only argonaut-core and argonaut-vector sources are copied later.
COPY pom.xml ./
COPY argonaut-core/pom.xml argonaut-core/pom.xml
COPY argonaut-spring-ai/pom.xml argonaut-spring-ai/pom.xml
COPY argonaut-langchain4j/pom.xml argonaut-langchain4j/pom.xml
COPY argonaut-langgraph4j/pom.xml argonaut-langgraph4j/pom.xml
COPY argonaut-embabel/pom.xml argonaut-embabel/pom.xml
COPY argonaut-koog/pom.xml argonaut-koog/pom.xml
COPY argonaut-vector/pom.xml argonaut-vector/pom.xml

# Pre-resolve dependencies for the minimal build graph (argonaut-core + argonaut-vector).
# The download-maven-plugin also caches the 90 MB ONNX model under .m2/.cache, so
# subsequent builds skip the HuggingFace download entirely.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl argonaut-vector -am dependency:go-offline

COPY argonaut-core argonaut-core
COPY argonaut-vector argonaut-vector

# generate-resources triggers the ONNX model download; package produces the fat jar.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl argonaut-vector -am package -DskipTests

COPY docker/HttpHealthcheck.java docker/HttpHealthcheck.java
RUN javac -d /tmp/healthcheck docker/HttpHealthcheck.java

FROM --platform=linux/amd64 eclipse-temurin:25-jdk AS runtime

WORKDIR /app
COPY --from=build /workspace/argonaut-vector/target/argonaut-vector-0.1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /tmp/healthcheck /opt/healthcheck

EXPOSE 8086

USER 10001

# --enable-native-access=ALL-UNNAMED: required for onnxruntime System::load native call.
# --add-modules jdk.incubator.vector: required by the Integrallis HNSW adapter,
# which is always instantiated as a Spring bean regardless of the active provider.
ENTRYPOINT ["java", \
    "--enable-native-access=ALL-UNNAMED", \
    "--add-modules", "jdk.incubator.vector", \
    "-jar", "/app/app.jar"]
