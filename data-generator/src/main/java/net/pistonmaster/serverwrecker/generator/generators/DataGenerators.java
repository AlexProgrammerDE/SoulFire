package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class DataGenerators {
    private static final List<IDataGenerator> GENERATORS = List.of(
            new BlockCollisionShapesDataGenerator.BlockShapesGenerator(),
            new BlockCollisionShapesDataGenerator.BlockStatesGenerator(),
            new BlocksDataGenerator(),
            new EffectsDataGenerator(),
            new EnchantmentsDataGenerator(),
            new EntitiesDataGenerator(),
            new FoodsDataGenerator(),
            new ItemsDataGenerator(),
            new LanguageDataGenerator(),
            new InstrumentsDataGenerator(),
            new TagsDataGenerator.BlockTagsDataGenerator(),
            new TagsDataGenerator.ItemTagsDataGenerator(),
            new TagsDataGenerator.EntityTypeTagsDataGenerator()
    );
    private static final Logger logger = LoggerFactory.getLogger(DataGenerators.class);

    public static boolean runDataGenerators(Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            logger.error("Failed to create data generator output directory at {}", outputDirectory, exception);
            return false;
        }

        var generatorsFailed = 0;
        logger.info("Running minecraft data generators, output at {}", outputDirectory);

        for (var dataGenerator : GENERATORS) {
            logger.info("Running generator {}", dataGenerator.getDataName());
            try {
                var outputFileName = dataGenerator.getDataName();
                var outputFilePath = outputDirectory.resolve(outputFileName);
                var outputElement = dataGenerator.generateDataJson();

                if (outputElement instanceof JsonElement jsonElement) {
                    try (Writer writer = Files.newBufferedWriter(outputFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        var jsonWriter = new JsonWriter(writer);
                        jsonWriter.setIndent("  ");
                        Streams.write(jsonElement, jsonWriter);
                    }
                } else if (outputElement instanceof String string) {
                    try (Writer writer = Files.newBufferedWriter(outputFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        writer.write(string);
                    }
                } else {
                    logger.error("Unknown output type for data generator {}", dataGenerator.getDataName());
                    generatorsFailed++;
                    continue;
                }

                logger.info("Generator: {} -> {}", dataGenerator.getDataName(), outputFileName);
            } catch (Throwable exception) {
                logger.error("Failed to run data generator {}", dataGenerator.getDataName(), exception);
                generatorsFailed++;
            }
        }

        logger.info("Running built-in data generator");
        try {
            net.minecraft.data.Main.main(new String[]{"--all", "--output", outputDirectory.resolve("built-in-generator").toString()});
        } catch (IOException e) {
            logger.error("Failed to run built-in data generator", e);
            generatorsFailed++;
        }

        logger.info("Finishing running data generators");
        return generatorsFailed == 0;
    }
}
