package schemanator.generator

import zio.*
import zio.schema.*
import zio.schema.annotation.*
import zio.json.ast.Json

private[schemanator] object RecordConverter:

  /** Convert a ZIO Schema Record to JSON Schema */
  def recordToJsonSchema(record: Schema.Record[?], ctx: TypeConverters.Context): Json =
    // Check if this is a tuple (fields named _1, _2, _3, etc.)
    val isTuple = record.fields.nonEmpty &&
      record.fields.zipWithIndex.forall { case (field, idx) =>
        field.name == s"_${idx + 1}"
      }

    if isTuple then
      TypeConverters.tupleToJsonSchema(record.fields.map(_.schema).toList, ctx)
    else
      val fields = record.fields
        .filter(Utilities.shouldIncludeField)
        .map { field =>
          val fieldSchema = TypeConverters.schemaToJsonSchema(field.schema, ctx)
          var fieldSchemaWithMetadata = MetadataExtractor.addMetadata(fieldSchema, field.annotations)

          // Add default value from field if it comes from @fieldDefaultValue annotation
          // The annotations check ensures we only use explicit annotation-based defaults
          val hasExplicitDefault = field.annotations.exists {
            case _: fieldDefaultValue[?] => true
            case _ => false
          }

          if hasExplicitDefault then
            field.defaultValue match
              case Some(defaultVal) =>
                fieldSchemaWithMetadata match
                  case Json.Obj(fields) =>
                    // Only add default if not already present (though it should be from extractMetadata)
                    if !fields.exists(_._1 == "default") then
                      fieldSchemaWithMetadata = Json.Obj((fields.toMap + ("default" -> Utilities.jsonFromAny(defaultVal))).toSeq*)
                  case _ => ()
              case None => ()

          val actualFieldName = Utilities.getFieldName(field)
          actualFieldName -> fieldSchemaWithMetadata
        }

      val required = record.fields
        .filter(Utilities.shouldIncludeField)
        .collect {
          case field if !Utilities.isOptional(field.schema) =>
            Json.Str(Utilities.getFieldName(field))
        }

      val properties = Json.Obj(fields.map { case (name, schema) => name -> schema }*)

      val baseObj = Map(
        "type" -> Json.Str("object"),
        "properties" -> properties
      )

      val withRequired = if required.nonEmpty then
        baseObj + ("required" -> Json.Arr(required*))
      else
        baseObj

      Json.Obj(withRequired.toSeq*)
