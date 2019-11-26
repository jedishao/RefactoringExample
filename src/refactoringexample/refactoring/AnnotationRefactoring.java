package refactoringexample.refactoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ListModel;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class AnnotationRefactoring extends Refactoring {
	ASTRewrite rewrite;
	private IJavaElement element;
	// List<Change> changeManager = new ArrayList<Change>();
	List<Change> changeManager = new ArrayList<Change>();
	// private TextChangeManager changeManager;
	private List<ICompilationUnit> compilationUnits;

	public AnnotationRefactoring(IJavaElement select) {
		element = select;
		// changeManager = new TextChangeManager();
		compilationUnits = findAllCompilationUnits(element.getJavaProject());
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		collectChanges();

		if (changeManager.size() == 0)
			return RefactoringStatus.createFatalErrorStatus("No  found!");
		else
			return RefactoringStatus.createInfoStatus("Final condition has been checked");

	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor arg0)
			throws CoreException, OperationCanceledException {
		return RefactoringStatus.createInfoStatus("Initial Condition is OK!");
	}

	@Override
	public Change createChange(IProgressMonitor arg0) throws CoreException, OperationCanceledException {
		Change[] changes = new Change[changeManager.size()];
		// TextChange[] changes = changeManager.getAllChanges();
		System.arraycopy(changeManager.toArray(), 0, changes, 0, changeManager.size());
		CompositeChange change = new CompositeChange(element.getJavaProject().getElementName(), changes);
		return change;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Add @Test annotation";
	}

	private void collectChanges() throws JavaModelException {
		for (IJavaElement element : compilationUnits) {
			// 创建一个document(jface)
			ICompilationUnit cu = (ICompilationUnit) element;
			String source = cu.getSource();
			Document document = new Document(source);
			// 创建AST
			ASTParser parser = ASTParser.newParser(AST.JLS11);
			parser.setSource(cu);
			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
			rewrite = ASTRewrite.create(astRoot.getAST());
			// 记录更改
			astRoot.recordModifications();

			List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
			getTypes(astRoot.getRoot(), types);
			
			for (TypeDeclaration ty : types) {
				collectChanges(astRoot, ty);
			}

			TextEdit edits = astRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			TextFileChange change = new TextFileChange("", (IFile) cu.getResource());
			change.setEdit(edits);
			changeManager.add(change);

		}
	}

	private void collectChanges(ICompilationUnit cu) throws JavaModelException {
		// create a document
		String source = "";
		try {
			source = cu.getSource();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		Document document = new Document(source);

		// creation of DOM/AST from a ICompilationUnit
		ASTParser parser = ASTParser.newParser(AST.JLS12);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		// creation of ASTRewrite
		final ASTRewrite rewrite = ASTRewrite.create(astRoot.getAST());
		astRoot.recordModifications();
		List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
		getTypes(astRoot.getRoot(), types);
		for (TypeDeclaration ty : types) {
			collectChanges(astRoot, ty);
		}

//		TextEdit edits = astRoot.rewrite(document, cu.getJavaProject().getOptions(true));
//		TextFileChange change = new TextFileChange("", (IFile) cu.getResource());
//		change.setEdit(edits);
//		changeManager.add(change);

		TextEdit edits = rewrite.rewriteAST(document, cu.getJavaProject().getOptions(true));
		TextFileChange change = new TextFileChange("", (IFile) cu.getResource());
		change.setEdit(edits);
		// changeManager.manage(cu, change);
	}

	private void getTypes(ASTNode cuu, final List types) {
		cuu.accept(new ASTVisitor() {
			@SuppressWarnings("unchecked")
			public boolean visit(TypeDeclaration node) {
				types.add(node);
				return false;
			}
		});
	}

	private boolean collectChanges(CompilationUnit root, TypeDeclaration types) {
		AST ast = types.getAST();
		// 获取类中所有字段
		FieldDeclaration[] fields = types.getFields();
		MethodDeclaration[] methods=types.getMethods();
		
		// import AtomicInteger包
		ImportDeclaration id1 = ast.newImportDeclaration();
		id1.setName(ast.newName(new String[] { "java", "util", "concurrent", "atomic", "AtomicInteger" }));
		root.imports().add(id1);

		
		for (FieldDeclaration f : fields) {
			if (f.getType().toString().equals("int")) {
				//定义类型为AtomicInteger
				Type ty = ast.newSimpleType(ast.newName("AtomicInteger"));
				f.setType(ty);
				//进行实例化　i=0 i=new AtomicInteger();
				ClassInstanceCreation creation = ast.newClassInstanceCreation();
				creation.setType(ast.newSimpleType(ast.newSimpleName("AtomicInteger")));
				VariableDeclarationFragment lock = ast.newVariableDeclarationFragment();
				lock.setInitializer(creation);
				lock.setName(ast.newSimpleName(((VariableDeclarationFragment)f.fragments().get(0)).getName().toString()));
				
				//这部分有待改进
				f.fragments().add(0, lock);
				f.fragments().remove(1);
				
			}
		}
		//修改方法里的AtomicInteger调用
		ExpressionStatement ex ;
		MethodInvocation addreadlock = ast.newMethodInvocation();
		for(MethodDeclaration m:methods) {
			for(int i=0;i<m.getBody().statements().size();i++) {			
				if(m.getBody().statements().get(i).toString().trim().equals("i++;")) {
					addreadlock.setExpression(ast.newSimpleName("i"));
					addreadlock.setName(ast.newSimpleName("getAndIncrement"));
					ex = ast.newExpressionStatement(addreadlock);
					
					m.getBody().statements().remove(i);
					m.getBody().statements().add(i,ex);
				}
			}
			
		}

		return true;
	}

	private List<ICompilationUnit> findAllCompilationUnits(IJavaProject project) {

		List<ICompilationUnit> cus = new ArrayList<ICompilationUnit>();

		try {
			for (IJavaElement element : project.getChildren()) { // IPackageFragmentRoot
				if (element.getElementName().equals("src")) {
					IPackageFragmentRoot root = (IPackageFragmentRoot) element;
					for (IJavaElement ele : root.getChildren()) {
						if (ele instanceof IPackageFragment) {
							IPackageFragment fragment = (IPackageFragment) ele;
							for (ICompilationUnit unit : fragment.getCompilationUnits()) {
								cus.add(unit);
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return cus;
	}

}
