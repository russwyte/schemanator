package object schemanator {
  // Re-export the implicit conversions from SchemaOps
  // This makes the extension methods available when importing schemanator._
  implicit def toValueSchemaOps[T](value: T): SchemaOps.ValueSchemaOps[T] =
    new SchemaOps.ValueSchemaOps[T](value)

  implicit def toSchemaSchemaOps[T](schema: zio.schema.Schema[T]): SchemaOps.SchemaSchemaOps[T] =
    new SchemaOps.SchemaSchemaOps[T](schema)
}
