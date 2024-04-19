import syntaxtree.*;
import visitor.*;
import java.util.Map;
import java.util.LinkedHashMap; 
import java.util.ArrayList; 

//Checking for the rest of the error type like undeclared variables/methods, correct math operations, array out of index, etc
public class MyVisitor2 extends GJDepthFirst<String, String> {

    public Map<String, MyData> classes;     //The class dictionary from MyVisitor
    String currentMethod;
    
    public MyVisitor2(Map<String, MyData> c){
        this.classes = new LinkedHashMap<String,MyData>(c);
        currentMethod = null;
    }

    public boolean isPrimitive(String type){
        return (type == "int" || type == "int[]" || type == "bool" || type == "bool[]" );
    }

    public boolean isChild(String name,String type){
        if (type.equals(name)){
            return true;
        }
        if (classes.get(name) != null) {
            String parent = classes.get(name).parent;
            while (parent != null){
                if (parent.equals(type)){
                    return true;
                }
                name = parent;
                parent = classes.get(name).parent;
            }
        }
        return false;
    }

    public String getTypeFromIdentifier(String classname, String name){
        MyData myclass = classes.get(classname);
        if (myclass != null && currentMethod != null){
            Method method = myclass.methods.get(currentMethod);
            if (method != null){
                if (method.arguments.get(name) != null){
                    return method.arguments.get(name);
                }
                if (method.variables.get(name) != null){
                    return method.variables.get(name);
                }
            }
        }
        if (myclass != null && myclass.variables.get(name) != null){
            return myclass.variables.get(name).type;
        }
        if (myclass != null){
            String parent = myclass.parent;
            while (parent != null){
                myclass = classes.get(parent);
                if (myclass!=null){
                    if (myclass.variables.get(name) != null){
                        return myclass.variables.get(name).type;
                    }
                }
                parent = myclass.parent;
            }
        }


        return null;
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

        for (int i =0;i< n.f14.size();i++){
            n.f14.elementAt(i).accept(this,classname);
        }
        for (int i =0;i< n.f15.size();i++){
            n.f15.elementAt(i).accept(this,classname);
        }

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

        for (int i =0;i< n.f3.size();i++){
            n.f3.elementAt(i).accept(this,classname);
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

        for (int i =0;i< n.f5.size();i++){
            n.f5.elementAt(i).accept(this,classname);
        }
        for (int i =0;i< n.f6.size();i++){
            n.f6.elementAt(i).accept(this,classname);
        }

        return null;
    }

        /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, String classname) throws Exception {
        String type = n.f0.accept(this, null);

        if (!isPrimitive(type) && !classes.containsKey(type)){  //checking for type to be a primitive or a class we found with MyVisitor
            throw new Exception("Invalid variable type: "+type);
        }

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
        String type = n.f1.accept(this, null);

        currentMethod = n.f2.accept(this, null);

        if (!isPrimitive(type) && !classes.containsKey(type)){  //checking for return type to be a primitive or a class we found with MyVisitor
            throw new Exception("Invalid return type: "+type);
        }
        if (n.f4.present()){    //Checking for method arguments
            n.f4.accept(this,classname);
        }

        for (int i =0;i< n.f7.size();i++){
            n.f7.elementAt(i).accept(this, classname);
        }
        for (int i =0;i< n.f8.size();i++){
            n.f8.elementAt(i).accept(this, classname);
        }

        String returnType = n.f10.accept(this,classname);
        if (!(type.equals(returnType))){
            if (!isChild(returnType,type)){
                throw new Exception("Method returns: "+returnType+" instead of "+type);
            }
        }

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
        if (!type1.equals("bool") || !type2.equals("bool")){
            throw new Exception("Only booleans allowed with expression && ,instead got: "+type1+", "+type2);
        }
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
        if (!type1.equals("int") || !type2.equals("int")){
            throw new Exception("Only integers allowed with expression < ,instead got: "+type1+", "+type2);
        }
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
        if (!type1.equals("int") || !type2.equals("int")){
            throw new Exception("Only integers allowed with expression + ,instead got: "+type1+", "+type2);
        }
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
        if (!type1.equals("int") || !type2.equals("int")){
            throw new Exception("Only integers allowed with expression - ,instead got: "+type1+", "+type2);
        }
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
        if (!type1.equals("int") || !type2.equals("int")){
            throw new Exception("Only integers allowed with expression * ,instead got: "+type1+", "+type2);
        }
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
        if (!type1.equals("bool[]") && !type1.equals("int[]")){
            throw new Exception("Array lookup on non-array type");
        }
        if (!type2.equals("int")){
            throw new Exception("Array lookup index must be int");
        }
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
        if (type != "bool[]" && type != "int[]"){
            throw new Exception(".length can only be uses with arrays");
        }
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
        if (!classes.containsKey(name) && name != "this"){
            throw new Exception("Invalid class name "+name);
        }
        if (name.equals("this")){
            name = classname;
        }

        String methodname = n.f2.accept(this,classname);
        // System.out.println(methodname);

        MyData myclass = classes.get(name);



        // if myclass

        Method method = null;

        method = myclass.methods.get(methodname);
        

        while (method == null){     //Check for the method at the class or parent classes
            if (myclass.parent == null){
                throw new Exception("Invalid method call for class "+name+", method: "+methodname);
            }
            myclass = classes.get(myclass.parent);
            method = myclass.methods.get(methodname);
        }

        if (n.f4.present()){
            String expr = n.f4.accept(this, classname);
            if (!expr.equals(method.getArgumentString()) ){
                String[] args1 = expr.split(",");
                String[] args2 = method.getArgumentString().split(",");

                if (args1.length != args2.length){
                    throw new Exception("Argument missmatch for method at class "+name+", method: "+methodname);
                }

                for (int i=0;i<args1.length;i++){
                    if(!isChild(args1[i],args2[i])){
                        throw new Exception("Argument missmatch for method at class "+name+", method: "+methodname);
                    }
                }
            }
        }

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

        if (type.equals("this")){
            return classname;
        }
        else if (!isPrimitive(type)){
            if (classes.containsKey(type)){
                return type;
            }
            return getTypeFromIdentifier(classname, type);
        }
        else{
            return type;
        }
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
        String type = getTypeFromIdentifier(classname, name);

        if (type == null){
            throw new Exception("Undeclared variable "+name);
        }
        String expr = n.f2.accept(this, classname);

        if (!type.equals(expr) && !isChild(expr, type)){
            throw new Exception("Can't assign "+expr+" to a variable of type "+type);
        }
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
        String type = getTypeFromIdentifier(classname, name);

        if (type == null){
            throw new Exception("Undeclared variable "+name);
        }

        if (!type.equals("int[]") && !type.equals("bool[]")){
            throw new Exception("Attempted assignment to non array");
        }

        String index = n.f2.accept(this, classname);
        if (!index.equals("int")){
            throw new Exception("Array index must be int");
        }

        String expr = n.f5.accept(this, classname);

        if (type.equals("int[]") && !expr.equals("int")){
            throw new Exception("Trying to assign "+expr+" to a int array");
        }
        if (type.equals("bool[]") && !expr.equals("bool")){
            throw new Exception("Trying to assign "+expr+" to a bool array");
        }

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
        if (!type.equals("int") && !type.equals("bool")){
            throw new Exception("Can't print type "+type);
        }
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
        if (!type.equals("bool")){
            throw new Exception("If condition must be of type bool");
        }
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
        if (!type.equals("bool")){
            throw new Exception("While condition must be of type bool");
        }
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
        return "int";
    }

       /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, String classname) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String classname) throws Exception {
        return "bool";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String classname) throws Exception {
        return "bool";
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
        return "this";
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
        if (!type.equals("int")){
            throw new Exception("Invalid type for array allocation. Should be int, instead was: "+type);
        }
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
        if (!type.equals("int")){
            throw new Exception("Invalid type for array allocation. Should be int, instead was: "+type);
        }
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
        if (!classes.containsKey(type)){
            throw new Exception("Allocation for invalid type/class "+type);
        }
        return type;
    }

    /**
     * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, String classname) throws Exception {
        String type = n.f1.accept(this, classname);
        if (!type.equals("bool")){
            throw new Exception("! can only be followed by boolean expression");
        }
        return "bool";
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
