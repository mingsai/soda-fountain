package com.socrata.soda.server.services

import com.socrata.http.server.responses._
import com.socrata.http.server.implicits._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}


object DatasetService {

  def query(datasetResourceName: String)(request:HttpServletRequest): HttpServletResponse => Unit =  {
    ImATeapot ~> ContentType("text/plain; charset=utf-8") ~> Content("resource request not implemented")
  }

  def get(datasetResourceName: String, rowId:String)(request:HttpServletRequest): HttpServletResponse => Unit =  {
    ImATeapot ~> ContentType("text/plain; charset=utf-8") ~> Content("resource request not implemented")
  }

  def set(datasetResourceName: String, rowId:String)(request:HttpServletRequest): HttpServletResponse => Unit =  {
    ImATeapot ~> ContentType("text/plain; charset=utf-8") ~> Content("resource request not implemented")
  }

  def create(datasetResourceName: String, rowId:String)(request:HttpServletRequest): HttpServletResponse => Unit = {
    ImATeapot ~> ContentType("text/plain; charset=utf-8") ~> Content("create request not implemented")
  }

  def delete(datasetResourceName: String, rowId:String)(request:HttpServletRequest): HttpServletResponse => Unit = {
    ImATeapot ~> ContentType("text/plain; charset=utf-8") ~> Content("delete request not implemented")
  }
}

