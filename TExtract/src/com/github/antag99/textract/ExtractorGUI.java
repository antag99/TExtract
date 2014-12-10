package com.github.antag99.textract;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.github.antag99.textract.extract.Steam;

public class ExtractorGUI extends Extractor implements Runnable, StatusReporter {
	private JFrame frame;

	private JPanel contentPane;
	private JProgressBar currentProgressBar;
	private JProgressBar overallProgressBar;
	private JLabel statusLabel;

	private JLabel overallStatusLabel;
	private JButton btnFinish;
	private JButton btnCancel;

	public ExtractorGUI() {
		super();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ex) {
		}

		setStatusReporter(this);
		frame = new JFrame();
		frame.setResizable(false);
		frame.setTitle("TExtract");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(100, 100, 450, 180);

		try {
			frame.setIconImage(ImageIO.read(ExtractorGUI.class.getResourceAsStream("/icon.png")));
		} catch (IOException ignored) {
		}

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		frame.setContentPane(contentPane);

		SpringLayout layout = new SpringLayout();
		contentPane.setLayout(layout);

		overallStatusLabel = new JLabel("Extracting Content...");
		overallStatusLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 11));
		layout.putConstraint(SpringLayout.NORTH, overallStatusLabel, 10, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.WEST, overallStatusLabel, 0, SpringLayout.WEST, contentPane);
		contentPane.add(overallStatusLabel);

		overallProgressBar = new JProgressBar();
		layout.putConstraint(SpringLayout.NORTH, overallProgressBar, 6, SpringLayout.SOUTH, overallStatusLabel);
		layout.putConstraint(SpringLayout.WEST, overallProgressBar, 0, SpringLayout.WEST, overallStatusLabel);
		layout.putConstraint(SpringLayout.SOUTH, overallProgressBar, 27, SpringLayout.SOUTH, overallStatusLabel);
		layout.putConstraint(SpringLayout.EAST, overallProgressBar, -15, SpringLayout.EAST, contentPane);
		contentPane.add(overallProgressBar);

		statusLabel = new JLabel("...");
		statusLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 11));
		layout.putConstraint(SpringLayout.NORTH, statusLabel, 6, SpringLayout.SOUTH, overallProgressBar);
		layout.putConstraint(SpringLayout.WEST, statusLabel, 0, SpringLayout.WEST, overallStatusLabel);
		contentPane.add(statusLabel);

		currentProgressBar = new JProgressBar();
		layout.putConstraint(SpringLayout.NORTH, currentProgressBar, 6, SpringLayout.SOUTH, statusLabel);
		layout.putConstraint(SpringLayout.WEST, currentProgressBar, 0, SpringLayout.WEST, contentPane);
		layout.putConstraint(SpringLayout.SOUTH, currentProgressBar, 27, SpringLayout.SOUTH, statusLabel);
		layout.putConstraint(SpringLayout.EAST, currentProgressBar, 0, SpringLayout.EAST, overallProgressBar);
		contentPane.add(currentProgressBar);

		btnFinish = new JButton("Finish");
		btnFinish.setEnabled(false);
		btnFinish.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		layout.putConstraint(SpringLayout.SOUTH, btnFinish, 0, SpringLayout.SOUTH, contentPane);
		layout.putConstraint(SpringLayout.EAST, btnFinish, 0, SpringLayout.EAST, contentPane);
		contentPane.add(btnFinish);

		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		layout.putConstraint(SpringLayout.NORTH, btnCancel, 0, SpringLayout.NORTH, btnFinish);
		layout.putConstraint(SpringLayout.EAST, btnCancel, -6, SpringLayout.WEST, btnFinish);
		contentPane.add(btnCancel);
	}

	public void start() {
		new Thread(this).start();
	}

	@Override
	public void run() {
		File terrariaDirectory = Steam.findTerrariaDirectory();
		if (terrariaDirectory == null) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select Terraria Installation Directory");
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				terrariaDirectory = chooser.getSelectedFile();
			} else {
				return;
			}

			if (!new File(terrariaDirectory, "Content").isDirectory()) {
				JOptionPane.showMessageDialog(frame, "Invalid terraria installation directory.\n"
						+ "Couldn't find 'Content' folder", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		frame.setVisible(true);

		File outputDirectory = new File("TerrariaAssets");
		int counter = 2;
		while (outputDirectory.exists())
			outputDirectory = new File("TerrariaAssets_" + counter++);

		setContentDirectory(new File(terrariaDirectory, "Content"));
		setOutputDirectory(outputDirectory);

		extract();

		overallStatusLabel.setText("Done! Extracted files are located in " + outputDirectory.getPath() + ".");
		statusLabel.setVisible(false);
		currentProgressBar.setVisible(false);

		btnFinish.setEnabled(true);
		btnCancel.setEnabled(false);
	}

	@Override
	public void reportTaskStatus(String status) {
		statusLabel.setText(status);
	}

	@Override
	public void reportTaskPercentage(float percentage) {
		currentProgressBar.setValue((int) (percentage * currentProgressBar.getMaximum()));
	}

	@Override
	public void reportTask(String task) {
		overallStatusLabel.setText(task);
	}

	@Override
	public void reportPercentage(float percentage) {
		overallProgressBar.setValue((int) (percentage * overallProgressBar.getMaximum()));
	}
}
