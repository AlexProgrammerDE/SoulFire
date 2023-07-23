# Command line flags

## List of flags

| Flag                             | Default                           | Description                                                  |
|----------------------------------|-----------------------------------|--------------------------------------------------------------|
| `--host`, `--target`             | `127.0.0.1`                       | Target url to connect to                                     |
| `--port`                         | `25565`                           | Target port to connect to                                    |
| `-a`, `--amount`                 | `1`                               | Amount of bots to connect to the server                      |
| `--min-join-delay`               | `1000`                            | The minimum delay between bot connections, in milliseconds   |
| `--max-join-delay`               | `3000`                            | The maximum delay between bot connections, in milliseconds   |
| `-mc`, `--mc-version`            | `1.20`                            | Minecraft version of the server to connect to                |
| `--read-timeout`                 | `30`                              | Bot read timeout                                             |
| `--write-timeout`                | `0`                               | Bot write timeout                                            |
| `--connect-timeout`              | `30`                              | Bot connect timeout                                          |
| `--try-srv`                      | `true`                            | Try to connect to the target using SRV records               |
| `--concurrent-connects`          | `1`                               | Amount of bots that can try to connect at the same time      |
| `--debug`                        | `false`                           | Log additional information useful for debugging the software |
| `--bots-per-proxy`               | `-1`                              | Amount of bots that can be on a single proxy                 |
| `--name-format`                  | `Bot%d`                           | Format for bot names. allows integer placeholder '%d'        |
| `--shuffle-accounts`             | `false`                           | Shuffle accounts before connecting                           |
| `--account-file`                 | `null`                            | File to load accounts from                                   |
| `--account-type`                 | `null`                            | Type of accounts in the account file                         |
| `--proxy-file`                   | `null`                            | File to load proxies from                                    |
| `--proxy-type`                   | `null`                            | Type of proxies in the proxy file                            |
| `--profile-file`                 | `null`                            | File to load a profile from                                  |
| `--generate-flags`               | `false`                           | Create a list of flags                                       |
| `-h`, `--help`                   | `false`                           | Show this help message and exit.                             |
| `-V`, `--version`                | `false`                           | Print version information and exit.                          |
| `--auto-reconnect`               | `true`                            | Reconnect bots after being disconnected                      |
| `--reconnect-min-delay`          | `1`                               | Minimum delay between reconnects                             |
| `--reconnect-max-delay`          | `5`                               | Maximum delay between reconnects                             |
| `--send-client-settings`         | `true`                            | Send client settings                                         |
| `--client-locale`                | `en_gb`                           | Client locale                                                |
| `--render-distance`              | `8`                               | Render distance                                              |
| `--chat-visibility`              | `FULL`                            | Chat visibility                                              |
| `--use-chat-colors`              | `true`                            | Use chat colors                                              |
| `--cape-enabled`                 | `true`                            | Cape enabled                                                 |
| `--jacket-enabled`               | `true`                            | Jacket enabled                                               |
| `--left-sleeve-enabled`          | `true`                            | Left sleeve enabled                                          |
| `--right-sleeve-enabled`         | `true`                            | Right sleeve enabled                                         |
| `--left-pants-leg-enabled`       | `true`                            | Left pants leg enabled                                       |
| `--right-pants-leg-enabled`      | `true`                            | Right pants leg enabled                                      |
| `--hat-enabled`                  | `true`                            | Hat enabled                                                  |
| `--hand-preference`              | `RIGHT_HAND`                      | Hand preference                                              |
| `--text-filtering-enabled`       | `true`                            | Text filtering enabled                                       |
| `--allows-listing`               | `true`                            | Allows listing                                               |
| `--send-client-brand`            | `true`                            | Send client brand                                            |
| `--client-brand`                 | `vanilla`                         | Client brand                                                 |
| `--auto-register`                | `false`                           | Make bots run the /register and /login command after joining |
| `--register-command`             | `/register %password% %password%` | Command to be executed to register                           |
| `--login-command`                | `/login %password%`               | Command to be executed to log in                             |
| `--captcha-command`              | `/captcha %captcha%`              | Command to be executed to confirm a captcha                  |
| `--password-format`              | `ServerWrecker`                   | The password for registering                                 |
| `--auto-respawn`                 | `true`                            | Respawn bots after death                                     |
| `--respawn-min-delay`            | `1`                               | Minimum delay between respawns                               |
| `--respawn-max-delay`            | `3`                               | Maximum delay between respawns                               |
| `--auto-jump`                    | `false`                           | Do auto jump?                                                |
| `--jump-min-delay`               | `2`                               | Minimum delay between jumps                                  |
| `--jump-max-delay`               | `5`                               | Maximum delay between jumps                                  |
| `--server-list-bypass`           | `false`                           | Do server list bypass?                                       |
| `--server-list-bypass-min-delay` | `1`                               | Minimum join delay after pinging the server                  |
| `--server-list-bypass-max-delay` | `3`                               | Maximum join delay after pinging the server                  |

## Example usage

```bash
java -jar ServerWrecker.jar --host 127.0.0.1 --port 25565 --amount 100 --mc-version 1.20 --min-join-delay 1000 --max-join-delay 3000
```

## How to generate this list

```bash
java -jar ServerWrecker.jar --generate-flags
```
