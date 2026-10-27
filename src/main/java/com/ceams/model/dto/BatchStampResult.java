package com.ceams.model.dto;

/**
 * 批量盖章结果
 */
public class BatchStampResult {

    private String taskId;
    private boolean success;
    private int stampedPages;
    private String outputPath;
    private String errorMessage;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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
