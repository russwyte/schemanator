# Schemanator

A comprehensive JSON Schema generator for ZIO Schema that converts Scala 3 types to [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/schema).

## ✨ Highlights

- 🎯 **Type-Safe Annotations** - Use of Scala 3 context bounds to preserve Schema information in annotations
- 🔧 **Accurate Complex Type Encoding** - Uses `zio-schema-json` to properly encode Lists, Maps, and nested case classes
- 📦 **ZIO Ecosystem Integration** - First-class support for zio-schema with automatic derivation
- 🔄 **Schema Evolution Tracking** - Built-in compatibility analysis for API versioning
- 📝 **JSON Schema 2020-12** - Modern JSON Schema support with extensive validation features

## Features

### Core Functionality
- ✅ **JSON Schema Draft 2020-12** support (~85-90% coverage)
- ✅ **ZIO Schema Integration** - Automatic schema derivation from case classes, enums, and sealed traits
- ✅ **Extension Methods** - Convenient syntax for generating JSON schemas
- ✅ **Schema Evolution** - Track changes between schema versions with compatibility analysis

### Type Support
- **Primitives**: String, Int, Long, Double, Float, Boolean, BigInt, BigDecimal
- **Collections**: List, Vector, Set, Chunk, Map
- **Tuples**: Tuple2, Tuple3, and beyond
- **Optional Fields**: `Option[T]` types
- **Algebraic Data Types**: Enums and sealed traits with discriminator support
- **Either Types**: Left/Right variant handling
- **Recursive Types**: Automatic detection with `$defs` and `$ref`

### Annotations

#### Field Annotations
- `@fieldName("custom_name")` - Custom field naming (e.g., for snake_case APIs)
- `@defaultValue(value)` - **Type-safe default values** (recommended)
- `@fieldDefaultValue(value)` - Default values (type-erased, for compatibility)
- `@transientField` - Exclude fields from schema
- `@requiredField` - Force optional fields to appear in required array (for APIs like OpenAI)
- `@description("text")` - Add documentation to types and fields
- `@readOnly` - Mark fields as read-only
- `@writeOnly` - Mark fields as write-only
- `@deprecated("message")` - Mark fields as deprecated
- `@exampleValues(values*)` - **Type-safe example values** (recommended)
- `@examples(values*)` - Example values (type-erased, for compatibility)

#### String Validation
- `@format("email" | "uri" | "date-time" | ...)` - String format validation
- `@stringEnum("val1", "val2", ...)` - Enum string values
- `@validate(Validation.minLength(n))` - Minimum string length
- `@validate(Validation.maxLength(n))` - Maximum string length
- `@validate(Validation.pattern(regex))` - Pattern matching

#### Numeric Validation
- `@minimum(value)` - Minimum numeric value
- `@maximum(value)` - Maximum numeric value
- `@multipleOf(value)` - Numeric multiple constraint

#### Array Validation
- `@minItems(n)` - Minimum array length
- `@maxItems(n)` - Maximum array length

#### Object Validation
- `@minProperties(n)` - Minimum number of properties
- `@maxProperties(n)` - Maximum number of properties

#### Enum/ADT Support
- `@discriminatorName("field")` - Custom discriminator field name
- `@noDiscriminator` - Disable discriminator for enums

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.russwyte" %% "schemanator" % "0.0.1"
```

## Usage

### Basic Usage

```scala
import schemanator.*
import zio.schema.*

// Define your types
case class Person(
  name: String,
  age: Int,
  email: Option[String]
) derives Schema

// Generate JSON Schema using extension methods
val person = Person("Alice", 30, Some("alice@example.com"))

// Get JSON Schema as AST
val jsonAst = person.jsonSchemaAst

// Get JSON Schema as compact string
val jsonString = person.jsonSchema

// Get JSON Schema as pretty-printed string
val prettyJson = person.jsonSchemaPretty
```

### Working with Schema Instances

```scala
// Generate schema directly from Schema type
val schema = Schema[Person]
val jsonSchema = schema.jsonSchemaPretty
```

### Advanced Features

#### Custom Field Naming

```scala
import zio.schema.annotation.*

case class ApiResponse(
  @fieldName("user_name") userName: String,
  @fieldName("user_id") userId: Int
) derives Schema
```

#### Validation Constraints

```scala
import zio.schema.validation.Validation
import schemanator.annotations.*

case class Username(
  @validate(Validation.minLength(3) && Validation.maxLength(20))
  value: String
) derives Schema

case class Age(
  @minimum(0.0) @maximum(120.0)
  value: Int
) derives Schema
```

#### Type-Safe Default Values and Examples

Schemanator provides type-safe annotations that properly encode complex types:

```scala
import schemanator.annotations.*

case class ServerConfig(
  @defaultValue("localhost") host: String,
  @defaultValue(8080) port: Int,
  @defaultValue(List("http", "https")) protocols: List[String],
  @exampleValues(30, 60, 120) timeoutSeconds: Int
) derives Schema
```

**Why use type-safe annotations?**
- ✅ Complex types (Lists, Maps, case classes) are properly encoded as JSON
- ✅ Type checking at compile time
- ✅ Uses `zio-schema-json` for accurate encoding
- ❌ Non-type-safe alternatives (`@fieldDefaultValue`, `@examples`) use `.toString()` for complex types

```scala
// Type-safe (recommended)
@defaultValue(List(1, 2, 3)) numbers: List[Int]
// Result: "default": [1, 2, 3]

