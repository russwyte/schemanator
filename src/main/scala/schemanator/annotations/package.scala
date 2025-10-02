package schemanator

/**
 * JSON Schema annotations for enhancing ZIO Schema with JSON Schema Draft 2020-12 features.
 *
 * This package provides annotations organized by category:
 * - Field annotations: readOnly, writeOnly, deprecated, examples
 * - String annotations: format, stringEnum
 * - Numeric annotations: multipleOf, minimum, maximum
 * - Array annotations: minItems, maxItems, contains
 * - Object annotations: minProperties, maxProperties
 * - Composition annotations: allOf, anyOf, not
 *
 * Import all annotations with: `import schemanator.annotations.*`
 */
package object annotations
