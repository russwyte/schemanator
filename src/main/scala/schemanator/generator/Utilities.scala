package schemanator.generator

import zio.*
import zio.schema.*
import zio.schema.annotation.*
import zio.schema.validation.*
import zio.schema.codec.JsonCodec
import zio.json.ast.Json
import schemanator.annotations.*

private[schemanator] object Utilities:

  /** Convert a typed value to JSON using ZIO Schema's JsonCodec.
    *
    * This is the preferred method when you have both a value and its schema with proper type information. It uses
    * zio-schema-json to properly encode the value, which handles complex types correctly.
    *
    * @param value
    *   The value to encode
    * @tparam A
    *   The type of the value (must have a Schema instance in scope)
    * @return
    *   JSON representation of the value
    *
    * Example:
    * {{{
    *   val value = List(1, 2, 3)
    *   val json = jsonFromValue(value)  // Schema[List[Int]] provided implicitly
    *   // Json.Arr(Json.Num(1), Json.Num(2), Json.Num(3))
    * }}}
    */
  private[schemanator] def jsonFromValue[A: Schema as schema](value: A): Json =
    try
      // Use zio-schema-json codec to encode the value
      val enc     = JsonCodec.jsonCodec(schema)
      val encoded = enc.encoder.toJsonAST(value)
      encoded match
        case Right(json) => json
        case Left(_)     => jsonFromAny(value)
    catch case _: Exception => jsonFromAny(value) // todo - we can do better error handling here

  // only used in cases where we don't have a schema - this is a best-effort conversion for default values, examples, etc.
  // todo: improve handling of complex types (e.g., nested case classes, collections of complex types)
  private[schemanator] def jsonFromAny(value: Any): Json =
    value match
      case s: String      => Json.Str(s)
      case i: Int         => Json.Num(i)
      case l: Long        => Json.Num(l)
      case d: Double      => Json.Num(d)
      case f: Float       => Json.Num(f)
      case b: Boolean     => Json.Bool(b)
      case bd: BigDecimal => Json.Num(bd)
      case bi: BigInt     => Json.Num(BigDecimal(bi))
      case s: Short       => Json.Num(s)
      case b: Byte        => Json.Num(b)
      case null           => Json.Null
      // For arrays and collections, convert elements recursively
      case arr: Array[?] => Json.Arr(arr.map(jsonFromAny)*)
      case seq: Seq[?]   => Json.Arr(seq.map(jsonFromAny)*)
      case set: Set[?]   => Json.Arr(set.toSeq.map(jsonFromAny)*)
      // Fallback to string representation for complex types
      case _ => Json.Str(value.toString)

  /** Extract the schema ID for named types */
  def getSchemaId(schema: Schema[?]): Option[String] =
    schema match
      case record: Schema.Record[?] => Some(record.id.name)
      case e: Schema.Enum[?]        => Some(e.id.name)
      case _                        => None

  /** Check if a schema represents an optional field */
  def isOptional(schema: Schema[?]): Boolean =
    schema match
      case _: Schema.Optional[?] => true
      case Schema.Lazy(schema0)  => isOptional(schema0())
      case _                     => false

  /** Get the custom field name from annotations */
  def getFieldName(field: Schema.Field[?, ?]): String =
    field.annotations
      .collectFirst { case fName: fieldName =>
        fName.name
      }
      .getOrElse(field.name)

  /** Check if a field should be included in the schema */
  def shouldIncludeField(field: Schema.Field[?, ?]): Boolean =
    !field.annotations.exists {
      case _: transientField => true
      case _                 => false
    }

  /** Get custom case name from annotations */
  def getCaseName(case_ : Schema.Case[?, ?]): String =
    case_.annotations
      .collectFirst { case cName: caseName =>
        cName.name
      }
      .getOrElse(case_.id)
end Utilities
