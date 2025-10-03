import zio._
import zio.test._
import zio.test.Assertion._
import zio.schema._
import zio.schema.annotation._
import zio.json.ast.Json
import schemanator._
import schemanator.generator._

object SumTypesSpec extends ZIOSpecDefault {

  sealed trait Status
  object Status {
    case object Active extends Status
    case object Inactive extends Status
    case object Pending extends Status
    implicit val schema: Schema[Status] = DeriveSchema.gen[Status]
  }

  sealed trait Shape
  case class Circle(radius: Double) extends Shape
  case class Rectangle(width: Double, height: Double) extends Shape
  case class Triangle(base: Double, height: Double) extends Shape
  object Shape {
    implicit val schema: Schema[Shape] = DeriveSchema.gen[Shape]
  }

  @discriminatorName("kind")
  sealed trait Vehicle
  case class Car(make: String) extends Vehicle
  case class Bike(gears: Int) extends Vehicle
  object Vehicle {
    implicit val schema: Schema[Vehicle] = DeriveSchema.gen[Vehicle]
  }

  def spec: Spec[Any, Nothing] = suite("SumTypes")(
    test("generates schema for enum") {
      val jsonSchema = Schema[Status].jsonSchemaAst

      // Enums are represented as oneOf with each case
      // Can be either inline or with $defs/$ref
      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasOneOf = fieldsWithoutSchema.exists { case (k, _) => k == "oneOf" }
          val hasRef = fieldsWithoutSchema.exists { case (k, _) => k == "$ref" }
          val hasDefs = fieldsWithoutSchema.exists { case (k, _) => k == "$defs" }

          // Should either have oneOf directly, or have $defs with a Status definition containing oneOf
          if (hasRef && hasDefs) {
            val defsOpt = fieldsWithoutSchema.collectFirst { case ("$defs", obj: Json.Obj) => obj }
            val statusDefHasOneOf = defsOpt.exists { defs =>
              defs.fields.collectFirst {
                case ("Status", Json.Obj(statusFields)) =>
                  statusFields.exists { case (k, _) => k == "oneOf" }
              }.getOrElse(false)
            }
            assertTrue(statusDefHasOneOf)
          } else {
            assertTrue(hasOneOf)
          }
        case _ =>
          assertTrue(false)
      }
    },
    test("generates schema for sealed trait ADT") {
      val jsonSchema = Schema[Shape].jsonSchemaAst

      // Sealed traits should use oneOf with each case
      jsonSchema match {
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
      }
    },
    test("generates schema for Either type") {
      val jsonSchema = Schema[Either[String, Int]].jsonSchemaAst

      // Either should use oneOf with left and right cases
      jsonSchema match {
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
      }
    },
    test("supports @discriminatorName annotation on enum") {
      val jsonSchema = Schema[Vehicle].jsonSchemaAst

      jsonSchema match {
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
      }
    },
  )
}
