/**
 * Exception lançada quando o checksum do pacote recebido não é válido
 */
public class CorruptedPacketException extends Exception {
    public CorruptedPacketException() {super();}
    public CorruptedPacketException(String msg) {super(msg);}
}
