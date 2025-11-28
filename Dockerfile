# syntax=docker/dockerfile:1

# --- Builder Stage ---
FROM ghcr.io/graalvm/native-image-community:25 AS builder

WORKDIR /app

RUN microdnf install -y tar xz && microdnf clean all

ARG UPX_VERSION=5.0.2
ADD https://github.com/upx/upx/releases/download/v${UPX_VERSION}/upx-${UPX_VERSION}-amd64_linux.tar.xz /tmp/upx.tar.xz

RUN tar -xf /tmp/upx.tar.xz -C /tmp && \
    mv /tmp/upx-${UPX_VERSION}-amd64_linux/upx /usr/bin/upx && \
    chmod +x /usr/bin/upx

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src

# nativ kompilieren mit StripDebugInfo (Basis-Bereinigung)
RUN ./mvnw -Pnative native:compile -DskipTests \
    -Dspring-boot.native-image.build-args="-H:+StripDebugInfo"

# Komprimieren mit UPX (gamechanger von ~ 170 auf unter 65 MB)
RUN upx --best --lzma /app/target/shortly

# --- Runtime Stage ---
# enthält zlib, glibc, libstdc++
FROM gcr.io/distroless/java-base-debian12:nonroot

WORKDIR /app

COPY --from=builder /app/target/shortly /app/shortly

EXPOSE 8080

ENTRYPOINT ["/app/shortly"]
