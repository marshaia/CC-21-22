/**
 * Exception lançada quando um IP dos argumentos não está num formato válido
 */
public class EnderecoIPInvalidoException extends Exception {
    EnderecoIPInvalidoException() {super();}
    EnderecoIPInvalidoException(String msg) {super(msg);}
}
