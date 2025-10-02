package schemanator.generator

import zio.*
import zio.schema.*
import zio.schema.annotation.*
import zio.schema.validation.*
import zio.json.ast.Json
import schemanator.annotations.*

private[schemanator] object MetadataExtractor:

  /** Extract metadata from ZIO Schema annotations */
  def extractMetadata(annotations: Chunk[Any]): Map[String, Json] =
    var metadata = Map.empty[String, Json]

    annotations.foreach {
      case desc: description =>
        metadata = metadata + ("description" -> Json.Str(desc.text))
      case v: validate[?] =>
        metadata = metadata ++ extractValidation(v.validation)
      case fName: fieldName =>
        // fieldName is handled separately in field processing
        ()
      case fDefault: fieldDefaultValue[?] =>
        // ZIO Schema's fieldDefaultValue uses type-erased Any
        metadata = metadata + ("default" -> Utilities.jsonFromAny(fDefault.value))
      case dv: defaultValue[?] =>
        // Our custom defaultValue annotation preserves the schema for proper encoding
        metadata = metadata + ("default" -> Utilities.jsonFromValue(dv.value)(using dv.schema))
      case _: readOnly =>
        metadata = metadata + ("readOnly" -> Json.Bool(true))
      case _: writeOnly =>
        metadata = metadata + ("writeOnly" -> Json.Bool(true))
      case dep: deprecated =>
        metadata = metadata + ("deprecated" -> Json.Bool(true))
        if dep.message.nonEmpty then metadata = metadata + ("x-deprecated-message" -> Json.Str(dep.message))
      case ex: examples =>
        // Type-erased examples - best effort conversion
        metadata = metadata + ("examples" -> Json.Arr(ex.values.map(Utilities.jsonFromAny)*))
      case exVals: exampleValues[?] =>
        // Type-safe examples with schema - proper encoding
        metadata =
          metadata + ("examples" -> Json.Arr(exVals.values.map(v => Utilities.jsonFromValue(v)(using exVals.schema))*))
      case fmt: format =>
        metadata = metadata + ("format" -> Json.Str(fmt.formatType))
      case mult: multipleOf =>
        metadata = metadata + ("multipleOf" -> Json.Num(mult.value))
      case min: minimum =>
        if min.exclusive then metadata = metadata + ("exclusiveMinimum" -> Json.Num(min.value))
        else metadata = metadata + ("minimum"                           -> Json.Num(min.value))
      case max: maximum =>
        if max.exclusive then metadata = metadata + ("exclusiveMaximum" -> Json.Num(max.value))
        else metadata = metadata + ("maximum"                           -> Json.Num(max.value))
      case minItems: minItems =>
        metadata = metadata + ("minItems" -> Json.Num(minItems.n))
      case maxItems: maxItems =>
        metadata = metadata + ("maxItems" -> Json.Num(maxItems.n))
      case minProps: minProperties =>
        metadata = metadata + ("minProperties" -> Json.Num(minProps.n))
      case maxProps: maxProperties =>
        metadata = metadata + ("maxProperties" -> Json.Num(maxProps.n))
      case strEnum: stringEnum =>
        metadata = metadata + ("enum" -> Json.Arr(strEnum.values.map(Json.Str(_))*))
      case _ => ()
    }

    metadata
  end extractMetadata

  /** Extract validation rules from ZIO Schema Validation */
  def extractValidation(validation: Validation[?]): Map[String, Json] =
    import zio.schema.validation.{Predicate, Bool, Regex}

    def extractFromBool[A](bool: Bool[Predicate[A]]): Map[String, Json] =
      bool match
        case Bool.And(left, right) =>
          extractFromBool(left) ++ extractFromBool(right)

        case Bool.Or(left, right) =>
          // For anyOf, we need to create separate schemas
          // For now, we'll extract both and let the user handle conflicts
          extractFromBool(left) ++ extractFromBool(right)

        case Bool.Not(value) =>
          // Handle negation if needed - complex to represent in JSON Schema
          Map.empty

        case Bool.Leaf(predicate) =>
          predicate match
            case Predicate.Str.MinLength(n) =>
              Map("minLength" -> Json.Num(n))

            case Predicate.Str.MaxLength(n) =>
              Map("maxLength" -> Json.Num(n))

            case Predicate.Str.Matches(r) =>
              // Convert ZIO Schema Regex to string pattern
              Map("pattern" -> Json.Str(r.toString))

            case Predicate.Num.GreaterThan(_, value) =>
              Map("exclusiveMinimum" -> Utilities.jsonFromAny(value))

            case Predicate.Num.LessThan(_, value) =>
              Map("exclusiveMaximum" -> Utilities.jsonFromAny(value))

            case Predicate.Num.EqualTo(_, value) =>
              Map("const" -> Utilities.jsonFromAny(value))

            case _ =>
              Map.empty

    extractFromBool(validation.bool)
  end extractValidation

  /** Add metadata to a JSON schema */
  def addMetadata(schema: Json, annotations: Chunk[Any]): Json =
    val metadata = extractMetadata(annotations)
    if metadata.isEmpty then schema
    else
      schema match
        case Json.Obj(fields) =>
          Json.Obj((fields.toMap ++ metadata).toSeq*)
        case other => other
end MetadataExtractor
