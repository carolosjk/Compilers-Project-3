import syntaxtree.*;
import visitor.*;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList; 


//My LLVM visitor. Wish me luck!
public class MyVisitorLLVM extends GJDepthFirst<String, String> {
    
    int registerID;
    int labelID;
    String fileName;
    public Map<String, MyData> classes;     //The class dictionary from MyVisitor
    File outputFile;

    public MyVisitorLLVM(String f,Map<String, MyData> c){
        this.registerID = 0;
        this.labelID = 0;
        this.fileName = f;
        this.classes = new LinkedHashMap<String,MyData>(c);
        CreateDirectoryAndFile("LLVM", fileName);
        emitTheGivenMethods();
    }

    
    public void CreateDirectoryAndFile(String directory, String fileName) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdir();
        }
        outputFile = new File(directory + "/" + fileName + ".ll");
        //delete file if already exists and recreate it for writing
        if (outputFile.exists()) {
            outputFile.delete();
            outputFile = new File(directory + "/" + fileName + ".ll");
        }
        
    }

    //Prints the LLVM code to the file
    public void emit(String  txt) {
        try {
            FileWriter fw = new FileWriter(outputFile, true);
            fw.write(txt);
            fw.close();
        } catch (IOException e) {
            System.out.println("Error writing to file\n");
        }
    }

    public void emitTheGivenMethods(){
        emit("declare i8* @calloc(i32, i32)\n");
        emit("declare i32 @printf(i8*, ...)\n");
        emit("declare void @exit(i32)\n");
        emit("\n");
        emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n");
        emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n");
        emit("define void @print_int(i32 %i) {\n");
        emit("    %_str = bitcast [4 x i8]* @_cint to i8*\n");
        emit("    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n");
        emit("    ret void\n");
        emit("}\n");
        emit("\n");
        emit("define void @throw_oob() {\n");
        emit("    %_str = bitcast [15 x i8]* @_cOOB to i8*\n");
        emit("    call i32 (i8*, ...) @printf(i8* %_str)\n");
        emit("    call void @exit(i32 1)\n");
        emit("    ret void\n");
        emit("}\n\n");
    }

    String getLLVMType(String type){
        if (type.equals("int")){
            return "i32";
        }
        else if (type.equals("int[]")){
            return "i32*";
        }
        else if (type.equals("bool")){
            return "i1";
        }
        else{
            return "i8*";
        }
    }

    String new_label(){
        return "label"+labelID++;
    }

    String new_temp(){
        return "%_"+registerID++;
    }


    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n,String classname) throws Exception {
        n.f0.accept(this,"main");

        for (int i =0;i< n.f1.size();i++){
            n.f1.elementAt(i).accept(this,null);
        }

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "String"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String classname) throws Exception {

        classname = n.f1.accept(this, classname);

        emit("define i32 @main() {\n");

        MyData myclass = classes.get(classname);

        //iterate myclass.variables map and emit the code for each variable
        for (Map.Entry<String, Variable> entry : myclass.variables.entrySet()) {
            String varName = entry.getKey();
            Variable var = entry.getValue();
            emit("    %"+varName+" = alloca "+getLLVMType(var.type)+"\n");
        }
        
        // for (int i =0;i< n.f15.size();i++){
        //     n.f15.elementAt(i).accept(this,classname);
        // }

        emit("    ret i32 0\n");
        emit("}\n\n");

        return null;
    }


    /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    @Override
    public String visit(TypeDeclaration n, String classname) throws Exception {
        n.f0.accept(this, classname);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    @Override
    public String visit(ClassDeclaration n, String classname) throws Exception {
        classname = n.f1.accept(this, null);

        MyData myclass = classes.get(classname);
        
        //iterate myclass.variables map and emit the code for each variable
         for (Map.Entry<String, Variable> entry : myclass.variables.entrySet()) {
            String varName = entry.getKey();
            Variable var = entry.getValue();
            emit("    %"+varName+" = alloca "+getLLVMType(var.type)+"\n");
        }
        for (int i =0;i< n.f4.size();i++){
            n.f4.elementAt(i).accept(this,classname);
        }

        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    @Override
    public String visit(ClassExtendsDeclaration n, String classname) throws Exception {
        classname = n.f1.accept(this, null);

        MyData myclass = classes.get(classname);
        
        //iterate myclass.variables map and emit the code for each variable
         for (Map.Entry<String, Variable> entry : myclass.variables.entrySet()) {
            String varName = entry.getKey();
            Variable var = entry.getValue();
            emit("    %"+varName+" = alloca "+getLLVMType(var.type)+"\n");
        }
        for (int i =0;i< n.f6.size();i++){
            n.f6.elementAt(i).accept(this,classname);
        }

        return null;
    }

    // /**
    // * f0 -> Type()
    // * f1 -> Identifier()
    // * f2 -> ";"
    // */
    @Override
    public String visit(VarDeclaration n, String classname) throws Exception {
        
        String type = n.f0.accept(this, null);
        String varName = n.f1.accept(this, null);

        emit(", " + getLLVMType(type) + " %" + varName);

        return null;
    }

    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    @Override
    public String visit(MethodDeclaration n, String classname) throws Exception {

        String methodName = n.f2.accept(this, null);

        MyData myclass = classes.get(classname);
        Method method = myclass.methods.get(methodName);

        String type = method.returnType;

        emit("define "+getLLVMType(type)+" @"+classname+"."+methodName+"(i8* %this");


        
        //iterate method.arguments map and emit the code for each argument decleration
        for (Map.Entry<String, String> entry : method.arguments.entrySet()) {
            String argName = entry.getKey();
            String argType = entry.getValue();
            emit(", "+getLLVMType(argType)+" %."+argName);
        }

        emit(") {\n");

        //iterate method.arguments map and emit the code for each argument
        for (Map.Entry<String, String> entry : method.arguments.entrySet()) {
            String varName = entry.getKey();
            String var = entry.getValue();
            emit("    %"+varName+" = alloca "+getLLVMType(var)+"\n");
            emit( "    store "+getLLVMType(var)+" %."+varName+", "+getLLVMType(var)+"* %"+varName+"\n");
        }

         //iterate method.variables map and emit the code for each variable
            for (Map.Entry<String, String> entry : method.variables.entrySet()) {
                String varName = entry.getKey();
                String var = entry.getValue();
                emit("    %"+varName+" = alloca "+getLLVMType(var)+"\n");
            }

        // for (int i =0;i< n.f8.size();i++){
        //     n.f8.elementAt(i).accept(this, classname);
        // }

        String returnType = n.f10.accept(this,classname);

        emit( "\n    ret "+getLLVMType(type)+" %"+returnType+"\n");
        emit( "}\n\n");

        return null;
    }

         /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    public String visit(Expression n, String classname) throws Exception {
        return n.f0.accept(this, classname);
    }

      /**
     * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, String classname) throws Exception {
        String type1 = n.f0.accept(this,classname);
        String type2 = n.f2.accept(this,classname);
        return "bool";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, String classname) throws Exception {
        String type1 = n.f0.accept(this,classname);
        String type2 = n.f2.accept(this,classname);

        return "bool";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, String classname) throws Exception {
        String type1 = n.f0.accept(this,classname);
        String type2 = n.f2.accept(this,classname);

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, String classname) throws Exception {
        String type1 = n.f0.accept(this,classname);
        String type2 = n.f2.accept(this,classname);

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, String classname) throws Exception {
        String type1 = n.f0.accept(this,classname);
        String type2 = n.f2.accept(this,classname);

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String classname) throws Exception {
        String type1 = n.f0.accept(this,classname);
        String type2 = n.f2.accept(this,classname);
        if (type1.equals("bool[]")){
            return "bool";
        }
        else{
            return "int";
        }
    }

      /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, String classname) throws Exception {
        String type = n.f0.accept(this,classname);

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, String classname) throws Exception {
        String name = n.f0.accept(this,classname);

        if (name.equals("%this")){
            name = classname;
        }

        String methodname = n.f2.accept(this,classname);
        // System.out.println(methodname);

        MyData myclass = classes.get(name);



        // if myclass

        Method method = null;

        method = myclass.methods.get(methodname);
        


        return method.returnType;

    }

       /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, String classname) throws Exception {
        String expr = n.f0.accept(this, classname);
        String exprtail = n.f1.accept(this, classname);
        if (exprtail != null){
            return expr + exprtail;
        }else{
            return expr;
        }
    }

    /**
     * f0 -> ( ExpressionTerm() )*
    */
    public String visit(ExpressionTail n, String classname) throws Exception {
        String expr = "";
        for (int i=0; i< n.f0.size();i++){
            expr += ','+n.f0.elementAt(i).accept(this,classname);
        }
        return expr;

    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, String classname) throws Exception {
        return n.f1.accept(this,classname);
    }

      /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n, String classname) throws Exception {
        return n.f0.accept(this, classname);
    }


      /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, String classname) throws Exception {
        String type = n.f0.accept(this,classname);

        return null;
    }

     /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public String visit(Statement n, String classname) throws Exception {
        n.f0.accept(this, classname);
        return null;
    }

    /**
     * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public String visit(Block n, String classname) throws Exception {
        for (int i=0; i<n.f1.size();i++){
            n.f1.accept(this,classname);
        }
        return null;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, String classname) throws Exception {
        String name = n.f0.accept(this,classname);


        String expr = n.f2.accept(this, classname);


        return null;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, String classname) throws Exception {
        String name = n.f0.accept(this,classname);



        String index = n.f2.accept(this, classname);


        String expr = n.f5.accept(this, classname);



        return null;
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String classname) throws Exception {
        String type = n.f2.accept(this,classname);

        return null;
    }

       /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, String classname) throws Exception {
        String type = n.f2.accept(this,classname);

        n.f4.accept(this,classname);
        n.f6.accept(this,classname);
        return null;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String classname) throws Exception {
        String type = n.f2.accept(this,classname);

        n.f4.accept(this,classname);
        return null;
    }


    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    @Override
    public String visit(Type n, String classname) throws Exception {
        return n.f0.accept(this, null);
    }

    /**
    * f0 -> BooleanArrayType()
    *       | IntegerArrayType()
    */
    @Override
    public String visit(ArrayType n, String classname) throws Exception {
        return n.f0.accept(this, null);
    }


    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    @Override
    public String visit(BooleanArrayType n, String classname) throws Exception {
        return "bool[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    @Override
    public String visit(IntegerArrayType n, String classname) throws Exception {
        return "int[]";

    }

    /**
    * f0 -> "boolean"
    */
    @Override
    public String visit(BooleanType n, String classname) throws Exception {
        return "bool";
    }

   /**
    * f0 -> "int"
    */
    @Override
    public String visit(IntegerType n, String classname) throws Exception {
        return n.f0.accept(this,classname);
    }

       /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, String classname) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String classname) throws Exception {
        return "1";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String classname) throws Exception {
        return "0";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    @Override
    public String visit(Identifier n, String classname) throws Exception {
        return n.f0.toString();
    }

      /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, String classname) throws Exception {
        return "%this";
    }

       /**
    * f0 -> BooleanArrayAllocationExpression()
    *       | IntegerArrayAllocationExpression()
    */
    public String visit(ArrayAllocationExpression n, String classname) throws Exception {
        return n.f0.accept(this,classname);
    }

    /**
     * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(BooleanArrayAllocationExpression n, String classname) throws Exception {
        String type = n.f3.accept(this, classname);

        return "bool[]";
    }

    /**
     * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(IntegerArrayAllocationExpression n, String classname) throws Exception {
        String type = n.f3.accept(this, classname);

        return "int[]";
    }

    /**
     * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String classname) throws Exception {
        String type = n.f1.accept(this, classname);

        return type;
    }

    /**
     * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, String classname) throws Exception {
        String expr = n.f1.accept(this, classname);
        String temp = new_temp() ;

        emit( "\t" + temp + " = xor i1 1, " + expr + "\n");


        return temp;
    }

    /**
     * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, String classname) throws Exception {
        return n.f1.accept(this,classname);
    }

}
