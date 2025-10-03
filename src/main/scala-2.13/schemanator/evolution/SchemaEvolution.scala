package schemanator.evolution

import zio.schema.*

/**
 * Schema Evolution tracking and compatibility analysis.
 *
 * Compares two schemas and detects changes, providing compatibility information.
 *
 * Example:
 * {{{
 *   case class PersonV1(name: String, age: Int)
 *   implicit val schemaV1: Schema[PersonV1] = DeriveSchema.gen[PersonV1]
 *
 *   case class PersonV2(name: String, age: Int, email: Option[String])
 *   implicit val schemaV2: Schema[PersonV2] = DeriveSchema.gen[PersonV2]
 *
 *   val result = SchemaEvolution.compareSchemas(schemaV1, schemaV2)
 *   // result.compatibility == CompatibilityType.FullyCompatible
 * }}}
 */
object SchemaEvolution {

  sealed trait FieldChange
  object FieldChange {
    case class FieldAdded(name: String, schema: Schema[?], optional: Boolean) extends FieldChange
    case class FieldRemoved(name: String, schema: Schema[?]) extends FieldChange
    case class FieldTypeChanged(name: String, oldSchema: Schema[?], newSchema: Schema[?]) extends FieldChange
  }

  sealed trait CompatibilityType
  object CompatibilityType {
    case object FullyCompatible extends CompatibilityType
    case object BackwardCompatible extends CompatibilityType
    case object ForwardCompatible extends CompatibilityType
    case object Breaking extends CompatibilityType
  }

  case class EvolutionResult(
    changes: List[FieldChange],
    compatibility: CompatibilityType,
    messages: List[String]
  )

  def compareSchemas[A, B](oldSchema: Schema[A], newSchema: Schema[B]): EvolutionResult =
    (oldSchema, newSchema) match {
      case (oldRecord: Schema.Record[?], newRecord: Schema.Record[?]) =>
        compareRecords(oldRecord, newRecord)
      case _ =>
        EvolutionResult(Nil, CompatibilityType.Breaking, List("Can only compare record schemas"))
    }

  private def compareRecords(oldRecord: Schema.Record[?], newRecord: Schema.Record[?]): EvolutionResult = {
    val oldFieldsSeq = oldRecord.fields.toList
    val newFieldsSeq = newRecord.fields.toList

    val oldFieldsByName: Map[String, Schema[?]] = oldFieldsSeq.map(f => f.name -> f.schema).toMap
    val newFieldsByName: Map[String, Schema[?]] = newFieldsSeq.map(f => f.name -> f.schema).toMap

    val addedFields: List[FieldChange.FieldAdded] = (newFieldsByName.keySet -- oldFieldsByName.keySet).toList.map { name =>
      val fieldSchema = newFieldsByName(name)
      FieldChange.FieldAdded(name, fieldSchema, isOptional(fieldSchema))
    }

    val removedFields: List[FieldChange.FieldRemoved] = (oldFieldsByName.keySet -- newFieldsByName.keySet).toList.map { name =>
      val fieldSchema = oldFieldsByName(name)
      FieldChange.FieldRemoved(name, fieldSchema)
    }

    val changedFields: List[FieldChange.FieldTypeChanged] = (oldFieldsByName.keySet intersect newFieldsByName.keySet).toList.flatMap { name =>
      val oldFieldSchema = oldFieldsByName(name)
      val newFieldSchema = newFieldsByName(name)
      if (!schemasEqual(oldFieldSchema, newFieldSchema)) {
        Some(FieldChange.FieldTypeChanged(name, oldFieldSchema, newFieldSchema))
      } else {
        None
      }
    }

    val allChanges: List[FieldChange] = addedFields ++ removedFields ++ changedFields
    val (compatibility, messages) = determineCompatibility(addedFields, removedFields, changedFields)

    EvolutionResult(allChanges, compatibility, messages)
  }

  private def determineCompatibility(
    added: List[FieldChange.FieldAdded],
    removed: List[FieldChange.FieldRemoved],
    changed: List[FieldChange.FieldTypeChanged]
  ): (CompatibilityType, List[String]) = {
    val messages = scala.collection.mutable.ListBuffer[String]()

    val hasBreakingAdditions = added.exists(!_.optional)
    val hasRemovals = removed.nonEmpty
    val hasChanges = changed.nonEmpty

    if (hasBreakingAdditions) {
      messages += "Added required fields break backward compatibility"
    }
    if (hasRemovals) {
      messages += "Removed fields break forward compatibility"
    }
    if (hasChanges) {
      messages ++= changed.map(c => s"Field '${c.name}' type changed")
    }

    val compatibility = (hasBreakingAdditions, hasRemovals, hasChanges) match {
      case (false, false, false) => CompatibilityType.FullyCompatible
      case (false, true, false) => CompatibilityType.BackwardCompatible
      case (true, false, false) => CompatibilityType.ForwardCompatible
      case _ => CompatibilityType.Breaking
    }

    (compatibility, messages.toList)
  }

  private def schemasEqual(s1: Schema[?], s2: Schema[?]): Boolean =
    (s1, s2) match {
      case (Schema.Lazy(inner1), s2) => schemasEqual(inner1(), s2)
      case (s1, Schema.Lazy(inner2)) => schemasEqual(s1, inner2())
      case (Schema.Primitive(t1, _), Schema.Primitive(t2, _)) => t1 == t2
      case (Schema.Optional(inner1, _), Schema.Optional(inner2, _)) => schemasEqual(inner1, inner2)
      case (Schema.Sequence(elem1, _, _, _, _), Schema.Sequence(elem2, _, _, _, _)) => schemasEqual(elem1, elem2)
      case (r1: Schema.Record[?], r2: Schema.Record[?]) => r1.id == r2.id
      case _ => false
    }

  private def isOptional(schema: Schema[?]): Boolean =
    schema match {
      case _: Schema.Optional[?] => true
      case Schema.Lazy(schema0) => isOptional(schema0())
      case _ => false
    }
}
