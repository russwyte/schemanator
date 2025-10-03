package schemanator

import schemanator.generator._

import zio.schema._
import zio.json.ast.Json
import zio.json.EncoderOps

/** Extension methods for types with Schema instances.
  *
  * Provides convenient methods to generate JSON Schema representations.
  *
  * Example:
  * {{{
  *   case class Person(name: String, age: Int)
  *   object Person {
  *     implicit val schema: Schema[Person] = DeriveSchema.gen[Person]
  *   }
  *
  *   val person = Person("Alice", 30)
  *   val jsonAst = person.jsonSchemaAst
  *   val jsonString = person.jsonSchema
  *   val prettyJson = person.jsonSchemaPretty
  * }}}
  */
object SchemaOps {

  implicit class ValueSchemaOps[T](private val value: T) extends AnyVal {
    /** Generate JSON Schema as a Json AST.
      *
      * @return
      *   JSON Schema representation as zio.json.ast.Json
      */
    def jsonSchemaAst(implicit schema: Schema[T]): Json =
      JsonSchemaGenerator.fromSchema(schema)

    /** Generate JSON Schema as a JSON string (compact format).
      *
      * @return
      *   JSON Schema representation as a compact string
      */
    def jsonSchema(implicit schema: Schema[T]): String =
      jsonSchemaAst.toJson

    /** Generate JSON Schema as a pretty-printed JSON string.
      *
      * @return
      *   JSON Schema representation as a formatted string with indentation
      */
    def jsonSchemaPretty(implicit schema: Schema[T]): String =
      jsonSchemaAst.toJsonPretty
  }

  /** Extension methods for Schema instances.
    *
    * Provides convenient methods to generate JSON Schema representations directly from a Schema.
    *
    * Example:
    * {{{
    *   case class Range(value: Double)
    *   object Range {
    *     implicit val schema: Schema[Range] = DeriveSchema.gen[Range]
    *   }
    *
    *   val schema = Schema[Range]
    *   val jsonAst = schema.jsonSchemaAst
    *   val jsonString = schema.jsonSchema
    *   val prettyJson = schema.jsonSchemaPretty
    * }}}
    */
  implicit class SchemaSchemaOps[T](private val schema: Schema[T]) extends AnyVal {
    /** Generate JSON Schema as a Json AST.
      *
      * @return
      *   JSON Schema representation as zio.json.ast.Json
      */
    def jsonSchemaAst: Json =
      JsonSchemaGenerator.fromSchema(schema)

    /** Generate JSON Schema as a JSON string (compact format).
      *
      * @return
      *   JSON Schema representation as a compact string
      */
    def jsonSchema: String =
      jsonSchemaAst.toJson

    /** Generate JSON Schema as a pretty-printed JSON string.
      *
      * @return
      *   JSON Schema representation as a formatted string with indentation
      */
    def jsonSchemaPretty: String =
      jsonSchemaAst.toJsonPretty
  }
}
