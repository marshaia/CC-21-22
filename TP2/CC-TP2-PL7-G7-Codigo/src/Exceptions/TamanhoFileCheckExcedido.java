/**
 * Exception lançada quando o tamanho do packet [FILECHECK] é excedido, ou seja,
 * quando o tamanho do nome dos ficheiros contidos na pasta alvo de sincronização
 * ultrapassa o limite definido
 */
public class TamanhoFileCheckExcedido extends Exception {
    TamanhoFileCheckExcedido(){super();}
    TamanhoFileCheckExcedido(String msg) {super(msg);}
}
