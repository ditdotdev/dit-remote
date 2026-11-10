# dit-remote

Dit remote provider (Kotlin) - server-side provider for d3 CLI integration with dit-remote-server.

## Overview

The `dit-remote` provider integrates the d3 CLI with [dit-remote-server](https://github.com/ditdotdev/dit-remote-server), providing a "GitHub for d3" experience. This Kotlin provider handles server-side operations for pushing, pulling, and listing commits via HTTP APIs.

## Architecture

This is the **server-side** Kotlin provider that runs within the dit-server. It works in tandem with:
- **dit-remote-go**: Client-side Go provider for d3 CLI (URL parsing, validation)
- **dit-remote-server**: HTTP API server for centralized commit storage

## Features

- HTTP-based push/pull operations using OkHttp3
- Archive-based volume transfers (extends `ArchiveRemote`)
- Commit metadata management with tag filtering
- Bearer token authentication support
- Automatic service discovery via ServiceLoader

## Remote Configuration

```
http://localhost:8080/org/repo
https://api.example.com:443/myorg/myrepo
```

### Required Properties
- `api_base_url`: Base URL of dit-remote-server (e.g., "http://localhost:8080")
- `org`: Organization name
- `repo`: Repository name

### Optional Properties
- `port`: Port number (if not in URL)
- `api_token`: Authentication token for API access

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## Related Repositories

- [dit-remote-go](https://github.com/ditdotdev/dit-remote-go) - Go client provider
- [dit-remote-server](https://github.com/ditdotdev/dit-remote-server) - HTTP API server
- [dit](https://github.com/ditdotdev/dit) - d3 CLI

## Contributing

This project follows the Dit community best practices:

  * [Contributing](https://github.com/ditdotdev/.github/blob/master/CONTRIBUTING.md)
  * [Code of Conduct](https://github.com/ditdotdev/.github/blob/master/CODE_OF_CONDUCT.md)
  * [Community Support](https://github.com/ditdotdev/.github/blob/master/SUPPORT.md)

It is maintained by the [Dit community maintainers](https://github.com/ditdotdev/.github/blob/master/MAINTAINERS.md)

For more information on how it works, and how to build and release new versions,
see the [Development Guidelines](DEVELOPING.md).


## License

This project is licensed under the Business Source License 1.1 (BUSL-1.1).
On the Change Date (four years from the publication of each version), the
license for that version converts to the Mozilla Public License 2.0
(MPL-2.0). See [LICENSE](LICENSE) for the full terms.
