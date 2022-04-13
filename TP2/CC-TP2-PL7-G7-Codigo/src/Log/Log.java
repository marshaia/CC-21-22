import java.io.*;
import java.net.InetAddress;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Classe responsável pela escrita dos Logs das várias conexões
 */
public class Log {

    ReentrantLock l = new ReentrantLock();
    private InetAddress local;
    private PrintWriter writer;

    public Log(InetAddress ip, InetAddress local) throws IOException {
        this.local = local;
        String log = "LOG ["+ip.toString().substring(1)+"]";
        FileWriter f = new FileWriter(log);
        this.writer = new PrintWriter(f);
    }

    /**
     * Adiciona uma linha formatada ao ficheiro de log.
     * @param msg Informação a ser adicionada.
     * @throws IOException
     */
    public void addPhrase(String msg) throws IOException {
        try {
            l.lock();
            writer.println("[SYSTEM]: "+msg);
            writer.flush();
        } finally {
            l.unlock();
        }
    }

}
