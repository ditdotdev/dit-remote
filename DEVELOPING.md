# Project Development

For general information about contributing changes, see the
[Contributor Guidelines](https://github.com/datadatdat/.github/blob/master/CONTRIBUTING.md).

## How it Works

Describe the internal mechanisms necessary for developers to understand how
to get started making changes.

## Building

For Docker-based projects:
```bash
docker build -t datadatdat/REPOSITORY_NAME:latest .
```

For other projects, describe the specific build process.

## Testing

Describe how to test the project locally:
```bash
# Example for Docker projects
docker run --rm datadatdat/REPOSITORY_NAME:latest
```

## Releasing

This repository uses automated releases via GitHub Actions:

### Creating a Release

1. **Create and push a version tag:**
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

2. **Automated workflow triggers:**
   - Builds the project (Docker image for containerized projects)
   - Publishes to appropriate registry (Docker Hub, etc.)
   - Creates GitHub release with auto-generated notes

### Docker Projects

For Docker-based projects, releases automatically publish to:
- `datadatdat/REPOSITORY_NAME:v1.0.1` (version-specific tag)
- `datadatdat/REPOSITORY_NAME:latest` (latest tag)

### Release Notes

The draft release workflow automatically updates release notes on each push to master. Release notes are categorized by:
- 🚀 Features
- 🐛 Bug Fixes  
- 🧰 Maintenance
- 🐳 Docker changes

Use appropriate labels on PRs to ensure proper categorization.
