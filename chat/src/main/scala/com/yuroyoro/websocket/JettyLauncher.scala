package com.yuroyoro.websocket

import org.eclipse.jetty.server._
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.webapp.WebAppContext

object JettyLauncher{
  def main( args:Array[String] ) = {
    val Array( path, port ) = args

    val server = new Server( port.toInt )
    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    server.setHandler(context)
    val web = new WebAppContext(path, "/");
    server.setHandler(web)
    server.start
    server.join
  }
}
