package PSG.backEnd.model.enums;

/**
 * Enum representing the available formats for report export.
 */
public enum ReportFormat {
    /**
     * PDF format - Portable Document Format
     */
    PDF("application/pdf", ".pdf"),

    /**
     * Excel format - Microsoft Excel Open XML Spreadsheet
     */
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String contentType;
    private final String fileExtension;

    ReportFormat(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}

