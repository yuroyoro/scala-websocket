package com.yuroyoro.websocket

import java.io.{InputStream, IOException}
import java.net.{Authenticator, PasswordAuthentication, URL, HttpURLConnection}
import javax.servlet.http._
import org.eclipse.jetty.websocket._
import org.eclipse.jetty.websocket.WebSocket.Outbound

class ChirpUserStreamsServlet extends WebSocketServlet {

  override def doGet(req:HttpServletRequest, res:HttpServletResponse ) =
    getServletContext.getNamedDispatcher("default").forward(req, res)

  override def doWebSocketConnect(req:HttpServletRequest, protocol:String ) =
    new ChirpUserStreamsWebSocket

  class ChirpUserStreamsWebSocket extends WebSocket {
    private val chirpUserStreamingURL = "http://chirpstream.twitter.com/2b/user.json"

    // BASIC認証orz
    private def setBasicAuth( username:String, passwd:String ) =
      Authenticator.setDefault( new Authenticator {
        override def getPasswordAuthentication =
          new PasswordAuthentication(username,  passwd.toCharArray)
      })

    private def connectSteaming( url:String ) = {
      val urlConn = new URL( url ).openConnection match {
        case con:HttpURLConnection => {
          con.connect()
          con.getResponseCode match {
            case 200 => { }
            case c =>
              throw new RuntimeException( "can't connect to %s : StatusCode = %s" format ( url, c))
          }
          con
        }
      }
      urlConn.getInputStream
    }

    def consume(in:InputStream)( f:Option[String]=>Unit){
      val buf = new Array[Byte](1024)
      var remains:String = ""
      try{
        // InputStreamから1行読んでfにわたす
        for(i <- Stream.continually(in.read(buf)).takeWhile(_ != -1)){
          val str = remains + new String(buf,  0,  i)
          remains = ( "" /: str){ (s,c) =>
            if( c == '\n'){
              f( Some(s) )
              ""
            }
            else s + c
          }
        }
     }
     catch{ case e:IOException => }
     finally{ in.close }
    }

    var outbound:Outbound = _

    override def onConnect(outbound:Outbound ) = this.outbound = outbound

    override def onMessage(frame:Byte, data:Array[Byte], offset:Int, length:Int ) = {}

    override def onMessage(frame:Byte, data:String ) = {
      // usernameとpasswdが送られてくるので切り出す
      val m = Map( data split('&') map{  _.split('=') match { case Array(k,v) => (k,v)}} : _* )
      m.get("username") foreach { u => m.get("passwd") foreach { p =>
        streaming( frame, u, p )
      }}
    }

    def streaming( frame:Byte, username:String, passwd:String ) = {
      setBasicAuth( username, passwd )  // BASIC認証設定する
      // ChirpUserStreamに接続
      val con = connectSteaming( chirpUserStreamingURL )
      // JSONをクライアントに送る
      consume( con ) { o => o match{
        case Some( s ) => {
          println(s)
          println("----------------------- %s Bytes. -------" format( s.length ) )
          outbound.sendMessage( frame , s )
        }
        case None =>
      }}
    }

    override def onDisconnect = {}

  }
}

