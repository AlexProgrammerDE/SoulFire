<img align="right" src="https://github.com/AlexProgrammerDE/SoulFire/blob/main/mod/src/main/resources/icons/icon.png?raw=true" height="150" width="150">

[![discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-singular_vector.svg)](https://discord.gg/vHgRd6YZmH) [![kofi](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg)](https://ko-fi.com/alexprogrammerde)

# SoulFire

Advanced Minecraft Server-Stresser Tool. Launch bot attacks on your own servers to stress-test them and measure
performance.

This repository only contains the CLI and server implementation. The official GUI client is in [another repository](https://github.com/AlexProgrammerDE/SoulFireClient).

---

> [!WARNING]
> This is a very powerful tool that shall only be used to attack your own servers. Ensure your own DDoS protection
> provider, and your host are okay with stress-tests. If you use this software to attack others, you make yourself (or
> your
> legal guardians) criminally liable; the developers of SoulFire are not responsible for your own actions.

---

## 🚀 Features

* GUI (Multiple themes) & CLI
* Configurable options for every attack like the number of bots, join delay and more
* Load and save profiles for quick access
* Support for online and offline mode servers
* Supports [almost every Minecraft version](#-version-support)
* Use `Microsoft` (Credentials & Device Code supported) and `Offline` accounts for both Java Edition and Bedrock Edition
* Use `HTTP`, `SOCKS4` and `SOCKS5` proxies
* Multiple [built-in plugins](#-plugins) like `AutoRespawn`, `AutoJump`, `ClientSettings` and more
* Console command support
* A* Pathfinding (Diagonal moves, parkour, mining blocks, placing blocks)

## 🖥 Installation

> [!TIP]
> Want to check out how SoulFire looks before installing it? Take a look at the official [demo page](https://demo.soulfiremc.com).

For installing SoulFire, please refer to the [installation guide](https://soulfiremc.com/docs/installation).

## 🍿 Version support

You can find an up-to-date list of supported versions in
the [documentation](https://soulfiremc.com/docs/usage/versions).

## ⌨ Commands

For a list of all available commands, look at the [documentation](https://soulfiremc.com/docs/usage/commands)
or run `help` in the GUI or CLI.

## 📻 Plugins

You can read about the SoulFire plugins in the [documentation](https://soulfiremc.com/docs/usage/plugins).

## 🗃 Import accounts and proxies

You can read about how to import accounts in the [account documentation](https://soulfiremc.com/docs/usage/accounts) and
how to import proxies in the [proxy documentation](https://soulfiremc.com/docs/usage/proxies).

## 🤿 Looking for proxies?

There are many websites that offer free proxies, but be careful as many of them are not reliable or secure.

Our recommended proxy provider is [Thordata](https://affiliate.thordata.com/soulfire), who sponsors SoulFire monthly.
They offer premium residential proxies with excellent speeds - use code **THORDATA** for 20% off your first purchase.

For a full list of recommended providers, check out our [Get Proxies](/get-proxies) page.

## 💻 Command Line Usage

If you want to use the CLI of SoulFire, please refer to the [CLI Mode Guide](https://soulfiremc.com/docs/guides/cli-mode).

## 🧵 Demo

SoulFire has a built-in GUI for easy usage. Try a SoulFire demo yourself at the [demo page](https://demo.soulfiremc.com).

https://github.com/user-attachments/assets/6244d54c-a1e5-4467-a705-c929b9de2b57

## ✨ Nightly builds

You can download the latest development version of SoulFire
through [nightly.link](https://nightly.link/AlexProgrammerDE/SoulFire/workflows/build/main).

## 🔧 Build from source

1. Install Java 25+
2. Download the latest source code from GitHub
3. Run `./gradlew build` in the project directory
4. Get the jar file from `client-launcher/build/libs` or `dedicated-launcher/build/libs`

## 👨‍💻 Developer API

SoulFire offers a Developer API to create your own plugins using the plugin API and mixins.
The software may regularly have breaking changes, so you'll have to stick to a version of SoulFire or update your plugin
regularly.

To learn more about the Developer API, take a look at the
official [plugin example](https://github.com/AlexProgrammerDE/SoulFirePluginExample).

## 🌈 Community

Feel free to join our Discord community server:

[![Discord Banner](https://discord.com/api/guilds/739784741124833301/widget.png?style=banner2)](https://discord.gg/vHgRd6YZmH)

This project is in active development, so if you have any feature requests or issues, please submit them here on GitHub.
PRs are welcome, too.

## 🏅 Sponsors

<table>
 <tbody>
  <tr>
   <td align="center"><img alt="[SignPath]" src="https://avatars.githubusercontent.com/u/34448643" height="30"/></td>
   <td>Free code signing on Windows provided by <a href="https://signpath.io/?utm_source=foundation&utm_medium=github&utm_campaign=soulfire">SignPath.io</a>, certificate by <a href="https://signpath.org/?utm_source=foundation&utm_medium=github&utm_campaign=soulfire">SignPath Foundation</a></td>
  </tr>
 </tbody>
</table>

## 🌟 Star History

<a href="https://star-history.com/#AlexProgrammerDE/SoulFire&Date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=AlexProgrammerDE/SoulFire&type=Date&theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=AlexProgrammerDE/SoulFire&type=Date" />
    <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=AlexProgrammerDE/SoulFire&type=Date" />
  </picture>
</a>
