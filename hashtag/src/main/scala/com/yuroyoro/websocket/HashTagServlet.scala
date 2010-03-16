package com.yuroyoro.websocket

import javax.servlet.http._
import org.eclipse.jetty.websocket._
import org.eclipse.jetty.websocket.WebSocket.Outbound
import scala.actors._
import scala.actors.Actor._
import scala.xml.XML
import scala.io.Source

class HashTagServlet extends WebSocketServlet {

  override def doGet(req:HttpServletRequest, res:HttpServletResponse ) =
    getServletContext.getNamedDispatcher("default").forward(req, res)

  override def doWebSocketConnect(req:HttpServletRequest, protocol:String ) =
    new HashTagWebSocket

  class HashTagWebSocket extends WebSocket {

    var outbound:Outbound = _

    object SearchActor extends Actor{
      def act() = {
        react {
          case Search( frame, tag, sinceId) => {
            val url = "http://search.twitter.com/search.atom?q=%%23%s&since_id=%s".format( tag, sinceId )
            val l =  XML.loadString( Source.fromURL( url, "utf-8").getLines.mkString) \\ "entry"
            println( "fetch:%s".format( url ))

            l.map{ e =>
                val c = (e \\ "content" toString)
                  .replace("&lt;", "<").replace("&gt;", ">")
                  .replace("&quot;", "\"").replace("&amp;", "&")
                val Some(img_url) = e \ "link" find { e => e \ "@rel" == "image" }
"""<div class="status">
  <span class="profile_img"><img src="%s"/></span>
  <span class="user_name">%s</span>
  <span class="message">%s</span>
</div>
""".format( img_url \ "@href" text, e \\ "author" \\ "name" text,c )
            }.reverse.foreach{ e => println(e);outbound.sendMessage( frame , e.toString ) }

            Thread.sleep(1000 * 30 )
            val lastId = ( sinceId /: l.map{ e => ( e \ "id" text).split(":").last.toLong }){
              (n, m) => if( n > m ) n else m }
            println( "lastId:%s".format( lastId))
            SearchActor ! Search( frame, tag , lastId)
            act()
          }
          case Dispose =>
        }
      }
    }

    override def onConnect(outbound:Outbound ) =  this.outbound = outbound

    override def onMessage(frame:Byte, data:Array[Byte], offset:Int, length:Int ) = {}

    override def onMessage(frame:Byte, data:String ) = {
      SearchActor.start
      SearchActor ! Search( frame, data, 0 )
    }

    override def onDisconnect = SearchActor ! Dispose

  }
}

case class Search( frame:Byte, tag:String , sinceId:Long)
case class Dispose()
