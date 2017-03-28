import java.io.*;
import java.util.*;

public class Assembler {


    static List<Instruction> OPTAB = new ArrayList<Instruction>();
    static List<Literals> LITTAB=new ArrayList<Literals>();
    static Map< String , String > SYMTAB = new HashMap<String , String>();
    static List <CodeLine> Assembly= new ArrayList<CodeLine>();
    static Map<String, Integer> registerTable = new HashMap<>();
    static int LOCCTR = 0 , startAddress = 0 , first_executable = -1 ; // initialized ot -1 to be updated only once
    static  int progLength = 0 ;
    static int baseAddres =0 ;
    static String [] assembler_directives = {"START" , "END" , "BASE" ,"NOBASE" ,"BYTE" , "WORD" , "RESB" , "RESW" };
    static  String fileName = "CODE.txt";

    public static void main(String []args) throws IOException {
        read_ISA();
        Pass1();
        pass2();
    }

    public static void read_ISA () throws IOException {
        BufferedReader optab_buffer = new BufferedReader(new FileReader("OPTAB.txt"));
        String str;
        String []parts;

        // reading the instruction set from text file

        while((str = optab_buffer.readLine()) != null){
            parts= str.split(" ");
            Instruction instruction=new Instruction(parts[0],parts[1], hex2decimal(parts[2]));
            OPTAB.add(instruction);
            Arrays.fill(parts,null);
        }
        optab_buffer.close();
    }

    public static void Pass1 () throws IOException  {
        String str ; int lineNum = 1;
        String [] lmo ; //label mnemonic operands

        // reading assembly code from text file
        BufferedReader asm = new BufferedReader(new FileReader(fileName));
        // writing to the intermediate text file
        BufferedWriter intermediate = new BufferedWriter(new FileWriter("intermediate.txt"));
        BufferedWriter symbol_table = new BufferedWriter(new FileWriter( "symbol_table.txt"));
        symbol_table.write("Symbol" +"\t"+"Address"+"\t"+"\n");
        BufferedWriter literal_table = new BufferedWriter(new FileWriter("literal_table.txt"));
        literal_table.write("Literal" + " \t"+ "Length" +" \t"+"Value" +" \t"+ "Address"+"\n" );

        while((str = asm.readLine()) != null){
            if (isComment(str)) { lineNum++; continue; }
            CodeLine line = CodeLine.parse1(str);
            line.address = Integer.toHexString(LOCCTR).toUpperCase();

            if(line.symbol != null) {
                if (SYMTAB.containsKey(line.symbol)) {
                    System.out.println("Duplicate Symbol ERROR");
                } else {
                    SYMTAB.put(line.symbol, Integer.toHexString(LOCCTR).toUpperCase());
                }
            }


            switch (line.mnemonic){
                case "START":
                    startAddress = hex2decimal(line.operands[0]);
                    LOCCTR = startAddress;
                    line.address = Integer.toHexString(LOCCTR).toUpperCase();
                    SYMTAB.put(line.symbol,line.address);
                    break;

                case "END":
                    break;

                case "RESW":
                    LOCCTR += 3 * Integer.parseInt(line.operands[0]);
                    break;

                case "RESB":
                    LOCCTR += Integer.parseInt(line.operands[0]);
                    break;

                case "BYTE":
                    String s = line.operands[0];  //Operand 1
                    switch (s.charAt(0)) {
                        case 'C':
                            int length= s.length()-3;
                            LOCCTR += (length);
                            // C'EOF' -> EOF -> 3 bytes
                            Literals literal=new Literals(s,length,s.substring(2,s.length()-1),Integer.toHexString(LOCCTR).toUpperCase(),0);
                            LITTAB.add(literal);
                            literal_table.write( literal.name+ "\t\t"+ literal.length +"\t\t"+literal.value +"\t\t"+ literal.address+"\n" );
                            break;
                        case 'X':
                            length = (s.length()-3)/2;
                            LOCCTR += (s.length() - 3) / 2; // X'05' -> 05 -> 2 half bytes
                             literal=new Literals(s,length,s.substring(2,s.length()-1),Integer.toHexString(LOCCTR).toUpperCase(),0);
                            LITTAB.add(literal);
                            literal_table.write( literal.name+ "\t\t"+ literal.length +"\t\t"+literal.value +"\t\t"+ literal.address+"\n" );
                            break;
                    }
                    break;

                case "WORD":
                    LOCCTR += 3 ;
                    break;

                case "BASE":
                    break;

                case "NOBASE":
                    break;

                default:
                    if(search(line.mnemonic)!= -1){
                        if(first_executable < 0){
                            first_executable = LOCCTR;
                        }
                        switch (search(line.mnemonic)){  //switch( format of the mnemonic )
                            case 1:
                                LOCCTR += 1;
                                break;
                            case 2:
                                LOCCTR += 2;
                                break;
                            case 7:         //case 3/4 :   named 7 for ease of use in the code
                                LOCCTR += 3 + ((line.extended)? 1:0);
                                break;
                        }
                    }
                    else{
                        System.out.println("INVALID OPERATION in line "+ lineNum + " : " +line.toString());
                        break;
                    }
            }
            // System.out.println(line);          //Uncomment to show the address and Source CodeLine
            lineNum ++;
            intermediate.write(line.toString()+"\n");
            if(line.symbol != null){symbol_table.write(line.symbol+" \t"+line.address+"\n");}
        }

        progLength = LOCCTR - startAddress ;
        intermediate.close();
        asm.close();
        symbol_table.close();
        literal_table.close();
    }

