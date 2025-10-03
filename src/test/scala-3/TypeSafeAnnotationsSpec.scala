import zio.*
import zio.test.*
import zio.schema.*
import zio.json.ast.Json
import schemanator.*
import schemanator.annotations.*

object TypeSafeAnnotationsSpec extends ZIOSpecDefault {

  def spec = suite("TypeSafeAnnotations")(
    suite("@defaultValue")(
    test("supports @defaultValue with primitive types") {
      case class Config(
        @defaultValue(42) port: Int,
        @defaultValue("localhost") host: String
      ) derives Schema

      val jsonSchema = Schema[Config].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasCorrectDefaults = propsOpt.exists { props =>
            val portHasDefault = props.fields.exists {
              case ("port", Json.Obj(portFields)) =>
                portFields.exists {
                  case ("default", Json.Num(value)) => value.intValue == 42
                  case _ => false
                }
              case _ => false
            }

            val hostHasDefault = props.fields.exists {
              case ("host", Json.Obj(hostFields)) =>
                hostFields.exists {
                  case ("default", Json.Str(value)) => value == "localhost"
                  case _ => false
                }
              case _ => false
            }

            portHasDefault && hostHasDefault
          }

          assertTrue(hasCorrectDefaults)
        case _ => assertTrue(false)
      }
    },
    test("supports @defaultValue with complex types") {
      case class ServerConfig(
        @defaultValue(List("http", "https")) protocols: List[String],
        @defaultValue(Map("timeout" -> 30, "retries" -> 3)) settings: Map[String, Int]
      ) derives Schema

      val jsonSchema = Schema[ServerConfig].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasProtocolsDefault = propsOpt.exists { props =>
            props.fields.exists {
              case ("protocols", Json.Obj(protocolFields)) =>
                protocolFields.exists {
                  case ("default", Json.Arr(vals)) =>
                    vals.size == 2 &&
                    vals(0) == Json.Str("http") &&
                    vals(1) == Json.Str("https")
                  case _ => false
                }
              case _ => false
            }
          }

          val hasSettingsDefault = propsOpt.exists { props =>
            props.fields.exists {
              case ("settings", Json.Obj(settingsFields)) =>
                settingsFields.exists {
                  case ("default", Json.Obj(obj)) =>
                    obj.exists { case ("timeout", Json.Num(n)) => n.intValue == 30; case _ => false } &&
                    obj.exists { case ("retries", Json.Num(n)) => n.intValue == 3; case _ => false }
                  case _ => false
                }
              case _ => false
            }
          }

          assertTrue(hasProtocolsDefault && hasSettingsDefault)
        case _ => assertTrue(false)
      }
    },
    test("supports @defaultValue with nested case classes") {
      case class Address(city: String, country: String) derives Schema
      case class Person(
        name: String,
        @defaultValue(Address("New York", "USA")) address: Address
      ) derives Schema

      val jsonSchema = Schema[Person].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasAddressDefault = propsOpt.exists { props =>
            props.fields.exists {
              case ("address", Json.Obj(addressFields)) =>
                addressFields.exists {
                  case ("default", Json.Obj(defaultObj)) =>
                    defaultObj.exists { case ("city", Json.Str("New York")) => true; case _ => false } &&
                    defaultObj.exists { case ("country", Json.Str("USA")) => true; case _ => false }
                  case _ => false
                }
              case _ => false
            }
          }

          assertTrue(hasAddressDefault)
        case _ => assertTrue(false)
      }
    },
    ),
    suite("@exampleValues")(
      test("supports @exampleValues with primitive types") {
        case class Color(
          @exampleValues("red", "green", "blue") name: String
        ) derives Schema

        val jsonSchema = Schema[Color].jsonSchemaAst

        jsonSchema match {
          case Json.Obj(fields) =>
            val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
            val hasExamples = fieldsWithoutSchema.collectFirst {
              case ("properties", Json.Obj(props)) =>
                props.exists {
                  case ("name", Json.Obj(fieldProps)) =>
                    fieldProps.exists {
                      case ("examples", Json.Arr(vals)) =>
                        vals.size == 3 &&
                        vals(0) == Json.Str("red") &&
                        vals(1) == Json.Str("green") &&
                        vals(2) == Json.Str("blue")
                      case _ => false
                    }
                  case _ => false
                }
            }.getOrElse(false)

            assertTrue(hasExamples)
          case _ => assertTrue(false)
        }
      },
      test("supports @exampleValues with complex types") {
        case class ApiEndpoint(
          @exampleValues(List(200, 201), List(400, 404), List(500)) responses: List[Int]
        ) derives Schema

        val jsonSchema = Schema[ApiEndpoint].jsonSchemaAst

        jsonSchema match {
          case Json.Obj(fields) =>
            val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
            val hasExamples = fieldsWithoutSchema.collectFirst {
              case ("properties", Json.Obj(props)) =>
                props.exists {
                  case ("responses", Json.Obj(fieldProps)) =>
                    fieldProps.exists {
                      case ("examples", Json.Arr(vals)) =>
                        vals.size == 3 &&
                        vals(0) == Json.Arr(Json.Num(200), Json.Num(201)) &&
                        vals(1) == Json.Arr(Json.Num(400), Json.Num(404)) &&
                        vals(2) == Json.Arr(Json.Num(500))
                      case _ => false
                    }
                  case _ => false
                }
            }.getOrElse(false)

            assertTrue(hasExamples)
          case _ => assertTrue(false)
        }
      },
      test("supports @exampleValues with nested case classes") {
        case class Point(x: Int, y: Int) derives Schema
        case class Shape(
          @exampleValues(Point(0, 0), Point(10, 20)) vertices: Point
        ) derives Schema

        val jsonSchema = Schema[Shape].jsonSchemaAst

        jsonSchema match {
          case Json.Obj(fields) =>
            val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
            val hasExamples = fieldsWithoutSchema.collectFirst {
              case ("properties", Json.Obj(props)) =>
                props.exists {
                  case ("vertices", Json.Obj(fieldProps)) =>
                    fieldProps.exists {
                      case ("examples", Json.Arr(vals)) =>
                        vals.size == 2 &&
                        vals(0).isInstanceOf[Json.Obj] &&
                        vals(1).isInstanceOf[Json.Obj]
                      case _ => false
                    }
                  case _ => false
                }
            }.getOrElse(false)

            assertTrue(hasExamples)
          case _ => assertTrue(false)
        }
      },
    ),
  )
}
