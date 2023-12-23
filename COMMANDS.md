# Commands

## List of commands

| Command               | Description                                                          |
|-----------------------|----------------------------------------------------------------------|
| `help`                | Prints a list of all available commands                              |
| `reload-history`      | Refreshes the loaded command history from the data file              |
| `clear-history`       | Wipes the command history data                                       |
| `clear-console`       | Clears the GUIs log panel                                            |
| `walkxyz <x> <y> <z>` | Makes all connected bots pathfind to the xyz coordinates             |
| `walkxz <x> <z>`      | Makes all connected bots pathfind to the xz coordinates              |
| `walky <y>`           | Makes all connected bots pathfind to the y coordinates               |
| `stop-path`           | Makes all connected bots stop pathfinding                            |
| `lookat <x> <y> <z>`  | Makes all connected bots look at the block at the xyz coordinates    |
| `forward`             | Makes all connected bots start walking forward                       |
| `backward`            | Makes all connected bots start walking backwards                     |
| `left`                | Makes all connected bots start walking left                          |
| `right`               | Makes all connected bots start walking right                         |
| `jump`                | Makes all connected bots jump up repeatedly                          |
| `reset`               | Resets the movement of all connected bots                            |
| `stop-attack`         | Stops the ongoing attacks                                            |
| `online`              | Shows connected bots from all attacks                                |
| `say <message>`       | Makes all connected bots send a message in chat or execute a command |
| `stats`               | Shows network stats                                                  |

## Example usage

`walkxyz 578 65 100`

## How to generate this list

In the GUI or CLI, execute the hidden command `help-markdown`.
