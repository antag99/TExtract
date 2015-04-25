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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

@SuppressWarnings("serial")
public class ConfigurationPanel extends JPanel {
	private JTextField inputDirectory;
	private JTextField outputDirectory;

	public File getInputDirectory() {
		return new File(inputDirectory.getText());
	}

	public void setInputDirectory(File inputDirectory) {
		this.inputDirectory.setText(inputDirectory.getAbsolutePath());
	}

	public File getOutputDirectory() {
		return new File(outputDirectory.getText());
	}

	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory.setText(outputDirectory.getAbsolutePath());
	}

	public ConfigurationPanel() {
		SpringLayout springLayout = new SpringLayout();
		setLayout(springLayout);

		JLabel inputDirectoryLabel = new JLabel("Input directory (XNB files):");
		springLayout.putConstraint(SpringLayout.NORTH, inputDirectoryLabel, 10, SpringLayout.NORTH, this);
		springLayout.putConstraint(SpringLayout.WEST, inputDirectoryLabel, 10, SpringLayout.WEST, this);
		add(inputDirectoryLabel);

		inputDirectory = new JTextField();
		springLayout.putConstraint(SpringLayout.NORTH, inputDirectory, 6, SpringLayout.SOUTH, inputDirectoryLabel);
		springLayout.putConstraint(SpringLayout.WEST, inputDirectory, 10, SpringLayout.WEST, this);
		springLayout.putConstraint(SpringLayout.EAST, inputDirectory, -129, SpringLayout.EAST, this);
		add(inputDirectory);
		inputDirectory.setColumns(10);

		JLabel outputDirectoryLabel = new JLabel("Output directory (Resulting files):");
		springLayout.putConstraint(SpringLayout.NORTH, outputDirectoryLabel, 6, SpringLayout.SOUTH, inputDirectory);
		springLayout.putConstraint(SpringLayout.WEST, outputDirectoryLabel, 0, SpringLayout.WEST, inputDirectoryLabel);
		add(outputDirectoryLabel);

		outputDirectory = new JTextField();
		springLayout.putConstraint(SpringLayout.NORTH, outputDirectory, 6, SpringLayout.SOUTH, outputDirectoryLabel);
		springLayout.putConstraint(SpringLayout.WEST, outputDirectory, 0, SpringLayout.WEST, inputDirectoryLabel);
		springLayout.putConstraint(SpringLayout.SOUTH, outputDirectory, 25, SpringLayout.SOUTH, outputDirectoryLabel);
		springLayout.putConstraint(SpringLayout.EAST, outputDirectory, 0, SpringLayout.EAST, inputDirectory);
		add(outputDirectory);
		outputDirectory.setColumns(10);

		JButton inputDirectoryButton = new JButton("Browse...");
		inputDirectoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("TExtract - Select an input directory");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setCurrentDirectory(getInputDirectory());
				chooser.setFileHidingEnabled(false);
				if (chooser.showDialog(getParent(), null) == JFileChooser.APPROVE_OPTION) {
					setInputDirectory(chooser.getSelectedFile());
				}
			}
		});
		springLayout.putConstraint(SpringLayout.NORTH, inputDirectoryButton, 0, SpringLayout.NORTH, inputDirectory);
		springLayout.putConstraint(SpringLayout.WEST, inputDirectoryButton, 17, SpringLayout.EAST, inputDirectory);
		springLayout.putConstraint(SpringLayout.SOUTH, inputDirectoryButton, 0, SpringLayout.SOUTH, inputDirectory);
		add(inputDirectoryButton);

		JButton outputDirectoryButton = new JButton("Browse...");
		outputDirectoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setDialogTitle("TExtract - Select an output directory");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setCurrentDirectory(getOutputDirectory());
				chooser.setFileHidingEnabled(false);
				if (chooser.showDialog(getParent(), null) == JFileChooser.APPROVE_OPTION) {
					setOutputDirectory(chooser.getSelectedFile());
				}
			}
		});
		springLayout.putConstraint(SpringLayout.EAST, inputDirectoryButton, 0, SpringLayout.EAST, outputDirectoryButton);
		springLayout.putConstraint(SpringLayout.NORTH, outputDirectoryButton, 0, SpringLayout.NORTH, outputDirectory);
		springLayout.putConstraint(SpringLayout.SOUTH, outputDirectoryButton, 0, SpringLayout.SOUTH, outputDirectory);
		springLayout.putConstraint(SpringLayout.EAST, outputDirectoryButton, -10, SpringLayout.EAST, this);
		add(outputDirectoryButton);
	}
}
