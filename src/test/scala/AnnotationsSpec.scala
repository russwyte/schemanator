import zio.*
import zio.test.*
import zio.schema.*
import zio.schema.annotation.*
import zio.schema.validation.Validation
import zio.json.ast.Json
import schemanator.*
import schemanator.annotations.*

object AnnotationsSpec extends ZIOSpecDefault {

  // Define all case classes at object level for Scala 2.13 DeriveSchema.gen compatibility

  case class ApiPerson(
      @fieldName("user_name") name: String,
      @fieldName("user_age") age: Int,
  )
  object ApiPerson {
    implicit val schema: Schema[ApiPerson] = DeriveSchema.gen[ApiPerson]
  }

  case class Document(
      id: String,
      @readOnly createdAt: String,
  )
  object Document {
    implicit val schema: Schema[Document] = DeriveSchema.gen[Document]
  }

  case class UserInput(
      username: String,
      @writeOnly password: String,
  )
  object UserInput {
    implicit val schema: Schema[UserInput] = DeriveSchema.gen[UserInput]
  }

  case class ApiResponse(
      data: String,
      @deprecated("Use newField instead") oldField: String,
  )
  object ApiResponse {
    implicit val schema: Schema[ApiResponse] = DeriveSchema.gen[ApiResponse]
  }

  case class Color(
      @examples("red", "green", "blue") name: String
  )
  object Color {
    implicit val schema: Schema[Color] = DeriveSchema.gen[Color]
  }

  case class Contact(
      @format("email") email: String,
      @format("uri") website: String,
  )
  object Contact {
    implicit val schema: Schema[Contact] = DeriveSchema.gen[Contact]
  }

  case class Username(
      @validate(Validation.minLength(3) && Validation.maxLength(20)) value: String
  )
  object Username {
    implicit val schema: Schema[Username] = DeriveSchema.gen[Username]
  }

  case class Status(
      @stringEnum("active", "inactive", "pending") value: String
  )
  object Status {
    implicit val schema: Schema[Status] = DeriveSchema.gen[Status]
  }

  case class Measurement(
      @multipleOf(0.01) value: Double
  )
  object Measurement {
    implicit val schema: Schema[Measurement] = DeriveSchema.gen[Measurement]
  }

  case class Range(
      @minimum(0.0) @maximum(100.0) value: Double
  )
  object Range {
    implicit val schema: Schema[Range] = DeriveSchema.gen[Range]
  }

  case class BoundedList(
      @minItems(1) @maxItems(10) items: List[String]
  )
  object BoundedList {
    implicit val schema: Schema[BoundedList] = DeriveSchema.gen[BoundedList]
  }

  case class Request(
      name: String,
      age: Int,
      email: Option[String],
      @requiredField description: Option[String],
      @requiredField tags: Option[List[String]],
  )
  object Request {
    implicit val schema: Schema[Request] = DeriveSchema.gen[Request]
  }

  @requireAll
  case class ApiRequest(
      endpoint: String,
      method: Option[String],
      headers: Option[Map[String, String]],
      body: Option[String],
  )
  object ApiRequest {
    implicit val schema: Schema[ApiRequest] = DeriveSchema.gen[ApiRequest]
  }

  @additionalProperties(false)
  case class StrictConfig(
      host: String,
      port: Int,
      enabled: Boolean,
  )
  object StrictConfig {
    implicit val schema: Schema[StrictConfig] = DeriveSchema.gen[StrictConfig]
  }

  @additionalProperties(true)
  case class FlexibleConfig(
      host: String,
      port: Int,
  )
  object FlexibleConfig {
    implicit val schema: Schema[FlexibleConfig] = DeriveSchema.gen[FlexibleConfig]
  }

  @strict
  case class StrictApiConfig(
      apiKey: String,
      timeout: Option[Int],
      retries: Option[Int],
  )
  object StrictApiConfig {
    implicit val schema: Schema[StrictApiConfig] = DeriveSchema.gen[StrictApiConfig]
  }

