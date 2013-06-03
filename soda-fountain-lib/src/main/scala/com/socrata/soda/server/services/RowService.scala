package com.socrata.soda.server.services

import com.socrata.http.server.responses._
import com.socrata.http.server.implicits._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import dispatch._
import com.socrata.datacoordinator.client._
import com.rojoma.json.ast._
import com.rojoma.json.util.JsonUtil
import com.socrata.soda.server.typefitting.TypeFitter

trait RowService extends SodaService {

  object rows {

    def upsert(resourceName: String, rowId: String)(request:HttpServletRequest): HttpServletResponse => Unit = {
      try {
        val fields = JsonUtil.readJson[Map[String,JValue]](request.getReader)
        fields match {
          case Some(map) => {
            withDatasetId(resourceName){ datasetId =>
              withDatasetSchema(datasetId){ schema =>
                map.foreach{ pair =>
                  val expectedTypeName = schema.schema.get(pair._1).getOrElse( return sendErrorResponse("no column " + pair._1, "column.not.found", BadRequest, Some(JObject(map))))
                  TypeFitter.check(expectedTypeName, pair._2) match {
                    case Right(v) => v
                    case Left(msg) => return sendErrorResponse(msg, "type.error", BadRequest, Some(pair._2))
                  }
                }
                val schemaHash = Option(request.getParameter("schema"))
                val response = dc.update(datasetId, schemaHash, mockUser, Array(UpsertRow(map)).iterator)
                response() match {
                  case Right(resp) => DataCoordinatorClient.passThroughResponse(resp)
                  case Left(th) => sendErrorResponse(th.getMessage, "internal.error", InternalServerError, None)
                }
              }
            }
          }
          case None => sendErrorResponse("could not parse request body as single JSON object", "parse.error", BadRequest, None)
        }
      } catch {
        case e: Exception => sendErrorResponse("could not parse request body as JSON: " + e.getMessage, "parse.error", UnsupportedMediaType, None)
        case _: Throwable => sendErrorResponse("error processing request", "internal.error", InternalServerError, None)
      }
    }

    def delete(resourceName: String, rowId: String)(request:HttpServletRequest): HttpServletResponse => Unit = ???
    def get(resourceName: String, rowId: String)(request:HttpServletRequest): HttpServletResponse => Unit = ???
  }
}
