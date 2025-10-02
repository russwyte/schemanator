import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.schema.*
import zio.json.ast.Json
import schemanator.*
import schemanator.generator.*

object BasicTypesSpec extends ZIOSpecDefault:

  def spec = suite("BasicTypes")(
    test("includes JSON Schema Draft 2020-12 version by default") {
      case class Simple(name: String) derives Schema
      val jsonSchema = Schema[Simple].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val hasSchemaVersion = fields.headOption.exists {
            case ("$schema", Json.Str(value)) => value == "https://json-schema.org/draft/2020-12/schema"
            case _ => false
          }
          assertTrue(hasSchemaVersion)
        case _ =>
          assertTrue(false)
    },
    test("can omit schema version when requested") {
      case class Simple(name: String) derives Schema
      val jsonSchema = JsonSchemaGenerator.fromSchema(Schema[Simple], includeSchemaVersion = false)

      jsonSchema match
        case Json.Obj(fields) =>
          val hasNoSchemaVersion = !fields.exists {
            case ("$schema", _) => true
            case _ => false
          }
          assertTrue(hasNoSchemaVersion)
        case _ =>
          assertTrue(false)
    },
    test("generates schema for primitive String type") {
      val jsonSchema = Schema[String].jsonSchemaAst

      // Strip $schema field for comparison
      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == Json.Obj("type" -> Json.Str("string")))
        case _ => assertTrue(false)
    },
    test("generates schema for primitive Int type") {
      val jsonSchema = Schema[Int].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == Json.Obj("type" -> Json.Str("integer")))
        case _ => assertTrue(false)
    },
    test("generates schema for primitive Boolean type") {
      val jsonSchema = Schema[Boolean].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == Json.Obj("type" -> Json.Str("boolean")))
        case _ => assertTrue(false)
    },
    test("generates schema for primitive Double type") {
      val jsonSchema = Schema[Double].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == Json.Obj("type" -> Json.Str("number")))
        case _ => assertTrue(false)
    },
    test("generates schema for List") {
      val jsonSchema = Schema[List[String]].jsonSchemaAst

      val expected = Json.Obj(
        "type"  -> Json.Str("array"),
        "items" -> Json.Obj("type" -> Json.Str("string")),
      )

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
    },
    test("generates schema for Set") {
      val jsonSchema = Schema[Set[Int]].jsonSchemaAst

      val expected = Json.Obj(
        "type"        -> Json.Str("array"),
        "items"       -> Json.Obj("type" -> Json.Str("integer")),
        "uniqueItems" -> Json.Bool(true),
      )

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
    },
    test("generates schema for Map") {
      val jsonSchema = Schema[Map[String, Int]].jsonSchemaAst

      val expected = Json.Obj(
        "type"                 -> Json.Str("object"),
        "additionalProperties" -> Json.Obj("type" -> Json.Str("integer")),
      )

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
    },
    test("generates schema for Tuple2") {
      val jsonSchema = Schema[(String, Int)].jsonSchemaAst

      val expected = Json.Obj(
        "type"  -> Json.Str("array"),
        "prefixItems" -> Json.Arr(
          Json.Obj("type" -> Json.Str("string")),
          Json.Obj("type" -> Json.Str("integer"))
        ),
        "items" -> Json.Bool(false),
        "minItems" -> Json.Num(2),
        "maxItems" -> Json.Num(2)
      )

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
    },
    test("generates schema for Tuple3") {
      val jsonSchema = Schema[(String, Int, Boolean)].jsonSchemaAst

      val expected = Json.Obj(
        "type"  -> Json.Str("array"),
        "prefixItems" -> Json.Arr(
          Json.Obj("type" -> Json.Str("string")),
          Json.Obj("type" -> Json.Str("integer")),
          Json.Obj("type" -> Json.Str("boolean"))
        ),
        "items" -> Json.Bool(false),
        "minItems" -> Json.Num(3),
        "maxItems" -> Json.Num(3)
      )

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
    },
    test("generates schema for Vector") {
      val jsonSchema = Schema[Vector[String]].jsonSchemaAst

      val expected = Json.Obj(
        "type"  -> Json.Str("array"),
        "items" -> Json.Obj("type" -> Json.Str("string"))
      )

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
    },
    test("generates schema for Chunk") {
      val jsonSchema = Schema[zio.Chunk[Int]].jsonSchemaAst

      val expected = Json.Obj(
        "type"  -> Json.Str("array"),
        "items" -> Json.Obj("type" -> Json.Str("integer"))
      )

      jsonSchema match
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
    },
  )
end BasicTypesSpec
