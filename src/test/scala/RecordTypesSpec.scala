import zio._
import zio.test._
import zio.test.Assertion._
import zio.schema._
import zio.schema.annotation._
import zio.json.ast.Json
import schemanator._
import schemanator.generator._

object RecordTypesSpec extends ZIOSpecDefault {

  case class Person(name: String, age: Int, email: Option[String])
  object Person {
    implicit val schema: Schema[Person] = DeriveSchema.gen[Person]
  }

  case class Address(street: String, city: String, zipCode: Int)
  object Address {
    implicit val schema: Schema[Address] = DeriveSchema.gen[Address]
  }

  case class Company(name: String, address: Address, employees: List[Person])
  object Company {
    implicit val schema: Schema[Company] = DeriveSchema.gen[Company]
  }

  case class Tree(value: Int, children: List[Tree])
  object Tree {
    implicit val schema: Schema[Tree] = DeriveSchema.gen[Tree]
  }

  @description("A user account in the system")
  case class User(
    @description("The user's full name") name: String,
    @description("The user's age in years") age: Int
  )
  object User {
    implicit val schema: Schema[User] = DeriveSchema.gen[User]
  }

  def spec: Spec[Any, Nothing] = suite("RecordTypes")(
    test("generates schema for case class with required fields") {
      val jsonSchema = Schema[Person].jsonSchemaAst

      val expected = Json.Obj(
        "type" -> Json.Str("object"),
        "properties" -> Json.Obj(
          "name"  -> Json.Obj("type" -> Json.Str("string")),
          "age"   -> Json.Obj("type" -> Json.Str("integer")),
          "email" -> Json.Obj("type" -> Json.Str("string")),
        ),
        "required" -> Json.Arr(Json.Str("name"), Json.Str("age")),
      )

      jsonSchema match {
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
      }
    },
    test("generates schema for nested case classes") {
      val jsonSchema = Schema[Company].jsonSchemaAst

      val addressSchema = Json.Obj(
        "type" -> Json.Str("object"),
        "properties" -> Json.Obj(
          "street"  -> Json.Obj("type" -> Json.Str("string")),
          "city"    -> Json.Obj("type" -> Json.Str("string")),
          "zipCode" -> Json.Obj("type" -> Json.Str("integer")),
        ),
        "required" -> Json.Arr(Json.Str("street"), Json.Str("city"), Json.Str("zipCode")),
      )

      val personSchema = Json.Obj(
        "type" -> Json.Str("object"),
        "properties" -> Json.Obj(
          "name"  -> Json.Obj("type" -> Json.Str("string")),
          "age"   -> Json.Obj("type" -> Json.Str("integer")),
          "email" -> Json.Obj("type" -> Json.Str("string")),
        ),
        "required" -> Json.Arr(Json.Str("name"), Json.Str("age")),
      )

      val expected = Json.Obj(
        "type" -> Json.Str("object"),
        "properties" -> Json.Obj(
          "name"    -> Json.Obj("type" -> Json.Str("string")),
          "address" -> addressSchema,
          "employees" -> Json.Obj(
            "type"  -> Json.Str("array"),
            "items" -> personSchema,
          ),
        ),
        "required" -> Json.Arr(Json.Str("name"), Json.Str("address"), Json.Str("employees")),
      )

      jsonSchema match {
        case Json.Obj(fields) =>
          val withoutSchema = Json.Obj(fields.filter(_._1 != "$schema")*)
          assertTrue(withoutSchema == expected)
        case _ => assertTrue(false)
      }
    },
    test("generates schema for recursive types") {
      val jsonSchema = Schema[Tree].jsonSchemaAst

      // Should have $defs and $ref
      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasDefs = fieldsWithoutSchema.exists { case (k, _) => k == "$defs" }
          val hasRef = fieldsWithoutSchema.exists { case (k, _) => k == "$ref" }

          assertTrue(hasDefs && hasRef)

          // Check that Tree definition exists
          val defsOpt = fieldsWithoutSchema.collectFirst {
            case ("$defs", obj: Json.Obj) => obj
          }

          val hasTreeDef = defsOpt.exists(_.fields.exists { case (k, _) => k == "Tree" })
          assertTrue(hasTreeDef)
        case _ =>
          assertTrue(false)
      }
    },
    test("generates schema with description annotations") {
      val jsonSchema = Schema[User].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          // Check for description at the type level
          val hasDescription = fieldsWithoutSchema.exists { case (k, v) =>
            k == "description" && v == Json.Str("A user account in the system")
          }

          // Check for field descriptions in properties
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasFieldDescriptions = propsOpt.exists { props =>
            props.fields.exists {
              case ("name", Json.Obj(nameFields)) =>
                nameFields.exists { case (k, v) =>
                  k == "description" && v == Json.Str("The user's full name")
                }
              case _ => false
            } && props.fields.exists {
              case ("age", Json.Obj(ageFields)) =>
                ageFields.exists { case (k, v) =>
                  k == "description" && v == Json.Str("The user's age in years")
                }
              case _ => false
            }
          }

          assertTrue(hasDescription && hasFieldDescriptions)
        case _ =>
          assertTrue(false)
      }
    },
  )
}
