# ServerWrecker

Advanced Minecraft Server-Stresser Tool

## Nightly builds

You can download the latest development version of ServerWrecker through [nightly.link](https://nightly.link/AlexProgrammerDE/ServerWrecker/workflows/build/main/ServerWrecker.zip).

## Features

* Graphical user interface
* Command line interface
* Configurable amount and join delay
* Configurable target
* Configurable name format or name list
* LogPanel to see errors directly
* Tested with Spigot, Paper, Purpur, Folia, Velocity/Bungeecord
* Disconnects gracefully after the end
* Automatically registers for cracked servers
* Supports SOCKS4 and 5 proxies

## Version support

ServerWrecker currently supports every single Minecraft version up to 1.7.__0__. This includes any single subversion, not just a few specific versions. How? Thanks to a built-in implementation of ViaVersion and ViaLegacy a ServerWrecker bot can connect to all these versions even without the server having ViaVersion installed. The packet translation happens "client/bot side".
This feature takes inspiration from services like ViAaaS and ViaProxy, which include client-side translation externally. ServerWrecker includes a similar implementation as these proxies and is comparable to ViaFabric.
This implementation is currently in-progress and the potential is huge. Currently the bot can connect to any server up to 1.7.__0__, but in theory ViaLegacy can add support for all classic, alpha, beta and all early releases. Support for these versions is currently not working due to protocol differences.

## Requirements

* Java 17
* Minecraft 1.7.10+ server

## Building

1. Download Gradle
2. Download the latest version of ServerWrecker
3. Run `./gradlew build` in the serverwrecker directory
4. Get the jar files from `build/libs`

## Community

Feel free to join our discord community server:

[![Discord Banner](https://discord.com/api/guilds/739784741124833301/widget.png?style=banner2)](https://discord.gg/CDrcxzH)

## Images

![GUI](./images/img.png)

## Command Line Usage

To run ServerWrecker from the command line without a GUI, enter  
`java -jar serverwrecker.jar <options>`.