    public static void pass2 () throws IOException {

        initRegisterTable();
        String str;
        TextRecord textRecord = new TextRecord(startAddress);
        ArrayList<ModificationRecord> modificationRecords = new ArrayList<>();
        BufferedReader asm = new BufferedReader(new FileReader("intermediate.txt"));
        BufferedWriter objectProgram = new BufferedWriter(new FileWriter("ObjectCode.txt"));
        while((str = asm.readLine()) != null) {

            if (isComment(str)) { continue; }

            CodeLine line = CodeLine.parse2(str);

            if(line.mnemonic.equals("START")){
                objectProgram.write(new HeaderRecord(line.symbol , startAddress , progLength ).toObjectProgram() + "\n" );
            }
            else if (line.mnemonic.equals("END")){break ;}
            else {
                String objectCode = assembleInstruction (line);      //TODO (in progress)

                if(line.extended == true && SYMTAB.containsKey(line.symbol)){ //TODO
                    modificationRecords.add(new ModificationRecord(hex2decimal(line.address)+1 , 5));
                }                                                                                  //TODO needs something i don't know what

                if ( line.mnemonic == "RESW" || line.mnemonic == "RESB" || textRecord.add(objectCode) == false){
                    objectProgram.write(textRecord.toObjectProgram() + '\n');
                    textRecord = new TextRecord(hex2decimal(line.address));
                    textRecord.add(objectCode);
                }
            }
        }
        objectProgram.write(textRecord.toObjectProgram() + '\n');
        for(ModificationRecord record : modificationRecords){
            objectProgram.write(record.toObjectProgram() + "\n");
        }
        objectProgram.write(new EndRecord(first_executable).toObjectProgram() + '\n');
        asm.close();
        objectProgram.close();
    }

