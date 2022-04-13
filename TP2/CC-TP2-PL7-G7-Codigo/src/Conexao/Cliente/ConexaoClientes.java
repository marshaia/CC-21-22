import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;


public class ConexaoClientes implements Runnable {

    private InfoTransfer info;

    public ConexaoClientes(InfoTransfer info) {this.info = info;}


    /**
     * Cria N threads conexaoCliente para cada IP dado como argumento ao programa
     */
    @Override
    public void run() {
        Iterator it = info.getParceiros().iterator();
        while(it.hasNext()) {
            try {
                Thread t = new Thread(new conexaoCliente(info,(InetAddress)it.next()));
                t.start();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}