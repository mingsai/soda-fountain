package com.socrata.soda.server

import javax.servlet.http.{HttpServletResponse, HttpServlet, HttpServletRequest}
import com.socrata.http.server._
import com.socrata.http.server.responses._
import com.socrata.http.server.implicits._
import scala.Some

class SodaFountainServlet extends HttpServlet {

  val fountain = new SodaFountain(SodaFountain.getConfiguredStore, SodaFountain.getConfiguredClient)
  val router: SodaRouter = fountain.router
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse)    {router.route(req)(resp)}
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse)   {router.route(req)(resp)}
  override def doPut(req: HttpServletRequest, resp: HttpServletResponse)    {router.route(req)(resp)}
  override def doDelete(req: HttpServletRequest, resp: HttpServletResponse) {router.route(req)(resp)}
}