    public static String assembleInstruction (CodeLine line){
        String objectCode = "";
        int format = search(line.mnemonic);
        if ( format != -1){
            switch (format){
                case 1:
                    objectCode = searchOPCODE(line.mnemonic);
                    break;
                case 2:
                    objectCode = searchOPCODE(line.mnemonic);
                    //TODO register table is hard coded
                    objectCode += Integer.toHexString(registerTable.get(line.operands[0]));
                    objectCode += Integer.toHexString(registerTable.get(line.operands[1]));
                    break;
                case 7:
                    int n = 1 << 5;
                    int i = 1 << 4;
                    int x = 1 << 3;
                    int b = 1 << 2;
                    int p = 1 << 1;
                    int e = 1;

                    int opcode = hex2decimal(searchOPCODE(line.mnemonic)) << 4 ;
                    String operand = line.operands[0]; // since format 3/4 only has one operand

                    if( operand == null){           //RSUB
                        opcode = opcode | n | i ;
                        opcode = opcode << 12 ;
                    }
                    else {
                        switch (operand.charAt(0)){
                            case '#':       //immediate addressing mode
                                opcode |= i;
                                operand = operand.substring(1);
                                break;
                            case '@':       //indirect addressing mode
                                opcode |= n;
                                operand = operand.substring(1);
                                break;
                            default:        //simple and direct addressing modes
                                opcode = opcode | n | i ;
                                if(line.operands[1] != null){
                                    opcode |= x;
                                }
                        }

                        int disp = 0 ;

                        if(SYMTAB.get(operand) == null){
                            disp = Integer.parseInt(operand);
                        }
                        else{
                            int targetAddress = hex2decimal(SYMTAB.get(operand));

                            if(line.extended == false){
                                disp = targetAddress - hex2decimal(line.address) ;
                                if( disp >= -2048 && disp <= 2047){
                                    opcode |= p ;
                                }
                                else {
                                    opcode |= b;
                                    disp = targetAddress - baseAddres ;
                                }
                            }
                        }

                        if(line.extended == true){
                            opcode |= e;
                            opcode = opcode << 20;
                            opcode |= disp;
                            objectCode = String.format("%08X" , opcode);
                        }
                        else{
                            opcode = opcode << 12;
                            opcode |= disp;
                            objectCode = String.format("%06X" , opcode);
                        }
                    }

                    break;
            }
        }
        else if (line.mnemonic == "BYTE"){
            char type  = line.operands[0].charAt(0);
            switch (type){
                case 'C' :
                    for(char c : line.operands[0].toCharArray()){
                        objectCode += Integer.toHexString(c).toUpperCase();
                    }
                    break;
                case 'X' :
                    objectCode = line.operands[0];
                    break;
            }
        }
        else if (line.mnemonic == "WORD"){
            objectCode = line.operands[0];
        }
        else if (line.mnemonic == "BASE"){
            baseAddres = hex2decimal(SYMTAB.get(line.operands[0]));
        }
        else if (line.mnemonic == "NOBASE"){
            baseAddres = 0;
        }
        return objectCode;
    }

    public static boolean isComment (String str) {
        return str.startsWith(".");
    }

    public static int search (String mnemonic) {
        int found=0;
        int i;
        for ( i=0 ; i< OPTAB.size() ; i++)
        {
            if(OPTAB.get(i).mnemonic.equals(mnemonic)) {

                found = 1;
                break;
            }
        }
        if(found == 1)
            return OPTAB.get(i).format;
        else
            return -1;
    }

    public static String searchOPCODE (String mnemonic) {
        int found=0;
        int i;
        for ( i=0 ; i< OPTAB.size() ; i++)
        {
            if(OPTAB.get(i).mnemonic.equals(mnemonic)) {
                found = 1;
                break;
            }
        }
        if(found == 1)
            return Integer.toHexString(OPTAB.get(i).opcode).toUpperCase();
        else
            return null;
    }

    public static void initRegisterTable (){
        registerTable.put("A", 0);
        registerTable.put("X", 1);
        registerTable.put("L", 2);
        registerTable.put("B", 3);
        registerTable.put("S", 4);
        registerTable.put("T", 5);
        registerTable.put("F", 6);
        registerTable.put("SW",7);
    }

    public static int hex2decimal(String S) {
        String s = S.trim();
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
}
