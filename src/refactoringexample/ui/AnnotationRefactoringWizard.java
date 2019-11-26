package refactoringexample.ui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;



public class AnnotationRefactoringWizard extends RefactoringWizard{

	UserInputWizardPage page;
//	AnnotationRefactoringWizardPage page;

	public AnnotationRefactoringWizard(Refactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);

	}

	@Override
	protected void addUserInputPages() {
//		page = new AnnotationRefactoringWizardPage("refactor annotation");
//		addPage(page);
	}
	
}
