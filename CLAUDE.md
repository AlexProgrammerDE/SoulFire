# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SoulFire is an advanced Minecraft bot automation tool written in Java 25. It enables deployment of automated bots for server testing, automation, and development. This repository contains the CLI and server implementation; the GUI client is in a separate repository.

## Build Commands

```bash
./gradlew build                    # Full compile, remap, test, and documentation
./gradlew test                     # Run JUnit 5 test suite
./gradlew spotlessCheck            # Check code formatting
./gradlew spotlessApply            # Apply code formatting (run before committing)
./gradlew :mod:remapJar            # Build Fabric-ready mod artifact
./gradlew :client-launcher:runSFCLI    # Run CLI client
```

Output jars are in `client-launcher/build/libs` (CLI) and `dedicated-launcher/build/libs` (server).

## Module Structure

- **mod/**: Core bot logic, Fabric mixins, plugins, scripting system, pathfinding
- **shared/**: Bootstrap hooks and early startup utilities
- **proto/**: gRPC/Protobuf service definitions (13 proto files)
- **launcher/**: Fabric launcher and game loading
- **client-launcher/**: CLI entry point (SoulFireCLI jar)
- **dedicated-launcher/**: Dedicated server entry point
- **build-data/**: Build constants and metadata

## Architecture

**Core Components:**
- `SoulFireServer`: Main singleton managing server lifecycle and instances
- `InstanceManager`: Manages multiple bot sessions per user
- `BotConnection`/`BotControlAPI`: Individual bot protocol handling and control
- `InternalPlugin`: Base class for all plugins (event-driven via LambdaEvents)

**Scripting System** (in `mod/src/main/java/com/soulfiremc/server/script/`):
- Node-based reactive engine using Project Reactor
- `ScriptGraph` is a DAG of `AbstractScriptNode` instances with typed ports
- Pause-based execution model (not activation-based)

**Settings & Persistence:**
- Multi-level settings (server-wide and per-instance)
- Hibernate ORM with SQLite (local) or MariaDB
- gRPC for client-server communication

## Coding Style

**Formatting (enforced by Spotless and .editorconfig):**
- 2-space indentation, LF line endings, UTF-8
- AGPL license header from `file_header.txt`

**Java Style:**
- Always use `var` for local variables: `var map = new HashMap<String, String>()`
- Use `_` for unused lambda parameters
- Use `///` Markdown Javadocs (not `/** */`)
- Prefer `List.of()`, `Map.of()`, `Set.of()` for immutable collections
- Use `Optional` to avoid nulls, `StringBuilder` for loop concatenation

**Imports:**
- Sorted in groups: `""`, `javax|java`, then `#` (static imports)
- No wildcard imports

**Naming:**
- Packages: `com.soulfiremc.*`
- Fields: `camelCase`
- Constants: `UPPER_SNAKE_CASE`

## Testing

- Tests go in `mod/src/test/java` mirroring production package structure
- JUnit 5 with `@Test` and `@ParameterizedTest`
- Name classes `<Feature>Test` or `<Component>IT`
- Run `./gradlew test` locally before submitting PRs

## Commit Guidelines

Follow Conventional Commits: `fix:`, `feat:`, `ci:`, `refactor:`, `style:`, `docs:`, etc.
- Keep subjects under 72 characters
- Reference issues in the body
- Add `BREAKING CHANGE:` notes when behavior shifts

## Developer Resources

**Protocol Sniffing** (for learning packet structure):
- pakkit: https://github.com/Heath123/pakkit
- SniffCraft: https://github.com/adepierre/SniffCraft

**MC Data Generation:**
Run `runServer` in the `data-generator` module to generate Minecraft data files to `data-generator/run/minecraft-data`.

**IntelliJ Setup:**
Import inspection profile from `config/intellij_inspections.xml`.
