package de.anpross.eeloghelper.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.LineComment;

import de.anpross.eeloghelper.dtos.LineCommentDto;

/**
 * records all methods and class variables.
 *
 * @author andreas
 */
public class LineCommentVisitor extends ASTVisitor {

	private String currMethodComment = new String();
	private int currCommentLineNumber;

	CompilationUnit compilationUnit;
	String[] source;

	List<FieldDeclaration> classFields = new ArrayList<FieldDeclaration>();
	List<LineCommentDto> comments = new ArrayList<LineCommentDto>();

	public LineCommentVisitor(CompilationUnit compilationUnit, String[] source) {
		this.compilationUnit = compilationUnit;
		this.source = source;
	}

	@Override
	public boolean visit(LineComment node) {
		currCommentLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
		String lineComment = source[currCommentLineNumber].trim();
		if (lineComment.length() > 2) {
			this.currMethodComment = lineComment.substring(2);
		}

		appendNewLineComment();

		return super.visit(node);
	}

	private void appendNewLineComment() {
		LineCommentDto newComment = new LineCommentDto();
		String currMethodComment = this.currMethodComment.replaceAll("EELOG", "").trim();
		newComment.setComment(currMethodComment);

		// not sure why but comment lines seam to be off by one.
		newComment.setLineNumber(currCommentLineNumber + 1);
		comments.add(newComment);
	}

	public List<LineCommentDto> getComments() {
		return comments;
	}
}
