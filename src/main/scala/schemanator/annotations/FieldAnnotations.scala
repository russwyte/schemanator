package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Marks a field as read-only (output only) */
case class readOnly() extends StaticAnnotation

/** Marks a field as write-only (input only) */
case class writeOnly() extends StaticAnnotation

/** Marks a field as deprecated with optional message */
case class deprecated(message: String = "") extends StaticAnnotation

/** Provides example values for a field (type-erased version for compatibility)
  *
  * Note: Values are type-erased. For complex types with proper encoding, use the Scala 3-only `exampleValues`
  * annotation which captures the Schema.
  */
case class examples(values: Any*) extends StaticAnnotation

/** Forces an optional field to appear in the required array.
  *
  * Some APIs (e.g., OpenAI) require optional fields to be listed in the required array. This annotation overrides the
  * default behavior where Option[T] fields are excluded from the required list.
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
