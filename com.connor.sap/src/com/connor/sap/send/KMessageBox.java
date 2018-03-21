package com.connor.sap.send;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.teamcenter.rac.aif.AbstractAIFDialog;

public class KMessageBox extends AbstractAIFDialog {

	private static final long serialVersionUID = -5328094563368370151L;
	private static KMessageBox kBox = new KMessageBox();
	private static JTextArea textArea = new JTextArea();
	static {
		initUI();
	}

	private KMessageBox() {
		super(false);
	}

	private static void initUI() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		JScrollPane scrollPane = new JScrollPane(textArea);
		textArea.setEditable(false);
		textArea.setBackground(new Color(240, 240, 240));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		JButton button = new JButton("È·¶¨");
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.BOTH;
		s.gridwidth = 2;
		s.gridheight = 1;
		s.weightx = 1;
		s.weighty = 1;
		s.gridx = 0;
		s.gridy = 0;
		s.insets = new Insets(20, 30, 0, 0);
		mainPanel.add(scrollPane, s);
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridy = 1;
		mainPanel.add(new JLabel(""), s);
		s.weightx = 0;
		s.gridx = 1;
		s.insets = new Insets(10, 0, 16, 11);
		button.setMinimumSize(new Dimension(86, 27));
		mainPanel.add(button, s);
		KMessageBox.kBox.setContentPane(mainPanel);
		KMessageBox.kBox.setMinimumSize(new Dimension(500, 150));
		KMessageBox.kBox.setPreferredSize(new Dimension(520, 300));
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				KMessageBox.kBox.disposeDialog();
			}
		});
	}

	public static void post(String contents, String title) {
		KMessageBox.kBox.setTitle(title);
		KMessageBox.textArea.setText(contents);
		KMessageBox.kBox.showDialog();
	}

}
