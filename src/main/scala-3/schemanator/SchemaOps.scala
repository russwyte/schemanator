package schemanator

import schemanator.generator.*

import zio.schema.*
import zio.json.ast.Json
import zio.json.EncoderOps

/** Extension methods for types with Schema instances.
  *
  * Provides convenient methods to generate JSON Schema representations.
  *
  * Example:
  * {{{
  *   case class Person(name: String, age: Int) derives Schema
  *
  *   val person = Person("Alice", 30)
  *   val jsonAst = person.jsonSchemaAst
  *   val jsonString = person.jsonSchema
  *   val prettyJson = person.jsonSchemaPretty
  * }}}
  */
extension [T](value: T)(using schema: Schema[T])

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
end extension

/** Extension methods for Schema instances.
  *
  * Provides convenient methods to generate JSON Schema representations directly from a Schema.
  *
  * Example:
  * {{{
  *   case class Range(value: Double) derives Schema
  *
  *   val schema = Schema[Range]
  *   val jsonAst = schema.jsonSchemaAst
  *   val jsonString = schema.jsonSchema
  *   val prettyJson = schema.jsonSchemaPretty
  * }}}
  */
extension [T](schema: Schema[T])
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
end extension
