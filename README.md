# ServerWrecker

<!--suppress HtmlDeprecatedAttribute -->
<p align="center">
<a href="https://ci.codemc.io/job/AlexProgrammerDE/job/ServerWrecker/"><img src="https://ci.codemc.io/job/AlexProgrammerDE/job/ServerWrecker/badge/icon" alt="Build Status"></a>
<a href="https://github.com/AlexProgrammerDE/ServerWrecker/actions/workflows/build.yml"><img src="https://github.com/AlexProgrammerDE/ServerWrecker/actions/workflows/build.yml/badge.svg?branch=main" alt="Java CI"></a>
<a href="https://github.com/AlexProgrammerDE/ServerWrecker/graphs/contributors"><img src="https://img.shields.io/github/contributors/AlexProgrammerDE/ServerWrecker.svg" alt="Contributors"></a>
<a href="https://github.com/AlexProgrammerDE/ServerWrecker/blob/main/LICENSE"><img src="https://img.shields.io/github/license/AlexProgrammerDE/ServerWrecker.svg" alt="License"></a>
<a href="https://github.com/AlexProgrammerDE/ServerWrecker/releases/latest"><img alt="GitHub all releases" src="https://img.shields.io/github/downloads/AlexProgrammerDE/ServerWrecker/total"></a>
<a href="https://github.com/AlexProgrammerDE/ServerWrecker/releases/latest"><img src="https://img.shields.io/github/release/AlexProgrammerDE/ServerWrecker.svg" alt="Current Release"></a>
</p>
<p align="center"><a href="https://discord.gg/vHgRd6YZmH"><img src="https://discordapp.com/api/guilds/739784741124833301/embed.png" alt="Discord embed"></a></p>

<p align="center">
<a href="https://github.com/AlexProgrammerDE/ServerWrecker/releases/latest"><img src="https://img.shields.io/badge/DOWNLOAD-LATEST-success?style=for-the-badge" alt="download badge"></a>
<a href="https://nightly.link/AlexProgrammerDE/ServerWrecker/workflows/build/main/ServerWrecker.zip"><img src="https://img.shields.io/badge/DOWNLOAD-DEV__BUILD-important?style=for-the-badge" alt="download2 badge"></a>
</p>

Advanced Minecraft Server-Stresser Tool. Launch bot attacks on your own servers to stress-test them and measure
performance.

## 🚀 Features

* GUI (Multiple themes) & CLI
* Configurable options for every attack like the number of bots, join delay and more
* Load and save profiles for quick access
* Support for online and offline mode servers
* Supports almost every Minecraft version (Release, April Fools Snapshots, Beta, Alpha, Classic and even Bedrock Edition)
* Use Microsoft, The Altening, EasyMC or Offline accounts
* Use HTTP/SOCKS4/SOCKS5 proxies
* Multiple modules like AutoRespawn and also AutoRegister for cracked servers
* Console command support
* A* Pathfinding (Diagonal moves, parkour, mining blocks, placing blocks)

## ✨ Nightly builds

