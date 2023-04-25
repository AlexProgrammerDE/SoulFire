# Developer resources

This document is intended to help developers get started with the project and learn how to learn the tools used.

## Protocol Sniffing

To learn about how some packets are structured and their data works, you need a MITM proxy. This is a proxy that sits between the client and the server and can intercept and modify packets. 
This is useful for learning how the client and server communicate and how to modify packets to get the desired result.

### pakkit

GitHub: https://github.com/Heath123/pakkit

### SniffCraft

GitHub: https://github.com/adepierre/SniffCraft

## MC Data

### Chunk Data

How do you calculate bitPerEntry on chunks???
It's the log2(maxBlockStateId). Download the latest mc server jar and run:

```bash
java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --all
```

Then go into the generated folder `generated/reports`. Then execute:
```bash
state_ids=$(jq -r '.[].states[].id' blocks.json)

# Sort the IDs in descending order and get the highest one
highest_id=$(echo "${state_ids}" | sort -rn | head -n 1)

echo "The highest state ID is ${highest_id}"
```

Take the ID, log2 it and then round up. That's your `bitsPerEntry`.
