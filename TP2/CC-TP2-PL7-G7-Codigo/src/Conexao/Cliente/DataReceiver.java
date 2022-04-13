import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Classe responsável pela receção do conteúdo de um ficheiro em específico, passado como argumento no construtor
 */
public class DataReceiver implements Runnable {

    private int numBytes = 0;
    private long inicio;
    private long fim;

    private boolean running;
    private int port;
    private Log log;
    private String fileName;
    private InfoTransfer info;
    private InetAddress ip;
    private Data packet;
    private DatagramPacket Dpacket;
    private DatagramSocket socket;
    private SortedMap<Integer, Data> pacotes;
    private FileOutputStream out;


    /**
     * Construtor da classe DataReceiver.
     * Cria o ficheiro, mas não escreve nele.
     *
     * @param data DatagramPacket recebido.
     * @param d Packet [DATA] recebido.
     * @param l Objeto Log para escrita de informação.
     * @param i Objeto InfoTransfer para armazenamento de informação pertinente ao processo HTTP.
     * @throws IOException
     */
    public DataReceiver (DatagramPacket data, Data d, Log l, InfoTransfer i) throws IOException {
        this.Dpacket = data;
        this.running = true;
        this.packet =  d;
        this.log = l;
        this.info = i;
        this.ip = data.getAddress();
        this.port = data.getPort();
        this.socket = new DatagramSocket();
        this.pacotes = new TreeMap<>();
        this.fileName = d.getFileName();
        info.setEstado(ip,"Recebido: "+this.fileName);

        File f = new File(info.getPathPasta()+"/"+this.fileName);
        f.createNewFile();
        this.out = new FileOutputStream( f, false);
    }


    /**
     * Começa por analisar o packet recebido através dos parâmetros do construtor.
     * Depois passa a um estado de escuta de packets [DATA] ou [FIN].
     */
    @Override
    public void run() {

        // --------------------------------------- PRIMEIRO PACKET -------------------------------------------
        try {
            log.addPhrase("(" +LocalTime.now()+") ("+Thread.currentThread().getName()+") RECEIVED -> ["+packet.getSubTipoString()+"] Size = "+Dpacket.getLength()+"  ;  Seq = "+packet.getSeqNum()+"  ;  Last="+packet.getLast());
            log.addPhrase("(" +Thread.currentThread().getName()+"): encarregue do ficheiro '"+this.fileName+"'.");

            analisaPacket(this.Dpacket,LocalTime.now(), System.nanoTime());

        } catch (IOException | CorruptedPacketException e) {e.printStackTrace();}


        // --------------------------------------- ESPERA PELOS PACKETS -----------------------------------
        while(this.running) {
            byte[] buf = new byte[1024];
            DatagramPacket p = new DatagramPacket(buf,buf.length);

            try {
                this.socket.receive(p);
                analisaPacket(p, LocalTime.now(), System.nanoTime());
            } catch (IOException | CorruptedPacketException e) {
                e.printStackTrace();
            }
        }
    }





    /**
     * Função principal da classe DataReceiver.
     * É responsável por decifrar os packets recebidos.
     *
     * Se receber [DATA], então vai armazená-lo na estrutura. Faz uma verificação adicional
     * necessária para o cálculo do tempo de transmissão e débito real do ficheiro.
     *
     * Se receber [FIN], então utiliza os tempos armazenados anteriormente e calcula
     * o tempo de transferência e o débito real do ficheiro. Para além disto, começa
     * finalmente a escrita no ficheiro.
     * Por fim, termina a conexão.
     *
     * Em ambos os casos, vai escrevendo para o ficheiro de log e
     * para o objeto InfoTransfer as informações necessárias.
     *
     *
     * @param packet
     * @param time
     * @throws CorruptedPacketException
     * @throws IOException
     */
    public void analisaPacket(DatagramPacket packet, LocalTime time, long nano) throws CorruptedPacketException, IOException {

        Packet p = Packet.BytesToPacket(packet.getData());

        // ----------------------------------------------------- RECEBU [FIN] --------------------------------------------------------------------
        if (p.getSubTipoString().equals("FIN")) {
            this.running = false;

            log.addPhrase("("+time+") ("+Thread.currentThread().getName()+") RECEIVED -> [FIN] Size=14  ;  Seq="+p.getSeqNum());
            log.addPhrase("("+LocalTime.now()+") ("+Thread.currentThread().getName()+") escrevendo no ficheiro: "+this.fileName);

            //TEMPO DE TRANSFERÊNCIA
            long nanoInicio = this.inicio;
            long nanoFim = this.fim;
            double ms = (double) ((nanoFim-nanoInicio)/1000000);
            System.out.println("Tempo de transferência do ficheiro '"+this.fileName+"': "+ms+" ms.");
            log.addPhrase("("+LocalTime.now()+") ("+Thread.currentThread().getName()+") Tempo de transferencia do ficheiro '"+this.fileName+"': "+ms+" ms");

            //DÉBITO
            double debito = (this.numBytes*8)/((ms/1000)*1000);
            System.out.println("Débito real do ficheiro '"+this.fileName+"': "+debito+" kbps.\n");
            log.addPhrase("("+LocalTime.now()+") ("+Thread.currentThread().getName()+") Debito de transferencia do ficheiro '"+this.fileName+"': "+debito+" kbps");

            escreveFicheiro();

            socket.close();
            return;
        }


        // ----------------------------------------------------- RECEBU [DATA] ----------------------------------------------------------------
        else {
            this.packet = (Data) Data.bytesToData(packet.getData());

            if (this.packet.getSeqNum() != 1) {
                pacotes.put(this.packet.getSeqNum(),this.packet);
                log.addPhrase("("+time+") ("+Thread.currentThread().getName()+") RECEIVED -> ["+this.packet.getSubTipoString()
                        +"] Size="+this.packet.tamanhoData()
                        +"  ;  Seq="+this.packet.getSeqNum()
                        +"  ;  Last="+this.packet.getLast());


                // informações para o tempo de transferência e débito real do ficheiro
                if (this.packet.getSeqNum() == 2) this.inicio = nano;
                if (this.packet.getLast()   == 1) {
                    this.numBytes += this.packet.getFileID();
                    this.fim = nano;
                }
                else this.numBytes += this.packet.getDados().length;
            }

            enviaAck(this.packet.getSeqNum());
        }
    }



    /**
     * Função responsável pela escrita no ficheiro no final da stream de packets.
     * @throws IOException
     */
    public void escreveFicheiro() throws IOException {
        for(Data p : pacotes.values()) {
            if(p.getLast()==1) {
                int quantos = p.getFileID();
                out.write(p.getDados(),0,quantos);
            }
            else out.write(p.getDados());
            out.flush();
        }
        out.close();
    }



    /**
     * Função responsável pelo envio de um packet [ACK] para o parceiro de conexão, avisando da receção do packet [DATA].
     * @param seq número de sequência associado ao packet [DATA] recebido.
     * @throws IOException
     */
    public void enviaAck(int seq) throws IOException {
        Packet ack = new Ack(seq);
        DatagramPacket p = new DatagramPacket(ack.PacketToBytes(),ack.PacketToBytes().length,ip,port);
        this.socket.send(p);
        log.addPhrase("(" +LocalTime.now()+") ("+Thread.currentThread().getName()+") SENT -> ["+ack.getSubTipoString()+"] Size="+p.getLength()+"  ;  Seq="+ack.getSeqNum());
    }


}