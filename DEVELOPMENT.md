# Developer resources

This document is intended to help developers get started with the project and learn how to learn the tools used.

## Protocol Sniffing
`
To learn about how some packets are structured and their data works, you need a MITM proxy. This is a proxy that sits
between the client and the server and can intercept and modify packets.
This is useful for learning how the client and server communicate and how to modify packets to get the desired result.

### pakkit

GitHub: https://github.com/Heath123/pakkit

### SniffCraft

GitHub: https://github.com/adepierre/SniffCraft
`
## MC Data

All data that SoulFire sources from the Minecraft game are inside the
`data-generator` module. Inside of it run `runServer` and it'll generate all
files for SoulFire into the `data-generator/run/minecraft-data` directory.
