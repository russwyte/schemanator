package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Sets the minimum number of properties an object must have */
case class minProperties(n: Int) extends StaticAnnotation

/** Sets the maximum number of properties an object can have */
case class maxProperties(n: Int) extends StaticAnnotation
