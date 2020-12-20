package de.uhingen.kielkopf.andreas.tasmoview.tasks;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;

public class TasmotaClientSessionHandler extends IoHandlerAdapter {
   private final Tasmota tasmota;
   public TasmotaClientSessionHandler(Tasmota t) {
      tasmota=t;
   }
   @Override
   public void messageReceived(IoSession session, Object message) throws Exception {
      System.out.println(message);
   }
   @Override
   public void sessionCreated(IoSession session) throws Exception {
      super.sessionCreated(session);
      // session.write(game.me); // versende die UserInfo
   }
   @Override
   public void sessionOpened(IoSession session) throws Exception {
      // game.start(session);
      // session.write(ServerGame.GAME.START.message);
      // session.write(ServerGame.GAME.HALLO_SERVER.message);
   }
   @Override
   public void sessionClosed(IoSession session) throws Exception {
      super.sessionClosed(session);
   }
   @Override
   public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
      session.closeNow();
      super.sessionIdle(session, status);
   }
   @Override
   public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
      System.out.println("Client Exception");
      super.exceptionCaught(session, cause);
   }
   @Override
   public void messageSent(IoSession session, Object message) throws Exception {
      System.out.println("Message sent"+message);
      super.messageSent(session, message);
   }
   @Override
   public void inputClosed(IoSession session) throws Exception {
      System.out.println("Input Closed");
      super.inputClosed(session);
   }
   @Override
   public void event(IoSession session, FilterEvent event) throws Exception {
      super.event(session, event);
   }
}
