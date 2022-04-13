import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;

public class httpWorker implements Runnable {

    Socket socket;
    InfoTransfer info;

    public httpWorker(Socket s, InfoTransfer info) {
        this.info = info;
        this.socket = s;
    }

    /**
     * Envia a resposta ao pedido de conexão HTTP, contendo toda a informação
     * presente no objeto InfoTransfer (passado como argumento no construtor).
     */
    @Override
    public void run() {
        PrintWriter out = null;
        try {
            out = new PrintWriter(socket.getOutputStream());

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println("\r\n");

            out.println("<h1> (TP2 - Comunicacoes por Computador) -> FFSync - FolderFastSync </h1>\n");
            out.println("<hr>");

            out.println("<h2> Grupo 77 - Joana Alves [a93290] & Jorge Vieira [a84240]</h2>");

            // IMPRIME OS VÁRIOS SISTEMAS COM OS QUAIS SE ESTÁ A TENTAR CONECTAR
            out.println("<p><b>SISTEMAS:</b></p>");
            Iterator p = info.getParceiros().iterator();
            while(p.hasNext()) out.println("<p>-> "+p.next());
            out.println("<hr>");


            // IMPRIME TODAS AS INFORMAÇÕES RELATIVAS AOS PARCEIROS DE CONEXÃO
            out.println("<h3> ESTADO DA SINCRONIZACAO NOS DIVERSOS SISTEMAS:</h3>");

            Iterator ips = info.getParceiros().iterator();
            while(ips.hasNext()) {
                InetAddress ip = (InetAddress) ips.next();
                out.println("<p><b>| SISTEMA "+ip+"|</b></p>");
                List<String> estados = info.getEstados(ip);
                Iterator states = estados.iterator();
                while (states.hasNext()) out.println("<p>   "+states.next()+"</p>");
            }

            out.flush();
            out.close();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
