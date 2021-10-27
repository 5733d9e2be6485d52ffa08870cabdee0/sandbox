# Build Parent Module

This module holds the Maven BOM file parent of all other modules. It's used to configure the project build process.

## Restrict Imports Enforce Rule

We rely on [restrict-imports-enforcer-rule](https://github.com/skuzzle/restrict-imports-enforcer-rule) to restrict some
imports in our code base.

Even though we do not ban a given dependency, some use cases require us to ban a class or package from a dependency.

If you need to add more rules to the plugin configuration, please see
their [README](https://github.com/skuzzle/restrict-imports-enforcer-rule/blob/master/README.md) for instructions.
