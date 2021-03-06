import java.util.ArrayList;
import java.util.Arrays;

public class CodeLine {

    String symbol;
    String mnemonic;
    String[] operands;
    boolean extended;
    String address ;


    private CodeLine(String symbol, String mnemonic, boolean extended, String[] operands) {
        this.symbol = symbol;
        this.mnemonic = mnemonic;
        this.extended = extended;
        this.operands = operands;
    }

    private CodeLine(String symbol, String mnemonic, boolean extended, String[] operands , String address) {
        this.symbol = symbol;
        this.mnemonic = mnemonic;
        this.extended = extended;
        this.operands = operands;
        this.address = address;
    }

    @Override
    public String toString() {
        return (this.address + " \t" + (this.symbol != null? this.symbol:"") + " \t" + this.mnemonic + " \t" + (this.operands[0] != null? this.operands[0]:"") + (this.operands[1] != null? (','+this.operands[1]) :"")) ;
    }

    static CodeLine parse1(String statement) {
        //String[] tokens = statement.trim().split("  ");
        ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(statement.split(" ")));
        tokens.removeAll(Arrays.asList("" ,null));

        String symbol= null;
        String mnemonic = null;
        String[] Operands=null;
        boolean extended = false;
        int index = 0;
        int Rflag=0;

       if(tokens.contains("RSUB"))
       {
           Rflag=1;
           if(tokens.size()==1)
               mnemonic=tokens.get(0);
           else if(tokens.size()==2)
           {
               symbol=tokens.get(0);
               mnemonic=tokens.get(1);

           }

       }

        if (tokens.size() == 3 && Rflag ==0) {
            symbol = tokens.get(index++); //tokens[index++];
            symbol = symbol.replace(" " ,"");
        } else if(Rflag==0) {
            symbol = null;
        }
        
         if(!tokens.contains("RSUB"))
        {
             mnemonic = tokens.get(index++); //tokens[index++];
        }
        assert mnemonic != null;
        mnemonic = mnemonic.replace(" ", "");
        try{
        if (mnemonic.charAt(0) == '+') {
            extended = true;
            //mnemonic = mnemonic.substring(1);
        }}catch (StringIndexOutOfBoundsException e){
            //TODO
        }


        Operands = new String[2];
        if(!tokens.contains("RSUB")) {
            if (index < tokens.size()) {
                int pos = tokens.get(index).indexOf(','); //tokens[index].indexOf(',');
                if (pos > 0) {
                    Operands[0] = tokens.get(index).substring(0, pos); //tokens[index].substring(0, pos);
                    Operands[0] = Operands[0].replace(" ", "");
                    Operands[1] = tokens.get(index).substring(pos + 1); //tokens[index].substring(pos + 1);
                    Operands[1] = Operands[1].replace(" ", "");
                } else {
                    Operands[0] = tokens.get(index); //tokens[index];
                    Operands[0] = Operands[0].replace(" ", "");
                    Operands[1] = null;
                }
            } else {
                Operands[0] = Operands[1] = null;
            }
        }
        Rflag=0;

        return new CodeLine(symbol, mnemonic, extended, Operands);
    }

    static CodeLine parse2(String statement) {
        //String[] tokens = statement.trim().split("  ");
        ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(statement.split("\t")));
        tokens.removeAll(Arrays.asList(" " ,null));

        String symbol, mnemonic = null;
        String[] Operands;
        boolean extended = false;
        int index = 0;
        String address = "";
        if(tokens.contains("RSUB ")) {
            if (tokens.size() == 2){
                address = tokens.get(0);
                address.replace(" ","");
                mnemonic = tokens.get(1);
                mnemonic.replace(" ","");
            }
            else if (tokens.size()==3)
            {
                address =  tokens.get(0);
                address.replace(" ","");
                symbol=tokens.get(1);
                mnemonic=tokens.get(2);
            }
        }

        if (tokens.size() == 4) {
            address =  tokens.get(index++);
            address.replace(" ","");
            symbol = tokens.get(index++); //tokens[index++];
            symbol = symbol.replace(" " ,"");
        } else {
            address =  tokens.get(index++);
            address.replace(" ","");
            symbol = null;
        }

        if(!tokens.contains("RSUB"))
        {
            mnemonic = tokens.get(index++); //tokens[index++];
            mnemonic = mnemonic.replace(" ", "");
        }
        mnemonic = mnemonic.replace(" ", "");
        try{
            if (mnemonic.charAt(0) == '+') {
                extended = true;
                mnemonic = mnemonic.substring(1);
            }}catch (StringIndexOutOfBoundsException e){
            //TODO
        }


        Operands = new String[2];
        if(!tokens.contains("RSUB")) {
            if (index < tokens.size()) {
                int pos = tokens.get(index).indexOf(','); //tokens[index].indexOf(',');
                if (pos > 0) {
                    Operands[0] = tokens.get(index).substring(0, pos); //tokens[index].substring(0, pos);
                    Operands[0] = Operands[0].replace(" ", "");
                    Operands[1] = tokens.get(index).substring(pos + 1); //tokens[index].substring(pos + 1);
                    Operands[1] = Operands[1].replace(" ", "");
                } else {
                    Operands[0] = tokens.get(index); //tokens[index];
                    Operands[0] = Operands[0].replace(" ", "");
                    Operands[1] = null;
                }
            } else {
                Operands[0] = Operands[1] = null;
            }
        }

        return new CodeLine(symbol, mnemonic, extended, Operands ,address);
    }
}