// Type-erased (compatibility only)
@fieldDefaultValue(List(1, 2, 3)) numbers: List[Int]
// Result: "default": "List(1, 2, 3)"  // Not valid JSON!
```

#### String Formats and Enums

```scala
import schemanator.annotations.*

case class Contact(
  @format("email") email: String,
  @format("uri") website: String,
  @stringEnum("active", "inactive", "pending") status: String
) derives Schema
```

#### Required Optional Fields

Some APIs (like OpenAI) require optional fields to be listed in the required array with nullable types. Use `@requiredField` to force an `Option[T]` field into the required list with a nullable type:

```scala
import schemanator.annotations.*

case class OpenAIRequest(
  name: String,
  @requiredField description: Option[String],
  @requiredField tags: Option[List[String]]
) derives Schema

// Generates:
// {
//   "type": "object",
//   "properties": {
//     "name": { "type": "string" },
//     "description": { "type": ["string", "null"] },
//     "tags": { "type": ["array", "null"], "items": { "type": "string" } }
//   },
//   "required": ["name", "description", "tags"]
// }
```

#### Enums with Discriminators

```scala
import zio.schema.annotation.*

@discriminatorName("type")
enum Vehicle derives Schema:
  case Car(make: String, model: String)
  case Bike(gears: Int)

// Generates oneOf with discriminator property
val schema = Schema[Vehicle]
println(schema.jsonSchemaPretty)
```

#### Nested Types

```scala
case class Address(street: String, city: String, zipCode: String)
case class Company(
  name: String,
  address: Address,
  employees: List[Person]
) derives Schema

// Schemas are automatically derived for nested types
val schema = Schema[Company]
```

#### Recursive Types

```scala
case class Tree(value: Int, children: List[Tree]) derives Schema

// Automatically generates $defs and $ref for recursive references
val schema = Schema[Tree]
```

### Schema Evolution

Track changes between schema versions and analyze compatibility:

```scala
import schemanator.evolution.*

case class PersonV1(name: String, age: Int) derives Schema
case class PersonV2(name: String, age: Int, email: Option[String]) derives Schema

val result = SchemaEvolution.compareSchemas(Schema[PersonV1], Schema[PersonV2])

result.changes.foreach(println)
// FieldAdded(email, ..., optional=true)

result.compatibility match
  case CompatibilityType.FullyCompatible => println("Fully compatible!")
  case CompatibilityType.BackwardCompatible => println("Backward compatible")
  case CompatibilityType.ForwardCompatible => println("Forward compatible")
  case CompatibilityType.Breaking => println("Breaking change!")
```

#### Compatibility Types

- **FullyCompatible**: Adding optional fields only
- **BackwardCompatible**: Removing fields (old data works with new schema)
- **ForwardCompatible**: Adding required fields (new data works with old schema)
- **Breaking**: Type changes or mixed additions/removals

### Schema Versioning

```scala
// Include $schema version (default)
val withVersion = JsonSchemaGenerator.fromSchema(schema)

// Omit $schema version
val withoutVersion = JsonSchemaGenerator.fromSchema(schema, includeSchemaVersion = false)
```

## Example Output

```scala
@main def run(): Unit =
  enum Color:
    case Red, Green, Blue

  case class Address(street: String, city: String, zipCode: String)

  case class Person(
    name: String,
    age: Int,
    isEmployed: Boolean,
    address: Address,
    favoriteColor: Color
  ) derives Schema

  val schema = Schema[Person]
  println(schema.jsonSchemaPretty)
```

Produces:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "age": { "type": "integer" },
    "isEmployed": { "type": "boolean" },
    "address": {
      "type": "object",
      "properties": {
        "street": { "type": "string" },
        "city": { "type": "string" },
        "zipCode": { "type": "string" }
      },
      "required": ["street", "city", "zipCode"]
    },
    "favoriteColor": {
      "oneOf": [
        { "type": "object", "properties": { "type": { "const": "Red" } } },
        { "type": "object", "properties": { "type": { "const": "Green" } } },
        { "type": "object", "properties": { "type": { "const": "Blue" } } }
      ],
      "discriminator": { "propertyName": "type" }
    }
  },
  "required": ["name", "age", "isEmployed", "address", "favoriteColor"]
}
```

## Architecture

The library is organized into logical packages:

- `schemanator.annotations` - Custom JSON Schema annotations
  - Type-safe annotations use Scala 3 context bounds to capture schemas
  - Backwards-compatible with ZIO Schema annotations
- `schemanator.generator` - Internal conversion logic
  - Uses `zio-schema-json` for accurate encoding of complex types
  - Fallback mechanisms for type-erased values
- `schemanator.evolution` - Schema evolution tracking
- `schemanator` - Public API and extension methods

### Type-Safe Annotation Design

Schemanator's type-safe annotations use Scala 3's context bounds to preserve type information:

```scala
// Traditional type-erased annotation
case class examples(values: Any*) extends StaticAnnotation

// Type-safe annotation
case class exampleValues[A: Schema](values: A*) extends StaticAnnotation:
  def schema: Schema[A] = summon[Schema[A]]
```

This allows the library to:
1. Capture the `Schema[A]` at the annotation site
2. Use `zio-schema-json` for proper JSON encoding
3. Handle complex types (nested case classes, collections, etc.) correctly
4. Provide compile-time type safety

## Development

```bash
# Compile
sbt compile

# Run tests
sbt test
```

## License

Apache 2.0

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
