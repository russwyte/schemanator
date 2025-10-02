package schemanator.annotations

import scala.annotation.StaticAnnotation

/**
 * Specifies the format for string validation.
 * Common formats: "email", "uri", "hostname", "ipv4", "ipv6", "date", "date-time", "uuid"
 */
case class format(formatType: String) extends StaticAnnotation

/** Defines a string enum with specific allowed values */
case class stringEnum(values: String*) extends StaticAnnotation
