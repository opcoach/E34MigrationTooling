 
package com.opcoach.e34tools.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

public class OpenView {
	@Execute
	public void execute( @Named("com.opcoach.e34tools.parameter.view.id") String viewID, EPartService ps) {
	
		MPart p = ps.showPart(viewID, EPartService.PartState.ACTIVATE);
		System.out.println("Part ouvert : " + p);
	}
		
}