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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.github.antag99.textract.extract.Steam;

class ExtractorGUI {
	private Extractor extractor;
	private JFrame frame;
	private JPanel contentPane;
	private JPanel panel;

	private ConfigurationPanel configurationPanel;
	private ExtractionPanel extractionPanel;
	private FinishmentPanel finishmentPanel;

	private JButton okButton;
	private JButton cancelButton;

	public ExtractorGUI() {
		configurationPanel = new ConfigurationPanel();
		extractionPanel = new ExtractionPanel();
		finishmentPanel = new FinishmentPanel();

		extractor = new Extractor();
		extractor.setStatusReporter(extractionPanel);

		frame = new JFrame();
		frame.setTitle("TExtract");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(100, 100, 450, 180);
		frame.setResizable(true);

		try {
			frame.setIconImage(ImageIO.read(getClass().getResource("/icon.png")));
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		frame.setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JPanel buttonPanel = new JPanel();
		contentPane.add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		buttonPanel.add(cancelButton);

		okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getPanel() == configurationPanel) {
					okButton.setText("Finish");
					okButton.setEnabled(false);

					extractor.setInputDirectory(configurationPanel.getInputDirectory());
					extractor.setOutputDirectory(configurationPanel.getOutputDirectory());
					finishmentPanel.setOutputDirectory(configurationPanel.getOutputDirectory());

					setPanel(extractionPanel);

					new Thread() {
						@Override
						public void run() {
							extractor.extract();
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									okButton.setEnabled(true);
									cancelButton.setVisible(false);
									setPanel(finishmentPanel);
								}
							});
						}
					}.start();
				} else if (getPanel() == finishmentPanel) {
					System.exit(0);
				}
			}
		});
		buttonPanel.add(okButton);
	}

	private void setPanel(JPanel panel) {
		if (this.panel != null)
			contentPane.remove(this.panel);
		this.panel = panel;
		contentPane.add(panel, BorderLayout.CENTER);
	}

	public JPanel getPanel() {
		return panel;
	}

	public void start() {
		setPanel(configurationPanel);

		// Try to find the Terraria installation directory
		File inputDirectory = Steam.findTerrariaDirectory();
		if (inputDirectory != null && new File(inputDirectory, "Content").isDirectory()) {
			configurationPanel.setInputDirectory(new File(inputDirectory, "Content"));
		}

		// Find possible output directory
		File outputDirectory = new File("TerrariaAssets");
		int counter = 2;
		while (outputDirectory.exists())
			outputDirectory = new File("TerrariaAssets_" + counter++);

		configurationPanel.setOutputDirectory(outputDirectory);

		frame.setVisible(true);
	}
}
