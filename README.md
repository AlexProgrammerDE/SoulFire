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
* Tested with Spigot, Paper, Purpur, Bungeecord, Velocity
* Disconnects gracefully after the end
* Automatically registers for cracked servers
* Supports SOCKS4 and 5 proxies

## Requirements

* Java 17
* Minecraft 1.7.10+ server

## Building

1. Download Gradle
2. Download the latest version of serverwrecker
3. Run `./gradlew build` in the serverwrecker directory
4. Get the jar files from `core/build/libs`

## Community

Feel free to join our discord community server:

[![Discord Banner](https://discord.com/api/guilds/739784741124833301/widget.png?style=banner2)](https://discord.gg/CDrcxzH)

## Images

![GUI](./images/img.png)

## Command Line Usage

To run ServerWrecker from the command line without a GUI, enter  
`java -jar serverwrecker.jar <options>`.

These are the available options:

| Name                  | Description                                                                                                    |
|-----------------------|----------------------------------------------------------------------------------------------------------------|
| -h, --host \<arg\>    | The hostname to connect to. Defaults to `127.0.0.1`                                                            |
| -p, --port \<arg\>    | The port to connect to. Defaults to `25565`                                                                    |
| -c, --count \<arg\>   | The amount of bots to connect to the server. Defaults to 20                                                    |
| -d, --delay \<arg\>   | The delay between bot spawns, in milliseconds. Defaults to 1000                                                |
| -n, --name \<arg\>    | The format for bot names. Requires exactly one integer placeholder `%d`. Defaults to `Bot-%d`                  |
| -v, --version \<arg\> | The Minecraft version of the server to connect to. Defaults to 1.15.2                                          |
| -r, --register        | Makes Bots run the /register and /login command after joining with username and password being `ServerWrecker` |
| --help                | Displays a help page                                                                                           |
