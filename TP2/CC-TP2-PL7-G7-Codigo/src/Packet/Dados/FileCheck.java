import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Classe responsável pela serialização de informação da pasta alvo de sincronização.
 * É uma extensão da classe Packet.
 */
public class FileCheck extends Packet {

    private String pasta; //pasta alvo de sincronização
    private List<String> ficheiros; //lista de ficheiros presente na pasta

    /**
     * Construtor da classe FileCheck.
     * @param seqNum Número de sequência a ser associado ao packet.
     * @param pasta Pasta a sincronizar.
     * @throws TamanhoFileCheckExcedido caso o tamanho do nome dos ficheiros exceda o limite máximo, então é lançada uma exceção.
     */
    public FileCheck(int seqNum, String pasta) throws TamanhoFileCheckExcedido {
        super(seqNum, (byte) 1, (byte) 1);
        this.pasta = pasta;

        if (pasta == null) this.ficheiros = new ArrayList<>();

        else{
            try {
                this.ficheiros = stringToSetFicheiros(this.pasta);
            } catch (IOException | InterruptedException e) { System.out.println(e);}
        }

        // Valida a pasta, isto é, verifica que o nome de todos os ficheiros cabem num só packet.
        if(quantosbyteFicheiros(this.ficheiros) > (1024-14)) throw new TamanhoFileCheckExcedido("Demasiados ficheiros: tamanho máximo do packet [FILECHECK] excedido!") ;
    }

    public FileCheck (int seq, List<String> f) {
        super(seq, (byte) 1, (byte) 1);
        this.ficheiros = f;
    }


    /**
     * Conta quantos bytes são necessários para a escrita do nome dos ficheiros no packet.
     * @param lista lista de strings a contabilizar.
     * @return bytes necessários.
     */
    public static int quantosbyteFicheiros(List<String> lista) {
        int soma = 0;
        for(int i=0; i<lista.size(); i++) {
            soma+=lista.get(i).length();
        }
        return soma;
    }


    public List<String> getFicheiros() {return this.ficheiros;}


    /**
     * Dado um path para uma pasta, obter uma String com o resultado do comando "ls -l -g -o"
     * @param pasta Pasta alvo.
     * @return String resultado.
     * @throws IOException
     * @throws InterruptedException
     */
    public static String lsPastaToString(String pasta) throws IOException, InterruptedException {
        StringBuffer output = new StringBuffer();

        Process p = Runtime.getRuntime().exec("ls -l -g -o "+pasta);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line;
        while ((line = reader.readLine())!= null) {
            output.append(line + "\n");
        }

        return output.toString();
    }


    /**
     * Após a obtenção da string resultado da função acima, aplica-se a separação dos nomes,
     * para obter uma lista de strings individuais, contendo apenas nomes de ficheiros.
     * Não inclui pastas nem subpastas.
     * @param pasta Pasta alvo.
     * @return Lista de Strings individuais com os nomes dos ficheiros presentes na pasta.
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<String> stringToSetFicheiros(String pasta) throws IOException, InterruptedException {
        String stream = lsPastaToString(pasta);
        List<String> ficheiros = new ArrayList<>();

        String[] lista = stream.split("\n");
        for(int i = 1;i < lista.length; i++) {
            String[] linha = lista[i].split("\\ +");
            if (linha[1].equals("1")) ficheiros.add(linha[6]); // verificar se é ficheiro, se for qualquer outra coisa não acrescenta
        }
        return ficheiros;
    }


    /**
     * Transforma uma lista de strings numa única String.
     * @return String formatada.
     */
    public String ficheirosToString () {
        StringBuilder string = new StringBuilder();

        if (this.ficheiros.size() != 0) {
            Iterator<String> a = this.ficheiros.iterator();
            while (a.hasNext()) string.append((String) a.next()).append('\n');
        }

        return string.toString();
    }


    /**
     * Serialize do packet [FILECHECK]
     * @return array de bytes serializados.
     */
    public byte[] fileCheckToBytes () {
        byte[] b = this.PacketToBytes(); // cabeçalho

        byte[] alldados = new byte[0];
        int tam = 0;
        if (this.ficheiros.size() > 0) { // caso a pasta seja não vazia de ficheiros, copia para um array de bytes
            alldados = ficheirosToString().getBytes(StandardCharsets.UTF_8);
            tam = alldados.length;
        }

        byte[] append = new byte[b.length + tam];
        System.arraycopy(b,0,append,0,b.length); // copia o cabeçalho

        // se tiver ficheiros para copiar, adiciona-os ao array
        if (this.ficheiros.size() > 0) System.arraycopy(alldados,0, append, b.length, alldados.length);

        return append;
    }


    /**
     * Deserialize do packet [FILECHECK].
     * @param array array de bytes serializado.
     * @return Packet deserialized.
     * @throws CorruptedPacketException caso o checksum não se confirme, lança uma exceção.
     */
    public static Packet bytesToFileCheck (byte[] array) throws CorruptedPacketException {

        Packet p  = Packet.BytesToPacket(array); // cabeçalho

        if (array.length > 14) { //se tiver mais dados para além do cabeçalho

            byte[] l = new byte[array.length - 14]; //copia os dados para um array
            System.arraycopy(array, 14, l, 0, array.length - 14);

            // faz separação dos nomes de ficheiros
            String data = new String(l);
            String[] ficheiros = data.split("\n");

            //cria uma lista com as mesmas strings
            List<String> f = new ArrayList<>();
            for (int i = 0; i < ficheiros.length; i++) f.add(ficheiros[i]);

            //cria a packet
            Packet file = new FileCheck(p.getSeqNum(), f);

            // define a checksum
            file.setCheckSum(p.getCheckSum());

            return file;
        }

        // caso não tenha nada para além do cabeçalho, cria um packet [FILECHECK] com uma lista de ficheiros vazia
        Packet file = new FileCheck(p.getSeqNum(), new ArrayList<>());
        return file;
    }

}
