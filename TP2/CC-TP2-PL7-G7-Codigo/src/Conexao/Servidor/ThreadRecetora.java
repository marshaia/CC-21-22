import java.io.IOException;
import java.net.*;

public class ThreadRecetora implements Runnable {

    private InfoTransfer info;
    private DatagramSocket socket;


    public ThreadRecetora(InfoTransfer info) throws SocketException {
        this.info = info;
        this.socket = new DatagramSocket();
    }


    /**
     * Função principal da classe.
     * Responsável por distribuir os vários pedidos de conexão por threads 'conexaoServidor' de
     * modo a manter a multiplicidade de ligações concorrentes.
     *
     * Modo de funcionamento:
     * Fica num loop infinito à espera de conexões. Quando as obtém, verifica se o IP de origem do packet
     * faz parte dos IPs dados como argumentos na inicialização do programa.
     * Se sim, passa a responsabilidade de resposta para outra classe: conexaoServidor.
     * Se não, lança uma exceção.
     */
    @Override
    public void run() {

        try { this.socket = new DatagramSocket(80);
        } catch (SocketException e) { e.printStackTrace(); }

        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);

                if(info.getParceiros().contains(packet.getAddress())) { //verifica se o IP de origem faz parte dos argumentos
                    try {
                        byte[] novo = new byte[buffer.length];
                        System.arraycopy(buffer,0,novo,0,buffer.length);

                        // cria uma thread para lidar com a comunicação entre localhost e cliente
                        Thread t = new Thread(new ConexaoServidor(packet.getAddress(), packet.getPort(), this.info, novo));
                        t.start();

                    } catch (CorruptedPacketException | IOException | InterruptedException e) {e.printStackTrace();}
                }
                // Não é concedida ligação ao IP recebido
                else throw new ConexaoInvalidaException("Conexao Invalida Detetada por parte do IP: "+packet.getAddress());
            } catch (IOException | ConexaoInvalidaException e) {e.printStackTrace();}
        }
    }

}
