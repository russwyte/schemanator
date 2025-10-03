import zio.*
import zio.test.*
import zio.schema.*
import zio.json.ast.Json
import schemanator.*
import schemanator.annotations.*

object ExtensionMethodsSpec extends ZIOSpecDefault {

  case class Person(name: String, age: Int)
  object Person {
    implicit val schema: Schema[Person] = DeriveSchema.gen[Person]
  }

  case class Simple(value: String)
  object Simple {
    implicit val schema: Schema[Simple] = DeriveSchema.gen[Simple]
  }

  case class RangeMin(@minimum(0.0) value: Double)
  object RangeMin {
    implicit val schema: Schema[RangeMin] = DeriveSchema.gen[RangeMin]
  }

  case class RangeMax(@maximum(100.0) value: Double)
  object RangeMax {
    implicit val schema: Schema[RangeMax] = DeriveSchema.gen[RangeMax]
  }

  def spec: Spec[Any, Nothing] = suite("ExtensionMethods")(
    test("extension method jsonSchemaAst generates JSON Schema AST") {
      val person  = Person("Alice", 30)
      val jsonAst = person.jsonSchemaAst

      jsonAst match {
        case Json.Obj(fields) =>
          val hasSchema = fields.exists { case (k, _) => k == "$schema" }
          val hasType   = fields.exists { case (k, v) => k == "type" && v == Json.Str("object") }
          assertTrue(hasSchema && hasType)
        case _ => assertTrue(false)
      }
    },
    test("extension method jsonSchema generates JSON string") {
      val simple     = Simple("test")
      val jsonString = simple.jsonSchema

      assertTrue(
        jsonString.nonEmpty,
        jsonString.contains("\"type\""),
        jsonString.contains("\"object\""),
      )
    },
    test("extension method jsonSchemaPretty generates formatted JSON string") {
      val simple     = Simple("test")
      val prettyJson = simple.jsonSchemaPretty

      assertTrue(
        prettyJson.nonEmpty,
        prettyJson.contains("\n"), // Should have newlines
        prettyJson.contains("\"type\""),
        prettyJson.contains("\"object\""),
      )
    },
    test("Schema extension method jsonSchemaAst generates JSON Schema AST") {
      val schema  = Schema[Person]
      val jsonAst = schema.jsonSchemaAst

      jsonAst match {
        case Json.Obj(fields) =>
          val hasSchema = fields.exists { case (k, _) => k == "$schema" }
          val hasType   = fields.exists { case (k, v) => k == "type" && v == Json.Str("object") }
          assertTrue(hasSchema && hasType)
        case _ => assertTrue(false)
      }
    },
    test("Schema extension method jsonSchema generates JSON string") {
      val schema     = Schema[RangeMin]
      val jsonString = schema.jsonSchema

      assertTrue(
        jsonString.nonEmpty,
        jsonString.contains("\"type\""),
        jsonString.contains("\"object\""),
      )
    },
    test("Schema extension method jsonSchemaPretty generates formatted JSON string") {
      val schema     = Schema[RangeMax]
      val prettyJson = schema.jsonSchemaPretty

      assertTrue(
        prettyJson.nonEmpty,
        prettyJson.contains("\n"), // Should have newlines
        prettyJson.contains("\"type\""),
        prettyJson.contains("\"object\""),
        prettyJson.contains("\"maximum\""),
      )
    },
  )
}
