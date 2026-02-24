# =============================================================================
# Dockerfile for Broadleaf Search Microservice
# =============================================================================
# Multi-stage build:
#   Stage 1: Build the Spring Boot fat JAR using Maven
#   Stage 2: Run the application using a minimal JRE base image
# =============================================================================

# --- Stage 1: Build ---
FROM maven:3.8-openjdk-8-slim AS build

WORKDIR /app

# Copy pom.xml first and download dependencies (leverages Docker layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the fat JAR
COPY src ./src
RUN mvn package -DskipTests -B

# --- Stage 2: Run ---
FROM openjdk:8-jre-slim

LABEL maintainer="Broadleaf Commerce"
LABEL description="Search & Indexing Microservice extracted from BroadleafCommerce monolith"

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the HTTP port (configurable via SERVER_PORT env var, default 8180)
EXPOSE 8180

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8180/actuator/health || exit 1

# Accept all configuration via environment variables
# Solr connection details:
#   SOLR_MODE          - "embedded", "standalone", or "cloud" (default: standalone)
#   SOLR_URL           - Standalone Solr URL (default: http://localhost:8983/solr)
#   SOLR_PRIMARY_NAME  - Primary core/collection name (default: primary)
#   SOLR_REINDEX_NAME  - Reindex core/collection name (default: reindex)
#
# SolrCloud configuration:
#   SOLR_CLOUD_ZK_HOST                   - ZooKeeper connection string
#   SOLR_CLOUD_CONFIG_NAME               - Solr config name in ZooKeeper (default: blc)
#   SOLR_CLOUD_DEFAULT_NUM_SHARDS        - Number of shards (default: 2)
#   SOLR_CLOUD_DEFAULT_REPLICATION_FACTOR - Replication factor (default: 1)
#
# Catalog database (read-only):
#   CATALOG_DB_URL      - JDBC URL for the monolith's catalog database
#   CATALOG_DB_USERNAME - Database username
#   CATALOG_DB_PASSWORD - Database password
#   CATALOG_DB_DRIVER   - JDBC driver class name
#
# Indexing configuration:
#   SOLR_INDEX_USE_SKU    - Use SKU-based indexing (default: false)
#   SOLR_INDEX_PAGE_SIZE  - Batch page size for indexing (default: 100)
#   SOLR_INDEX_COMMIT     - Commit after indexing (default: true)

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