You can download the latest development version of ServerWrecker
through [nightly.link](https://nightly.link/AlexProgrammerDE/ServerWrecker/workflows/build/main/ServerWrecker.zip).

## 🔭 Requirements

* Java 17+
* Minecraft server (Tested with Spigot/Paper/Purpur/Folia, Velocity/BungeeCord)

## 🍿 Version support

ServerWrecker supports many version of Minecraft, including releases, beta, alpha, classic (including [CPE](https://wiki.vg/Classic_Protocol_Extension)) and even Minecraft Bedrock edition,
and that includes every single subversion, not just a few specific versions.

You can find a up-to-date list of supported versions in [VERSIONS.md](./VERSIONS.md).

### How does this work?

Thanks to a built-in implementation of [ViaVersion](https://github.com/ViaVersion/ViaVersion),
[ViaLegacy](https://github.com/RaphiMC/ViaLegacy), [ViaBedrock](https://github.com/RaphiMC/ViaBedrock) and many others,
a ServerWrecker bot can connect to all these versions even without the server having ViaVersion installed.
The packet translation happens "client/bot side".
This feature takes inspiration from services like [ViAaaS](https://github.com/ViaVersion/VIAaaS)
and [ViaProxy](https://github.com/RaphiMC/ViaProxy),
which include client-side translation externally.

## 🎬 Videos

[Download & install ServerWrecker](https://youtu.be/X9hREa17yAo)

## ⌨ Commands

For a list of all available commands, look at [COMMANDS.md](./COMMANDS.md)
or run `help` in the GUI or CLI.

## 📻 Built-in plugins

### AutoRespawn

Automatically respawns bots with a random delay when they die.
Bots do not cause a lot of lags when they are dead, so this module is useful for ensuring
that the bots are always alive and performing their actions.

### AutoRegister

Automatically runs `/register` and `/login` on cracked servers.
Also supports `/captcha` for servers that have it.

### AutoJump

Automatically jumps with a random delay.
Useful for slowing the server down.
It will also make the bots send a lot of movement packets,
which is useful for testing the server's performance.

### ClientSettings & ClientBrand

Configurable client settings like chat visibility, chat colours, skin customization, and more.
Client brand can also be changed to any string; by default, it's set to "vanilla".
Useful for bypassing anti-bot plugins that check for client settings.

## 🗃 Import accounts & proxies

### Accounts

ServerWrecker supports Microsoft, The Altening and Offline accounts.

If no account is selected, ServerWrecker will generate offline accounts based on the configured name format.

Select the account type in the GUI or CLI and use one of these formats with one account per line:

#### Microsoft

```text
email:password
email2:password2
email3:password3
```

#### The Altening

```text
token@alt.com
token2@alt.com
token3@alt.com
```

#### Offline

```text
Username
Username2
Username3
```

### Proxies

ServerWrecker supports HTTP, SOCKS4 and SOCKS5 proxies.

Select the proxy type in the GUI or CLI and use one of these formats with one proxy per line:

#### HTTP

```text
ip:port <- no authentication
ip:port:username:password <- authentication
```

#### SOCKS4

```text
ip:port <- no authentication
ip:port:username <- authentication (socks4 doesn't support passwords)
```

#### SOCKS5

```text
ip:port <- no authentication
ip:port:username:password <- authentication
```

## 🧵 Demo

ServerWrecker has a built-in GUI for easy usage.
![GUI](./assets/demo.png)

## 💻 Command Line Usage

To run ServerWrecker from the command line without a GUI, enter:
`java -jar serverwrecker.jar <flags>`

> **Note:** Some features are not available in CLI mode, we recommend using the GUI.

### CLI Flags

For a list of all available CLI flags, look at [CLI_FLAGS.md](./CLI_FLAGS.md)
or run `java -jar serverwrecker.jar --help`.

## 🔧 Build from source

1. Install Java 17
2. Download Gradle
3. Download the latest version of ServerWrecker
4. Run `./gradlew build` in the ServerWrecker directory
5. Get the jar file from `build/libs`

## 👨‍💻 Developer API

ServerWrecker offers a Developer API to create your own plugins using the plugin API and mixins.
The software may regularly have breaking changes, so you'll have to stick to a verson of ServerWrecker or update your plugin regularly.

To learn more about the Dveloper API, take a look at the official [plugin example](https://github.com/AlexProgrammerDE/ServerWreckerPluginExample).

## 🌈 Community

Feel free to join our Discord community server:

[![Discord Banner](https://discord.com/api/guilds/739784741124833301/widget.png?style=banner2)](https://discord.gg/vHgRd6YZmH)

This project is in active development, so if you have any feature requests or issues, please submit them here on GitHub.
PRs are welcome, too.
:octocat:

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=AlexProgrammerDE/ServerWrecker&type=Date)](https://star-history.com/#AlexProgrammerDE/ServerWrecker&Date)

## 📜 Attribution

This project is based on [LambdaAttack](https://github.com/games647/LambdaAttack), but over the years the code has been
remade multiple times to be far more complex than the original project.
