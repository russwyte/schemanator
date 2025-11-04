import zio.*
import zio.test.*
import zio.schema.*
import zio.schema.annotation.*
import zio.json.ast.Json
import schemanator.*
import schemanator.annotations.*

object DiagnosticSpec extends ZIOSpecDefault {
  // Define case classes and schemas at object level for Scala 2.13 DeriveSchema.gen compatibility

  case class SimplePerson(name: String, age: Int)
  object SimplePerson {
    implicit val schema: Schema[SimplePerson] = DeriveSchema.gen[SimplePerson]
  }

  case class Document(
      id: String,
      @readOnly createdAt: String,
  )
  object Document {
    implicit val schema: Schema[Document] = DeriveSchema.gen[Document]
  }

  case class ApiPerson(
      @fieldName("user_name") name: String,
      @fieldName("user_age") age: Int,
  )
  object ApiPerson {
    implicit val schema: Schema[ApiPerson] = DeriveSchema.gen[ApiPerson]
  }

  def spec: Spec[Any, Nothing] = suite("Diagnostic Tests")(
    test("simple case class without annotations - Scala 2.13") {
      assertTrue(SimplePerson.schema != null)
    },
    test("case class with readOnly annotation - Scala 2.13") {
      assertTrue(Document.schema != null)
    },
    test("case class with fieldName annotation - Scala 2.13") {
      assertTrue(ApiPerson.schema != null)
    },
    test("field order matches case class definition order") {
      val jsonSchema = Schema[ApiPerson].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val propsOpt = fields.collectFirst { case ("properties", Json.Obj(props)) =>
            props.map(_._1)
          }

          // Verify field order: user_name (from @fieldName), user_age (from @fieldName)
          val fieldOrder    = propsOpt.getOrElse(Chunk.empty)
          val expectedOrder = Chunk("user_name", "user_age")

          assertTrue(fieldOrder == expectedOrder)
        case _ => assertTrue(false)
      }
    },
  )
}
