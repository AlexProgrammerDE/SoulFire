# Contributing to SoulFire

Thank you for considering to contribute to SoulFire! Please read the following paragraphs about how to adapt your code style.

## Code style

This project uses the google-java-format loosely. While most recommended options are enforced, some others are not.
As a rule of thumb, if your IDE spits the same code out with the "Reformat" task, it should be fine.

To setup the google java format, please read the official page and follow the instructions for your IDE:
https://github.com/google/google-java-format

### Var keyword

SoulFire exclusively uses the var keyword instead of explicitly declaring field types.
That also includes types like `Map<String, String> map = new HashMap<>()`,
those should be abbreviated to `var map = new HashMap<String, String>()`

### IntelliJ inspections

If you use IntelliJ you can import the file `config/checkstyle/checkstyle.xml` under `Settings -> Editor -> Code Style`.
(Requires the Checkstyle plugin to be installed)
You can also import the recommended inspections at `config/intellij_inspections.xml` at `Settings -> Editor -> Inspections`.
