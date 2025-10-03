import zio.*
import zio.test.*
import zio.schema.*
import schemanator.evolution.*

object SchemaEvolutionSpec extends ZIOSpecDefault {

  // PersonV1 variants
  case class PersonV1a(name: String, age: Int)
  object PersonV1a {
    implicit val schema: Schema[PersonV1a] = DeriveSchema.gen[PersonV1a]
  }

  case class PersonV1b(name: String)
  object PersonV1b {
    implicit val schema: Schema[PersonV1b] = DeriveSchema.gen[PersonV1b]
  }

  case class PersonV1c(name: String, age: Int, email: String)
  object PersonV1c {
    implicit val schema: Schema[PersonV1c] = DeriveSchema.gen[PersonV1c]
  }

  case class PersonV1d(name: String, age: Int)
  object PersonV1d {
    implicit val schema: Schema[PersonV1d] = DeriveSchema.gen[PersonV1d]
  }

  case class PersonV1e(name: String, age: Int)
  object PersonV1e {
    implicit val schema: Schema[PersonV1e] = DeriveSchema.gen[PersonV1e]
  }

  // PersonV2 variants
  case class PersonV2a(name: String, age: Int, email: Option[String])
  object PersonV2a {
    implicit val schema: Schema[PersonV2a] = DeriveSchema.gen[PersonV2a]
  }

  case class PersonV2b(name: String, age: Int)
  object PersonV2b {
    implicit val schema: Schema[PersonV2b] = DeriveSchema.gen[PersonV2b]
  }

  case class PersonV2c(name: String, age: Int)
  object PersonV2c {
    implicit val schema: Schema[PersonV2c] = DeriveSchema.gen[PersonV2c]
  }

  case class PersonV2d(name: String, age: String)
  object PersonV2d {
    implicit val schema: Schema[PersonV2d] = DeriveSchema.gen[PersonV2d]
  }

  case class PersonV2e(name: String, age: String, email: String)
  object PersonV2e {
    implicit val schema: Schema[PersonV2e] = DeriveSchema.gen[PersonV2e]
  }

  def spec: Spec[Any, Nothing] = suite("SchemaEvolution")(
    test("detects field addition with optional field") {
      val result = SchemaEvolution.compareSchemas(Schema[PersonV1a], Schema[PersonV2a])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldAdded("email", _, true) => true
          case _                                                        => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.FullyCompatible,
      )
    },
    test("detects field addition with required field") {
      val result = SchemaEvolution.compareSchemas(Schema[PersonV1b], Schema[PersonV2b])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldAdded("age", _, false) => true
          case _                                                       => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.ForwardCompatible,
      )
    },
    test("detects field removal") {
      val result = SchemaEvolution.compareSchemas(Schema[PersonV1c], Schema[PersonV2c])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldRemoved("email", _) => true
          case _                                                    => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.BackwardCompatible,
      )
    },
    test("detects field type change") {
      val result = SchemaEvolution.compareSchemas(Schema[PersonV1d], Schema[PersonV2d])

      assertTrue(
        result.changes.size == 1,
        result.changes.head match {
          case SchemaEvolution.FieldChange.FieldTypeChanged("age", _, _) => true
          case _                                                         => false
        },
        result.compatibility == SchemaEvolution.CompatibilityType.Breaking,
      )
    },
    test("detects multiple changes as breaking") {
      val result = SchemaEvolution.compareSchemas(Schema[PersonV1e], Schema[PersonV2e])

      assertTrue(
        result.changes.size == 2,
        result.compatibility == SchemaEvolution.CompatibilityType.Breaking,
      )
    },
  )
}
