package schemanator.evolution

import zio.schema.*

/**
 * Schema Evolution tracking and compatibility analysis.
 *
 * Compares two schemas and detects changes, providing compatibility information.
 *
 * Example:
 * {{{
 *   case class PersonV1(name: String, age: Int) derives Schema
 *   case class PersonV2(name: String, age: Int, email: Option[String]) derives Schema
 *
 *   val result = SchemaEvolution.compareSchemas(Schema[PersonV1], Schema[PersonV2])
 *   // result.compatibility == FullyCompatible
 * }}}
 */
object SchemaEvolution:

  enum FieldChange:
    case FieldAdded(name: String, schema: Schema[?], optional: Boolean)
    case FieldRemoved(name: String, schema: Schema[?])
    case FieldTypeChanged(name: String, oldSchema: Schema[?], newSchema: Schema[?])

  enum CompatibilityType:
    case FullyCompatible
    case BackwardCompatible
    case ForwardCompatible
    case Breaking

  case class EvolutionResult(
    changes: List[FieldChange],
    compatibility: CompatibilityType,
    messages: List[String]
  )

  def compareSchemas[A, B](oldSchema: Schema[A], newSchema: Schema[B]): EvolutionResult =
    (oldSchema, newSchema) match
      case (oldRecord: Schema.Record[?], newRecord: Schema.Record[?]) =>
        compareRecords(oldRecord, newRecord)
      case _ =>
        EvolutionResult(Nil, CompatibilityType.Breaking, List("Can only compare record schemas"))

  private def compareRecords(oldRecord: Schema.Record[?], newRecord: Schema.Record[?]): EvolutionResult =
    val oldFields = oldRecord.fields.map(f => f.name -> f).toMap
    val newFields = newRecord.fields.map(f => f.name -> f).toMap

    val addedFields: List[FieldChange.FieldAdded] = newFields.keySet.diff(oldFields.keySet).toList.map { name =>
      val field = newFields(name)
      FieldChange.FieldAdded(name, field.schema, isOptional(field.schema))
    }

    val removedFields: List[FieldChange.FieldRemoved] = oldFields.keySet.diff(newFields.keySet).toList.map { name =>
      val field = oldFields(name)
      FieldChange.FieldRemoved(name, field.schema)
    }

    val changedFields: List[FieldChange.FieldTypeChanged] = oldFields.keySet.intersect(newFields.keySet).toList.flatMap { name =>
      val oldField = oldFields(name)
      val newField = newFields(name)
      if !schemasEqual(oldField.schema, newField.schema) then
        Some(FieldChange.FieldTypeChanged(name, oldField.schema, newField.schema))
      else
        None
    }

    val allChanges: List[FieldChange] = addedFields ++ removedFields ++ changedFields
    val (compatibility, messages) = determineCompatibility(addedFields, removedFields, changedFields)

    EvolutionResult(allChanges, compatibility, messages)

  private def determineCompatibility(
    added: List[FieldChange.FieldAdded],
    removed: List[FieldChange.FieldRemoved],
    changed: List[FieldChange.FieldTypeChanged]
  ): (CompatibilityType, List[String]) =
    val messages = scala.collection.mutable.ListBuffer[String]()

    val hasBreakingAdditions = added.exists(!_.optional)
    val hasRemovals = removed.nonEmpty
    val hasChanges = changed.nonEmpty

    if hasBreakingAdditions then
      messages += "Added required fields break backward compatibility"
    if hasRemovals then
      messages += "Removed fields break forward compatibility"
    if hasChanges then
      messages ++= changed.map(c => s"Field '${c.name}' type changed")

    val compatibility = (hasBreakingAdditions, hasRemovals, hasChanges) match
      case (false, false, false) => CompatibilityType.FullyCompatible
      case (false, true, false) => CompatibilityType.BackwardCompatible
      case (true, false, false) => CompatibilityType.ForwardCompatible
      case _ => CompatibilityType.Breaking

    (compatibility, messages.toList)

  private def schemasEqual(s1: Schema[?], s2: Schema[?]): Boolean =
    (s1, s2) match
      case (Schema.Lazy(inner1), s2) => schemasEqual(inner1(), s2)
      case (s1, Schema.Lazy(inner2)) => schemasEqual(s1, inner2())
      case (Schema.Primitive(t1, _), Schema.Primitive(t2, _)) => t1 == t2
      case (Schema.Optional(inner1, _), Schema.Optional(inner2, _)) => schemasEqual(inner1, inner2)
      case (Schema.Sequence(elem1, _, _, _, _), Schema.Sequence(elem2, _, _, _, _)) => schemasEqual(elem1, elem2)
      case (r1: Schema.Record[?], r2: Schema.Record[?]) => r1.id == r2.id
      case _ => false

  private def isOptional(schema: Schema[?]): Boolean =
    schema match
      case _: Schema.Optional[?] => true
      case Schema.Lazy(schema0) => isOptional(schema0())
      case _ => false
