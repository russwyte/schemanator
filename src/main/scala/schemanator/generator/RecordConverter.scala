package schemanator.generator

import zio.*
import zio.schema.*
import zio.schema.annotation.*
import zio.json.ast.Json

private[schemanator] object RecordConverter {

  /** Convert a ZIO Schema Record to JSON Schema */
  def recordToJsonSchema(record: Schema.Record[?], ctx: TypeConverters.Context): Json = {
    // Get the schema ID if present}
    // Check if this is a tuple (fields named _1, _2, _3, etc.)
    val isTuple = record.fields.nonEmpty &&
      record.fields.zipWithIndex.forall { case (field, idx) =>
        field.name == s"_${idx + 1}"
      }

    if (isTuple)
      TypeConverters.tupleToJsonSchema(record.fields.map(_.schema).toList, ctx)
    else {
      // Check if the record has @requireAll or @strict annotation
      val hasRequireAll = record.annotations.exists {
        case _: schemanator.annotations.requireAll => true
        case _: schemanator.annotations.strict => true
        case _ => false
      }

      val fields = record.fields
        .filter(Utilities.shouldIncludeField)
        .map { field =>
          // Check if this is an optional field with @requiredField or if @requireAll is set
          val isRequiredOptional = Utilities.isOptional(field.schema) && (hasRequireAll || Utilities.hasRequiredAnnotation(field))

          val fieldSchema = if (isRequiredOptional)
            // For optional fields marked as required, make the type nullable
            Utilities.makeNullable(TypeConverters.schemaToJsonSchema(Utilities.unwrapOptional(field.schema), ctx))
          else
            TypeConverters.schemaToJsonSchema(field.schema, ctx)

          var fieldSchemaWithMetadata = MetadataExtractor.addMetadata(fieldSchema, field.annotations)

          // Add default value from field if it comes from @fieldDefaultValue annotation
          // The annotations check ensures we only use explicit annotation-based defaults
          val hasExplicitDefault = field.annotations.exists {
            case _: fieldDefaultValue[?] => true
            case _ => false
          }

          if (hasExplicitDefault)
            field.defaultValue match {
              case Some(defaultVal) =>
                fieldSchemaWithMetadata match {
                  case Json.Obj(fields) =>
                    // Only add default if not already present (though it should be from extractMetadata)
                    if (!fields.exists(_._1 == "default"))
                      fieldSchemaWithMetadata = Json.Obj((fields.toMap + ("default" -> Utilities.jsonFromAny(defaultVal))).toSeq*)
                  case _ => ()
                }
              case None => ()
            }

          val actualFieldName = Utilities.getFieldName(field)
          actualFieldName -> fieldSchemaWithMetadata
        }

      val required = record.fields
        .filter(Utilities.shouldIncludeField)
        .collect {
          case field if hasRequireAll || !Utilities.isOptional(field.schema) || Utilities.hasRequiredAnnotation(field) =>
            Json.Str(Utilities.getFieldName(field))
        }

      val properties = Json.Obj(fields.map { case (name, schema) => name -> schema }*)

      // Check if the record has @additionalProperties or @strict annotation
      val additionalPropsValue: Option[Boolean] = record.annotations.collectFirst {
        case ap: schemanator.annotations.additionalProperties => ap.allowed
        case _: schemanator.annotations.strict => false
      }

      // Build result preserving field order: type, properties, required (if present), additionalProperties (if present)
      val baseFields = Chunk(
        "type" -> Json.Str("object"),
        "properties" -> properties
      )

      val withRequired = if (required.nonEmpty)
        baseFields :+ ("required" -> Json.Arr(required*))
      else
        baseFields

      val allFields = additionalPropsValue match {
        case Some(allowed) => withRequired :+ ("additionalProperties" -> Json.Bool(allowed))
        case None => withRequired
      }

      Json.Obj(allFields*)
    }
  }
}