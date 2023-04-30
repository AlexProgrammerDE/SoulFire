import config from 'generator-config.json'
import mcDataPackage from 'minecraft-data'
import fs from 'fs'
import process from 'process'

const mcData = mcDataPackage(config.version)
const enumReplace = "// VALUES REPLACE"

if (mcData == null) {
    console.error(`Version ${config.version} not found`)
    process.exit(1)
} else {
    const getNameOfItemId = (id: number): string | null => {
        return mcData.items[id].name.toUpperCase();
    }

    fs.mkdirSync("output", {recursive: true})

    if (config["generate-blocks"]) {
        let result = fs.readFileSync("templates/BlockType.java", "utf-8");
        let enumValues: string[] = []
        for (const block of mcData.blocksArray) {
            let harvestData: string | null = null
            if (block.harvestTools) {
                const harvestTools = block.harvestTools;
                harvestData = "Map.ofEntries("
                const toolList: string[] = []
                for (const tool of Object.keys(harvestTools)) {
                    toolList.push(`Map.entry(ItemType.${getNameOfItemId(Number(tool))}, ${harvestTools[tool]})`)
                }
                harvestData += toolList.join(", ") + ")"
            }
            let drops: string | null = null
            if (block.drops) {
                drops = "List.of("
                const dropList: string[] = []
                for (const drop of block.drops) {
                    if (typeof drop === "number") {
                        dropList.push(`new DropData(ItemType.${getNameOfItemId(drop)})`)
                    } else if (typeof drop === "object") {
                        if (typeof drop.drop === "number") {
                            dropList.push(`new DropData(ItemType.${getNameOfItemId(drop.drop)}, ${drop.minCount}, ${drop.maxCount})`)
                        } else if (typeof drop.drop === "object") {
                            dropList.push(`new DropData(ItemType.${getNameOfItemId(drop.drop.id)}, ${drop.drop.metadata}, ${drop.minCount}, ${drop.maxCount})`)
                        }
                    }
                }
                drops += dropList.join(", ") + ")"
            }
            enumValues.push(`public static final BlockType ${block.name.toUpperCase()} = register(new BlockType(${block.id}, "${block.name}", "${block.displayName}", ${block.hardness ?? -1}, ${block.resistance}, ${block.stackSize}, ${block.diggable}, "${block.material}", BoundingBoxType.${block.boundingBox.toUpperCase()}));`)
        }

        result = result.replace(enumReplace, enumValues.join("\n    "))

        fs.writeFileSync("output/BlockType.java", result)
    }


    if (config["generate-items"]) {
        let result = fs.readFileSync("templates/ItemType.java", "utf-8");
        let enumValues: string[] = []
        for (const item of mcData.itemsArray) {
            enumValues.push(`public static final ItemType ${item.name.toUpperCase()} = register(new ItemType(${item.id}, "${item.name}", "${item.displayName}", ${item.stackSize}, ${stringArrayToJavaList(item.enchantCategories)}, ${stringArrayToJavaList(item.repairWith)}, ${item.maxDurability ?? "-1"}));`)
        }

        result = result.replace(enumReplace, enumValues.join("\n    "))

        fs.writeFileSync("output/ItemType.java", result)
    }
}

function stringArrayToJavaList(array?: string[]): string {
    if (array == null) {
        return "null"
    }

    return `List.of(${array.map(data => `"${data}"`).join(", ")})`
}
