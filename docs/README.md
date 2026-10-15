# UnionClef documentation

UnionClef is a chat- and GUI-driven automation bot for **Minecraft 26.3** (Fabric, Java 25).
Start with the [project README](../README.md) for installation and usage.

## Guides

- [DEVELOP.md](DEVELOP.md) — build the project, run a dev client, debug
- [RELEASE.md](RELEASE.md) — versioning and how a release is published

## Planning

- [../TODOS.md](../TODOS.md) — what is deliberately not done yet: item-coverage gaps, the
  verification pass, chain work, and the things decided against

## Features

- [features/MCP_SERVER.md](features/MCP_SERVER.md) — the LAN control surface (Model Context Protocol)
- [features/AGENT_PY4J_LEVERS.md](features/AGENT_PY4J_LEVERS.md) — the catalog of levers an external
  agent drives the bot with
- [features/ITEM_BLOCK_COVERAGE.md](features/ITEM_BLOCK_COVERAGE.md) — how much of the 26.3 registry
  UnionClef can reach, and how to extend it

## Release notes

Newest first. One file per version, named after `mod_version`.

- [0.97.0](releases/0.97.0.md) — Baritone as the movement engine, every task routed, camera/cost
  fixes, generic craft chains, command cleanup, GPL-3.0
- [0.96.1](releases/0.96.1.md) — mining and water fixes
- [0.96.0](releases/0.96.0.md) — the Minecraft 26.3 port

## Archive

- [archive/releases/](archive/releases/) — release notes inherited from the upstream project, from
  before this 26.3 native port. Kept for history; they are not this project's versions.
