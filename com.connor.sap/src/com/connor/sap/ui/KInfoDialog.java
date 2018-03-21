package com.connor.sap.ui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JTextField;

import com.teamcenter.rac.aif.AbstractAIFDialog;

public class KInfoDialog extends AbstractAIFDialog {
	private static final long serialVersionUID = 8373304225150075908L;
	private JTextField infoField = new JTextField();

	// public MyProgressDialog(Window owner, String content, Dimension dimension) {
	public KInfoDialog(String content, Dimension dimension) {
		super(false);
		infoField.setText(content);
		infoField.setForeground(Color.WHITE);
		infoField.setBackground(new Color(43, 43, 43));
		// infoField.setBackground(new Color(250, 250, 250));
		// infoField.setBackground(new Color(230,243,250));
		infoField.setBorder(BorderFactory.createEmptyBorder());
		if (dimension != null)
			infoField.setPreferredSize(dimension);
		infoField.setEditable(false);
		infoField.setHorizontalAlignment(JTextField.CENTER);
		this.add(infoField);
		this.setUndecorated(true);
		this.pack();
	}

	public void setText(String text) {
		this.infoField.setText(text);
	}

	/*
	 * public void showDialog() { centerToScreen(); setVisible(true); }
	 */

	/*
	 * public void centerToScreen() { UIUtilities.centerToScreen(this); }
	 */

	/*
	 * public void centerToDialog() { this.setLocationRelativeTo(super.getOwner());
	 * }
	 */
}
