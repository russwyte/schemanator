import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.schema.*
import zio.schema.annotation.*
import zio.schema.validation.Validation
import zio.json.ast.Json
import schemanator.*
import schemanator.generator.*
import schemanator.annotations.*

object AnnotationsSpec extends ZIOSpecDefault:

  def spec = suite("Annotations")(
    test("supports @fieldName annotation") {
      case class ApiPerson(
        @fieldName("user_name") name: String,
        @fieldName("user_age") age: Int
      ) derives Schema

      val jsonSchema = Schema[ApiPerson].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasCorrectNames = propsOpt.exists { props =>
            props.fields.exists(_._1 == "user_name") &&
            props.fields.exists(_._1 == "user_age") &&
            !props.fields.exists(_._1 == "name") &&
            !props.fields.exists(_._1 == "age")
          }

          assertTrue(hasCorrectNames)
        case _ =>
          assertTrue(false)
    },
    test("supports @fieldDefaultValue annotation") {
      case class PersonWithDefaults(
        name: String,
        @fieldDefaultValue(18) age: Int
      ) derives Schema

      val jsonSchema = Schema[PersonWithDefaults].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasDefault = propsOpt.exists { props =>
            props.fields.collectFirst {
              case ("age", Json.Obj(ageFields)) =>
                ageFields.exists {
                  case ("default", Json.Num(value)) => value.intValue == 18
                  case _ => false
                }
            }.getOrElse(false)
          }

          assertTrue(hasDefault)
        case _ =>
          assertTrue(false)
    },
    test("supports @transientField annotation") {
      case class PersonWithTransient(
        name: String,
        age: Int,
        @transientField password: String = "hidden"
      ) derives Schema

      val jsonSchema = Schema[PersonWithTransient].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasNoPassword = propsOpt.exists { props =>
            !props.fields.exists(_._1 == "password")
          }

          assertTrue(hasNoPassword)
        case _ =>
          assertTrue(false)
    },
    test("automatically extracts default values from case class") {
      case class Product(
        name: String,
        price: Double = 9.99,
        inStock: Boolean = true,
        quantity: Int = 0
      ) derives Schema

      val jsonSchema = Schema[Product].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasCorrectDefaults = propsOpt.exists { props =>
            val priceHasDefault = props.fields.exists {
              case ("price", Json.Obj(priceFields)) =>
                priceFields.exists {
                  case ("default", Json.Num(value)) => value.doubleValue == 9.99
                  case _ => false
                }
              case _ => false
            }

            val inStockHasDefault = props.fields.exists {
              case ("inStock", Json.Obj(stockFields)) =>
                stockFields.exists {
                  case ("default", Json.Bool(value)) => value == true
                  case _ => false
                }
              case _ => false
            }

            val quantityHasDefault = props.fields.exists {
              case ("quantity", Json.Obj(qtyFields)) =>
                qtyFields.exists {
                  case ("default", Json.Num(value)) => value.intValue == 0
                  case _ => false
                }
              case _ => false
            }

            priceHasDefault && inStockHasDefault && quantityHasDefault
          }

          assertTrue(hasCorrectDefaults)
        case _ =>
          assertTrue(false)
    },
    test("annotation default value takes precedence over case class default") {
      case class Item(
        name: String = "default-name",
        @fieldDefaultValue("override-name") label: String = "default-label"
      ) derives Schema

      val jsonSchema = Schema[Item].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst {
            case ("properties", obj: Json.Obj) => obj
          }

          val hasCorrectDefaults = propsOpt.exists { props =>
            val nameHasDefault = props.fields.exists {
              case ("name", Json.Obj(nameFields)) =>
                nameFields.exists {
                  case ("default", Json.Str(value)) => value == "default-name"
                  case _ => false
                }
              case _ => false
            }

            val labelHasOverride = props.fields.exists {
              case ("label", Json.Obj(labelFields)) =>
                labelFields.exists {
                  case ("default", Json.Str(value)) => value == "override-name"
                  case _ => false
                }
              case _ => false
            }

            nameHasDefault && labelHasOverride
          }

          assertTrue(hasCorrectDefaults)
        case _ =>
          assertTrue(false)
    },
    test("supports @readOnly annotation") {
      case class Document(
        id: String,
        @readOnly createdAt: String
      ) derives Schema

      val jsonSchema = Schema[Document].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasReadOnly = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("createdAt", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("readOnly", Json.Bool(true)) => true; case _ => false }
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasReadOnly)
        case _ => assertTrue(false)
    },
    test("supports @writeOnly annotation") {
      case class UserInput(
        username: String,
        @writeOnly password: String
      ) derives Schema

      val jsonSchema = Schema[UserInput].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasWriteOnly = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("password", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("writeOnly", Json.Bool(true)) => true; case _ => false }
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasWriteOnly)
        case _ => assertTrue(false)
    },
    test("supports @deprecated annotation") {
      case class ApiResponse(
        data: String,
        @deprecated("Use newField instead") oldField: String
      ) derives Schema

      val jsonSchema = Schema[ApiResponse].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasDeprecated = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("oldField", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("deprecated", Json.Bool(true)) => true; case _ => false }
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasDeprecated)
        case _ => assertTrue(false)
    },
    test("supports @examples annotation") {
      case class Color(
        @examples("red", "green", "blue") name: String
      ) derives Schema

      val jsonSchema = Schema[Color].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasExamples = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("name", Json.Obj(fieldProps)) =>
                  fieldProps.exists {
                    case ("examples", Json.Arr(vals)) => vals.size == 3
                    case _ => false
                  }
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasExamples)
        case _ => assertTrue(false)
    },
    test("supports @format annotation") {
      case class Contact(
        @format("email") email: String,
        @format("uri") website: String
      ) derives Schema

      val jsonSchema = Schema[Contact].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasFormats = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              val hasEmail = props.exists {
                case ("email", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("format", Json.Str("email")) => true; case _ => false }
                case _ => false
              }
              val hasUri = props.exists {
                case ("website", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("format", Json.Str("uri")) => true; case _ => false }
                case _ => false
              }
              hasEmail && hasUri
          }.getOrElse(false)

          assertTrue(hasFormats)
        case _ => assertTrue(false)
    },
    test("supports @validate with string constraints") {
      case class Username(
        @validate(Validation.minLength(3) && Validation.maxLength(20)) value: String
      ) derives Schema

      val jsonSchema = Schema[Username].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasValidation = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  val hasMinLength = fieldProps.exists { case ("minLength", Json.Num(n)) => n.intValue == 3; case _ => false }
                  val hasMaxLength = fieldProps.exists { case ("maxLength", Json.Num(n)) => n.intValue == 20; case _ => false }
                  hasMinLength && hasMaxLength
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasValidation)
        case _ => assertTrue(false)
    },
    test("supports @stringEnum annotation") {
      case class Status(
        @stringEnum("active", "inactive", "pending") value: String
      ) derives Schema

      val jsonSchema = Schema[Status].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasEnum = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  fieldProps.exists {
                    case ("enum", Json.Arr(vals)) => vals.size == 3
                    case _ => false
                  }
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasEnum)
        case _ => assertTrue(false)
    },
    test("supports @multipleOf annotation") {
      case class Measurement(
        @multipleOf(0.01) value: Double
      ) derives Schema

      val jsonSchema = Schema[Measurement].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasMultipleOf = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("multipleOf", Json.Num(n)) => n.doubleValue == 0.01; case _ => false }
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasMultipleOf)
        case _ => assertTrue(false)
    },
    test("supports @minimum and @maximum annotations") {
      case class Range(
        @minimum(0.0) @maximum(100.0) value: Double
      ) derives Schema

      val jsonSchema = Schema[Range].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasRange = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  val hasMin = fieldProps.exists { case ("minimum", Json.Num(n)) => n.doubleValue == 0.0; case _ => false }
                  val hasMax = fieldProps.exists { case ("maximum", Json.Num(n)) => n.doubleValue == 100.0; case _ => false }
                  hasMin && hasMax
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasRange)
        case _ => assertTrue(false)
    },
    test("supports @minItems and @maxItems annotations") {
      case class BoundedList(
        @minItems(1) @maxItems(10) items: List[String]
      ) derives Schema

      val jsonSchema = Schema[BoundedList].jsonSchemaAst

      jsonSchema match
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasItemsBounds = fieldsWithoutSchema.collectFirst {
            case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("items", Json.Obj(fieldProps)) =>
                  val hasMin = fieldProps.exists { case ("minItems", Json.Num(n)) => n.intValue == 1; case _ => false }
                  val hasMax = fieldProps.exists { case ("maxItems", Json.Num(n)) => n.intValue == 10; case _ => false }
                  hasMin && hasMax
                case _ => false
              }
          }.getOrElse(false)

          assertTrue(hasItemsBounds)
        case _ => assertTrue(false)
    },
  )
end AnnotationsSpec
