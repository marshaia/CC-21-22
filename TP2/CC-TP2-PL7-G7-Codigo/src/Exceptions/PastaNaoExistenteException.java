/**
 * Exception lançada quando a pasta passada como argumento não existe na máquina local
 */
public class PastaNaoExistenteException extends Exception {
    PastaNaoExistenteException() {super();}
    PastaNaoExistenteException(String msg) {super(msg);}
}
