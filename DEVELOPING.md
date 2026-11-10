# Project Development

For general information about contributing changes, see the
[Contributor Guidelines](https://github.com/ditdotdev/.github/blob/master/CONTRIBUTING.md).

## How it Works

The provider uses the Dit `remote-sdk` to provide interfaces for
`dit-server` to use. It implements the server side of the `dit` remote: push,
pull and commit listing against the dit-remote-server HTTP API. The resulting
server jar is incorporated into the dit-server build and discovered through
`ServiceLoader`.

## Building

Run `./gradlew build`.

## Testing

Tests are run as part of `./gradlew build`. Tests can also be explicitly run
via `./gradlew test`.

## Releasing

Releases are triggered by pushing tags to the master branch.
