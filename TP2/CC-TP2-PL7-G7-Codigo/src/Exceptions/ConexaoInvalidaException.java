/**
 * Exception lançada quando um IP fora dos passados como argumentos
 * se tenta conectar à máquina local.
 */
public class ConexaoInvalidaException extends Exception {
    ConexaoInvalidaException() {super();}
    ConexaoInvalidaException(String msg) {super(msg);}
}
