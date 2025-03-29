package com.soulfiremc.doclet.tsdoclet;

import com.soulfiremc.doclet.options.OutputDirectoryOption;
import com.soulfiremc.doclet.options.VersionOption;
import jdk.javadoc.doclet.DocletEnvironment;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TypeScriptGenerator {
  private final DocletEnvironment environment;
  private final File outputDirectory;
  private final String version;
  private final Map<String, String> typeMap;

  public TypeScriptGenerator(DocletEnvironment environment) {
    this.environment = environment;
    this.outputDirectory = OutputDirectoryOption.outputDir;
    this.version = VersionOption.version;
    this.typeMap = createTypeMap();
  }

  private Map<String, String> createTypeMap() {
    Map<String, String> map = new HashMap<>();
    // Primitive mappings
    map.put("boolean", "boolean");
    map.put("byte", "number");
    map.put("short", "number");
    map.put("int", "number");
    map.put("long", "number");
    map.put("float", "number");
    map.put("double", "number");
    map.put("char", "string");
    map.put("void", "void");
    map.put("String", "string");
    // Collection types
    map.put("java.util.List", "Array");
    map.put("java.util.Set", "Set");
    map.put("java.util.Map", "Map");
    map.put("java.util.Collection", "Array");
    // Other common types
    map.put("java.util.Optional", "Optional");
    map.put("java.lang.Object", "any");
    return map;
  }

  public void generate() throws IOException {
    if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
      throw new IOException("Failed to create output directory: " + outputDirectory);
    }

    // Generate index file with version info
    generateIndexFile();

    // Process all classes
    for (var classElement : ElementFilter.typesIn(environment.getIncludedElements())) {
      if (shouldProcess(classElement)) {
        generateTypeScriptDefinition(classElement);
      }
    }
  }

  private boolean shouldProcess(TypeElement element) {
    // Skip private classes, annotations, and anonymous classes
    return element.getModifiers().contains(Modifier.PUBLIC)
      && element.getKind() != ElementKind.ANNOTATION_TYPE
      && !element.getQualifiedName().toString().contains("$");
  }

  private void generateIndexFile() throws IOException {
    var indexFile = new File(outputDirectory, "index.ts");
    try (var writer = new PrintWriter(new FileWriter(indexFile))) {
      writer.println("/**");
      writer.println(" * TypeScript definitions generated from Java API");
      writer.println(" * Version: " + version);
      writer.println(" * Generated on: " + new Date());
      writer.println(" */");
      writer.println();
      writer.println("export * from './types';");

      // Add re-exports for all generated files
      var files = outputDirectory.listFiles((ignored, name) -> name.endsWith(".ts") && !name.equals("index.ts"));
      if (files != null) {
        for (var file : files) {
          var name = file.getName().replace(".ts", "");
          writer.println("export * from './" + name + "';");
        }
      }
    }
  }

  private void generateTypeScriptDefinition(TypeElement classElement) throws IOException {
    var className = classElement.getSimpleName().toString();
    var fileName = className + ".ts";
    var outputFile = new File(outputDirectory, fileName);

    try (var writer = new PrintWriter(new FileWriter(outputFile))) {
      // Add file header with documentation
      writer.println("/**");
      writer.println(" * TypeScript definition for " + classElement.getQualifiedName());
      writer.println(" */");

      // Add imports if needed
      Set<String> imports = new HashSet<>();
      gatherImports(classElement, imports);
      for (var importType : imports) {
        writer.println("import { " + importType + " } from './" + importType + "';");
      }

      if (!imports.isEmpty()) {
        writer.println();
      }

      // Generate interface or type definition based on class kind
      if (classElement.getKind() == ElementKind.ENUM) {
        generateEnum(classElement, writer);
      } else if (classElement.getKind() == ElementKind.INTERFACE) {
        generateInterface(classElement, writer);
      } else {
        generateClass(classElement, writer);
      }
    }
  }

  private void gatherImports(TypeElement classElement, Set<String> imports) {
    // Add imports for superclass, interfaces, and field types
    var superclass = classElement.getSuperclass();
    if (superclass.getKind() != TypeKind.NONE && !superclass.toString().equals("java.lang.Object")) {
      var superType = (DeclaredType) superclass;
      var superName = superType.asElement().getSimpleName().toString();
      if (!typeMap.containsKey(superName)) {
        imports.add(superName);
      }
    }

    // Add imports for interfaces
    for (var interfaceType : classElement.getInterfaces()) {
      var declaredType = (DeclaredType) interfaceType;
      var interfaceName = declaredType.asElement().getSimpleName().toString();
      if (!typeMap.containsKey(interfaceName)) {
        imports.add(interfaceName);
      }
    }

    // Add imports for field types
    for (var field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
      if (!field.getModifiers().contains(Modifier.PUBLIC)) {
        continue;
      }

      var fieldType = field.asType();
      if (fieldType.getKind() == TypeKind.DECLARED) {
        var declaredType = (DeclaredType) fieldType;
        var typeName = declaredType.asElement().getSimpleName().toString();
        if (!typeMap.containsKey(typeName) && !typeName.equals(classElement.getSimpleName().toString())) {
          imports.add(typeName);
        }
      }
    }
  }

  private void generateEnum(TypeElement enumElement, PrintWriter writer) {
    writer.println("export enum " + enumElement.getSimpleName() + " {");

    var enumConstants = ElementFilter.fieldsIn(enumElement.getEnclosedElements())
      .stream()
      .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
      .toList();

    for (var i = 0; i < enumConstants.size(); i++) {
      var constant = enumConstants.get(i);
      writer.println("  " + constant.getSimpleName() + " = \"" + constant.getSimpleName() + "\"" +
        (i < enumConstants.size() - 1 ? "," : ""));
    }

    writer.println("}");
  }

  private void generateInterface(TypeElement interfaceElement, PrintWriter writer) {
    writer.println("export interface " + interfaceElement.getSimpleName() + " {");

    // Add methods
    for (var method : ElementFilter.methodsIn(interfaceElement.getEnclosedElements())) {
      if (!method.getModifiers().contains(Modifier.PUBLIC)) {
        continue;
      }

      var returnType = convertType(method.getReturnType());
      var methodName = method.getSimpleName().toString();

      writer.print("  " + methodName + "(");
      var parameters = method.getParameters();
      for (var i = 0; i < parameters.size(); i++) {
        var param = parameters.get(i);
        writer.print(param.getSimpleName() + ": " + convertType(param.asType()));
        if (i < parameters.size() - 1) {
          writer.print(", ");
        }
      }
      writer.println("): " + returnType + ";");
    }

    writer.println("}");
  }

  private void generateClass(TypeElement classElement, PrintWriter writer) {
    // Check if it's an abstract class
    writer.print("export interface " + classElement.getSimpleName());

    // Add extends clause for superclass
    var superclass = classElement.getSuperclass();
    if (superclass.getKind() != TypeKind.NONE && !superclass.toString().equals("java.lang.Object")) {
      var superType = (DeclaredType) superclass;
      var superName = superType.asElement().getSimpleName().toString();
      writer.print(" extends " + superName);
    }

    // Add implements clause for interfaces
    var interfaces = classElement.getInterfaces();
    if (!interfaces.isEmpty()) {
      writer.print(superclass.getKind() != TypeKind.NONE ? ", " : " extends ");
      for (var i = 0; i < interfaces.size(); i++) {
        var interfaceType = (DeclaredType) interfaces.get(i);
        var interfaceName = interfaceType.asElement().getSimpleName().toString();
        writer.print(interfaceName);
        if (i < interfaces.size() - 1) {
          writer.print(", ");
        }
      }
    }

    writer.println(" {");

    // Add fields
    for (var field : ElementFilter.fieldsIn(classElement.getEnclosedElements())) {
      if (!field.getModifiers().contains(Modifier.PUBLIC) ||
        field.getKind() == ElementKind.ENUM_CONSTANT) {
        continue;
      }

      writer.println("  " + field.getSimpleName() +
        (field.getModifiers().contains(Modifier.FINAL) ? ": " : "?: ") +
        convertType(field.asType()) + ";");
    }

    // Add methods (only if they are not already in parent interfaces)
    for (var method : ElementFilter.methodsIn(classElement.getEnclosedElements())) {
      if (!method.getModifiers().contains(Modifier.PUBLIC) ||
        isConstructor(method) ||
        isOverride(method, classElement)) {
        continue;
      }

      var returnType = convertType(method.getReturnType());
      var methodName = method.getSimpleName().toString();

      writer.print("  " + methodName + "(");
      var parameters = method.getParameters();
      for (var i = 0; i < parameters.size(); i++) {
        var param = parameters.get(i);
        writer.print(param.getSimpleName() + ": " + convertType(param.asType()));
        if (i < parameters.size() - 1) {
          writer.print(", ");
        }
      }
      writer.println("): " + returnType + ";");
    }

    writer.println("}");
  }

  private boolean isConstructor(ExecutableElement method) {
    return method.getSimpleName().toString().equals("<init>");
  }

  private boolean isOverride(ExecutableElement method, TypeElement classElement) {
    var methodName = method.getSimpleName().toString();
    var parameters = method.getParameters();
    var paramTypes = parameters.stream()
      .map(param -> param.asType().toString())
      .toList();

    // Check superclass
    var superclass = classElement.getSuperclass();
    if (superclass.getKind() != TypeKind.NONE) {
      var superElement = (TypeElement) ((DeclaredType) superclass).asElement();
      if (hasMatchingMethod(superElement, methodName, paramTypes)) {
        return true;
      }
    }

    // Check interfaces
    for (var interfaceType : classElement.getInterfaces()) {
      var interfaceElement = (TypeElement) ((DeclaredType) interfaceType).asElement();
      if (hasMatchingMethod(interfaceElement, methodName, paramTypes)) {
        return true;
      }
    }

    return false;
  }

  private boolean hasMatchingMethod(TypeElement typeElement, String methodName, List<String> paramTypes) {
    for (var method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
      if (method.getSimpleName().toString().equals(methodName)) {
        var parameters = method.getParameters();
        if (parameters.size() == paramTypes.size()) {
          var match = true;
          for (var i = 0; i < parameters.size(); i++) {
            if (!parameters.get(i).asType().toString().equals(paramTypes.get(i))) {
              match = false;
              break;
            }
          }
          if (match) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private String convertType(TypeMirror type) {
    var typeName = type.toString();

    // Handle primitive types
    if (type.getKind().isPrimitive()) {
      return typeMap.getOrDefault(typeName, "any");
    }

    // Handle arrays
    if (type.getKind() == TypeKind.ARRAY) {
      var componentType = convertType(((ArrayType) type).getComponentType());
      return componentType + "[]";
    }

    // Handle generic types
    if (type.getKind() == TypeKind.DECLARED) {
      var declaredType = (DeclaredType) type;
      var element = (TypeElement) declaredType.asElement();
      var baseType = element.getSimpleName().toString();

      // Map known Java types to TypeScript
      var mappedType = typeMap.getOrDefault(element.getQualifiedName().toString(), baseType);

      // Handle generic parameters
      if (!declaredType.getTypeArguments().isEmpty()) {
        return mapGenericType(mappedType, declaredType.getTypeArguments());
      }

      return mappedType;
    }

    // Default to any for unknown types
    return "any";
  }

  private String mapGenericType(String baseType, List<? extends TypeMirror> typeArguments) {
    // Special case for common collections
    switch (baseType) {
      case "Array", "Set" -> {
        return baseType + "<" + convertType(typeArguments.getFirst()) + ">";
      }
      case "Map" -> {
        return baseType + "<" + convertType(typeArguments.get(0)) + ", " +
          convertType(typeArguments.get(1)) + ">";
      }
      case "Optional" -> {
        return convertType(typeArguments.getFirst()) + " | null";
      }
    }

    // Generic handler for other types
    var sb = new StringBuilder(baseType).append("<");
    for (var i = 0; i < typeArguments.size(); i++) {
      sb.append(convertType(typeArguments.get(i)));
      if (i < typeArguments.size() - 1) {
        sb.append(", ");
      }
    }
    sb.append(">");
    return sb.toString();
  }
}
