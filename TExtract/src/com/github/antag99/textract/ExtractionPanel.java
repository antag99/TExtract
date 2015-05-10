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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;

@SuppressWarnings("serial")
class ExtractionPanel extends JPanel implements StatusReporter {
	private JProgressBar currentProgressBar;
	private JProgressBar overallProgressBar;
	private JLabel statusLabel;

	private JLabel overallStatusLabel;

	public ExtractionPanel() {
		SpringLayout layout = new SpringLayout();
		setLayout(layout);

		overallStatusLabel = new JLabel("Extracting Content");
		layout.putConstraint(SpringLayout.NORTH, overallStatusLabel, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.WEST, overallStatusLabel, 0, SpringLayout.WEST, this);
		add(overallStatusLabel);

		overallProgressBar = new JProgressBar();
		layout.putConstraint(SpringLayout.NORTH, overallProgressBar, 6, SpringLayout.SOUTH, overallStatusLabel);
		layout.putConstraint(SpringLayout.WEST, overallProgressBar, 0, SpringLayout.WEST, overallStatusLabel);
		layout.putConstraint(SpringLayout.SOUTH, overallProgressBar, 27, SpringLayout.SOUTH, overallStatusLabel);
		layout.putConstraint(SpringLayout.EAST, overallProgressBar, 0, SpringLayout.EAST, this);
		add(overallProgressBar);

		statusLabel = new JLabel("Preparing...");
		layout.putConstraint(SpringLayout.NORTH, statusLabel, 6, SpringLayout.SOUTH, overallProgressBar);
		layout.putConstraint(SpringLayout.WEST, statusLabel, 0, SpringLayout.WEST, overallStatusLabel);
		add(statusLabel);

		currentProgressBar = new JProgressBar();
		layout.putConstraint(SpringLayout.NORTH, currentProgressBar, 6, SpringLayout.SOUTH, statusLabel);
		layout.putConstraint(SpringLayout.WEST, currentProgressBar, 0, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.SOUTH, currentProgressBar, 27, SpringLayout.SOUTH, statusLabel);
		layout.putConstraint(SpringLayout.EAST, currentProgressBar, 0, SpringLayout.EAST, overallProgressBar);
		add(currentProgressBar);
	}

	@Override
	public void reportTaskStatus(String status) {
		System.out.println(status);
		statusLabel.setText(status);
	}

	@Override
	public void reportTaskPercentage(float percentage) {
		currentProgressBar.setValue((int) (percentage * currentProgressBar.getMaximum()));
	}

	@Override
	public void reportOverallStatus(String status) {
		System.out.println("========  " + status + "  ========");
		overallStatusLabel.setText(status);
	}

	@Override
	public void reportOverallPercentage(float percentage) {
		overallProgressBar.setValue((int) (percentage * overallProgressBar.getMaximum()));
	}
}
