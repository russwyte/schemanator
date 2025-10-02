package schemanator.annotations

import scala.annotation.StaticAnnotation
import zio.schema.Schema

/** Marks a field as read-only (output only) */
case class readOnly() extends StaticAnnotation

/** Marks a field as write-only (input only) */
case class writeOnly() extends StaticAnnotation

/** Marks a field as deprecated with optional message */
case class deprecated(message: String = "") extends StaticAnnotation

/** Provides example values for a field (type-erased version for compatibility)
  *
  * Note: Values are type-erased. For complex types with proper encoding,
  * prefer using `exampleValues` which captures the Schema.
  */
case class examples(values: Any*) extends StaticAnnotation

/** Provides type-safe example values for a field.
  *
  * This annotation captures both the values and their schema, allowing for proper
  * JSON encoding of complex types.
  *
  * Example:
  * {{{
  *   case class Product(
  *     @exampleValues(List("red", "green", "blue")) colors: List[String]
  *   ) derives Schema
  * }}}
  *
  * @param values The example values
  * @tparam A The type of the values (must have a Schema instance)
  */
case class exampleValues[A: Schema](values: A*) extends StaticAnnotation:
  def schema: Schema[A] = summon[Schema[A]]

/** Provides a default value for a field with type-safe encoding.
  *
  * This annotation captures both the value and its schema, allowing for proper
  * JSON encoding of complex types.
  *
  * Example:
  * {{{
  *   case class Config(
  *     @defaultValue(List(1, 2, 3)) numbers: List[Int]
  *   ) derives Schema
  * }}}
  *
  * @param value The default value
  * @tparam A The type of the default value (must have a Schema instance)
  */
case class defaultValue[A: Schema](value: A) extends StaticAnnotation:
  def schema: Schema[A] = summon[Schema[A]]

/** Forces an optional field to appear in the required array.
  *
  * Some APIs (e.g., OpenAI) require optional fields to be listed in the required array.
  * This annotation overrides the default behavior where Option[T] fields are excluded
  * from the required list.
  *
  * Example:
  * {{{
  *   case class Request(
  *     name: String,
  *     @requiredField email: Option[String]  // Will appear in required array
  *   ) derives Schema
  * }}}
  */
case class requiredField() extends StaticAnnotation
