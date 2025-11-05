package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Sets the minimum number of items in an array */
case class minItems(n: Int) extends StaticAnnotation

/** Sets the maximum number of items in an array */
case class maxItems(n: Int) extends StaticAnnotation
