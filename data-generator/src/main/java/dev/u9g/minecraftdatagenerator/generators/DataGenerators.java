package dev.u9g.minecraftdatagenerator.generators;

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
import java.util.ArrayList;
import java.util.List;

public class DataGenerators {

    private static List<IDataGenerator> GENERATORS = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(DataGenerators.class);

    public static void register(IDataGenerator generator) {
        GENERATORS.add(generator);
    }

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

        logger.info("Finishing running data generators");
        return generatorsFailed == 0;
    }

    static {
        register(new BiomesDataGenerator());
        register(new BlockCollisionShapesDataGenerator());
        register(new BlocksDataGenerator());
        register(new EffectsDataGenerator());
        register(new EnchantmentsDataGenerator());
        register(new EntitiesDataGenerator());
        register(new FoodsDataGenerator());
        register(new ItemsDataGenerator());
        register(new ParticlesDataGenerator());
        register(new TintsDataGenerator());
        register(new MaterialsDataGenerator());
//        register(new RecipeDataGenerator()); - On hold until mcdata supports multiple materials for a recipe
        register(new LanguageDataGenerator());
        register(new InstrumentsDataGenerator());
    }
}
