package com.socrata.soda.clients.datacoordinator

import com.rojoma.json.v3.util._
import com.rojoma.json.v3.ast._
import com.rojoma.simplearm.v2.ResourceScope
import com.socrata.soda.server.id._
import com.socrata.soda.server.persistence.ColumnRecord
import com.socrata.soda.server.util.CopySpecifier
import com.socrata.soda.server.util.schema.SchemaSpec
import com.socrata.http.server.util.{Precondition, EntityTag}
import org.joda.time.DateTime

object DataCoordinatorClient {

  val client = "DC"


  case class VersionReport(val version: Long)
  object VersionReport{
    implicit val codec = SimpleJsonCodecBuilder[VersionReport].build("version", _.version)
  }

  case class ReportMetaData(val datasetId: DatasetId, val version: Long, val lastModified: DateTime)

  sealed abstract class ReportItem
  case class UpsertReportItem(data: Iterator[JValue] /* Note: this MUST be completely consumed before calling hasNext/next on parent iterator! */) extends ReportItem
  case object OtherReportItem extends ReportItem


  sealed abstract class Result
  sealed class FailResult extends Result
  sealed class SuccessResult extends Result

  // SUCCESS CASES
  case class NonCreateScriptResult(report: Iterator[ReportItem], etag: Option[EntityTag], copyNumber: Long, newVersion: Long, lastModified: DateTime) extends SuccessResult
  case class ExportResult(json: Iterator[JValue], etag: Option[EntityTag]) extends SuccessResult



  // FAIL CASES
  case class SchemaOutOfDateResult(newSchema: SchemaSpec) extends FailResult
  case class NotModifiedResult(etags: Seq[EntityTag]) extends FailResult
  case class IncorrectLifecycleStageResult(actualStage: String, expectedStage: Set[String]) extends FailResult
  case class NoSuchRollupResult(name: RollupName, commandIndex: Long) extends FailResult
  case object PreconditionFailedResult extends FailResult
  case class InternalServerErrorResult(code: String, tag: String, data: String) extends FailResult
  case class InvalidLocaleResult(locale: String, commandIndex: Long) extends FailResult
  case object InvalidRowIdResult extends FailResult



  // FAIL CASES: Rows
  case class NoSuchRowResult(id: RowSpecifier, commandIndex: Long) extends FailResult
  case class RowPrimaryKeyNonexistentOrNullResult(id: RowSpecifier, commandIndex: Long) extends FailResult
  case class UnparsableRowValueResult(columnId: ColumnId,tp: String ,value: JValue, commandIndex: Long, commandSubIndex: Long) extends FailResult
  case class RowNoSuchColumnResult(columnId: ColumnId, commandIndex: Long, commandSubIndex: Long) extends FailResult
  case class CannotDeleteRowIdResult(commandIndex: Long) extends FailResult


  // FAIL CASES: Columns
  case class DuplicateValuesInColumnResult(datasetId: DatasetId, columnId: ColumnId, commandIndex: Long) extends FailResult
  case class ColumnExistsAlreadyResult(datasetId: DatasetId, columnId: ColumnId, commandIndex: Long) extends FailResult
  case class IllegalColumnIdResult(columnId: ColumnId, commandIndex: Long) extends FailResult
  case class InvalidSystemColumnOperationResult(datasetId: DatasetId, column: ColumnId, commandIndex: Long) extends FailResult
  case class ColumnNotFoundResult(datasetId: DatasetId, column: ColumnId, commandIndex: Long) extends FailResult

  // FAIL CASES: Datasets
  case class DatasetNotFoundResult(datasetId: DatasetId) extends FailResult
  case class SnapshotNotFoundResult(datasetId: DatasetId, snapshot: CopySpecifier) extends FailResult
  case class CannotAcquireDatasetWriteLockResult(datasetId: DatasetId) extends FailResult
  case class InitialCopyDropResult(datasetId: DatasetId, commandIndex: Long) extends FailResult
  case class OperationAfterDropResult(datasetId: DatasetId, commandIndex: Long) extends FailResult

