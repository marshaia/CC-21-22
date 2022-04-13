import java.nio.ByteBuffer;

/**
 * Classe responsável pela serialização de informação de um ficheiro em particular.
 * É uma extensão da classe Packet.
 */
public class Data extends Packet {

    private String fileName; // ficheiro alvo
    private byte[] dados; // conteúdo do ficheiro
    private byte last; // 1 - último da stream; 0 - não é último
    private int fileID; // nº de stream



    public Data(int seq,String fD,int id, byte l, byte[] d) {
        super(seq,(byte)1,(byte)0);
        this.fileName = fD;
        this.fileID = id;
        this.last = l;
        this.dados = d;
    }

    public Data(int seq, int id, byte l, byte[] d) {
        super(seq,(byte)1,(byte)0);
        this.fileID = id;
        this.last = l;
        this.dados = d;
    }



    public int tamanhoData() {return (this.tamanhoPacket()+5+this.dados.length);}
    public byte getLast() { return last;}
    public byte[] getDados() {return dados;}
    public int getFileID() { return fileID; }
    public String getFileName() {return this.fileName;}


    /**
     * Serialize do packet [DATA]
     * @return array serialized
     */
    public byte[] dataToBytes() {
        byte[] cabecalho = this.PacketToBytes(); // cabeçalho

        byte[] ID = ByteBuffer.allocate(4).putInt(this.fileID).array(); //nº de stream

        byte[] last = new byte[1]; // se é o último da stream ou não
        last[0] = this.last;

        byte[] append;

        // se for o primeiro packet a ser enviado (Sequencia=1) então nos dados apenas vai o nome do ficheiro
        if (this.getSeqNum() == 1) append = new byte[cabecalho.length + ID.length + last.length + (this.fileName.length()+1)];
        // se não for o primeiro packet a ser enviado (Sequencia!=1) então nos dados vai o conteúdo do ficheiro
        else append = new byte[cabecalho.length + ID.length + last.length + this.dados.length];

        System.arraycopy(cabecalho,0,append,0,cabecalho.length); //copia o cabeçalho
        System.arraycopy(ID,0,append,14,ID.length); //copia o NºStream
        System.arraycopy(last,0,append,18,last.length); //copia se é o último ou não

        if (this.getSeqNum() == 1) { // se for o primeiro packet da sequência então faz append do nome do ficheiro
            String fich = this.fileName + "\n";
            System.arraycopy(fich.getBytes(),0,append,19,fich.length());
        }
        else System.arraycopy(this.dados,0,append,19,this.dados.length); //caso contrário faz append dos dados

        return append;
    }


    /**
     * Deserialize do array de bytes para packet [DATA]
     * @param array array de bytes serialized.
     * @return Packet deserialized.
     * @throws CorruptedPacketException caso o checksum não se confirme, lança uma exceção.
     */
    public static Packet bytesToData(byte[] array) throws CorruptedPacketException {
        Packet p = Packet.BytesToPacket(array); // cabeçalho

        byte[] l = new byte[array.length-14]; //copia o resto dos dados
        System.arraycopy(array,14,l,0,array.length-14);

        int stream = ByteBuffer.wrap(l,0,4).getInt(); //NºStream
        byte ultimo = l[4]; // last

        byte[] data = new byte[l.length-5]; //dados propriamente ditos
        System.arraycopy(l,5,data,0,l.length-5);

        Packet packet;
        if (p.getSeqNum() == 1) { //se for Sequencia=1 então só contém o nome do ficheiro
            String nomeFich = new String(data).split("\n")[0];
            packet = new Data(p.getSeqNum(),nomeFich,stream,ultimo,null);
        }
        //se não é o primeiro é porque contém conteúdo de ficheiro.
        else packet = new Data(p.getSeqNum(),stream,ultimo,data);

        //define a checksum
        packet.setCheckSum(p.getCheckSum());

        return packet;
    }

}
