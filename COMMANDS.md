# Commands

## List of commands

| Command               | Description                                                          |
|-----------------------|----------------------------------------------------------------------|
| `reload-history`      | Refreshes the loaded command history from the data file              |
| `clear-history`       | Wipes the command history data                                       |
| `online`              | Shows connected bots from all attacks                                |
| `clear-console`       | Clears the GUIs log panel                                            |
| `say <message>`       | Makes all connected bots send a message in chat or execute a command |
| `stats`               | Shows network stats                                                  |
| `help`                | Prints a list of all available commands                              |
| `walkxyz <x> <y> <z>` | Makes all connected bots pathfind to the xyz coordinates             |
| `walkxz <x> <z>`      | Makes all connected bots pathfind to the xz coordinates              |
| `walky <y>`           | Makes all connected bots pathfind to the y coordinates               |
| `lookat <x> <y> <z>`  | Makes all connected bots look at the block at the xyz coordinates    |
| `forward`             | Makes all connected bots start walking forward                       |
| `jump`                | Makes all connected bots jump up repeatedly                          |
| `reset`               | Resets the movement of all connected bots                            |
| `stop-path`           | Makes all connected bots stop pathfinding                            |
| `stop-attack`         | Stops the ongoing attack                                             |

## Example usage

`walkxyz 578 65 100`

## How to generate this list

In the GUI or CLI, execute the hidden command `help-markdown`.
