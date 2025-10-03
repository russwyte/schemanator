package schemanator.annotations

import scala.annotation.StaticAnnotation
import zio.schema.Schema

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
case class exampleValues[A: Schema](values: A*) extends StaticAnnotation {
  def schema: Schema[A] = summon[Schema[A]]
}

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
case class defaultValue[A: Schema](value: A) extends StaticAnnotation {
  def schema: Schema[A] = summon[Schema[A]]
}
