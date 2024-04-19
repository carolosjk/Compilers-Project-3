import syntaxtree.*;
import visitor.*;
import java.util.Map;
import java.util.LinkedHashMap; 
import java.util.ArrayList; 

//Visitor for type checking, encountering for all classes, variables and methods and reject all redeclarations
public class MyVisitor extends GJDepthFirst<String, MyData> {
    // Good luck....

    public Map<String, MyData> classes;     //Keep a dictionary with all classes

    public MyVisitor(){
        this.classes = new LinkedHashMap<String,MyData>();
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, MyData argu) throws Exception {
        n.f0.accept(this,null);

        for (int i =0;i< n.f1.size();i++){
            n.f1.elementAt(i).accept(this,null);
        }

        //Print offsets hopefully
        // for (Map.Entry<String, MyData> entry : classes.entrySet()) {
        //     MyData parent = null;
        //     if (entry.getValue().parent != null){
        //         parent = classes.get(entry.getValue().parent);
        //     }
        //     entry.getValue().printOffsets(entry.getKey(),parent);
        // }

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
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
    public String visit(MainClass n, MyData argu) throws Exception {
        String classname = n.f1.accept(this, null);
        // System.out.println("Class: " + classname);

        MyData data = new MyData(null);


        for (int i =0;i< n.f14.size();i++){
            n.f14.elementAt(i).accept(this,data);
        }
        for (int i =0;i< n.f15.size();i++){
            n.f15.elementAt(i).accept(this,data);
        }

        System.out.println();

        classes.put(classname,data);    //add class to classes dictionary
        return null;
    }

       /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    @Override
    public String visit(TypeDeclaration n, MyData argu) throws Exception {
        n.f0.accept(this, argu);
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
    public String visit(ClassDeclaration n, MyData argu) throws Exception {
        String classname = n.f1.accept(this, null);
        // System.out.println("Class: " + classname);

        if (this.classes.containsKey(classname)){       //class redeclaration
            throw new Exception("Redecleration of class: "+classname);
        }

        MyData data = new MyData(null);

        for (int i =0;i< n.f3.size();i++){
            n.f3.elementAt(i).accept(this,data);
        }
        for (int i =0;i< n.f4.size();i++){
            n.f4.elementAt(i).accept(this,data);
        }

        classes.put(classname,data);    //add class to classes dictionary
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
    public String visit(ClassExtendsDeclaration n, MyData argu) throws Exception {
        String classname = n.f1.accept(this, null);
        String parentname = n.f3.accept(this, null);
        // System.out.println("Class: " + classname);

        if (this.classes.containsKey(classname)){       //class redeclaration
            throw new Exception("Redecleration of class: "+classname);
        }
        if (!this.classes.containsKey(parentname)){     //class parent not declared
            throw new Exception("Class: "+classname+" extends class: "+parentname+" which has not been previously declared");
        }

        MyData data = new MyData(parentname);

        for (int i =0;i< n.f5.size();i++){
            n.f5.elementAt(i).accept(this,data);
        }
        for (int i =0;i< n.f6.size();i++){
            n.f6.elementAt(i).accept(this,data);
        }

        classes.put(classname,data);    //add class to classes dictionary
        return null;
    }

       /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, MyData argu) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);


        if (argu != null && argu.currentMethod == null && argu.variables.containsKey(name)){    //class variable redeclaration
            throw new Exception("Redeclaration of class variable: "+name);
        }
        else if (argu != null && argu.checkCurrentMethodForArgument(name)){    //class variable redeclaration
            throw new Exception("Redeclaration of method variable: "+name);
        }
        else if (argu != null && argu.checkCurrentMethodForVariable(name)){    //class variable redeclaration
            throw new Exception("Redeclaration of method variable: "+name);
        }

        if (argu.currentMethod == null){
            argu.addVariable(name, type);
        }else{
            argu.addMethodVariable(name, type);
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
    public String visit(MethodDeclaration n, MyData argu) throws Exception {
        String type = n.f1.accept(this, null);
        String name = n.f2.accept(this, null);

        if (argu.hasMethod(name)){      //Check for duplicate method names
            throw new Exception("Redeclaration of class method: "+name);
        }

        argu.addMethod(name,type);


        if (n.f4.present()){    //Checking for method arguments

            String args = n.f4.accept(this, null);
            for (String a: args.split(",")){
                if (argu.checkCurrentMethodForArgument(a.split("\\s+")[1])){
                    throw new Exception("Duplicate method argument: "+a.split("\\s+")[1]);
                }
                argu.addMethodArgument(a.split("\\s+")[1],a.split("\\s+")[0]);
            }
        }

        if (argu.parent != null && classes.get(argu.parent).hasMethod(name)){   //check for superclass method overriding
            Method parentMethod = classes.get(argu.parent).methods.get(name);
            if (type != parentMethod.returnType){
                throw new Exception("Method: "+name+" has different return type from parent class. Type expected: "+parentMethod.returnType);
            }
            if( !new ArrayList<>(argu.getCurrentMethod().arguments.entrySet()).equals(new ArrayList<>(parentMethod.arguments.entrySet()) ) ){
                throw new Exception("Method: "+name+" has different arguments from parent class. Type expected: "+parentMethod.returnType);
            }
            argu.setCurrentMethodIsOverride();
        }

        for (int i =0;i< n.f7.size();i++){
            n.f7.elementAt(i).accept(this, argu);
        }
        for (int i =0;i< n.f8.size();i++){
            n.f8.elementAt(i).accept(this, argu);
        }

        return null;
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    @Override
    public String visit(FormalParameterList n, MyData argu) throws Exception {
        return n.f0.accept(this, null) + n.f1.accept(this, null);
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    @Override
    public String visit(FormalParameter n, MyData argu) throws Exception {
        return n.f0.accept(this,null) + " " + n.f1.accept(this,null);
    }

    /**
    * f0 -> ( FormalParameterTerm() )*
    */
    @Override
    public String visit(FormalParameterTail n, MyData argu) throws Exception {
        String fpt = "";
        for (int i =0;i< n.f0.size();i++){
            fpt += ',' + n.f0.elementAt(i).accept(this,null);
        }
        return fpt;
    }

    /**
     * f0 -> ","
    * f1 -> FormalParameter()
    */
    @Override
    public String visit(FormalParameterTerm n, MyData argu) throws Exception {
        return n.f1.accept(this, null);
    }


       /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    @Override
    public String visit(Type n, MyData argu) throws Exception {
        return n.f0.accept(this, null);
    }

    /**
    * f0 -> BooleanArrayType()
    *       | IntegerArrayType()
    */
    @Override
    public String visit(ArrayType n, MyData argu) throws Exception {
        return n.f0.accept(this, null);
    }


    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    @Override
    public String visit(BooleanArrayType n, MyData argu) throws Exception {
        return "bool[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    @Override
    public String visit(IntegerArrayType n, MyData argu) throws Exception {
        return "int[]";

    }

    /**
    * f0 -> "boolean"
    */
    @Override
    public String visit(BooleanType n, MyData argu) throws Exception {
        return "bool";
    }

   /**
    * f0 -> "int"
    */
    @Override
    public String visit(IntegerType n, MyData argu) throws Exception {
        return "int";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    @Override
    public String visit(Identifier n, MyData argu) throws Exception {
        return n.f0.toString();
    }
}
