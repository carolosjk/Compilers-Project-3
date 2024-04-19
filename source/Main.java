import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length == 0){
            System.err.println("Usage: java Main [file1] [file2] ... [fileN]");
            System.exit(1);
        }



        FileInputStream fis = null;
        for (String argument: args){
            try{
                fis = new FileInputStream(argument);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                MyVisitor eval = new MyVisitor();
                root.accept(eval, null);

                MyVisitor2 check = new MyVisitor2(eval.classes);
                root.accept(check, null);

                //Strip a filename from its extension
                String fileName = argument.substring(0, argument.lastIndexOf('.'));

                MyVisitorLLVM llvm = new MyVisitorLLVM(fileName,check.classes);
                root.accept(llvm, null);

                System.out.println("Program parsed successfully.");

            }
            catch(Exception ex){
                System.out.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
