package com.connor.sap.send;

import com.connor.sap.ui.KInfoDialog;
import com.teamcenter.rac.aif.AbstractAIFCommand;
import com.teamcenter.rac.aif.AbstractAIFUIApplication;
import com.teamcenter.rac.util.MessageBox;

public class ToSapCommand extends AbstractAIFCommand {

	private AbstractAIFUIApplication app;
	private String commandId;
	private KInfoDialog infoDialog;

	public ToSapCommand(AbstractAIFUIApplication app, String commandId, KInfoDialog infoDialog) {
		this.app=app;
		this.commandId=commandId;
		this.infoDialog=infoDialog;
	}

	@Override
	public void executeModal(){
		try {
			new ToSapOperation(app,commandId,infoDialog).executeOperation();
		} catch (Exception e) {
			infoDialog.disposeDialog();
			MessageBox.post(e.getMessage(),"´íÎó",MessageBox.ERROR);
			e.printStackTrace();
		}
	}
	
	

}
