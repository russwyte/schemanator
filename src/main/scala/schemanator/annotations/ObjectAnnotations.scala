package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Sets the minimum number of properties an object must have */
case class minProperties(n: Int) extends StaticAnnotation

/** Sets the maximum number of properties an object can have */
case class maxProperties(n: Int) extends StaticAnnotation

/** Marks all fields in the case class as required, even if they are Option types. This is useful for APIs like OpenAI
  * that require all fields to be marked as required.
  */
case class requireAll() extends StaticAnnotation

/** Controls whether additional properties not defined in the schema are allowed. Set to false to disallow extra
  * properties (strict validation). Set to true to allow extra properties.
  *
  * @param allowed
  *   whether additional properties are allowed
  */
case class additionalProperties(allowed: Boolean) extends StaticAnnotation

/** Combines @requireAll and @additionalProperties(false) for strict schema validation. All fields are marked as
  * required (with nullable types for Option fields), and additional properties not defined in the schema are
  * disallowed. This is ideal for APIs requiring strict schema conformance.
  */
case class strict() extends StaticAnnotation
