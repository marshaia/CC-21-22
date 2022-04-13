import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;


/**
 * Classe responsável pelo envio de um ficheiro em específico.
 * É passado como parâmetro no construtor o nome do ficheiro, o IP e porta para onde tem de enviar os packets.
 */
public class DataSender implements Runnable {

    private boolean running = true;
    private int id;
    private String folder;
    private String ficheiro;
    private InetAddress ip;
    private int port;
    private DatagramSocket socket;
    private Map<Integer, Data> packets; // map de packets enviados: key->NumSequencia; value->[DATA]
                                        // para caso precise de reenviar, o processo seja mais rápido


    public DataSender(int id, String f, String ficheiro, InetAddress ip, int port) throws SocketException {
        this.id = id;
        this.folder = f;
        this.ficheiro = ficheiro;
        this.ip = ip;
        this.port = port;
        this.socket = new DatagramSocket();
        this.packets = new HashMap<>();
    }



    /**
     * Função responsável pelo envio do primeiro packet [DATA] contendo apenas o nome do ficheiro.
     */
    public void sendData() throws FileNotFoundException {
        try {

            // --------------------------- ENVIA O PRIMEIRO PACKET ----------------------------------
            Data pack = new Data(1, this.ficheiro, this.id, (byte)0, null);

            this.packets.put(1, pack);
            DatagramPacket datap = new DatagramPacket(pack.dataToBytes(), pack.dataToBytes().length, ip, port);

            this.socket.send(datap);


            // ---------------------------- FICA À ESPERA DO ACK ---------------------------------------
            byte[] buffer = new byte[20];
            DatagramPacket datapa = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(datapa);
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.ip = datapa.getAddress();
            this.port = datapa.getPort();

            Packet pac = Packet.BytesToPacket(datapa.getData());

            // se recebeu um [ACK] com Seq=1 então manda o conteúdo do ficheiro através da função axuliar sendAll()
            if (pac.getSubTipoString().equals("ACK") && pac.getSeqNum() == 1) sendAll();

        } catch (IOException | CorruptedPacketException e) {
            e.printStackTrace();
        }

    }



    /**
     * Função auxiliar que envia todos os packets [DATA] com conteúdo do ficheiro.
     * Para cada packet enviado espera pelo [ACK] de confirmação de receção.
     */
    public void sendAll() {
        try {
            String path = this.folder + "/" + this.ficheiro;
            FileInputStream b = new FileInputStream(path);
            int tamanho = b.readAllBytes().length;

            FileInputStream read = new FileInputStream(path);

            byte last;
            int max = tamanho / (1024 - 19); //tamanho ficheiro a dividir pelo tamanho dos dados
            if ((tamanho % (1024 - 19)) > 0) max += 1; //se der um valor certinho não adiciona uma packet desnecessária


            // --------------------------------------- ENVIA TODOS OS DADOS ---------------------------------------------------
            for (int sequencia = 2; sequencia <= (max + 1); sequencia++) {
                int disponivel = 1024-19;
                if (disponivel > tamanho) disponivel = tamanho;

                byte[] data = read.readNBytes(disponivel); //começa a leitura do ficheiro

                tamanho -= disponivel;

                // define se é o último packet da stream ou não
                if (sequencia == (max + 1)) last = 1;
                else last = 0;

                Data packet;
                if (last == 1) //se for o último packet da stream, então substitui o NºStream pelo número de bytes que é necessário ler
                    packet = new Data(sequencia, this.ficheiro, data.length, last, data);

                else packet = new Data(sequencia, this.ficheiro, this.id, last, data);

                packets.put(sequencia, packet);

                DatagramPacket p = new DatagramPacket(packet.dataToBytes(), packet.dataToBytes().length, ip, port);
                this.socket.send(p);

                // fica à espera do [ACK]
                byte[] receive = new byte[20];
                DatagramPacket receiveP = new DatagramPacket(receive, receive.length, ip, port);
                this.socket.receive(receiveP);

                // quando recebe o [ACK] de confirmação de receção do último packet, então termina a conexão
                if (last == 1) {
                    enviaFIN();
                    this.running = false;
                    socket.close();
                }
            }
        } catch (IOException e) {e.printStackTrace();}

    }




    /**
     * Função auxiliar que apenas envia um packet [FIN] a simbolizar o término da conexão.
     * @throws IOException
     */
    public void enviaFIN() throws IOException {
        Packet fin = new Fin();
        DatagramPacket pa = new DatagramPacket(fin.PacketToBytes(), fin.PacketToBytes().length, ip, port);
        socket.send(pa);
    }





    @Override
    public void run() {

        try { sendData();
        } catch (FileNotFoundException ex) { ex.printStackTrace(); }

        byte[] bytes = new byte[20]; //de acordo com o protocolo apenas recebe [ACK] com tamanho máximo de 14

        while (this.running) {
            DatagramPacket p = new DatagramPacket(bytes, bytes.length);
            try { socket.receive(p);
            } catch (IOException e) { e.printStackTrace(); }
        }

    }



}