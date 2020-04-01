# LambdaAttack

## Description

Minecraft bot. Currently used for stress testing.

## Features

* Graphical user interface
* Command line interface
* Configurable amount and join delay
* Configurable target
* Configurable name format or name list
* LogPanel to see errors directly
* Test with Spigot, Paper
* Disconnects gracefully after the end
* Automatically registers for cracked servers
* Supports SOCKS proxies
* Free
* Open source

## Requirements

* Java 9
* Minecraft 1.11, 1.12, 1.14 or 1.15 server

## Downloads

https://github.com/games647/LambdaAttack/releases

## Building

1. Download Apache [Maven](https://maven.apache.org/download.cgi)
2. Unpack it
3. Optionally add the bin folder to your PATH variable to invoke Maven with mvn without specifying the complete path to the bin folder for every command
4. Download this project with Git (git clone <URL/git@github:...>) or as zip
5. Move to the top folder of the project conting the pom.xml
6. Run `mvn clean install`
7. The final version is inside the core/target folder

## Images

![Gui](https://i.imgur.com/6U00ZwA.png)

## Command Line Usage
LambdaAttack can be used from the command line without a GUI.  
To get a list of all available options, run
`java -jar lambdaattack.jar --help`.

## Dependencies

* Java 9
* McProtocolLib: https://github.com/Steveice10/MCProtocolLib
