package com.ceams.model.dto;

/**
 * 批量盖章进度
 */
public class BatchStampProgress {

    private double progress;
    private String status;
    private String statusMessage;
    private int stampedPages;
    private String outputPath;
    private String errorMessage;

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getStampedPages() {
        return stampedPages;
    }

    public void setStampedPages(int stampedPages) {
        this.stampedPages = stampedPages;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
