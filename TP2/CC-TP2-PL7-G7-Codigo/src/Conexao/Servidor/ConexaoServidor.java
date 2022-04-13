import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável pela gestão do envio de ficheiros ao parceiro de conexão.
 */
public class ConexaoServidor implements Runnable {

    private int totalFiles = 0;
    private int totalFilesRead = 0;
    private int sequence;
    private boolean running;
    private byte[] buffer;
    private InfoTransfer info;
    private InetAddress ip;
    private int port;
    private DatagramSocket socket;

    /**
     * Construtor da classe ConexaoServidor.
     * Como lhe é passado um packet na inicialização, tem de o decifrar antes de poder receber outros.
     */
    public ConexaoServidor(InetAddress ip, int port, InfoTransfer info, byte[] b) throws IOException, CorruptedPacketException, InterruptedException {
        this.sequence = 0;
        this.ip = ip;
        this.port = port;
        this.buffer = b;
        this.info = info;
        this.running = true;
        this.socket = new DatagramSocket();

        decifraPacket(b);
    }


    /**
     * Função principal da classe.
     * Responsável por decifrar os packets recebidos do Cliente.
     * De acordo com o protocolo desenvolvido, esta thread apenas recebe dois tipos de packets: [FIN] ou [FILECHECK].
     * Como tal, esta função está definida apenas para estes dois tipos de packets.
     *
     * Se receber um [FILECHECK], verifica os ficheiros que tem de enviar através de uma função auxiliar (ficheirosNecessarios)
     * de comparação de strings. Esta apenas retorna a lista de strings com os nomes dos ficheiros que vão
     * ser precisos enviar, isto é, que o parceiro de ligação não contém na sua pasta. De acordo com o resultado
     * da função ficheirosNecessarios há dois cenários: não há ficheiros a enviar ou precisamos de enviar ficheiros.
     *   - NÃO há ficheiros para enviar: enviamos um packet [ACK].
     *   - HÁ ficheiros para enviar: É criada uma thread para cada ficheiro necessário, passando a responsabilidade
     *     de enviar os ficheiros para outra classe (DataSender) que fica encarregue de enviar o ficheiro que lhe é
     *     passado como argumento no construtor.
     *
     * Se receber um [FIN] apenas termina a conexão, fechando o socket.
     *
     * @param packet packet para decifrar.
     * @throws CorruptedPacketException
     * @throws IOException
     * @throws InterruptedException
     */
    public void decifraPacket(byte[] packet) throws CorruptedPacketException, IOException, InterruptedException {

            if (packet[8] == 1 && packet[9] == 1) { //recebeu um FILECHECK

                FileCheck ficheiros = (FileCheck) FileCheck.bytesToFileCheck(packet);
                this.sequence = ficheiros.getSeqNum();

                List<String> local = FileCheck.stringToSetFicheiros(info.getPathPasta()); //ficheiros da pasta local
                List<String> necessarios = ficheirosNecessarios(ficheiros.getFicheiros(),local); //verifico os que necessito

                if(necessarios.size() == 0) { //Se os ficheiros forem iguais então não tenho nada a sincronizar
                    Packet ack = new Ack(this.sequence);
                    DatagramPacket p = new DatagramPacket(ack.PacketToBytes(),ack.PacketToBytes().length,ip,port);
                    socket.send(p);
                }

                else { //Sincroniza os ficheiros, criando threads para cada um
                    for(int i=0; i<necessarios.size();i++) {
                        Thread t = new Thread(new DataSender(i, info.getPathPasta(), necessarios.get(i),ip,port));
                        t.start();
                    }
                    this.running = false;
                    socket.close();
                    return;
                }
            }

            else if (packet[8] == 0 && packet[9] == 1) { //recebeu um FIN
                this.running = false;
                socket.close();
                return;
            }
    }


    /**
     * Função responsável por filtrar o nome dos ficheiros necessários para o parceiro de conexão.
     * @param outsider lista de ficheiros presentes no parceiro de conexão.
     * @param local lista de ficheiros presentes na máquina local.
     * @return lista de ficheiros necessários ao parceiro de conexão.
     */
    public List<String> ficheirosNecessarios(List<String> outsider, List<String> local) {
        List<String> n = new ArrayList<>();
        if (local.size() !=0 ) {
            for (int i=0; i<local.size(); i++) {
                if(!outsider.contains(local.get(i))) n.add(local.get(i));
            }
        }
        return n;
    }


    /**
     * Função responsável por receber e decifrar os packets.
     * Quando recebe um [FIN], através da função decifraPacket a variável 'running' fica com o valor false,
     * ou seja, para de receber packets e fecha o socket.
     */
    @Override
    public void run() {

        while (running) {
            DatagramPacket in = new DatagramPacket(this.buffer, this.buffer.length);

            try {
                this.socket.receive(in);
                decifraPacket(buffer);
            } catch (IOException | InterruptedException | CorruptedPacketException e) {e.printStackTrace();}

        }

        socket.close();
    }
}
