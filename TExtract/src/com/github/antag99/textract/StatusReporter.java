package com.github.antag99.textract;

public interface StatusReporter {
	public static StatusReporter mutedReporter = new StatusReporter() {
		public void reportTask(String task) {
		}

		public void reportTaskStatus(String status) {
		}

		public void reportTaskPercentage(float percentage) {
		}

		public void reportPercentage(float percentage) {
		}
	};

	void reportTaskStatus(String status);

	void reportTaskPercentage(float percentage);

	void reportTask(String task);

	void reportPercentage(float percentage);
}
