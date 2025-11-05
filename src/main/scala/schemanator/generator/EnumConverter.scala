package schemanator.generator

import zio.*
import zio.schema.*
import zio.schema.annotation.*
import zio.json.ast.Json

private[schemanator] object EnumConverter {

  /** Convert a ZIO Schema Enum to JSON Schema */
  def enumToJsonSchema(e: Schema.Enum[?], ctx: TypeConverters.Context): Json = {
    val hasNoDiscriminator = e.annotations.exists {
      case _: noDiscriminator => true
      case _                  => false
    }

    val discriminatorField = e.annotations
      .collectFirst { case d: discriminatorName =>
        d.tag
      }
      .getOrElse("type")

    // Check which composition annotation is present (validate no conflicts)
    val hasAnyOf = e.annotations.exists {
      case _: schemanator.annotations.anyOf => true
      case _                                => false
    }
    val hasAllOf = e.annotations.exists {
      case _: schemanator.annotations.allOf => true
      case _                                => false
    }

    // Error if both are present - TODO: revisit error handling to use Either/ZIO
    if (hasAnyOf && hasAllOf) {
      throw new IllegalArgumentException(
        "Cannot use both @anyOf and @allOf annotations on the same type. Choose one composition strategy."
      )
    }

    val compositionKey = if (hasAnyOf) "anyOf" else if (hasAllOf) "allOf" else "oneOf"

    // anyOf implies no discriminator per JSON Schema semantics and OpenAI requirements
    // Discriminators are for oneOf (exactly one match), not anyOf (one or more matches)
    val shouldUseDiscriminator = !hasNoDiscriminator && !hasAnyOf

    val caseSchemas = e.cases.map { case_ =>
      // Generate case schema inline, bypassing recursion detection
      // Enum cases shouldn't be treated as separate named types
      val baseSchema = TypeConverters.schemaToJsonSchema(case_.schema, ctx, checkRecursion = false)
      val caseName   = Utilities.getCaseName(case_)

      if (!shouldUseDiscriminator)
        baseSchema
      else
        // Add discriminator property to each case
        baseSchema match {
          case Json.Obj(fields) if fields.exists(_._1 == "$ref") =>
            // If it's a reference, we can't add discriminator inline
            // Just return the reference as-is
            baseSchema
          case Json.Obj(fields) =>
            val propsMap = fields.toMap.get("properties") match {
              case Some(Json.Obj(props)) =>
                props.toMap + (discriminatorField -> Json.Obj("const" -> Json.Str(caseName)))
              case _ =>
                Map(discriminatorField -> Json.Obj("const" -> Json.Str(caseName)))
            }

            val updatedFields = fields.toMap + ("properties" -> Json.Obj(propsMap.toSeq*))
            Json.Obj(updatedFields.toSeq*)
          case _ => baseSchema
        }
    }

    val baseResult = Json.Obj(compositionKey -> Json.Arr(caseSchemas*))

    if (!shouldUseDiscriminator)
      baseResult
    else {
      // Add discriminator object per JSON Schema spec (baseResult is always Json.Obj)
      val Json.Obj(fields) = baseResult: @unchecked
      Json.Obj((fields.toMap + ("discriminator" -> Json.Obj("propertyName" -> Json.Str(discriminatorField)))).toSeq*)
    }
  }
}
