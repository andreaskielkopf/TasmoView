package de.uhingen.kielkopf.andreas.tasmoview.log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Logger implements Runnable {
   /**
    * https://stackoverflow.com/questions/413807/is-there-a-way-for-non-root-processes-to-bind-to-privileged-ports-on-linux
    * 
    * setcap 'cap_net_bind_service=+ep' /path/to/program
    */
   private static final int SYSLOG_PORT=1514;
   private static final int MAXBYTES   =4096;
   enum Severity {
      Emergency, Alert, Critical, Error, Warning, Notice, Informational, Debug
   }

   enum Facility {
      kernel_messages, user_level_messages, mail_system, system_daemons, security_messages, messages_generated_internally_by_syslogd, line_printer_subsystem,
      network_news_subsystem, UUCP_subsystem, clock_daemon, authorization_messages, FTP_daemon, NTP_subsystem, log_audit, log_alert, clock_daemon2, local0,
      local1, local2, local3, local4, local5, local6, local7
   }
   public static void main(String[] args) {
      Thread t=new Thread(new Logger());
      t.start();
   }
   @Override
   public void run() {
      try {
         DatagramChannel channel=DatagramChannel.open();
         channel.socket().bind(new InetSocketAddress(SYSLOG_PORT));
         while (channel.isOpen()) {
            try {
               ByteBuffer buffer=ByteBuffer.allocate(MAXBYTES);
               buffer.clear();
               channel.receive(buffer);
               String s=new String(buffer.array());
               System.out.println(s);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      } catch (IOException e1) {
         e1.printStackTrace();
      }
   }
}
