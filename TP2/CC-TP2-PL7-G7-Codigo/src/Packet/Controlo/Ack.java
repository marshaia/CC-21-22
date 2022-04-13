/**
 * Classe da mensagem protocolar [ACK], sendo esta uma extensão da classe PACKET
 */
public class Ack extends Packet {
    public Ack(int seqNum) {
        super(seqNum, (byte) 0, (byte) 0);
    }
}