package schemanator.generator

import zio.*
import zio.schema.*
import zio.json.ast.Json
import scala.collection.mutable

private[schemanator] object TypeConverters:

  case class Context(
    definitions: mutable.Map[String, Json] = mutable.Map.empty,
    inProgress: mutable.Set[String] = mutable.Set.empty
  )

  /** Convert a ZIO Schema to JSON Schema */
  def schemaToJsonSchema(schema: Schema[?], ctx: Context): Json =
    schemaToJsonSchema(schema, ctx, checkRecursion = true)

  /**
   * Convert a ZIO Schema to JSON Schema with optional recursion checking.
   * When checkRecursion is false, always generates inline schemas.
   */
  def schemaToJsonSchema(schema: Schema[?], ctx: Context, checkRecursion: Boolean): Json =
    if !checkRecursion then
      return schemaToJsonSchemaInner(schema, ctx)

    // Check if this is a named type that might be recursive
    Utilities.getSchemaId(schema) match
      case Some(id) =>
        // If we're already processing this type, it's recursive - return a $ref
        if ctx.inProgress.contains(id) then
          // Store a placeholder to mark this as recursive
          if !ctx.definitions.contains(id) then
            ctx.definitions(id) = Json.Obj("type" -> Json.Str("object"))
          return Json.Obj("$ref" -> Json.Str(s"#/$$defs/$id"))

        // Mark as in progress
        ctx.inProgress.add(id)

        // Generate the schema
        val result = schemaToJsonSchemaInner(schema, ctx)

        // Remove from in progress
        ctx.inProgress.remove(id)

        // If this type was marked as recursive (definition was added), return a $ref
        if ctx.definitions.contains(id) then
          // Update the definition with the actual schema
          ctx.definitions(id) = result
          Json.Obj("$ref" -> Json.Str(s"#/$$defs/$id"))
        else
          // Not recursive, return inline
          result

      case None =>
        schemaToJsonSchemaInner(schema, ctx)

  private def schemaToJsonSchemaInner(schema: Schema[?], ctx: Context): Json =
    schema match
      case Schema.Primitive(standardType, annotations) =>
        MetadataExtractor.addMetadata(primitiveToJsonSchema(standardType), annotations)

      case Schema.Optional(schema, _) =>
        schemaToJsonSchema(schema, ctx)

      case Schema.Sequence(elementSchema, _, _, _, _) =>
        Json.Obj(
          "type" -> Json.Str("array"),
          "items" -> schemaToJsonSchema(elementSchema, ctx)
        )

      case Schema.Map(keySchema, valueSchema, _) =>
        Json.Obj(
          "type" -> Json.Str("object"),
          "additionalProperties" -> schemaToJsonSchema(valueSchema, ctx)
        )

      case Schema.Set(elementSchema, _) =>
        Json.Obj(
          "type" -> Json.Str("array"),
          "items" -> schemaToJsonSchema(elementSchema, ctx),
          "uniqueItems" -> Json.Bool(true)
        )

      case record: Schema.Record[?] =>
        MetadataExtractor.addMetadata(RecordConverter.recordToJsonSchema(record, ctx), record.annotations)

      case e: Schema.Enum[?] =>
        MetadataExtractor.addMetadata(EnumConverter.enumToJsonSchema(e, ctx), e.annotations)

      case Schema.Transform(schema, _, _, _, _) =>
        schemaToJsonSchema(schema, ctx)

      case Schema.Lazy(schema0) =>
        schemaToJsonSchema(schema0(), ctx)

      case either: Schema.Either[?, ?] =>
        MetadataExtractor.addMetadata(eitherToJsonSchema(either, ctx), either.annotations)

      case tuple: Schema.Tuple2[?, ?] =>
        MetadataExtractor.addMetadata(tupleToJsonSchema(List(tuple.left, tuple.right), ctx), tuple.annotations)

      case _ =>
        Json.Obj("type" -> Json.Str("object"))

  /** Convert primitive types to JSON Schema */
  def primitiveToJsonSchema(standardType: StandardType[?]): Json =
    standardType match
      case StandardType.StringType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.BoolType =>
        Json.Obj("type" -> Json.Str("boolean"))
      case StandardType.ByteType | StandardType.ShortType | StandardType.IntType | StandardType.LongType =>
        Json.Obj("type" -> Json.Str("integer"))
      case StandardType.FloatType | StandardType.DoubleType | StandardType.BigDecimalType =>
        Json.Obj("type" -> Json.Str("number"))
      case StandardType.BinaryType =>
        Json.Obj("type" -> Json.Str("string"), "contentEncoding" -> Json.Str("base64"))
      case StandardType.CharType =>
        Json.Obj("type" -> Json.Str("string"), "minLength" -> Json.Num(1), "maxLength" -> Json.Num(1))
      case StandardType.UUIDType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("uuid"))
      case StandardType.BigIntegerType =>
        Json.Obj("type" -> Json.Str("integer"))
      case StandardType.DayOfWeekType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.MonthType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.MonthDayType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.PeriodType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.YearType =>
        Json.Obj("type" -> Json.Str("integer"))
      case StandardType.YearMonthType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.ZoneIdType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.ZoneOffsetType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.DurationType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.InstantType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("date-time"))
      case StandardType.LocalDateType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("date"))
      case StandardType.LocalTimeType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("time"))
      case StandardType.LocalDateTimeType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("date-time"))
      case StandardType.OffsetTimeType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("time"))
      case StandardType.OffsetDateTimeType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("date-time"))
      case StandardType.ZonedDateTimeType =>
        Json.Obj("type" -> Json.Str("string"), "format" -> Json.Str("date-time"))
      case StandardType.CurrencyType =>
        Json.Obj("type" -> Json.Str("string"))
      case StandardType.UnitType =>
        Json.Obj("type" -> Json.Str("null"))

  /** Convert Either to JSON Schema */
  def eitherToJsonSchema(either: Schema.Either[?, ?], ctx: Context): Json =
    val leftSchema = schemaToJsonSchema(either.left, ctx)
    val rightSchema = schemaToJsonSchema(either.right, ctx)

    Json.Obj(
      "oneOf" -> Json.Arr(leftSchema, rightSchema)
    )

  /** Flatten nested tuples */
  def flattenTupleSchemas(schemas: List[Schema[?]]): List[Schema[?]] =
    schemas.flatMap {
      case tuple: Schema.Tuple2[?, ?] =>
        flattenTupleSchemas(List(tuple.left, tuple.right))
      case schema => List(schema)
    }

  /** Convert tuple to JSON Schema */
  def tupleToJsonSchema(schemas: List[Schema[?]], ctx: Context): Json =
    val flattened = flattenTupleSchemas(schemas)
    val itemSchemas = flattened.map(schemaToJsonSchema(_, ctx))
    val size = flattened.size

    Json.Obj(
      "type" -> Json.Str("array"),
      "prefixItems" -> Json.Arr(itemSchemas*),
      "items" -> Json.Bool(false),
      "minItems" -> Json.Num(size),
      "maxItems" -> Json.Num(size)
    )
