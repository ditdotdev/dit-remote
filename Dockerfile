# Template Dockerfile for datadatdat projects
# Replace with project-specific base image and configuration

FROM ubuntu:24.04

# Set working directory
WORKDIR /app

# Install common dependencies
RUN apt-get update && \
    apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy project files
# COPY . .

# Expose port (if applicable)
# EXPOSE 8080

# Set entrypoint
# CMD ["your-application"]