package com.socrata.querycoordinator.fusion

import com.socrata.querycoordinator.QueryParser.SuccessfulParse
import com.socrata.querycoordinator.{QueryParser, TestBase}
import com.socrata.querycoordinator.caching.SoQLAnalysisDepositioner
import com.socrata.soql.environment.ColumnName
import com.socrata.soql.functions._
import com.socrata.soql.SoQLAnalyzer
import com.socrata.soql.typed._
import com.socrata.soql.types._

import scala.util.parsing.input.NoPosition

class QueryParserFuseTest extends TestBase {
  import QueryParserFuseTest._ // scalastyle:ignore import.grouping

  test("SELECT * -- compound type fusing") {
    val query = "select * where location.latitude = 1.1"
    val starPos = query.indexOf("*") + 1

    val locationFc = FunctionCall(SoQLFunctions.Location.monomorphic.get, Seq(
      ColumnRef("location", SoQLPoint.t)(NoPosition),
      ColumnRef("location_address", SoQLText.t)(NoPosition),
      ColumnRef("location_city", SoQLText.t)(NoPosition),
      ColumnRef("location_state", SoQLText.t)(NoPosition),
      ColumnRef("location_zip", SoQLText.t)(NoPosition)
    ))(NoPosition, NoPosition)

    val phoneFc = FunctionCall(SoQLFunctions.Phone.monomorphic.get, Seq(
      ColumnRef("phone", SoQLText.t)(NoPosition),
      ColumnRef("phone_type", SoQLText.t)(NoPosition)
    ))(NoPosition, NoPosition)

    val eqBindings = SoQLFunctions.Eq.parameters.map {
      case VariableType(name) => name -> SoQLNumber
      case _ => throw new Exception("Unexpected function signature")
    }.toMap
    val eq = MonomorphicFunction(SoQLFunctions.Eq, eqBindings)

    val ptlFc = FunctionCall(SoQLFunctions.PointToLatitude.monomorphic.get,
      Seq(ColumnRef("location", SoQLPoint.t)(NoPosition)))(NoPosition, NoPosition)

    val expectedWhere: CoreExpr[String, SoQLType] =
      FunctionCall(eq, Seq(ptlFc, NumberLiteral(1.1, SoQLNumber.t)(NoPosition)))(NoPosition, NoPosition)

    val expectedSelection = com.socrata.soql.collection.OrderedMap(
      ColumnName("a") -> ColumnRef("ai", SoQLText)(NoPosition),
      ColumnName("b") -> ColumnRef("bi", SoQLText)(NoPosition),
      ColumnName("location") -> locationFc,
      ColumnName("phone") -> phoneFc
    )

    val fuse = Map("location" -> "location", "phone" -> "phone")
    val actual = qp.apply(query, truthColumns, schema, fuse) match {
      case SuccessfulParse(analyses) =>
        val actual = SoQLAnalysisDepositioner(analyses.head)
        actual.selection should be(expectedSelection)
        actual.where should be(Some(expectedWhere))
      case _: QueryParser.Result =>
        fail("fail to parse soql")
    }
  }
}

object QueryParserFuseTest {

  val defaultRowLimit = 20

  val maxRowLimit = 200

  val analyzer = new SoQLAnalyzer(SoQLTypeInfo, SoQLFunctionInfo)

  val qp = new QueryParser(analyzer, Some(maxRowLimit), defaultRowLimit)

  val truthColumns = Map[ColumnName, String](
    ColumnName("a") -> "ai",
    ColumnName("b") -> "bi",
    ColumnName("location") -> "location",
    ColumnName("location_address") -> "location_address",
    ColumnName("location_city") -> "location_city",
    ColumnName("location_state") -> "location_state",
    ColumnName("location_zip") -> "location_zip",
    ColumnName("phone") -> "phone",
    ColumnName("phone_type") -> "phone_type"
  )

  val schema = Map[String, SoQLType](
    "ai" -> SoQLText,
    "bi" -> SoQLText,
    "location" -> SoQLPoint,
    "location_address" -> SoQLText,
    "location_city" -> SoQLText,
    "location_state" -> SoQLText,
    "location_zip" -> SoQLText,
    "phone" -> SoQLText,
    "phone_type" -> SoQLText
  )
}
