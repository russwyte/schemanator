package schemanator.generator

import zio.*
import zio.schema.*
import zio.json.ast.Json

/** JSON Schema Generator for ZIO Schema.
  *
  * Converts ZIO Schema types to JSON Schema Draft 2020-12 format.
  *
  * Example:
  * {{{
  *   case class Person(name: String, age: Int) derives Schema
  *
  *   val schema = Schema[Person]
  *   val jsonSchema = JsonSchemaGenerator.fromSchema(schema)
  * }}}
  */
object JsonSchemaGenerator {

  /** Convert a ZIO Schema to JSON Schema with version included by default.
    *
    * @param schema
    *   The ZIO Schema to convert
    * @return
    *   JSON Schema representation
    */
  def fromSchema[A](schema: Schema[A]): Json =
    fromSchema(schema, includeSchemaVersion = true)

  /** Convert a ZIO Schema to JSON Schema with optional version.
    *
    * @param schema
    *   The ZIO Schema to convert
    * @param includeSchemaVersion
    *   Whether to include the $schema property
    * @return
    *   JSON Schema representation
    */
  def fromSchema[A](schema: Schema[A], includeSchemaVersion: Boolean): Json = {
    val ctx        = TypeConverters.Context()
    val mainSchema = TypeConverters.schemaToJsonSchema(schema, ctx)

    val baseSchema =
      if (ctx.definitions.isEmpty) mainSchema
      else {
        // Check if mainSchema is a $ref or an inline schema
        mainSchema match {
          case Json.Obj(fields) if fields.exists(_._1 == "$ref") =>
            // mainSchema is a reference, extract it
            val refValue = fields.collectFirst { case ("$ref", ref) => ref }.get
            // Preserve field order: $defs, $ref
            Json.Obj(
              Chunk(
                "$defs" -> Json.Obj(ctx.definitions.toSeq*),
                "$ref"  -> refValue
              )*
            )
          case _ =>
            // mainSchema is inline, merge with $defs
            mainSchema match {
              case Json.Obj(fields) =>
                // Prepend $defs to existing fields
                Json.Obj((Chunk(("$defs", Json.Obj(ctx.definitions.toSeq*))) ++ fields)*)
              case other =>
                // Shouldn't happen, but fallback
                Json.Obj(
                  Chunk(
                    "$defs"  -> Json.Obj(ctx.definitions.toSeq*),
                    "schema" -> other
                  )*
                )
            }
        }
      }

    // Add $schema property to indicate JSON Schema Draft 2020-12
    if (includeSchemaVersion)
      baseSchema match {
        case Json.Obj(fields) =>
          // Prepend $schema as first field
          Json.Obj((Chunk(("$schema", Json.Str("https://json-schema.org/draft/2020-12/schema"))) ++ fields)*)
        case other => other
      }
    else baseSchema
  }
}
