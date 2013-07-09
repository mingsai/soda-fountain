package com.socrata.soda.server.services

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.socrata.http.server.responses._
import com.socrata.http.server.implicits._
import com.rojoma.json.ast._
import dispatch._
import com.socrata.datacoordinator.client.{AddColumnInstruction, RenameColumnInstruction, DropColumnInstruction}
import com.socrata.soda.server.services.ClientRequestExtractor.ColumnSpec
import com.socrata.soda.server.services.ClientRequestExtractor._
import com.rojoma.json.util.JsonUtil

trait ColumnService extends SodaService {

  object columns {

    val log = org.slf4j.LoggerFactory.getLogger(classOf[ColumnService])

    def update(resourceName: String, columnName:String)(request:HttpServletRequest): HttpServletResponse => Unit =  {
      val start = System.currentTimeMillis
      if (!validName(resourceName)) { return sendInvalidNameError(resourceName, request)}
      jsonSingleObjectStream(request, MAX_DATUM_SIZE) match {
        case Right(obj) => ColumnSpec(obj) match {
            case Right(newCol) =>  withDatasetId(resourceName) { datasetId =>
              withDatasetSchema(datasetId) { schema =>
                schema.schema.get(columnName) match {
                  case Some(oldColType) => {
                    if (oldColType.equals(newCol.dataType.toString)){
                      if (columnName.equals(newCol.fieldName)) {
                        log.info(s"column.update for ${columnName} in ${resourceName} ${datasetId} no change OK - 204")
                        NoContent
                      }
                      else { //rename
                      val f = dc.update(datasetId, schemaHash(request), mockUser, Array(RenameColumnInstruction(columnName, newCol.fieldName)).iterator)
                        passThroughResponse(f, start, "column.rename", columnName, "to", newCol.fieldName, "in", resourceName, datasetId)
                      }
                    }
                    else { //drop and create
                    val f = dc.update(datasetId, schemaHash(request), mockUser, Array(DropColumnInstruction(columnName), AddColumnInstruction(newCol.fieldName, newCol.dataType)).iterator)
                      passThroughResponse(f, start, "column.drop.add", columnName, "dropped", newCol.fieldName, "added in", resourceName, datasetId)
                    }
                  }
                  case None => {
                    val f = dc.update(datasetId, schemaHash(request), mockUser, Array(AddColumnInstruction(newCol.fieldName, newCol.dataType)).iterator)
                    passThroughResponse(f, start, "column.add", newCol.fieldName, "in", resourceName, datasetId)
                  }
                }
              }
            }
            case Left(errs) => sendErrorResponse("bad column definition", "column.update.definition.invalid", BadRequest, Some(Map(("errors" -> JArray(errs.map(JString(_)))))), resourceName, columnName)
          }
        case Left(err) => sendErrorResponse("bad column definition", "column.update.definition.invalid", BadRequest, Some(Map(("error" -> JString(err)))), resourceName, columnName)
      }
    }

    def drop(resourceName: String, columnName:String)(request:HttpServletRequest): HttpServletResponse => Unit =  {
      val start = System.currentTimeMillis
      if (!validName(resourceName)) { return sendInvalidNameError(resourceName, request)}
      withDatasetId(resourceName) { datasetId =>
        val f = dc.update(datasetId, schemaHash(request), mockUser, Array(DropColumnInstruction(columnName)).iterator)
        f() match {
          case Right(response) =>
            passThroughResponse(response, start, "column.drop", columnName, resourceName, datasetId)
          case Left(err) => sendErrorResponse(err, "internal error during column drop", "column.drop.internal.error", InternalServerError, Some(Map(("column_name" -> JString(columnName)))), resourceName)
        }
      }
    }

    def getSchema(resourceName: String, columnName:String)(request:HttpServletRequest): HttpServletResponse => Unit =  {
      val start = System.currentTimeMillis
      if (!validName(resourceName)) { return sendInvalidNameError(resourceName, request)}
      withDatasetId(resourceName) { datasetId =>
        val f = dc.getSchema(datasetId)
        f() match {
          case Right(schema) =>
            val colType = schema.schema.get(columnName).getOrElse{ return sendErrorResponse(s"column ${columnName} not found in schema for ${resourceName}", "column.getSchema.not-found", NotFound, Some(Map(("column_name" -> JString(columnName)))), resourceName, columnName) }
            log.info(s"getSchema for ${columnName} (type ${colType}) in ${resourceName} ${datasetId} took ${System.currentTimeMillis - start} OK - 200")
            val obj = JObject(Map(
              "hash" -> JString(schema.hash),
              "field_name" -> JString(columnName),
              "datatype" -> JString(colType)
            ))
            OK ~> ContentType("application/json; charset=utf-8") ~> Content(obj.toString)
          case Left(err) => sendErrorResponse(new Exception(s"no schema found for dataset ${resourceName} ${datasetId}"), "internal error requesting dataset schema", "column.getSchema.internal.error", NotFound, Some(Map(("resource_name" -> JString(resourceName)))), resourceName)
        }
      }
    }
  }
}
