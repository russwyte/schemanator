import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.schema.*
import zio.json.ast.Json
import schemanator.*
import schemanator.annotations.*

object ExtensionMethodsSpec extends ZIOSpecDefault:

  def spec = suite("ExtensionMethods")(
    test("extension method jsonSchemaAst generates JSON Schema AST") {
      case class Person(name: String, age: Int) derives Schema

      val person = Person("Alice", 30)
      val jsonAst = person.jsonSchemaAst

      jsonAst match
        case Json.Obj(fields) =>
          val hasSchema = fields.exists { case (k, _) => k == "$schema" }
          val hasType = fields.exists { case (k, v) => k == "type" && v == Json.Str("object") }
          assertTrue(hasSchema && hasType)
        case _ => assertTrue(false)
    },
    test("extension method jsonSchema generates JSON string") {
      case class Simple(value: String) derives Schema

      val simple = Simple("test")
      val jsonString = simple.jsonSchema

      assertTrue(
        jsonString.nonEmpty,
        jsonString.contains("\"type\""),
        jsonString.contains("\"object\"")
      )
    },
    test("extension method jsonSchemaPretty generates formatted JSON string") {
      case class Simple(value: String) derives Schema

      val simple = Simple("test")
      val prettyJson = simple.jsonSchemaPretty

      assertTrue(
        prettyJson.nonEmpty,
        prettyJson.contains("\n"),  // Should have newlines
        prettyJson.contains("\"type\""),
        prettyJson.contains("\"object\"")
      )
    },
    test("Schema extension method jsonSchemaAst generates JSON Schema AST") {
      case class Person(name: String, age: Int) derives Schema

      val schema = Schema[Person]
      val jsonAst = schema.jsonSchemaAst

      jsonAst match
        case Json.Obj(fields) =>
          val hasSchema = fields.exists { case (k, _) => k == "$schema" }
          val hasType = fields.exists { case (k, v) => k == "type" && v == Json.Str("object") }
          assertTrue(hasSchema && hasType)
        case _ => assertTrue(false)
    },
    test("Schema extension method jsonSchema generates JSON string") {
      case class Range(@minimum(0.0) value: Double) derives Schema

      val schema = Schema[Range]
      val jsonString = schema.jsonSchema

      assertTrue(
        jsonString.nonEmpty,
        jsonString.contains("\"type\""),
        jsonString.contains("\"object\"")
      )
    },
    test("Schema extension method jsonSchemaPretty generates formatted JSON string") {
      case class Range(@maximum(100.0) value: Double) derives Schema

      val schema = Schema[Range]
      val prettyJson = schema.jsonSchemaPretty

      assertTrue(
        prettyJson.nonEmpty,
        prettyJson.contains("\n"),  // Should have newlines
        prettyJson.contains("\"type\""),
        prettyJson.contains("\"object\""),
        prettyJson.contains("\"maximum\"")
      )
    },
  )
end ExtensionMethodsSpec
