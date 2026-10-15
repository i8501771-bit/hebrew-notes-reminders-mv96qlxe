package com.generated.app;

public class Note {
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_CHECKLIST = "CHECKLIST";

    public long id;
    public String title;
    public String content;
    public String noteType; // TYPE_TEXT or TYPE_CHECKLIST
    public int colorIndex;  // 0 to 5
    public String tag;
    public long reminderTimestamp; // 0 if none
    public boolean isPinned;
    public long createdAt;
    public long updatedAt;

    public Note() {
        this.noteType = TYPE_TEXT;
        this.colorIndex = 0;
        this.tag = "כללי";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
