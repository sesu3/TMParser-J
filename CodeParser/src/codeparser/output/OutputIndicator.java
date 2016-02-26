package codeparser.output;

import java.io.File;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class OutputIndicator implements ParseWriter
{
	private ParseWriter standard;
	private ParseWriter file;
	
	public OutputIndicator()
	{
		this.standard=new StandardParseWriter();
		this.file=new NullParseWriter();
	}
	public OutputIndicator(File file,boolean useStandard)
	{
		this.standard=useStandard?new StandardParseWriter():new NullParseWriter();
		this.file=new FileParseWriter();
	}

	@Override
	public void printDeclarationState(TypeDeclaration node)
	{
		this.standard.printDeclarationState(node);
		this.file.printDeclarationState(node);
	}

	@Override
	public void printDeclarationState(MethodDeclaration node)
	{
		this.standard.printDeclarationState(node);
		this.file.printDeclarationState(node);
	}
	
	
}