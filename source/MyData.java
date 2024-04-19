import java.io.*;
import java.util.Map;
import java.util.LinkedHashMap; 

public class MyData {
    String parent;      //Name of parent class (if it exists)
    Map<String,Variable> variables;     // A dictionary with all class member variables
    Map<String,Method> methods;     // A dictionary with all class member methods
    String currentMethod;       //The current method we are checking
    int offset, methodOffset;     //current class offset


    public MyData(String p){
        parent = p;
        variables = new LinkedHashMap<String,Variable>();
        methods = new LinkedHashMap<String,Method>();
        currentMethod = null;
        offset = 0;
        methodOffset = 0;
    }

    public void addVariable(String name, String type){
        // System.out.println("Adding variable "+name);
        Variable var = new Variable(type, offset);
        getTypeOffset(type);
        this.variables.put(name,var);
    }

    public void addMethod(String name, String type){
        // System.out.println("Adding method "+name);
        currentMethod = name;
        Method meth = new Method(type, methodOffset);
        methodOffset = methodOffset + 8;
        this.methods.put(name,meth);
    }

    public Boolean hasMethod(String a){
        return this.methods.containsKey(a);
    }

    public Method getCurrentMethod(){
        if (currentMethod == null){
            return null;
        }
        else{
            return methods.get(currentMethod);
        }
    }

    public void setCurrentMethodIsOverride(){
        if (currentMethod == null){
        }
        else{
            methods.get(currentMethod).isOverride();
        }
    }

    public Boolean checkCurrentMethodForArgument(String a){
        if (currentMethod == null){
            return false;
        }
        else{
            return this.methods.get(currentMethod).arguments.containsKey(a);
        }
    }

    public Boolean checkCurrentMethodForVariable(String a){
        if (currentMethod == null){
            return false;
        }
        else{
            return this.methods.get(currentMethod).variables.containsKey(a);
        }
    }

    public void addMethodVariable(String name, String type){
        this.methods.get(currentMethod).variables.put(name,type);
    }

    public void addMethodArgument(String name, String type){
        this.methods.get(currentMethod).arguments.put(name,type);
    }

    public void printOffsets(String name, MyData parentdata){
        int parentOffset = 0;
        if (parentdata != null){
            parentOffset = parentdata.offset;
        }
        for (Map.Entry<String, Variable> entry : variables.entrySet()) {
            System.out.println(name+'.'+entry.getKey()+": "+(entry.getValue().offset+parentOffset));
        }
        int parentMethodOffset = 0;
        if (parentdata != null){
            parentMethodOffset = parentdata.methodOffset;
        }
        for (Map.Entry<String, Method> entry : methods.entrySet()) {
            if (entry.getValue().isOverride == false){
                System.out.println(name+'.'+entry.getKey()+": "+(entry.getValue().offset+parentMethodOffset));
            }
            else{
                parentMethodOffset -= 8;
            }
        }
        offset += parentOffset;
        methodOffset += parentMethodOffset;
    }

    public int getTypeOffset(String type){
        if (type=="bool"){
            this.offset += 1;
        }else if (type=="int"){
            this.offset += 4;
        }else{
            this.offset += 8;
        }
        return this.offset;
    }
}

class Variable{
    String type;    //The variable type
    int offset;     //The variable offset (if I ever get there...)


    public Variable(String t,int o){
        type = t;
        offset = o;
    }
}

class Method{
    String returnType;  //The method return type
    int offset;     //The method offset
    boolean isOverride;    //If the method overrides a parent method
    Map<String,String> arguments, variables;   //A dictionary with the method arguments (if any exist) name->type


    public Method(String rt,int o){
        returnType = rt;
        offset = o;
        arguments = new LinkedHashMap<String,String>();
        variables = new LinkedHashMap<String,String>();
        isOverride = false;
    }

    public Boolean checkForArgument(String a){
        return this.arguments.containsKey(a);
    }

    public void isOverride(){
        this.isOverride = true;
    }

    public void addArgument(String a, String type){
        this.arguments.put(a,type);
    }

    public Boolean checkForVariable(String a){
        return this.variables.containsKey(a);
    }

    public void addVariable(String a, String type){
        this.variables.put(a,type);
    }

    public String getArgumentString(){
        String arg = "";
        for (Map.Entry<String, String> argument : arguments.entrySet()) {
            if (arg.equals("")){
                arg = argument.getValue();
            }
            else{
                arg += ","+argument.getValue();
            }
        }
        return arg;
    }
}