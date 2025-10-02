package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Sets the minimum number of items in an array */
case class minItems(n: Int) extends StaticAnnotation

/** Sets the maximum number of items in an array */
case class maxItems(n: Int) extends StaticAnnotation

/** Specifies that an array must contain certain elements */
case class contains(description: String = "") extends StaticAnnotation
