import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class InfoTransfer {

    private final String pathPasta;
    private final InetAddress first;
    private final List<InetAddress> parceiros;
    private Map<InetAddress,List<String>> estados;

    ReentrantLock lock = new ReentrantLock();


    public InfoTransfer(InetAddress f, List<InetAddress> lista,String p) {
        this.first = f;
        this.parceiros = lista;
        this.pathPasta = p;
        this.estados = new HashMap<>();
        this.parceiros.stream().forEach(e -> this.estados.put(e, new ArrayList<>()));
    }


    public String getPathPasta() {return this.pathPasta;}
    public InetAddress getFirstAddress() {return this.first;}
    public List<InetAddress> getParceiros() {return this.parceiros;}



    /**
     * Adiciona a informação ao IP fornecido.
     * @param ip IP alvo de atualização.
     * @param e Informação a ser adicionada.
     */
    public void setEstado(InetAddress ip, String e) {
        try {
            lock.lock();
            this.estados.get(ip).add(e);
        } finally {
            lock.unlock();
        }
    }


    /**
     * Devolve a lista de strings caracterizadoras do estado da sincronização com o IP fornecido.
     * @param ip IP a pesquisar.
     * @return Lista de Strings com a informação do ip.
     */
    public List<String> getEstados(InetAddress ip) {
        try{
            lock.lock();
            List<String> nova = new ArrayList<>();
            nova.addAll(this.estados.get(ip));
            return nova;
        } finally {
            lock.unlock();
        }
    }


}