  // FAIL CASES: Updates
  case class NotPrimaryKeyResult(datasetId: DatasetId, columnId: ColumnId, commandIndex: Long) extends FailResult
  case class NullsInColumnResult(datasetId: DatasetId, columnId: ColumnId, commandIndex: Long) extends FailResult
  case class InvalidTypeForPrimaryKeyResult(datasetId: DatasetId, columnId: ColumnId,
                                            tp: String, commandIndex: Long) extends FailResult
  case class PrimaryKeyAlreadyExistsResult(datasetId: DatasetId, columnId: ColumnId,
                                           existing: ColumnId, commandIndex: Long) extends FailResult
  case class NoSuchTypeResult(tp: String, commandIndex: Long) extends FailResult
  case class RowVersionMismatchResult(dataset: DatasetId,
                                      value: JValue,
                                      commandIndex: Long,
                                      expected: Option[JValue],
                                      actual: Option[JValue]) extends FailResult
  case class VersionOnNewRowResult(datasetId: DatasetId, commandIndex: Long) extends FailResult
  case class ScriptRowDataInvalidValueResult(datasetId: DatasetId, value: JValue,
                                             commandIndex: Long, commandSubIndex: Long) extends FailResult
}

trait DataCoordinatorClient {
  import DataCoordinatorClient._

  def propagateToSecondary(datasetId: DatasetId,
                           secondaryId: SecondaryId,
                           extraHeaders: Map[String, String] = Map.empty)
  def getSchema(datasetId: DatasetId): Option[SchemaSpec]

  def create(instance: String,
             user: String,
             instructions: Option[Iterator[DataCoordinatorInstruction]],
             locale: String = "en_US",
             extraHeaders: Map[String, String] = Map.empty) : (ReportMetaData, Iterable[ReportItem])

  def update[T](datasetId: DatasetId,
                schemaHash: String,
                user: String,
                instructions: Iterator[DataCoordinatorInstruction],
                extraHeaders: Map[String, String] = Map.empty)
               (f: Result => T): T

  def copy[T](datasetId: DatasetId,
              schemaHash: String,
              copyData: Boolean,
              user: String,
              instructions: Iterator[DataCoordinatorInstruction] = Iterator.empty,
              extraHeaders: Map[String, String] = Map.empty)
             (f: Result => T): T

  def publish[T](datasetId: DatasetId,
                 schemaHash: String,
                 keepSnapshot:Option[Boolean],
                 user: String,
                 instructions: Iterator[DataCoordinatorInstruction] = Iterator.empty,
                 extraHeaders: Map[String, String] = Map.empty)
                (f: Result => T): T

  def dropCopy[T](datasetId: DatasetId,
                  schemaHash: String,
                  user: String,
                  instructions: Iterator[DataCoordinatorInstruction] = Iterator.empty,
                  extraHeaders: Map[String, String] = Map.empty)
                 (f: Result => T): T

  def deleteAllCopies[T](datasetId: DatasetId,
                         schemaHash: String,
                         user: String,
                         extraHeaders: Map[String, String] = Map.empty)
                        (f: Result => T): T

  def checkVersionInSecondary(datasetId: DatasetId,
                              secondaryId: SecondaryId,
                              extraHeaders: Map[String, String] = Map.empty): VersionReport

  def datasetsWithSnapshots(): Set[DatasetId]
  def listSnapshots(datasetId: DatasetId): Option[Seq[Long]]
  def deleteSnapshot(datasetId: DatasetId, copy: Long): Either[FailResult, Unit]

  def exportSimple(datasetId: DatasetId, copy: String, resourceScope: ResourceScope): Result

  def export(datasetId: DatasetId,
             schemaHash: String,
             columns: Seq[String],
             precondition: Precondition,
             ifModifiedSince: Option[DateTime],
             limit: Option[Long],
             offset: Option[Long],
             copy: String,
             sorted: Boolean,
             rowId: Option[String],
             extraHeaders: Map[String, String],
             resourceScope: ResourceScope): Result
}
