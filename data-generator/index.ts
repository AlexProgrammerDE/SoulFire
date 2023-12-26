const version = "1.20.4"

const mcData = null
const enumReplace = "// VALUES REPLACE"

if (mcData == null) {
  console.error(`Version ${version} not found`)
  process.exit(1)
} else {
  const getNameOfItemId = (id: number): string | null => {
    return mcData.items[id].name.toUpperCase();
  }

  {
    let enumValues: string[] = []
    for (const item of Object.keys(mcData.blockCollisionShapes.shapes)) {
      const id = Number(item)
      const shape = mcData.blockCollisionShapes.shapes[item as any]

      let shapeData = item
      const shapeList: string[] = []
      for (const shapePart of shape) {
        shapeList.push(`${shapePart[0]},${shapePart[1]},${shapePart[2]},${shapePart[3]},${shapePart[4]},${shapePart[5]}`)
      }
      if (shapeList.length > 0) {
        shapeData += "|"
      }
      shapeData += shapeList.join("|")

      enumValues.push(shapeData)
    }
  }

  {
    let enumValues: string[] = []
    for (const block of mcData.blocksArray) {
      let shapes = block.name
      const collisionShapes = mcData.blockCollisionShapes.blocks[block.name]
      if (collisionShapes) {
        shapes += "|"
        // noinspection SuspiciousTypeOfGuard
        if (typeof collisionShapes === "number") {
          shapes += collisionShapes
        } else {
          let shapeList: string[] = []
          for (const shape of collisionShapes) {
            shapeList.push(`${shape}`)
          }
          shapes += shapeList.join(",")
        }
      }

      enumValues.push(shapes)
    }
  }

  {
    let result = fs.readFileSync("templates/BlockType.java", "utf-8");
    let enumValues: string[] = []
    for (const block of mcData.blocksArray) {
      enumValues.push(`public static final BlockType ${block.name.toUpperCase()} = register(new BlockType(${block.id}, "${block.name}", "${block.displayName}", ${block.hardness ?? -1}F, ${block.stackSize}, ${block.diggable}, ResourceData.BLOCK_PROPERTY_MAP.get("${block.name}"), BlockStateLoader.getBlockShapes("${block.name}")));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }

  {
    let result = fs.readFileSync("templates/ItemType.java", "utf-8");
    let enumValues: string[] = []
    for (const item of mcData.itemsArray) {
      enumValues.push(`public static final ItemType ${item.name.toUpperCase()} = register(new ItemType(${item.id}, "${item.name}", "${item.displayName}", ${item.stackSize}, ${stringArrayToJavaList(item.enchantCategories)}, ${stringArrayToJavaList(item.repairWith)}, ${item.maxDurability ?? "-1"}));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }

  {
    let result = fs.readFileSync("templates/EntityType.java", "utf-8");
    let enumValues: string[] = []
    for (const item of mcData.entitiesArray) {
      enumValues.push(`public static final EntityType ${item.name.toUpperCase()} = register(new EntityType(${item.id}, "${item.name}", "${item.displayName}", "${item.type}", ${item.width}F, ${item.height}F, "${item.category}"));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }

  {
    let result = fs.readFileSync("templates/FoodType.java", "utf-8");
    let enumValues: string[] = []
    for (const food of mcData.foodsArray) {
      enumValues.push(`public static final FoodType ${food.name.toUpperCase()} = register(new FoodType(ItemType.${getNameOfItemId(food.id)}, ${food.foodPoints}, ${food.saturation}, ${food.effectiveQuality}, ${food.saturationRatio}));`)
    }

    result = result.replace(enumReplace, enumValues.join("\n    "))
  }
}

function stringArrayToJavaList(array?: string[]): string {
  if (array == null) {
    return "null"
  }

  return `List.of(${array.map(data => `"${data}"`).join(", ")})`
}
