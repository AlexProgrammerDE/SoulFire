# Contributing to SoulFire

Thank you for considering to contribute to SoulFire! Please read the following paragraphs about how to adapt your code
style.

## Code style

This project does not have a strict code style.
However, we will only accept pull requests that use the format specified in `.editorconfig`.
Everything enforced by Spotless is also required.

### Var keyword

SoulFire exclusively uses the var keyword instead of explicitly declaring field types.
That also includes types like `Map<String, String> map = new HashMap<>()`,
those should be abbreviated to `var map = new HashMap<String, String>()`

### IntelliJ inspections

You can also import the recommended inspections at `config/intellij_inspections.xml`
at `Settings -> Editor -> Inspections`.
