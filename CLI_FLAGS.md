# Command line flags

## List of flags

| Flag                             | Default                           | Description                                                           |
|----------------------------------|-----------------------------------|-----------------------------------------------------------------------|
| `--account-file`                 | `null`                            | File to load accounts from                                            |
| `--account-type`                 | `null`                            | Type of accounts in the account file                                  |
| `--proxy-file`                   | `null`                            | File to load proxies from                                             |
| `--proxy-type`                   | `null`                            | Type of proxies in the proxy file                                     |
| `--profile-file`                 | `null`                            | File to load a profile from                                           |
| `-h`, `--help`                   | `false`                           | Show this help message and exit.                                      |
| `-V`, `--version`                | `false`                           | Print version information and exit.                                   |
| `--host`                         | `127.0.0.1`                       | Host to connect to                                                    |
| `--port`                         | `25565`                           | Port to connect to                                                    |
| `--amount`                       | `1`                               | Amount of bots to connect                                             |
| `--min-join-delay-ms`            | `1000`                            | Minimum delay between joins in milliseconds                           |
| `--max-join-delay-ms`            | `3000`                            | Maximum delay between joins in milliseconds                           |
| `--protocol-version`             | `1.20.2`                          | Protocol version to use                                               |
| `--read-timeout`                 | `30`                              | Read timeout in seconds                                               |
| `--write-timeout`                | `0`                               | Write timeout in seconds                                              |
| `--connect-timeout`              | `30`                              | Connect timeout in seconds                                            |
| `--try-srv`                      | `true`                            | Try to use SRV records                                                |
| `--concurrent-connects`          | `1`                               | Amount of concurrent connects                                         |
| `--via-debug`                    | `false`                           | Enable Via debug                                                      |
| `--netty-debug`                  | `false`                           | Enable Netty debug                                                    |
| `--grpc-debug`                   | `false`                           | Enable GRPC debug                                                     |
| `--core-debug`                   | `false`                           | Enable Core debug                                                     |
| `--name-format`                  | `Bot_%d`                          | The format of the bot names. %d will be replaced with the bot number. |
| `--shuffle-accounts`             | `false`                           | Should the accounts be shuffled?                                      |
| `--bots-per-proxy`               | `-1`                              | Amount of bots that can be on a single proxy                          |
| `--send-client-brand`            | `true`                            | Send client brand                                                     |
| `--client-brand`                 | `vanilla`                         | Client brand                                                          |
| `--send-client-settings`         | `true`                            | Send client settings                                                  |
| `--client-locale`                | `en_gb`                           | Client locale                                                         |
| `--render-distance`              | `8`                               | Render distance                                                       |
| `--chat-visibility`              | `FULL`                            | Chat visibility                                                       |
| `--use-chat-colors`              | `true`                            | Use chat colors                                                       |
| `--cape-enabled`                 | `true`                            | Cape enabled                                                          |
| `--jacket-enabled`               | `true`                            | Jacket enabled                                                        |
| `--left-sleeve-enabled`          | `true`                            | Left sleeve enabled                                                   |
| `--right-sleeve-enabled`         | `true`                            | Right sleeve enabled                                                  |
| `--left-pants-leg-enabled`       | `true`                            | Left pants leg enabled                                                |
| `--right-pants-leg-enabled`      | `true`                            | Right pants leg enabled                                               |
| `--hat-enabled`                  | `true`                            | Hat enabled                                                           |
| `--hand-preference`              | `RIGHT_HAND`                      | Hand preference                                                       |
| `--text-filtering-enabled`       | `true`                            | Text filtering enabled                                                |
| `--allows-listing`               | `true`                            | Allows listing                                                        |
| `--auto-reconnect`               | `true`                            | Do Auto Reconnect?                                                    |
| `--reconnect-min-delay`          | `1`                               | Minimum delay between reconnects                                      |
| `--reconnect-max-delay`          | `5`                               | Maximum delay between reconnects                                      |
| `--auto-register`                | `false`                           | Make bots run the /register and /login command after joining          |
| `--register-command`             | `/register %password% %password%` | Command to be executed to register                                    |
| `--login-command`                | `/login %password%`               | Command to be executed to log in                                      |
| `--captcha-command`              | `/captcha %captcha%`              | Command to be executed to confirm a captcha                           |
| `--password-format`              | `ServerWrecker`                   | The password for registering                                          |
| `--auto-respawn`                 | `true`                            | Do Auto Respawn?                                                      |
| `--respawn-min-delay`            | `1`                               | Minimum delay between respawns                                        |
| `--respawn-max-delay`            | `3`                               | Maximum delay between respawns                                        |
| `--auto-totem`                   | `true`                            | Do Auto Totem?                                                        |
| `--totem-min-delay`              | `1`                               | Minimum delay between using totems                                    |
| `--totem-max-delay`              | `2`                               | Maximum delay between using totems                                    |
| `--auto-jump`                    | `true`                            | Do Auto Jump?                                                         |
| `--jump-min-delay`               | `2`                               | Minimum delay between jumps                                           |
| `--jump-max-delay`               | `5`                               | Maximum delay between jumps                                           |
| `--auto-armor`                   | `true`                            | Do Auto Armor?                                                        |
| `--armor-min-delay`              | `1`                               | Minimum delay between putting on armor                                |
| `--armor-max-delay`              | `2`                               | Maximum delay between putting on armor                                |
| `--auto-eat`                     | `true`                            | Do Auto Eat?                                                          |
| `--eat-min-delay`                | `1`                               | Minimum delay between eating                                          |
| `--eat-max-delay`                | `2`                               | Maximum delay between eating                                          |
| `--log-chat`                     | `true`                            | If this is enabled, all chat messages will be logged to the terminal  |
| `--server-list-bypass`           | `false`                           | Do Server List Bypass?                                                |
| `--server-list-bypass-min-delay` | `1`                               | Minimum delay between joining the server                              |
| `--server-list-bypass-max-delay` | `3`                               | Maximum delay between joining the server                              |

## Example usage

```bash
java -jar ServerWrecker.jar --host 127.0.0.1 --port 25565 --amount 100 --mc-version 1.20 --min-join-delay 1000 --max-join-delay 3000
```

## How to generate this list

```bash
java -jar ServerWrecker.jar --generate-flags
```
