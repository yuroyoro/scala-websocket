package com.yuroyoro.websocket

import scala.collection.mutable.Set
import javax.servlet.http._
import org.eclipse.jetty.websocket._
import org.eclipse.jetty.websocket.WebSocket.Outbound

class ChatServlet extends WebSocketServlet {
  val clients = Set.empty[ChatWebSocket]

  override def doGet(req:HttpServletRequest, res:HttpServletResponse ) =
    getServletContext.getNamedDispatcher("default").forward(req, res)

  override def doWebSocketConnect(req:HttpServletRequest, protocol:String ) =
    new ChatWebSocket

  class ChatWebSocket extends WebSocket {

    var outbound:Outbound = _

    override def onConnect(outbound:Outbound ) = {
      this.outbound = outbound
      clients += this
      onMessage( 0, "WebSocket is success!!!");
    }

    override def onMessage(frame:Byte, data:Array[Byte], offset:Int, length:Int ) = {}

    override def onMessage(frame:Byte, data:String ) =
      clients.foreach{ c => c.outbound.sendMessage( frame, data ) }

    override def onDisconnect = clients -= this

  }
}

