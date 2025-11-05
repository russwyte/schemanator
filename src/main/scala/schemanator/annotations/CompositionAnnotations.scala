package schemanator.annotations

import scala.annotation.StaticAnnotation

/** Indicates all schemas must match (allOf composition) */
case class allOf(description: String = "") extends StaticAnnotation

/** Indicates at least one schema must match (anyOf composition) */
case class anyOf(description: String = "") extends StaticAnnotation

// /** Indicates the schema must not match (not composition)
//   *
//   * Note: Currently not implemented as the use case for negation on sum types is unclear.
//   * May be reconsidered in the future for field-level validation.
//   */
// case class not(description: String = "") extends StaticAnnotation
