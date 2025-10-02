package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Indicates all schemas must match (allOf composition) */
case class allOf(description: String = "") extends StaticAnnotation

/** Indicates at least one schema must match (anyOf composition) */
case class anyOf(description: String = "") extends StaticAnnotation

/** Indicates the schema must not match (not composition) */
case class not(description: String = "") extends StaticAnnotation
