import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.schema.*
import zio.schema.annotation.*
import zio.json.ast.Json
import schemanator.*
import schemanator.generator.*

object SumTypesSpec extends ZIOSpecDefault:

  enum Status derives Schema:
    case Active
    case Inactive
    case Pending

  sealed trait Shape derives Schema
  case class Circle(radius: Double) extends Shape
  case class Rectangle(width: Double, height: Double) extends Shape
  case class Triangle(base: Double, height: Double) extends Shape

  def spec = suite("SumTypes")(
    test("generates schema for enum") {
      val jsonSchema = Schema[Status].jsonSchemaAst

      // Enums are represented as oneOf with each case
      // Can be either inline or with $defs/$ref
      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasOneOf = fieldsWithoutSchema.exists { case (k, _) => k == "oneOf" }
          val hasRef = fieldsWithoutSchema.exists { case (k, _) => k == "$ref" }
          val hasDefs = fieldsWithoutSchema.exists { case (k, _) => k == "$defs" }

          // Should either have oneOf directly, or have $defs with a Status definition containing oneOf
          if hasRef && hasDefs then
            val defsOpt = fieldsWithoutSchema.collectFirst { case ("$defs", obj: Json.Obj) => obj }
            val statusDefHasOneOf = defsOpt.exists { defs =>
              defs.fields.collectFirst {
                case ("Status", Json.Obj(statusFields)) =>
                  statusFields.exists { case (k, _) => k == "oneOf" }
              }.getOrElse(false)
            }
            assertTrue(statusDefHasOneOf)
          else
            assertTrue(hasOneOf)
        case _ =>
          assertTrue(false)
    },
    test("generates schema for sealed trait ADT") {
      val jsonSchema = Schema[Shape].jsonSchemaAst

      // Sealed traits should use oneOf with each case
      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasOneOf = fieldsWithoutSchema.exists { case (k, _) => k == "oneOf" }

          // Extract the oneOf array
          val oneOfOpt = fieldsWithoutSchema.collectFirst {
            case ("oneOf", arr: Json.Arr) => arr
          }

          val correctSize = oneOfOpt.exists(_.elements.size == 3)

          assertTrue(hasOneOf && oneOfOpt.isDefined && correctSize)
        case _ =>
          assertTrue(false)
    },
    test("generates schema for Either type") {
      val jsonSchema = Schema[Either[String, Int]].jsonSchemaAst

      // Either should use oneOf with left and right cases
      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasOneOf = fieldsWithoutSchema.exists { case (k, _) => k == "oneOf" }

          val oneOfOpt = fieldsWithoutSchema.collectFirst {
            case ("oneOf", arr: Json.Arr) => arr
          }

          val correctSize = oneOfOpt.exists(_.elements.size == 2)

          assertTrue(hasOneOf && oneOfOpt.isDefined && correctSize)
        case _ =>
          assertTrue(false)
    },
    test("supports @discriminatorName annotation on enum") {
      @discriminatorName("kind")
      enum Vehicle derives Schema:
        case Car(make: String)
        case Bike(gears: Int)

      val jsonSchema = Schema[Vehicle].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasDiscriminator = fieldsWithoutSchema.exists {
            case ("discriminator", Json.Obj(discFields)) =>
              discFields.exists {
                case ("propertyName", Json.Str("kind")) => true
                case _ => false
              }
            case _ => false
          }

          assertTrue(hasDiscriminator)
        case _ =>
          assertTrue(false)
    },
  )
end SumTypesSpec
