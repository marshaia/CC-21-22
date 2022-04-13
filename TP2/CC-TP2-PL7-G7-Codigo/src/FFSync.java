import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class FFSync {


    /**
     * Verifica se os argumentos são válidos.
     * Começa por verificar se o número de argumentos é inferior a 2.
     * De seguida, passa para a verificação da existência da pasta no sistema.
     * Por fim, verifica se os endereços IP dados têm um formato válido.
     *
     * Para todas as verificações, caso estas não se verifiquem é lançada uma exceção
     * e o programa termina.
     *
     * @param args argumentos do executável
     * @throws PastaNaoExistenteException
     */
    public static void verificaArgumentos (String[] args) throws NumeroArgumentosInvalidoException,PastaNaoExistenteException,EnderecoIPInvalidoException {
        if (args.length < 2) throw new NumeroArgumentosInvalidoException("Número de argumentos inválido!");

        File file = new File(args[0]);
        if (!file.isDirectory() || !file.exists()) throw new PastaNaoExistenteException("Ups! Pasta não existente...");

        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
        for (int i=1; i<args.length;i++) if(!args[i].matches(PATTERN)) throw new EnderecoIPInvalidoException("Endereço IP inválido!");

    }





    public static void main(String[] args) throws IOException {

        System.out.println("\nA iniciar verificação de argumentos...");

        // VERIFICA OS ARGUMENTOS
        try {
            verificaArgumentos(args);
        } catch (NumeroArgumentosInvalidoException | PastaNaoExistenteException | EnderecoIPInvalidoException e) {
            System.out.println(e);
            return;
        }

        //Adiciona os endereços à informação atual da sincronização
        List<InetAddress> lista = new ArrayList<>();
        for(int i=1; i< args.length; i++) {
            InetAddress n = InetAddress.getByName(args[i]);
            lista.add(n);
        }

        InfoTransfer info = new InfoTransfer(InetAddress.getByName("localhost"),lista, args[0]);


        //atualiza o estado da sincronização
        System.out.println("A preparar a sincronizacao da pasta '"+args[0]+"' com os parceiros: ");
        lista.stream().forEach(e -> System.out.println(e));
        System.out.println("");



        //Inicia protocolo HTTP com a informação já adquirida
        try{
            Thread t = new Thread(new HTTP(info));
            t.start();
        } catch (IOException e) {
            System.out.println(e);
        }
        System.out.println("Protocolo HTTP inicializado!");


        //Inicia a thread recetora de pedidos
        Thread t1 = new Thread(new ThreadRecetora(info));
        t1.start();
        System.out.println("Thread Recetora iniciada!");


        //Inicia as threads cliente de comunicação com cada parceiro
        Thread t2 = new Thread(new ConexaoClientes(info));
        t2.start();
        System.out.println("Sincronizações iniciadas!\n");


    }
}