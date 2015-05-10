/*******************************************************************************
 * Copyright (C) 2014-2015 Anton Gustafsson
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.github.antag99.textract;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import java.awt.Font;

@SuppressWarnings("serial")
class FinishmentPanel extends JPanel {
	private File outputDirectory;
	private JLabel fileLabel;

	public FinishmentPanel() {
		SpringLayout springLayout = new SpringLayout();
		setLayout(springLayout);

		JLabel doneLabel = new JLabel("Done! Extracted files are located in");
		springLayout.putConstraint(SpringLayout.NORTH, doneLabel, 10, SpringLayout.NORTH, this);
		springLayout.putConstraint(SpringLayout.WEST, doneLabel, 10, SpringLayout.WEST, this);
		add(doneLabel);

		JLabel logo = new JLabel(new ImageIcon(getClass().getResource("/icon.png")));
		springLayout.putConstraint(SpringLayout.NORTH, logo, -42, SpringLayout.SOUTH, this);
		springLayout.putConstraint(SpringLayout.WEST, logo, -42, SpringLayout.EAST, this);
		springLayout.putConstraint(SpringLayout.SOUTH, logo, -10, SpringLayout.SOUTH, this);
		springLayout.putConstraint(SpringLayout.EAST, logo, -10, SpringLayout.EAST, this);
		logo.setMinimumSize(new Dimension(32, 32));
		logo.setMaximumSize(new Dimension(32, 32));
		add(logo);

		fileLabel = new JLabel("<file>");
		fileLabel.setFont(new Font("Dialog", Font.BOLD | Font.ITALIC, 12));
		fileLabel.setForeground(new Color(0, 153, 255));
		fileLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		fileLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().open(outputDirectory);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});

		springLayout.putConstraint(SpringLayout.NORTH, fileLabel, 6, SpringLayout.SOUTH, doneLabel);
		springLayout.putConstraint(SpringLayout.WEST, fileLabel, 10, SpringLayout.WEST, doneLabel);
		add(fileLabel);
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
		fileLabel.setText(outputDirectory.getPath());
	}

	public File getOutputDirectory() {
		return outputDirectory;
	}
}
