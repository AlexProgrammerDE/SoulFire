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
            new BlockCollisionShapesDataGenerator(),
            new BlocksDataGenerator(),
            new EffectsDataGenerator(),
            new EnchantmentsDataGenerator(),
            new EntitiesDataGenerator(),
            new FoodsDataGenerator(),
            new ItemsDataGenerator(),
            new LanguageDataGenerator(),
            new InstrumentsDataGenerator(),
            new TagsDataGenerator()
    );
    private static final Logger logger = LoggerFactory.getLogger(DataGenerators.class);

    public static boolean runDataGenerators(Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException exception) {
            logger.error("Failed to create data generator output directory at {}", outputDirectory, exception);
            return false;
        }

        int generatorsFailed = 0;
        logger.info("Running minecraft data generators, output at {}", outputDirectory);

        for (IDataGenerator dataGenerator : GENERATORS) {
            logger.info("Running generator {}", dataGenerator.getDataName());
            try {
                String outputFileName = String.format("%s.json", dataGenerator.getDataName());
                JsonElement outputElement = dataGenerator.generateDataJson();
                Path outputFilePath = outputDirectory.resolve(outputFileName);

                try (Writer writer = Files.newBufferedWriter(outputFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    JsonWriter jsonWriter = new JsonWriter(writer);
                    jsonWriter.setIndent("  ");
                    Streams.write(outputElement, jsonWriter);
                }
                logger.info("Generator: {} -> {}", dataGenerator.getDataName(), outputFileName);

            } catch (Throwable exception) {
                logger.error("Failed to run data generator {}", dataGenerator.getDataName(), exception);
                generatorsFailed++;
            }
        }

        logger.info("Running built-in data generator");
        try {
            net.minecraft.data.Main.main(new String[] {"--all", "--output", outputDirectory.resolve("built-in-generator").toString()});
        } catch (IOException e) {
            logger.error("Failed to run built-in data generator", e);
            generatorsFailed++;
        }

        logger.info("Finishing running data generators");
        return generatorsFailed == 0;
    }
}
