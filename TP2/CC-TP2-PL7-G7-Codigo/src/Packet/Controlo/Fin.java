/**
 * Classe da mensagem protocolar [FIN], sendo esta uma extensão da classe PACKET
 */

public class Fin extends Packet {
    public Fin() {super(1, (byte) 0, (byte) 1);}
}
