package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Requires numeric values to be multiples of the specified value */
case class multipleOf(value: Double) extends StaticAnnotation

/** Sets the minimum value for a numeric field */
case class minimum(value: Double, exclusive: Boolean = false) extends StaticAnnotation

/** Sets the maximum value for a numeric field */
case class maximum(value: Double, exclusive: Boolean = false) extends StaticAnnotation
