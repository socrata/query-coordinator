package com.socrata.querycoordinator.resources

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

import com.rojoma.json.v3.ast.{JString, JValue}
import com.rojoma.json.v3.codec.{JsonEncode, JsonDecode, DecodeError}
import com.rojoma.json.v3.util.{WrapperJsonCodec, AutomaticJsonCodec, NullForNone, AllowMissing, SimpleHierarchyCodecBuilder, InternalTag, AutomaticJsonCodecBuilder, JsonUtil}
import com.rojoma.simplearm.v2.ResourceScope
import org.apache.commons.io.HexDump
import org.slf4j.LoggerFactory
import org.joda.time.{DateTime, LocalDateTime}

import com.socrata.http.client.HttpClient
import com.socrata.http.server.{HttpRequest, HttpResponse}
import com.socrata.http.server.ext.{ResourceExt, Json, HeaderMap, HeaderName}
import com.socrata.soql.analyzer2.{MetaTypes, FoundTables, SoQLAnalyzer, UserParameters, DatabaseTableName, SoQLAnalysis, CanonicalName}
import com.socrata.soql.analyzer2.rewrite.Pass
import com.socrata.soql.environment.HoleName
import com.socrata.soql.serialize.{WriteBuffer, Writable}
import com.socrata.soql.types.{SoQLType, SoQLValue, SoQLFixedTimestamp, SoQLFloatingTimestamp}
import com.socrata.soql.functions.{SoQLTypeInfo, SoQLFunctionInfo}
import com.socrata.soql.stdlib.analyzer2.Context

import com.socrata.querycoordinator.Secondary
import com.socrata.querycoordinator.util.Lazy

class NewQueryResource(
  httpClient: HttpClient,
  secondary: Secondary
) extends QCResource with ResourceExt {
  val log = LoggerFactory.getLogger(classOf[NewQueryResource])

  case class DatasetInternalName(underlying: String)
  object DatasetInternalName {
    implicit val codec = WrapperJsonCodec[DatasetInternalName](apply(_), _.underlying)
    implicit object serialize extends Writable[DatasetInternalName] {
      def writeTo(buffer: WriteBuffer, s: DatasetInternalName) = buffer.write(s.underlying)
    }
  }

  case class Stage(underlying: String)
  object Stage {
    implicit val codec = WrapperJsonCodec[Stage]({ s: String => Stage(s.toLowerCase) }, _.underlying)
    implicit object serialize extends Writable[Stage] {
      def writeTo(buffer: WriteBuffer, s: Stage) = buffer.write(s.underlying)
    }
  }

  case class ColumnId(underlying: String)
  object ColumnId {
    implicit val codec = WrapperJsonCodec[ColumnId](apply(_), _.underlying)
    implicit object serialize extends Writable[ColumnId] {
      def writeTo(buffer: WriteBuffer, cid: ColumnId) = buffer.write(cid.underlying)
    }
  }

  final abstract class MT extends MetaTypes {
    override type ResourceNameScope = Int
    override type ColumnType = SoQLType
    override type ColumnValue = SoQLValue
    override type DatabaseTableNameImpl = (DatasetInternalName, Stage)
    override type DatabaseColumnNameImpl = ColumnId
  }

  val analyzer = new SoQLAnalyzer[MT](SoQLTypeInfo, SoQLFunctionInfo)

  private case class Request(analysis: SoQLAnalysis[MT], systemContext: Map[String, String], rewritePasses: Seq[Seq[Pass]])
  private object Request {
    implicit def serialize(implicit ev: Writable[SoQLAnalysis[MT]]) = new Writable[Request] {
      def writeTo(buffer: WriteBuffer, req: Request): Unit = {
        buffer.write(0)
        buffer.write(req.analysis)
        buffer.write(req.systemContext)
        buffer.write(req.rewritePasses)
      }
    }
  }

  @AutomaticJsonCodec
  case class Body(
    foundTables: FoundTables[MT],
    @AllowMissing("Context.empty")
    context: Context,
    @AllowMissing("Nil")
    rewritePasses: Seq[Seq[Pass]]
  )

  def doit(rs: ResourceScope, headers: HeaderMap, json: Json[Body]): HttpResponse = {
    val Json(
      reqData@Body(
        foundTables,
        context,
        rewritePasses
      )
    ) = json

    log.debug("Received request {}", Lazy(JsonUtil.renderJson(reqData)))

    analyzer(foundTables, context.user.toUserParameters) match {
      case Right(analysis) =>
        val serialized = WriteBuffer.asBytes(Request(analysis, context.system, rewritePasses))

        log.debug("Serialized analysis as:\n{}", Lazy(locally {
          val baos = new ByteArrayOutputStream
          HexDump.dump(serialized, 0, baos, 0)
          new String(baos.toByteArray, StandardCharsets.ISO_8859_1)
        }))

        var allSecondaries: Stream[Option[String]] =
          analysis.statement.allTables.toStream.scanLeft((Option.empty[String], Set.empty[String])) { (acc, dss) =>
            val (_, alreadySeen) = acc
            val DatabaseTableName((DatasetInternalName(n), Stage(s))) = dss
            secondary.chosenSecondaryName(None, n, Some(s), alreadySeen) match {
              case Some(secondary) => (Some(secondary), alreadySeen + secondary)
              case None => (None, alreadySeen)
            }
          }.map(_._1).tail

        // ok so it's possible that allTables is empty, in which case
        // we don't care what secondary we're sending this to.
        if(allSecondaries.isEmpty) {
          allSecondaries = Stream(Some(secondary.arbitrarySecondary()))
        }

        val additionalHeaders = Seq("if-none-match", "if-match", "if-modified-since").flatMap { h =>
          headers.lookup(HeaderName(h)).map { v => h -> v.underlying }
        }

        for {
          chosenSecondary <- allSecondaries.takeWhile(_.isDefined).map(_.get)
          instance <- secondary.unassociatedServiceInstance(chosenSecondary)
        } {
          val req = secondary.reqBuilder(instance)
            .p("new-query")
            .addHeaders(additionalHeaders)
            .blob(new ByteArrayInputStream(serialized))
          val resp = httpClient.execute(req, rs)

          return resp.resultCode match {
            case 200 =>
              import com.socrata.http.server.responses._
              import com.socrata.http.server.implicits._

              val base = Seq("content-type", "etag", "last-modified").foldLeft[HttpResponse](OK) { (acc, hdrName) =>
                resp.headers(hdrName).foldLeft(acc) { (acc, value) =>
                  acc ~> Header(hdrName, value)
                }
              }

              base ~> Stream(resp.inputStream())

            case 304 =>
              import com.socrata.http.server.responses._
              import com.socrata.http.server.implicits._

              val base = Seq("content-type", "etag", "last-modified").foldLeft[HttpResponse](NotModified) { (acc, hdrName) =>
                resp.headers(hdrName).foldLeft(acc) { (acc, value) =>
                  acc ~> Header(hdrName, value)
                }
              }

              base ~> Stream(resp.inputStream())

            case other =>
              throw new Exception("Unexpected result code " + other)
          }
        }

        throw new Exception("Failed to find a secondary") // TODO
      case Left(err) =>
        throw new Exception("Compiler error: " + err) // TODO: compiler error
    }
  }

  override val query: HttpRequest => HttpResponse = handle(_)(doit _)
}
