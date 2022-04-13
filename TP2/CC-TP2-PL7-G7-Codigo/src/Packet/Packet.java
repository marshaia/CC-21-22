import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Superclasse responsável pelos métodos necessários para manipulação do cabeçalho
 * standard aplicado a todas as mensagens protocolares.
 */
public class Packet {

    private byte tipo;
    private byte subtipo;
    private int seqNum;
    private long checkSum;

    public Packet(int seqNum,byte type, byte subtype){
        this.seqNum = seqNum;
        this.tipo = type;
        this.subtipo = subtype;
    }

    public String toString() {
        return "Packet{" +
                "tipo=" + tipo +
                ", subtipo=" + subtipo +
                ", seqNum=" + seqNum +
                ", checkSum=" + checkSum +
                '}';
    }

    public byte getTipo() {
        return tipo;
    }

    /**
     * Método utilizado na escrita do ficheiro de log.
     * @return Subtipo de Mensagem Protocolar
     */
    public String getSubTipoString() {
        String s = null;
        if (this.tipo == 0 ) {
            if (this.subtipo == 0) s = "ACK";
            else s = "FIN";
        }
        else if (this.tipo == 1) {
            if (this.subtipo == 0) s = "DATA";
            else s = "FILECHECK";
        }
        return s;
    }

    public int tamanhoPacket() {return 14;}
    public byte getSubtipo() {
        return subtipo;
    }
    public int getSeqNum() {
        return seqNum;
    }
    public long getCheckSum() {
        return checkSum;
    }
    public void setCheckSum(long checkSum) {
        this.checkSum = checkSum;
    }
    public byte[] geraChecksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes,8,6);
        long c = crc32.getValue();
        this.checkSum = c;
        byte[] checksBytes = ByteBuffer.allocate(8).putLong(this.checkSum).array();
        return checksBytes;
    }

    /**
     * Serialize do cabeçalho (comum a todas as mensagens protocolares (subclasses)).
     * @return cabeçalho serializado.
     */
    public byte[] PacketToBytes() {

        byte[] pdu = new byte[14];
        pdu[8] = this.tipo;
        pdu[9] = this.subtipo;

        byte[] seqnum = ByteBuffer.allocate(4).putInt(this.seqNum).array();

        int p = 10;
        for(int i=0; i<4; i++, p++) pdu[p] = seqnum[i];

        byte[] checksum = geraChecksum(pdu);
        for(int i=0; i<checksum.length; i++) pdu[i] = checksum[i];

        return pdu;
    }

    /**
     * Deserialize do cabeçalho.
     * @param bytes array alvo.
     * @return Packet deserialized.
     * @throws CorruptedPacketException
     */
    public static Packet BytesToPacket(byte[] bytes) throws CorruptedPacketException {

        CRC32 check = new CRC32();
        check.update(bytes,8,6);
        long c = check.getValue();
        long checksum = ByteBuffer.wrap(bytes,0,8).getLong();
        if (c != checksum) throw new CorruptedPacketException("Pacote corrompido (verificação de checksum falhou)");

        byte type = bytes[8];
        byte subtype = bytes[9];
        int seqNum = ByteBuffer.wrap(bytes,10,4).getInt();

        Packet packet = new Packet(seqNum,type,subtype);
        packet.setCheckSum(checksum);

        return packet;
    }

}
