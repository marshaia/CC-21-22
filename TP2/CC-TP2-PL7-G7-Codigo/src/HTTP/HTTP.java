import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Iterator;
import java.util.List;


public class HTTP implements Runnable{

    private ServerSocket server;
    private InfoTransfer info;

    public HTTP(InfoTransfer info) throws IOException {this.info = info;}

    /**
     * Quando recebe um pedido de conexão, envia a resposta através da classe httpWorker
     */
    public void run () {
        try {
            this.server = new ServerSocket(80);
            while (true) {
                Socket socket = server.accept();
                Thread t = new Thread(new httpWorker(socket,info));
                t.start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}