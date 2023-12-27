package net.pistonmaster.serverwrecker.generator.generators;

public interface IDataGenerator {
    String getDataName();

    Object generateDataJson();
}
