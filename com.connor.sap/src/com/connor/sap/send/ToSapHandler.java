package com.connor.sap.send;

import java.awt.Dimension;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.teamcenter.rac.aif.AbstractAIFUIApplication;
import com.teamcenter.rac.aifrcp.AIFUtility;

public class ToSapHandler extends AbstractHandler{
	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		AbstractAIFUIApplication app = AIFUtility.getCurrentApplication();
		KInfoDialog infoDialog=new KInfoDialog("loading...", new Dimension(190, 50));
		infoDialog.showDialog();
		new Thread(new ToSapAction(app,arg0.getCommand().getId(),infoDialog)).start();
		return null;
	}
}
