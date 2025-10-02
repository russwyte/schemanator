import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.schema.*
import schemanator.evolution.*

object SchemaEvolutionSpec extends ZIOSpecDefault:

  def spec = suite("SchemaEvolution")(
    test("detects field addition with optional field") {
      case class PersonV1(name: String, age: Int) derives Schema
      case class PersonV2(name: String, age: Int, email: Option[String]) derives Schema

      val result = SchemaEvolution.compareSchemas(Schema[PersonV1], Schema[PersonV2])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldAdded("email", _, true) => true
          case _ => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.FullyCompatible
      )
    },
    test("detects field addition with required field") {
      case class PersonV1(name: String) derives Schema
      case class PersonV2(name: String, age: Int) derives Schema

      val result = SchemaEvolution.compareSchemas(Schema[PersonV1], Schema[PersonV2])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldAdded("age", _, false) => true
          case _ => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.ForwardCompatible
      )
    },
    test("detects field removal") {
      case class PersonV1(name: String, age: Int, email: String) derives Schema
      case class PersonV2(name: String, age: Int) derives Schema

      val result = SchemaEvolution.compareSchemas(Schema[PersonV1], Schema[PersonV2])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldRemoved("email", _) => true
          case _ => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.BackwardCompatible
      )
    },
    test("detects field type change") {
      case class PersonV1(name: String, age: Int) derives Schema
      case class PersonV2(name: String, age: String) derives Schema

      val result = SchemaEvolution.compareSchemas(Schema[PersonV1], Schema[PersonV2])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldTypeChanged("age", _, _) => true
          case _ => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.Breaking
      )
    },
    test("detects multiple changes as breaking") {
      case class PersonV1(name: String, age: Int) derives Schema
      case class PersonV2(name: String, age: String, email: String) derives Schema

      val result = SchemaEvolution.compareSchemas(Schema[PersonV1], Schema[PersonV2])

      assertTrue(
        result.changes.size == 2,
        result.compatibility == SchemaEvolution.CompatibilityType.Breaking
      )
    },
  )
end SchemaEvolutionSpec