  def spec: Spec[Any, Nothing] = suite("Annotations")(
    test("supports @fieldName annotation") {
      val jsonSchema = Schema[ApiPerson].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val propsOpt = fieldsWithoutSchema.collectFirst { case ("properties", obj: Json.Obj) =>
            obj
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
      }
    },
    /* TODO: Scala 2.13 - DeriveSchema.gen doesn't work well with default values
    test("supports @fieldDefaultValue annotation") {
      case class PersonWithDefaults(
        name: String,
        @fieldDefaultValue(18) age: Int
      )
      object PersonWithDefaults {
        implicit val schema: Schema[PersonWithDefaults] = DeriveSchema.gen[PersonWithDefaults]
      }

      val jsonSchema = Schema[PersonWithDefaults].jsonSchemaAst

      jsonSchema match {
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
      }
    },
     */
    /* TODO: Scala 2.13 - transientField requires default value
    test("supports @transientField annotation") {
      case class PersonWithTransient(
        name: String,
        age: Int,
        @transientField password: String = "hidden"
      )
      object PersonWithTransient {
        implicit val schema: Schema[PersonWithTransient] = DeriveSchema.gen[PersonWithTransient]
      }

      val jsonSchema = Schema[PersonWithTransient].jsonSchemaAst

      jsonSchema match {
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
      }
    },
     */
    /* TODO: Scala 2.13 - DeriveSchema.gen doesn't work with default values
    test("automatically extracts default values from case class") {
      case class Product(
        name: String,
        price: Double = 9.99,
        inStock: Boolean = true,
        quantity: Int = 0
      )
      object Product {
        implicit val schema: Schema[Product] = DeriveSchema.gen[Product]
      }

      val jsonSchema = Schema[Product].jsonSchemaAst

      jsonSchema match {
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
      }
    },
     */
    /* TODO: Scala 2.13 - default values issue
    test("annotation default value takes precedence over case class default") {
      case class Item(
        name: String = "default-name",
        @fieldDefaultValue("override-name") label: String = "default-label"
      )
      object Item {
        implicit val schema: Schema[Item] = DeriveSchema.gen[Item]
      }

      val jsonSchema = Schema[Item].jsonSchemaAst

      jsonSchema match {
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
      }
    },
     */
    test("supports @readOnly annotation") {
      val jsonSchema = Schema[Document].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasReadOnly = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("createdAt", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("readOnly", Json.Bool(true)) => true; case _ => false }
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasReadOnly)
        case _ => assertTrue(false)
      }
    },
    test("supports @writeOnly annotation") {
      val jsonSchema = Schema[UserInput].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasWriteOnly = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("password", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("writeOnly", Json.Bool(true)) => true; case _ => false }
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasWriteOnly)
        case _ => assertTrue(false)
      }
    },
    test("supports @deprecated annotation") {
      val jsonSchema = Schema[ApiResponse].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasDeprecated = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("oldField", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("deprecated", Json.Bool(true)) => true; case _ => false }
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasDeprecated)
        case _ => assertTrue(false)
      }
    },
    test("supports @examples annotation") {
      val jsonSchema = Schema[Color].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasExamples = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("name", Json.Obj(fieldProps)) =>
                  fieldProps.exists {
                    case ("examples", Json.Arr(vals)) => vals.size == 3
                    case _                            => false
                  }
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasExamples)
        case _ => assertTrue(false)
      }
    },
    test("supports @format annotation") {
      val jsonSchema = Schema[Contact].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasFormats = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
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
            }
            .getOrElse(false)

          assertTrue(hasFormats)
        case _ => assertTrue(false)
      }
    },
    test("supports @validate with string constraints") {
      val jsonSchema = Schema[Username].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasValidation = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  val hasMinLength =
                    fieldProps.exists { case ("minLength", Json.Num(n)) => n.intValue == 3; case _ => false }
                  val hasMaxLength =
                    fieldProps.exists { case ("maxLength", Json.Num(n)) => n.intValue == 20; case _ => false }
                  hasMinLength && hasMaxLength
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasValidation)
        case _ => assertTrue(false)
      }
    },
    test("supports @stringEnum annotation") {
      val jsonSchema = Schema[Status].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasEnum = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  fieldProps.exists {
                    case ("enum", Json.Arr(vals)) => vals.size == 3
                    case _                        => false
                  }
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasEnum)
        case _ => assertTrue(false)
      }
    },
    test("supports @multipleOf annotation") {
      val jsonSchema = Schema[Measurement].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasMultipleOf = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  fieldProps.exists { case ("multipleOf", Json.Num(n)) => n.doubleValue == 0.01; case _ => false }
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasMultipleOf)
        case _ => assertTrue(false)
      }
    },
    test("supports @minimum and @maximum annotations") {
      val jsonSchema = Schema[Range].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasRange = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("value", Json.Obj(fieldProps)) =>
                  val hasMin =
                    fieldProps.exists { case ("minimum", Json.Num(n)) => n.doubleValue == 0.0; case _ => false }
                  val hasMax =
                    fieldProps.exists { case ("maximum", Json.Num(n)) => n.doubleValue == 100.0; case _ => false }
                  hasMin && hasMax
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasRange)
        case _ => assertTrue(false)
      }
    },
    test("supports @minItems and @maxItems annotations") {
      val jsonSchema = Schema[BoundedList].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")
          val hasItemsBounds = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("items", Json.Obj(fieldProps)) =>
                  val hasMin = fieldProps.exists { case ("minItems", Json.Num(n)) => n.intValue == 1; case _ => false }
                  val hasMax = fieldProps.exists { case ("maxItems", Json.Num(n)) => n.intValue == 10; case _ => false }
                  hasMin && hasMax
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasItemsBounds)
        case _ => assertTrue(false)
      }
    },
    test("supports @requiredField annotation on optional fields") {
      val jsonSchema = Schema[Request].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")

          // Check required array includes name, age, description, and tags, but NOT email
          val hasCorrectRequired = fieldsWithoutSchema
            .collectFirst { case ("required", Json.Arr(items)) =>
              val requiredFields = items.collect { case Json.Str(name) => name }
              requiredFields.contains("name") &&
              requiredFields.contains("age") &&
              !requiredFields.contains("email") &&
              requiredFields.contains("description") &&
              requiredFields.contains("tags")
            }
            .getOrElse(false)

          // Check that description has type ["string", "null"]
          val hasNullableDescriptionType = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("description", Json.Obj(descFields)) =>
                  descFields.exists {
                    case ("type", Json.Arr(types)) =>
                      types.contains(Json.Str("string")) && types.contains(Json.Str("null"))
                    case _ => false
                  }
                case _ => false
              }
            }
            .getOrElse(false)

          // Check that tags has type ["array", "null"]
          val hasNullableArrayType = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              props.exists {
                case ("tags", Json.Obj(tagFields)) =>
                  tagFields.exists {
                    case ("type", Json.Arr(types)) =>
                      types.contains(Json.Str("array")) && types.contains(Json.Str("null"))
                    case _ => false
                  }
                case _ => false
              }
            }
            .getOrElse(false)

          assertTrue(hasCorrectRequired && hasNullableDescriptionType && hasNullableArrayType)
        case _ => assertTrue(false)
      }
    },
    test("supports @requireAll annotation on case class") {
      val jsonSchema = Schema[ApiRequest].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")

          // Check that all fields are in the required array (endpoint, method, headers, body)
          val hasAllRequired = fieldsWithoutSchema
            .collectFirst { case ("required", Json.Arr(items)) =>
              val requiredFields = items.collect { case Json.Str(name) => name }
              requiredFields.contains("endpoint") &&
              requiredFields.contains("method") &&
              requiredFields.contains("headers") &&
              requiredFields.contains("body") &&
              requiredFields.size == 4
            }
            .getOrElse(false)

          // Check that optional fields have nullable types
          val hasNullableTypes = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              val methodIsNullable = props.exists {
                case ("method", Json.Obj(methodFields)) =>
                  methodFields.exists {
                    case ("type", Json.Arr(types)) =>
                      types.contains(Json.Str("string")) && types.contains(Json.Str("null"))
                    case _ => false
                  }
                case _ => false
              }

              val bodyIsNullable = props.exists {
                case ("body", Json.Obj(bodyFields)) =>
                  bodyFields.exists {
                    case ("type", Json.Arr(types)) =>
                      types.contains(Json.Str("string")) && types.contains(Json.Str("null"))
                    case _ => false
                  }
                case _ => false
              }

              methodIsNullable && bodyIsNullable
            }
            .getOrElse(false)

          assertTrue(hasAllRequired && hasNullableTypes)
        case _ => assertTrue(false)
      }
    },
    test("supports @additionalProperties(false) annotation") {
      val jsonSchema = Schema[StrictConfig].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")

          // Check that additionalProperties is set to false
          val hasAdditionalPropertiesFalse = fieldsWithoutSchema.exists {
            case ("additionalProperties", Json.Bool(false)) => true
            case _                                          => false
          }

          assertTrue(hasAdditionalPropertiesFalse)
        case _ => assertTrue(false)
      }
    },
    test("supports @additionalProperties(true) annotation") {
      val jsonSchema = Schema[FlexibleConfig].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")

          // Check that additionalProperties is set to true
          val hasAdditionalPropertiesTrue = fieldsWithoutSchema.exists {
            case ("additionalProperties", Json.Bool(true)) => true
            case _                                         => false
          }

          assertTrue(hasAdditionalPropertiesTrue)
        case _ => assertTrue(false)
      }
    },
    test("supports @strict annotation (combines @requireAll and @additionalProperties(false))") {
      val jsonSchema = Schema[StrictApiConfig].jsonSchemaAst

      jsonSchema match {
        case Json.Obj(fields) =>
          val fieldsWithoutSchema = fields.filter(_._1 != "$schema")

          // Check that all fields are required (apiKey, timeout, retries)
          val hasAllRequired = fieldsWithoutSchema
            .collectFirst { case ("required", Json.Arr(items)) =>
              val requiredFields = items.collect { case Json.Str(name) => name }
              requiredFields.contains("apiKey") &&
              requiredFields.contains("timeout") &&
              requiredFields.contains("retries") &&
              requiredFields.size == 3
            }
            .getOrElse(false)

          // Check that optional fields have nullable types
          val hasNullableTypes = fieldsWithoutSchema
            .collectFirst { case ("properties", Json.Obj(props)) =>
              val timeoutIsNullable = props.exists {
                case ("timeout", Json.Obj(timeoutFields)) =>
                  timeoutFields.exists {
                    case ("type", Json.Arr(types)) =>
                      types.contains(Json.Str("integer")) && types.contains(Json.Str("null"))
                    case _ => false
                  }
                case _ => false
              }

              val retriesIsNullable = props.exists {
                case ("retries", Json.Obj(retriesFields)) =>
                  retriesFields.exists {
                    case ("type", Json.Arr(types)) =>
                      types.contains(Json.Str("integer")) && types.contains(Json.Str("null"))
                    case _ => false
                  }
                case _ => false
              }

              timeoutIsNullable && retriesIsNullable
            }
            .getOrElse(false)

          // Check that additionalProperties is false
          val hasAdditionalPropertiesFalse = fieldsWithoutSchema.exists {
            case ("additionalProperties", Json.Bool(false)) => true
            case _                                          => false
          }

          assertTrue(hasAllRequired && hasNullableTypes && hasAdditionalPropertiesFalse)
        case _ => assertTrue(false)
      }
    },
  )
}
