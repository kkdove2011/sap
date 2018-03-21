package com.connor.sap.send;

import com.connor.sap.ui.KInfoDialog;
import com.teamcenter.rac.aif.AbstractAIFUIApplication;
import com.teamcenter.rac.aif.common.actions.AbstractAIFAction;

public class ToSapAction extends AbstractAIFAction {

	private AbstractAIFUIApplication app;
	private String commandId;
	private KInfoDialog infoDialog;

	public ToSapAction(AbstractAIFUIApplication app, String string, KInfoDialog infoDialog) {
		super(app,string);
		this.app=app;
		this.commandId=string;
		this.infoDialog=infoDialog;
	}

	@Override
	public void run() {
		try {
			new ToSapCommand(app,commandId,infoDialog).executeModal();
			if(infoDialog.isVisible())
				infoDialog.disposeDialog();
		} catch (Exception e) {
			if(infoDialog.isVisible())
				infoDialog.disposeDialog();
			e.printStackTrace();
		}
	}

}
