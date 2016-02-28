package codeparser.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import codeparser.core.ASTTool;
import codeparser.core.object.Variable;

public class ParserDBHandler implements DBHandler
{
	private Connection connection;
	private int fileId=-1;

	public ParserDBHandler(String server,String user,String password,String dbName)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		String url="jdbc:mysql://"+server;
		this.connection=DriverManager.getConnection(url, user, password);
		this.createDatabase(dbName);
	}

	public boolean createDatabase(String dbName) throws SQLException
	{
		if(!dbName.matches("[a-zA-Z][a-zA-Z_]*")){
			return false;
		}
		Statement stmt = this.connection.createStatement();
		stmt.execute("create database "+dbName);
		stmt.execute("use "+dbName);
		stmt.execute(CREATE_TABLE_FILE);
		stmt.execute(CREATE_TABLE_TYPE);
		stmt.execute(CREATE_TABLE_TDEXMODIFIER);
		stmt.execute(CREATE_TABLE_IMPLEMENTS);
		stmt.execute(CREATE_TABLE_FIELD);
		stmt.execute(CREATE_TABLE_FDEXMODIFIER);
		stmt.execute(CREATE_TABLE_METHOD);
		stmt.execute(CREATE_TABLE_MDEXMODIFIER);
		stmt.execute(CREATE_TABLE_THROWS);
		stmt.execute(CREATE_TABLE_ARGUMENT);
		stmt.execute(CREATE_TABLE_VARIABLE);
		stmt.close();
		return true;
	}

	@Override
	public int register(String filePath) throws SQLException
	{
		PreparedStatement pstmt=this.connection.prepareStatement("insert into file (path) values (?)", Statement.RETURN_GENERATED_KEYS);
		pstmt.setString(1,filePath);
		pstmt.execute();
		ResultSet rs=pstmt.getGeneratedKeys();
		int autoGeneratedKey=-1;
		if(rs.next()){
			autoGeneratedKey=rs.getInt(1);
		}
		rs.close();
		pstmt.close();
		this.fileId=autoGeneratedKey;
		return autoGeneratedKey;
	}

	@Override
	public void register(TypeDeclaration node) throws SQLException
	{
		PreparedStatement pstmt=
				this.connection.prepareStatement("insert into type (fileId,isInterface,FQCN,startLine,endLine,super) values (?,?,?,?,?,?)",
				Statement.RETURN_GENERATED_KEYS);
		pstmt.setInt(1,this.fileId);
		pstmt.setBoolean(2,node.isInterface());
		pstmt.setString(3,ASTTool.getFullyQualifiedClassName(node));
		pstmt.setInt(4,ASTTool.getStartLineNumber(node));
		pstmt.setInt(5,ASTTool.getEndLineNumber(node));
		pstmt.setString(6,ASTTool.getSuperclassType(node));
		pstmt.execute();
		ResultSet rs=pstmt.getGeneratedKeys();
		int autoGeneratedTypeId=-1;
		if(rs.next()){
			autoGeneratedTypeId=rs.getInt(1);
		}
		pstmt=this.connection.prepareStatement("insert into tdExModifier (typeId,keyword) values (?,?)");
		for(Iterator<String> iter=ASTTool.getModifiers(node).iterator();iter.hasNext();){
			pstmt.setInt(1,autoGeneratedTypeId);
			pstmt.setString(2,iter.next());
			pstmt.execute();
		}
		pstmt=this.connection.prepareStatement("insert into implements (typeId,name) values (?,?)");
		for(Iterator<Type> iter=node.superInterfaceTypes().iterator();iter.hasNext();){
			pstmt.setInt(1,autoGeneratedTypeId);
			pstmt.setString(2,iter.next().toString());
		}
		for(Iterator<Variable> iter=ASTTool.getFields(node).iterator();iter.hasNext();){
			Variable v=iter.next();
			pstmt=this.connection.prepareStatement("insert into field (typeId,type,name) values (?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			pstmt.setInt(1,autoGeneratedTypeId);
			pstmt.setString(2,v.getType());
			pstmt.setString(3,v.getName());
			pstmt.execute();
			rs=pstmt.getGeneratedKeys();
			int autoGeneratedFieldId=-1;
			if(rs.next()){
				autoGeneratedFieldId=rs.getInt(1);
			}
			pstmt=this.connection.prepareStatement("insert into fdExModifier (fieldId,keyword) values (?,?)");
			for(Iterator<String> iter2=v.getModifiers().iterator();iter2.hasNext();){
				pstmt.setInt(1,autoGeneratedFieldId);
				pstmt.setString(2,iter2.next());
				pstmt.execute();
			}
		}
		for(Iterator<MethodDeclaration> iter=Arrays.asList(node.getMethods()).iterator();iter.hasNext();){
			register(iter.next(),autoGeneratedTypeId);
		}
		rs.close();
		pstmt.close();
	}

	public void register(MethodDeclaration node,int typeId) throws SQLException
	{
		PreparedStatement pstmt=
				this.connection.prepareStatement("insert into method (typeId,isConstructor,name,returnType,startLine,endLine) values (?,?,?,?,?,?)",
				Statement.RETURN_GENERATED_KEYS);
		pstmt.setInt(1,typeId);
		pstmt.setBoolean(2,node.isConstructor());
		pstmt.setString(3,node.getName().toString());
		pstmt.setString(4,ASTTool.getReturnType(node));
		pstmt.setInt(5,ASTTool.getStartLineNumber(node));
		pstmt.setInt(6,ASTTool.getEndLineNumber(node));
		pstmt.execute();
		ResultSet rs=pstmt.getGeneratedKeys();
		int autoGeneratedMethodId=-1;
		if(rs.next()){
			autoGeneratedMethodId=rs.getInt(1);
		}
		pstmt=this.connection.prepareStatement("insert into mdExModifier (methodId,keyword) values (?,?)");
		for(Iterator<String> iter=ASTTool.getModifiers(node).iterator();iter.hasNext();){
			pstmt.setInt(1,autoGeneratedMethodId);
			pstmt.setString(2,iter.next());
			pstmt.execute();
		}
		pstmt=this.connection.prepareStatement("insert into argument (methodId,type,name) values (?,?,?)");
		for(Iterator<SingleVariableDeclaration> iter=node.parameters().iterator();iter.hasNext();){
			SingleVariableDeclaration argument=iter.next();
			pstmt.setInt(1,autoGeneratedMethodId);
			pstmt.setString(2,argument.getType().toString());
			pstmt.setString(3,argument.getName().toString());
			pstmt.execute();
		}
		pstmt=this.connection.prepareStatement("insert into throws (methodId,type) values (?,?)");
		for(Iterator<Type> iter=node.thrownExceptionTypes().iterator();iter.hasNext();){
			pstmt.setInt(1,autoGeneratedMethodId);
			pstmt.setString(2,iter.next().toString());
			pstmt.execute();
		}
		pstmt=this.connection.prepareStatement("insert into variable (methodId,type,name) values (?,?,?)");
		for(Iterator<Variable> iter=ASTTool.getLocalVariables(node).iterator();iter.hasNext();){
			Variable v=iter.next();
			pstmt.setInt(1,autoGeneratedMethodId);
			pstmt.setString(2,v.getType());
			pstmt.setString(3,v.getName());
			pstmt.execute();
		}
		rs.close();
		pstmt.close();
	}

	@Override
	protected void finalize() throws Throwable
	{
		try {
			super.finalize();
		} finally {
			if(this.connection!=null){
				this.connection.close();
			}
		}
	}

	private static final String CREATE_TABLE_FILE=
			"create table file(id integer primary key auto_increment,path text not null)";
	private static final String CREATE_TABLE_TYPE=
			"create table type(id integer primary key auto_increment,fileId integer not null,"+
					"isInterface boolean not null,FQCN text not null,startLine integer not null,"+
					"endLine integer not null,super text not null)";
	private static final String CREATE_TABLE_TDEXMODIFIER=
			"create table tdExModifier(typeId integer not null,keyword varchar(16) not null)";
	private static final String CREATE_TABLE_IMPLEMENTS=
			"create table implements(typeId integer not null,name text not null)";
	private static final String CREATE_TABLE_FIELD=
			"create table field(id integer primary key auto_increment,typeId integer not null,"+
					"type text not null,name text not null)";
	private static final String CREATE_TABLE_FDEXMODIFIER=
			"create table fdExModifier(fieldId integer not null,keyword varchar(16) not null)";
	private static final String CREATE_TABLE_METHOD=
			"create table method(id integer primary key auto_increment,typeId integer not null,"+
					"isConstructor boolean not null,name text not null,returnType text not null,"+
					"startLine integer not null,endLine integer not null)";
	private static final String CREATE_TABLE_MDEXMODIFIER=
			"create table mdExModifier(methodId integer not null,keyword varchar(16) not null)";
	private static final String CREATE_TABLE_THROWS=
			"create table throws(methodId integer not null,type text not null)";
	private static final String CREATE_TABLE_ARGUMENT=
			"create table argument(methodId integer not null,type text not null,name text not null)";
	private static final String CREATE_TABLE_VARIABLE=
			"create table variable(methodId integer not null,type text not null,name text not null)";

}
