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
      let harvestData: string | null = "List.of("
      if (block.harvestTools) {
        const harvestTools = block.harvestTools;
        const toolList: string[] = []
        for (const tool of Object.keys(harvestTools)) {
          toolList.push(`ItemType.${getNameOfItemId(Number(tool))}`)
        }
        harvestData += toolList.join(", ")
      }
      harvestData += ")"

      let collisionHeight = 0
      const collisionShapes = mcData.blockCollisionShapes.blocks[block.name]
      if (collisionShapes) {
        let minShape
        // noinspection SuspiciousTypeOfGuard
        if (typeof collisionShapes === "number") {
          minShape = collisionShapes
        } else {
          minShape = collisionShapes[0]
        }

        // Work around typing mistake in node-minecraft-data
        const shape = mcData.blockCollisionShapes.shapes[String(minShape) as any]
        if (shape && shape.length > 0) {
          collisionHeight = shape[0][4]
        }
      }

      enumValues.push(`public static final BlockType ${block.name.toUpperCase()} = register(new BlockType(${block.id}, "${block.name}", "${block.displayName}", ${block.hardness ?? -1}, ${block.resistance}, ${block.stackSize}, ${block.diggable}, BoundingBoxType.${block.boundingBox.toUpperCase()}, ${harvestData}, ${collisionHeight}));`)
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

  if (config["generate-entities"]) {
    let result = fs.readFileSync("templates/EntityType.java", "utf-8");
    let enumValues: string[] = []
    for (const item of mcData.entitiesArray) {
      enumValues.push(`public static final EntityType ${item.name.toUpperCase()} = register(new EntityType(${item.id}, ${item.internalId}, "${item.name}", "${item.displayName}", "${item.type}", ${item.width}, ${item.height}, ${valueToNullStringFallback(-1, item.length)}, ${valueToNullStringFallback(-1, item.offset)}, "${item.category}"));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))

    fs.writeFileSync("output/EntityType.java", result)
  }

  if (config["generate-food"]) {
    let result = fs.readFileSync("templates/FoodType.java", "utf-8");
    let enumValues: string[] = []
    for (const food of mcData.foodsArray) {
      enumValues.push(`public static final FoodType ${food.name.toUpperCase()} = register(new FoodType(ItemType.${getNameOfItemId(food.id)}, ${food.foodPoints}, ${food.saturation}, ${food.effectiveQuality}, ${food.saturationRatio}));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))

    fs.writeFileSync("output/FoodType.java", result)
  }
}

function stringArrayToJavaList(array?: string[]): string {
  if (array == null) {
    return "null"
  }

  return `List.of(${array.map(data => `"${data}"`).join(", ")})`
}

function valueToNullStringFallback(fallback: any, array?: any): string {
  if (array == null) {
    return fallback
  }

  return `"${array}"`
}
