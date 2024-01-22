# Command line flags

## List of flags

| Flag                             | Default                           | Description                                                                               |
|----------------------------------|-----------------------------------|-------------------------------------------------------------------------------------------|
| `--account-file`                 | `null`                            | File to load accounts from                                                                |
| `--account-type`                 | `null`                            | Type of accounts in the account file                                                      |
| `--proxy-file`                   | `null`                            | File to load proxies from                                                                 |
| `--proxy-type`                   | `null`                            | Type of proxies in the proxy file                                                         |
| `--profile-file`                 | `null`                            | File to load a profile from                                                               |
| `-h`, `--help`                   | `false`                           | Show this help message and exit.                                                          |
| `-V`, `--version`                | `false`                           | Print version information and exit.                                                       |
| `--address`                      | `127.0.0.1:25565`                 | Address to connect to                                                                     |
| `--amount`                       | `1`                               | Amount of bots to connect                                                                 |
| `--join-min-delay`               | `1000`                            | Minimum delay between joins in milliseconds                                               |
| `--join-max-delay`               | `3000`                            | Maximum delay between joins in milliseconds                                               |
| `--protocol-version`             | `1.20.3`                          | Minecraft protocol version to use                                                         |
| `--read-timeout`                 | `30`                              | Read timeout in seconds                                                                   |
| `--write-timeout`                | `0`                               | Write timeout in seconds                                                                  |
| `--connect-timeout`              | `30`                              | Connect timeout in seconds                                                                |
| `--resolve-srv`                  | `true`                            | Try to resolve SRV records from the address                                               |
| `--concurrent-connects`          | `1`                               | Max amount of bots attempting to connect at once                                          |
| `--core-debug`                   | `false`                           | Enable core code debug logging                                                            |
| `--via-debug`                    | `false`                           | Enable Via* code debug logging                                                            |
| `--netty-debug`                  | `false`                           | Enable Netty debug logging                                                                |
| `--grpc-debug`                   | `false`                           | Enable gRPC debug logging                                                                 |
| `--name-format`                  | `Bot_%d`                          | The format of the bot names. %d will be replaced with the bot number.                     |
| `--shuffle-accounts`             | `false`                           | Should the accounts order be random when connecting bots?                                 |
| `--bots-per-proxy`               | `-1`                              | Amount of bots that can be on a single proxy                                              |
| `--send-client-brand`            | `true`                            | Send client brand to the server                                                           |
| `--client-brand`                 | `vanilla`                         | The client brand to send to the server                                                    |
| `--send-client-settings`         | `true`                            | Send client settings to the server when joining                                           |
| `--client-locale`                | `en_gb`                           | The locale the client uses for translations                                               |
| `--render-distance`              | `8`                               | How far the client renders chunks. (Use this to load more or less chunks from the server) |
| `--chat-visibility`              | `FULL`                            | What type of chat messages the client will receive                                        |
| `--use-chat-colors`              | `true`                            | Whether the client will use chat colors                                                   |
| `--cape-enabled`                 | `true`                            | Whether to display the bots cape if it has one                                            |
| `--jacket-enabled`               | `true`                            | Whether to render the jacket overlay skin layer                                           |
| `--left-sleeve-enabled`          | `true`                            | Whether to render the left overlay skin layer                                             |
| `--right-sleeve-enabled`         | `true`                            | Whether to render the right overlay skin layer                                            |
| `--left-pants-leg-enabled`       | `true`                            | Whether to render the left pants leg overlay skin layer                                   |
| `--right-pants-leg-enabled`      | `true`                            | Whether to render the right pants leg overlay skin layer                                  |
| `--hat-enabled`                  | `true`                            | Whether to render the hat overlay skin layer                                              |
| `--hand-preference`              | `RIGHT_HAND`                      | What hand the client prefers to use for items                                             |
| `--text-filtering-enabled`       | `true`                            | Whether to filter chat messages from the server                                           |
| `--allows-listing`               | `true`                            | Whether the client wants their username to be shown in the server list                    |
| `--auto-reconnect`               | `true`                            | Reconnect a bot when it times out/is kicked                                               |
| `--reconnect-min-delay`          | `1`                               | Minimum delay between reconnects                                                          |
| `--reconnect-max-delay`          | `5`                               | Maximum delay between reconnects                                                          |
| `--auto-register`                | `false`                           | Make bots run the /register and /login command after joining                              |
| `--register-command`             | `/register %password% %password%` | Command to be executed to register                                                        |
| `--login-command`                | `/login %password%`               | Command to be executed to log in                                                          |
| `--captcha-command`              | `/captcha %captcha%`              | Command to be executed to confirm a captcha                                               |
| `--password-format`              | `SoulFire`                        | The password for registering                                                              |
| `--auto-respawn`                 | `true`                            | Respawn automatically after death                                                         |
| `--respawn-min-delay`            | `1`                               | Minimum delay between respawns                                                            |
| `--respawn-max-delay`            | `3`                               | Maximum delay between respawns                                                            |
| `--auto-totem`                   | `true`                            | Always put available totems in the offhand slot                                           |
| `--totem-min-delay`              | `1`                               | Minimum delay between using totems                                                        |
| `--totem-max-delay`              | `2`                               | Maximum delay between using totems                                                        |
| `--auto-jump`                    | `false`                           | Attempt to jump automatically in random intervals                                         |
| `--jump-min-delay`               | `2`                               | Minimum delay between jumps                                                               |
| `--jump-max-delay`               | `5`                               | Maximum delay between jumps                                                               |
| `--auto-armor`                   | `true`                            | Put on best armor automatically                                                           |
| `--armor-min-delay`              | `1`                               | Minimum delay between putting on armor                                                    |
| `--armor-max-delay`              | `2`                               | Maximum delay between putting on armor                                                    |
| `--auto-eat`                     | `true`                            | Eat available food automatically when hungry                                              |
| `--eat-min-delay`                | `1`                               | Minimum delay between eating                                                              |
| `--eat-max-delay`                | `2`                               | Maximum delay between eating                                                              |
| `--log-chat`                     | `true`                            | Log all received chat messages to the terminal                                            |
| `--chat-interval`                | `2`                               | This is the minimum delay between logging chat messages                                   |
| `--server-list-bypass`           | `false`                           | Whether to ping the server list before connecting. (Bypasses anti-bots like EpicGuard)    |
| `--server-list-bypass-min-delay` | `1`                               | Minimum delay between joining the server                                                  |
| `--server-list-bypass-max-delay` | `3`                               | Maximum delay between joining the server                                                  |
| `--fake-virtual-host`            | `false`                           | Whether to fake the virtual host or not                                                   |
| `--fake-virtual-host-hostname`   | `localhost`                       | The hostname to fake                                                                      |
| `--fake-virtual-host-port`       | `25565`                           | The port to fake                                                                          |

## Example usage

```bash
java -jar SoulFire.jar --host 127.0.0.1 --port 25565 --amount 100 --mc-version 1.20 --min-join-delay 1000 --max-join-delay 3000
```

## How to generate this list

```bash
java -jar SoulFire.jar --generate-flags
```
