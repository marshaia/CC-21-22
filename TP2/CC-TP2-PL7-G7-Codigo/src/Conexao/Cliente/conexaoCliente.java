import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.*;


public class conexaoCliente implements Runnable {

    private Log log;
    private int seqNum = 1;
    private InfoTransfer info;
    private final InetAddress ip;
    private DatagramSocket socket;
    private Map<Integer, String> ids;


    public conexaoCliente(InfoTransfer info, InetAddress ip) throws IOException {
        this.log = new Log(ip, info.getFirstAddress());
        this.ip = ip;
        this.info = info;
        this.socket = new DatagramSocket();
        this.ids = new HashMap<>();
    }




    /**
     * Função auxiliar de envio de packet [FIN] a simbolizar o término da conexão.
     * @param port porta de envio.
     * @throws IOException
     */
    public void enviaFIN(int port) throws IOException {
        Packet fin = new Fin();
        DatagramPacket p = new DatagramPacket(fin.PacketToBytes(),fin.PacketToBytes().length,ip,port);
        this.socket.send(p);
        log.addPhrase("(" +LocalTime.now()+") SENT -> [FIN] Size = 14  ;  Seq = "+fin.getSeqNum());
    }


    /**
     * Função principal da classe conexaoCliente, responsável pela receção e distribuição de packets [ACK] ou [DATA].
     * Faz no máximo 3 tentativas de conexão ao parceiro, esperando 5 segundos entre cada uma.
     *
     * Se receber um [ACK] como resposta, então, de acordo com o protocolo, não tem nada a sincronizar e portanto termina a ligação.
     * Se receber um [DATA] então passa a responsabilidade de receção do ficheiro para a classe DataReceiver.
     *
     * @throws IOException
     * @throws CorruptedPacketException
     * @throws InterruptedException
     * @throws TamanhoFileCheckExcedido
     */
    public void conexao () throws IOException, CorruptedPacketException, InterruptedException, TamanhoFileCheckExcedido {

        int maxTentativas = 3;
        int i = 0;

        info.setEstado(this.ip,"Iniciando Sincronizacao!");

        while(i < maxTentativas) {

            // ----------------------------------------------- ENVIA [FILECHECK] ----------------------------------------------------
            List<String> ficheiros = FileCheck.stringToSetFicheiros(info.getPathPasta()); //lista de ficheiros da pasta alvo de sincronização

            FileCheck primeiro = new FileCheck(seqNum,info.getPathPasta());
            byte[] b = primeiro.fileCheckToBytes();
            this.seqNum++;

            DatagramPacket packet = new DatagramPacket(b, b.length, ip, 80);

            LocalTime inicio = LocalTime.now();
            socket.send(packet); //envia [FILECHECK] contendo o nome dos ficheiros

            log.addPhrase("(" + inicio + ") SENT -> [FILECHECK] Size = " + packet.getLength() + "  ;  Seq = " + primeiro.getSeqNum());
            log.addPhrase("Meta-Dados Enviados: " + ficheiros); //atualiza ficherio log



           // ------------------------------------------------------------- ESPERA PELA RESPOSTA ---------------------------------------------------------------------
            boolean run = true;
            while (run) {
                byte[] ack = new byte[1024];
                DatagramPacket in = new DatagramPacket(ack, ack.length);

                try {
                    socket.setSoTimeout(150); //tempo de timeout definido através de testes iniciais
                    socket.receive(in);
                } catch (IOException e) {
                    System.out.println((i+1)+"º tentativa de conexao com: "+ip+" falhada (timeout excedido).\nNova tentativa em 5 segundos!\n");
                    i++;
                    break;
                }

                LocalTime now = LocalTime.now();
                Packet input = Packet.BytesToPacket(in.getData());

                // ----------------------------------------- RECEBEU [ACK] ----------------------------------------------
                if (input.getSubTipoString().equals("ACK")) {
                    log.addPhrase("(" + now + ") RECEIVED -> [" + input.getSubTipoString()+ "] Size = " + input.tamanhoPacket()+ "  ;  Seq = " + input.getSeqNum());
                    enviaFIN(in.getPort());
                    run = false;
                }

                // ----------------------------------------- RECEBEU [DATA] ----------------------------------------------
                else {
                    Data d = (Data) Data.bytesToData(in.getData());
                    if (!ids.containsKey(d.getFileID())) { //passa a responsabilidade para a classe DataReceiver
                        Thread t = new Thread(new DataReceiver(in, d, log, info));
                        t.start();

                        ids.put(d.getFileID(), t.getName());
                    }
                }
            }

            log.addPhrase("("+LocalTime.now()+") "+Thread.currentThread().getName()+" sleeping for five seconds...");
            Thread.sleep(5000);
        }


        System.out.println((i+1)+"º tentativa de conexao com: "+ip+" falhada (numero de tentativas ("+maxTentativas+") excedido).");
        info.setEstado(ip, "Sincronização Terminada!");
    }





    @Override
    public void run() {

        try { conexao();
        } catch (IOException | CorruptedPacketException | InterruptedException | TamanhoFileCheckExcedido e) { e.printStackTrace(); }

    }

}