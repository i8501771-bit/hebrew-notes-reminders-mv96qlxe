package com.generated.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // Note background color palette (soft modern tones)
    private static final int[] NOTE_CARD_COLORS = {
            0xFFFFFFFF, // 0: White
            0xFFFEF3C7, // 1: Warm Amber / Gold
            0xFFE0F2FE, // 2: Sky Blue
            0xFFDCFCE7, // 3: Mint Green
            0xFFFCE7F3, // 4: Rose
            0xFFF3E8FF  // 5: Lavender
    };

    private static final int[] NOTE_CARD_BORDERS = {
            0xFFE2E8F0,
            0xFFFDE68A,
            0xFFBAE6FD,
            0xFFBBF7D0,
            0xFFFBCFE8,
            0xFFE9D5FF
    };

    private static final String[] TAGS_LIST = {
            "כללי",
            "עבודה",
            "אישי",
            "תורה ויהדות",
            "קניות"
    };

    private NotesDatabaseHelper dbHelper;
    private LinearLayout notesContainer;
    private View emptyStateView;
    private EditText searchInput;
    private TextView gregorianDateText;
    private TextView hebrewDateBadge;

    private TextView filterAllBtn;
    private TextView filterRemindersBtn;
    private TextView filterTasksBtn;
    private TextView filterPinnedBtn;

    private String currentFilter = "ALL";
    private String currentSearchText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDatabaseHelper(this);

        initViews();
        setupHeaderDate();
        setupFilters();
        setupSearch();
        setupFabButtons();
        checkNotificationPermission();

        // Seed welcome notes on first launch
        seedInitialNotesIfEmpty();
        loadNotes();
    }

    private void initViews() {
        notesContainer = findViewById(R.id.notes_list_container);
        emptyStateView = findViewById(R.id.empty_state_view);
        searchInput = findViewById(R.id.search_input);
        gregorianDateText = findViewById(R.id.gregorian_date_text);
        hebrewDateBadge = findViewById(R.id.hebrew_date_badge);

        filterAllBtn = findViewById(R.id.filter_all);
        filterRemindersBtn = findViewById(R.id.filter_reminders);
        filterTasksBtn = findViewById(R.id.filter_tasks);
        filterPinnedBtn = findViewById(R.id.filter_pinned);
    }

    private void setupHeaderDate() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("he", "IL"));
        String gregDateStr = sdf.format(now.getTime());
        gregorianDateText.setText(gregDateStr);

        HebrewCalendarUtil.HebrewDate hDate = HebrewCalendarUtil.getHebrewDate(now);
        String hebrewStr = hDate.formatHebrewDateString();
        hebrewDateBadge.setText("✡ " + hebrewStr);
    }

    private void setupFilters() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == filterAllBtn) {
                    currentFilter = "ALL";
                } else if (v == filterRemindersBtn) {
                    currentFilter = "REMINDERS";
                } else if (v == filterTasksBtn) {
                    currentFilter = "TASKS";
                } else if (v == filterPinnedBtn) {
                    currentFilter = "PINNED";
                }
                updateFilterButtonsUI();
                loadNotes();
            }
        };

        filterAllBtn.setOnClickListener(listener);
        filterRemindersBtn.setOnClickListener(listener);
        filterTasksBtn.setOnClickListener(listener);
        filterPinnedBtn.setOnClickListener(listener);
    }

    private void updateFilterButtonsUI() {
        setFilterPillState(filterAllBtn, "ALL".equals(currentFilter));
        setFilterPillState(filterRemindersBtn, "REMINDERS".equals(currentFilter));
        setFilterPillState(filterTasksBtn, "TASKS".equals(currentFilter));
        setFilterPillState(filterPinnedBtn, "PINNED".equals(currentFilter));
    }

    private void setFilterPillState(TextView btn, boolean selected) {
        if (selected) {
            btn.setBackgroundResource(R.drawable.bg_pill_selected);
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundResource(R.drawable.bg_pill_unselected);
            btn.setTextColor(Color.parseColor("#475569"));
        }
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchText = s.toString();
                loadNotes();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void setupFabButtons() {
        FrameLayout fabNote = findViewById(R.id.fab_add_note);
        FrameLayout fabChecklist = findViewById(R.id.fab_add_checklist);

        fabNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNoteEditorDialog(null, Note.TYPE_TEXT);
            }
        });

        fabChecklist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNoteEditorDialog(null, Note.TYPE_CHECKLIST);
            }
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }
    }

    private void seedInitialNotesIfEmpty() {
        List<Note> existing = dbHelper.getAllNotes(null, null);
        if (existing.isEmpty()) {
            Note n1 = new Note();
            n1.title = "ברוכים הבאים לפתקים ותזכורות 🌟";
            n1.content = "אפליקציה מתקדמת וידידותית לניהול פתקים, רשימות משימות ותזכורות עם לוח שנה עברי.\n\nתוכלו לבחור צבעים לפתקים, להצמיד פתקים חשובים, ולקבוע תזכורות מדויקות.";
            n1.noteType = Note.TYPE_TEXT;
            n1.colorIndex = 1;
            n1.tag = "תורה ויהדות";
            n1.isPinned = true;
            dbHelper.insertNote(n1);

            Note n2 = new Note();
            n2.title = "הכנות לשבת קודש 🕯️🕯️";
            n2.content = "חלות טריות לשבת;1\nדגים וסלטים;1\nיין לקידוש;0\nפרחים לכבוד שבת;0";
            n2.noteType = Note.TYPE_CHECKLIST;
            n2.colorIndex = 2;
            n2.tag = "אישי";
            n2.isPinned = false;
            dbHelper.insertNote(n2);
        }
    }

    private void loadNotes() {
        // Clear all note cards except the empty state view
        for (int i = notesContainer.getChildCount() - 1; i >= 0; i--) {
            View child = notesContainer.getChildAt(i);
            if (child != emptyStateView) {
                notesContainer.removeViewAt(i);
            }
        }

        List<Note> notes = dbHelper.getAllNotes(currentSearchText, currentFilter);

        if (notes.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            for (final Note note : notes) {
                View card = createNoteCard(note);
                notesContainer.addView(card);
            }
        }
    }

    private View createNoteCard(final Note note) {
        int pad = dp(16);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        card.setPadding(pad, pad, pad, pad);

        // Styling Card Shape
        int colorIdx = Math.max(0, Math.min(note.colorIndex, NOTE_CARD_COLORS.length - 1));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(18));
        bg.setColor(NOTE_CARD_COLORS[colorIdx]);
        bg.setStroke(dp(1), NOTE_CARD_BORDERS[colorIdx]);
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }

        // Header: Title & Pin/Options
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView titleView = new TextView(this);
        titleView.setText(note.title != null && !note.title.isEmpty() ? note.title : "(ללא כותרת)");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleView.setTextColor(Color.parseColor("#0F172A"));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleLp);
        headerRow.addView(titleView);

        if (note.isPinned) {
            TextView pinIcon = new TextView(this);
            pinIcon.setText("📌");
            pinIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            pinIcon.setPadding(dp(6), 0, dp(6), 0);
            headerRow.addView(pinIcon);
        }

        card.addView(headerRow);

        // Content Display
        if (Note.TYPE_CHECKLIST.equals(note.noteType)) {
            // Render interactive checklist preview
            LinearLayout tasksLayout = new LinearLayout(this);
            tasksLayout.setOrientation(LinearLayout.VERTICAL);
            tasksLayout.setPadding(0, dp(8), 0, dp(4));

            String[] items = note.content != null ? note.content.split("\n") : new String[0];
            int previewCount = Math.min(items.length, 6);
            for (int i = 0; i < previewCount; i++) {
                final String itemLine = items[i].trim();
                if (itemLine.isEmpty()) continue;

                String text = itemLine;
                boolean checked = false;
                if (itemLine.contains(";")) {
                    String[] parts = itemLine.split(";", 2);
                    text = parts[0];
                    checked = "1".equals(parts[1]);
                }

                final int itemIdx = i;
                final String finalItemText = text;

                CheckBox cb = new CheckBox(this);
                cb.setText(finalItemText);
                cb.setChecked(checked);
                cb.setTextColor(checked ? Color.parseColor("#94A3B8") : Color.parseColor("#334155"));
                cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                if (checked) {
                    cb.setPaintFlags(cb.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }

                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CheckBox c = (CheckBox) v;
                        toggleChecklistItem(note, itemIdx, c.isChecked());
                        loadNotes();
                    }
                });
                tasksLayout.addView(cb);
            }
            if (items.length > 6) {
                TextView more = new TextView(this);
                more.setText("ועוד " + (items.length - 6) + " פריטים…");
                more.setTextColor(Color.parseColor("#64748B"));
                more.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                more.setPadding(dp(8), dp(4), 0, 0);
                tasksLayout.addView(more);
            }
            card.addView(tasksLayout);
        } else {
            // Standard Text Note
            if (note.content != null && !note.content.trim().isEmpty()) {
                TextView contentView = new TextView(this);
                contentView.setText(note.content);
                contentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                contentView.setTextColor(Color.parseColor("#334155"));
                contentView.setMaxLines(5);
                contentView.setPadding(0, dp(6), 0, dp(6));
                card.addView(contentView);
            }
        }

        // Reminder pill if active
        if (note.reminderTimestamp > 0) {
            LinearLayout reminderBadge = new LinearLayout(this);
            reminderBadge.setOrientation(LinearLayout.HORIZONTAL);
            reminderBadge.setGravity(Gravity.CENTER_VERTICAL);
            reminderBadge.setBackgroundResource(R.drawable.bg_reminder_badge);
            reminderBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rLp.setMargins(0, dp(6), 0, dp(6));
            reminderBadge.setLayoutParams(rLp);

            Calendar rCal = Calendar.getInstance();
            rCal.setTimeInMillis(note.reminderTimestamp);
            HebrewCalendarUtil.HebrewDate hDate = HebrewCalendarUtil.getHebrewDate(rCal);
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String rText = "⏰ " + tf.format(rCal.getTime()) + " | " + hDate.formatHebrewDateString();

            TextView rTv = new TextView(this);
            rTv.setText(rText);
            rTv.setTextColor(Color.parseColor("#B45309"));
            rTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            rTv.setTypeface(null, android.graphics.Typeface.BOLD);
            reminderBadge.addView(rTv);
            card.addView(reminderBadge);
        }

        // Footer: Tag and Hebrew/Date updated info
        LinearLayout footerRow = new LinearLayout(this);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER_VERTICAL);
        footerRow.setPadding(0, dp(8), 0, 0);

        if (note.tag != null && !note.tag.isEmpty()) {
            TextView tagView = new TextView(this);
            tagView.setText("# " + note.tag);
            tagView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tagView.setTextColor(Color.parseColor("#475569"));
            tagView.setBackgroundResource(R.drawable.bg_tag_chip);
            tagView.setPadding(dp(8), dp(2), dp(8), dp(2));
            footerRow.addView(tagView);
        }

        Calendar updCal = Calendar.getInstance();
        updCal.setTimeInMillis(note.updatedAt);
        HebrewCalendarUtil.HebrewDate noteHebDate = HebrewCalendarUtil.getHebrewDate(updCal);

        TextView dateView = new TextView(this);
        dateView.setText(noteHebDate.formatHebrewDateString());
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        dateView.setTextColor(Color.parseColor("#94A3B8"));
        dateView.setPadding(dp(10), 0, dp(10), 0);
        footerRow.addView(dateView);

        card.addView(footerRow);

        // Click to edit
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNoteEditorDialog(note, note.noteType);
            }
        });

        // Long click for fast actions
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showQuickActionDialog(note);
                return true;
            }
        });

        return card;
    }

    private void toggleChecklistItem(Note note, int index, boolean checked) {
        if (note.content == null) return;
        String[] lines = note.content.split("\n");
        if (index >= 0 && index < lines.length) {
            String line = lines[index].trim();
            String text = line;
            if (line.contains(";")) {
                text = line.split(";", 2)[0];
            }
            lines[index] = text + ";" + (checked ? "1" : "0");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) sb.append("\n");
                sb.append(lines[i]);
            }
            note.content = sb.toString();
            dbHelper.updateNote(note);
        }
    }

    private void showQuickActionDialog(final Note note) {
        String[] options = new String[]{
                note.isPinned ? "הסר נעיצה 📌" : "הצמד לראש הרשימה 📌",
                note.reminderTimestamp > 0 ? "הסר תזכורת ⏰" : "הגדר תזכורת ⏰",
                "שנה צבע 🎨",
                "מחק פתק 🗑️"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(note.title != null && !note.title.isEmpty() ? note.title : "פעולות מהירות");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                note.isPinned = !note.isPinned;
                dbHelper.updateNote(note);
                loadNotes();
            } else if (which == 1) {
                if (note.reminderTimestamp > 0) {
                    cancelReminder(note);
                    note.reminderTimestamp = 0;
                    dbHelper.updateNote(note);
                    Toast.makeText(MainActivity.this, "התזכורת הוסרה", Toast.LENGTH_SHORT).show();
                    loadNotes();
                } else {
                    pickReminderDateTime(note, null);
                }
            } else if (which == 2) {
                showColorPickerDialog(note);
            } else if (which == 3) {
                confirmDelete(note);
            }
        });
        builder.show();
    }

    private void showColorPickerDialog(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחירת צבע פתק");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));

        final AlertDialog dialog = builder.setView(layout).create();

        for (int i = 0; i < NOTE_CARD_COLORS.length; i++) {
            final int colorIndex = i;
            View colorCircle = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(42));
            lp.setMargins(dp(6), 0, dp(6), 0);
            colorCircle.setLayoutParams(lp);

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(NOTE_CARD_COLORS[i]);
            gd.setStroke(dp(2), NOTE_CARD_BORDERS[i]);
            colorCircle.setBackground(gd);

            colorCircle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    note.colorIndex = colorIndex;
                    dbHelper.updateNote(note);
                    dialog.dismiss();
                    loadNotes();
                }
            });
            layout.addView(colorCircle);
        }

        dialog.show();
    }

    private void confirmDelete(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton("מחק", (dialog, which) -> {
                    cancelReminder(note);
                    dbHelper.deleteNote(note.id);
                    Toast.makeText(MainActivity.this, "הפתק נמחק", Toast.LENGTH_SHORT).show();
                    loadNotes();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // -------------------------------------------------------------
    // Full Custom Editor Dialog (Standard Note or Checklist)
    // -------------------------------------------------------------
    private void openNoteEditorDialog(final Note existingNote, final String defaultType) {
        final boolean isNew = (existingNote == null);
        final Note note = isNew ? new Note() : existingNote;
        if (isNew) {
            note.noteType = defaultType;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        scrollView.addView(root);

        // Dialog Header Title
        TextView headerTitle = new TextView(this);
        headerTitle.setText(isNew ? (Note.TYPE_CHECKLIST.equals(note.noteType) ? "רשימת משימות חדשה" : "פתק חדש") : "עריכת פתק");
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        headerTitle.setTextColor(Color.parseColor("#0F172A"));
        headerTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        headerTitle.setPadding(0, 0, 0, dp(16));
        root.addView(headerTitle);

        // Title Input
        final EditText etTitle = new EditText(this);
        etTitle.setHint(R.string.title_hint);
        etTitle.setText(note.title != null ? note.title : "");
        etTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        etTitle.setTextColor(Color.parseColor("#0F172A"));
        etTitle.setBackgroundResource(R.drawable.bg_search_input);
        etTitle.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(etTitle);

        // Tag Selector Bar
        TextView tagLabel = new TextView(this);
        tagLabel.setText("קטגוריה:");
        tagLabel.setTextColor(Color.parseColor("#475569"));
        tagLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tagLabel.setPadding(0, dp(12), 0, dp(4));
        root.addView(tagLabel);

        HorizontalScrollView tagScroll = new HorizontalScrollView(this);
        final LinearLayout tagContainer = new LinearLayout(this);
        tagContainer.setOrientation(LinearLayout.HORIZONTAL);
        tagScroll.addView(tagContainer);

        final String[] selectedTag = new String[]{note.tag != null ? note.tag : "כללי"};
        final List<TextView> tagViews = new ArrayList<>();

        for (final String tagOption : TAGS_LIST) {
            final TextView tagPill = new TextView(this);
            tagPill.setText(tagOption);
            tagPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tagPill.setPadding(dp(12), dp(6), dp(12), dp(6));
            LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tagLp.setMargins(0, 0, dp(6), 0);
            tagPill.setLayoutParams(tagLp);

            boolean isCurrent = tagOption.equals(selectedTag[0]);
            tagPill.setBackgroundResource(isCurrent ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
            tagPill.setTextColor(isCurrent ? Color.WHITE : Color.parseColor("#475569"));

            tagPill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedTag[0] = tagOption;
                    for (TextView tv : tagViews) {
                        boolean sel = tv.getText().toString().equals(tagOption);
                        tv.setBackgroundResource(sel ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
                        tv.setTextColor(sel ? Color.WHITE : Color.parseColor("#475569"));
                    }
                }
            });

            tagViews.add(tagPill);
            tagContainer.addView(tagPill);
        }
        root.addView(tagScroll);

        // Color Palette Selector
        TextView colorLabel = new TextView(this);
        colorLabel.setText("צבע כרטיס:");
        colorLabel.setTextColor(Color.parseColor("#475569"));
        colorLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        colorLabel.setPadding(0, dp(12), 0, dp(4));
        root.addView(colorLabel);

        LinearLayout colorLayout = new LinearLayout(this);
        colorLayout.setOrientation(LinearLayout.HORIZONTAL);
        final int[] selectedColor = new int[]{note.colorIndex};
        final List<View> colorViews = new ArrayList<>();

        for (int i = 0; i < NOTE_CARD_COLORS.length; i++) {
            final int cIdx = i;
            final View colorDot = new View(this);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(dp(36), dp(36));
            cLp.setMargins(0, 0, dp(8), 0);
            colorDot.setLayoutParams(cLp);

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(NOTE_CARD_COLORS[i]);
            gd.setStroke(selectedColor[0] == i ? dp(3) : dp(1), selectedColor[0] == i ? Color.parseColor("#2563EB") : NOTE_CARD_BORDERS[i]);
            colorDot.setBackground(gd);

            colorDot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColor[0] = cIdx;
                    for (int k = 0; k < colorViews.size(); k++) {
                        GradientDrawable d = new GradientDrawable();
                        d.setShape(GradientDrawable.OVAL);
                        d.setColor(NOTE_CARD_COLORS[k]);
                        d.setStroke(selectedColor[0] == k ? dp(3) : dp(1), selectedColor[0] == k ? Color.parseColor("#2563EB") : NOTE_CARD_BORDERS[k]);
                        colorViews.get(k).setBackground(d);
                    }
                }
            });

            colorViews.add(colorDot);
            colorLayout.addView(colorDot);
        }
        root.addView(colorLayout);

        // Body Content Input (Text or Checklist Items)
        final EditText etContent = new EditText(this);
        final LinearLayout checklistContainer = new LinearLayout(this);
        final List<ChecklistItemHolder> checklistItems = new ArrayList<>();

        if (Note.TYPE_TEXT.equals(note.noteType)) {
            TextView bodyLabel = new TextView(this);
            bodyLabel.setText("תוכן הפתק:");
            bodyLabel.setTextColor(Color.parseColor("#475569"));
            bodyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            bodyLabel.setPadding(0, dp(12), 0, dp(4));
            root.addView(bodyLabel);

            etContent.setHint(R.string.content_hint);
            etContent.setText(note.content != null ? note.content : "");
            etContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            etContent.setTextColor(Color.parseColor("#0F172A"));
            etContent.setBackgroundResource(R.drawable.bg_search_input);
            etContent.setGravity(Gravity.TOP | Gravity.START);
            etContent.setMinLines(5);
            etContent.setMaxLines(12);
            etContent.setPadding(dp(14), dp(12), dp(14), dp(12));
            root.addView(etContent);
        } else {
            // Checklist items builder
            TextView tasksLabel = new TextView(this);
            tasksLabel.setText("פריטי משימה:");
            tasksLabel.setTextColor(Color.parseColor("#475569"));
            tasksLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tasksLabel.setPadding(0, dp(12), 0, dp(4));
            root.addView(tasksLabel);

            checklistContainer.setOrientation(LinearLayout.VERTICAL);
            root.addView(checklistContainer);

            // Populate existing checklist items
            if (note.content != null && !note.content.trim().isEmpty()) {
                String[] lines = note.content.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    String text = line;
                    boolean checked = false;
                    if (line.contains(";")) {
                        String[] parts = line.split(";", 2);
                        text = parts[0];
                        checked = "1".equals(parts[1]);
                    }
                    addChecklistRow(checklistContainer, checklistItems, text, checked);
                }
            } else {
                // Add first blank row
                addChecklistRow(checklistContainer, checklistItems, "", false);
            }

            // Button to add new task row
            TextView btnAddTask = new TextView(this);
            btnAddTask.setText("+ הוסף פריט נוסף");
            btnAddTask.setTextColor(Color.parseColor("#2563EB"));
            btnAddTask.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            btnAddTask.setPadding(dp(8), dp(8), dp(8), dp(8));
            btnAddTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addChecklistRow(checklistContainer, checklistItems, "", false);
                }
            });
            root.addView(btnAddTask);
        }

        // Reminder Status & Setter
        final long[] reminderHolder = new long[]{note.reminderTimestamp};
        final TextView reminderTextStatus = new TextView(this);
        reminderTextStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        reminderTextStatus.setPadding(0, dp(8), 0, dp(8));
        updateReminderStatusText(reminderTextStatus, reminderHolder[0]);
        root.addView(reminderTextStatus);

        LinearLayout reminderButtonsRow = new LinearLayout(this);
        reminderButtonsRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnSetReminder = new TextView(this);
        btnSetReminder.setText("⏰ בחר תזכורת");
        btnSetReminder.setBackgroundResource(R.drawable.bg_button_secondary);
        btnSetReminder.setTextColor(Color.parseColor("#1E293B"));
        btnSetReminder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnSetReminder.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnSetReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickReminderDateTime(note, new ReminderCallback() {
                    @Override
                    public void onReminderSelected(long timeMs) {
                        reminderHolder[0] = timeMs;
                        updateReminderStatusText(reminderTextStatus, timeMs);
                    }
                });
            }
        });
        reminderButtonsRow.addView(btnSetReminder);

        TextView btnClearReminder = new TextView(this);
        btnClearReminder.setText("הסר");
        btnClearReminder.setBackgroundResource(R.drawable.bg_button_danger);
        btnClearReminder.setTextColor(Color.parseColor("#DC2626"));
        btnClearReminder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnClearReminder.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        clp.setMargins(dp(8), 0, 0, 0);
        btnClearReminder.setLayoutParams(clp);
        btnClearReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reminderHolder[0] = 0;
                updateReminderStatusText(reminderTextStatus, 0);
            }
        });
        reminderButtonsRow.addView(btnClearReminder);

        root.addView(reminderButtonsRow);

        // Pin Checkbox
        final CheckBox cbPin = new CheckBox(this);
        cbPin.setText("נעץ פתק זה בראש הרשימה 📌");
        cbPin.setChecked(note.isPinned);
        cbPin.setTextColor(Color.parseColor("#334155"));
        cbPin.setPadding(dp(8), dp(12), dp(8), dp(12));
        root.addView(cbPin);

        // Action Buttons Row: Cancel and Save
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(16), 0, dp(8));

        TextView btnSave = new TextView(this);
        btnSave.setText(R.string.save);
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setTextColor(Color.WHITE);
        btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnSave.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSave.setBackgroundResource(R.drawable.bg_button_primary);
        LinearLayout.LayoutParams btnSaveLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        btnSaveLp.setMargins(0, 0, dp(8), 0);
        btnSave.setLayoutParams(btnSaveLp);

        TextView btnCancel = new TextView(this);
        btnCancel.setText(R.string.cancel);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextColor(Color.parseColor("#475569"));
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnCancel.setBackgroundResource(R.drawable.bg_button_secondary);
        LinearLayout.LayoutParams btnCancelLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        btnCancel.setLayoutParams(btnCancelLp);

        actionRow.addView(btnSave);
        actionRow.addView(btnCancel);
        root.addView(actionRow);

        final AlertDialog dialog = builder.setView(scrollView).create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String content = "";

                if (Note.TYPE_TEXT.equals(note.noteType)) {
                    content = etContent.getText().toString().trim();
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (ChecklistItemHolder item : checklistItems) {
                        String txt = item.etText.getText().toString().trim();
                        if (!txt.isEmpty()) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(txt).append(";").append(item.cbCheck.isChecked() ? "1" : "0");
                        }
                    }
                    content = sb.toString();
                }

                if (title.isEmpty() && content.isEmpty()) {
                    Toast.makeText(MainActivity.this, "אנא הזן כותרת או תוכן לפתק", Toast.LENGTH_SHORT).show();
                    return;
                }

                note.title = title;
                note.content = content;
                note.tag = selectedTag[0];
                note.colorIndex = selectedColor[0];
                note.isPinned = cbPin.isChecked();
                note.reminderTimestamp = reminderHolder[0];

                if (isNew) {
                    long id = dbHelper.insertNote(note);
                    note.id = id;
                } else {
                    dbHelper.updateNote(note);
                }

                if (note.reminderTimestamp > System.currentTimeMillis()) {
                    scheduleReminder(note);
                } else if (note.reminderTimestamp == 0) {
                    cancelReminder(note);
                }

                dialog.dismiss();
                loadNotes();
            }
        });

        dialog.show();
    }

    private static class ChecklistItemHolder {
        CheckBox cbCheck;
        EditText etText;
        View deleteBtn;
    }

    private void addChecklistRow(final LinearLayout container, final List<ChecklistItemHolder> list, String text, boolean checked) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setPadding(0, dp(4), 0, dp(4));

        final CheckBox cb = new CheckBox(this);
        cb.setChecked(checked);
        row.addView(cb);

        final EditText et = new EditText(this);
        et.setText(text);
        et.setHint(R.string.add_task_hint);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        et.setTextColor(Color.parseColor("#0F172A"));
        et.setBackgroundResource(R.drawable.bg_search_input);
        et.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        et.setLayoutParams(etLp);
        row.addView(et);

        TextView del = new TextView(this);
        del.setText("✕");
        del.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        del.setTextColor(Color.parseColor("#EF4444"));
        del.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.addView(del);

        final ChecklistItemHolder holder = new ChecklistItemHolder();
        holder.cbCheck = cb;
        holder.etText = et;
        holder.deleteBtn = del;
        list.add(holder);

        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                container.removeView(row);
                list.remove(holder);
            }
        });

        container.addView(row);
    }

    private void updateReminderStatusText(TextView tv, long timestamp) {
        if (timestamp > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(timestamp);
            HebrewCalendarUtil.HebrewDate hd = HebrewCalendarUtil.getHebrewDate(c);
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            tv.setText("תזכורת: " + hd.formatHebrewDateString() + " בשעה " + tf.format(c.getTime()));
            tv.setTextColor(Color.parseColor("#B45309"));
        } else {
            tv.setText("אין תזכורת מוגדרת");
            tv.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private interface ReminderCallback {
        void onReminderSelected(long timeMs);
    }

    private void pickReminderDateTime(final Note note, final ReminderCallback callback) {
        final Calendar cal = Calendar.getInstance();
        if (note != null && note.reminderTimestamp > System.currentTimeMillis()) {
            cal.setTimeInMillis(note.reminderTimestamp);
        }

        DatePickerDialog dpd = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Then pick time
                    TimePickerDialog tpd = new TimePickerDialog(
                            MainActivity.this,
                            (view1, hourOfDay, minute) -> {
                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                cal.set(Calendar.MINUTE, minute);
                                cal.set(Calendar.SECOND, 0);

                                long chosenTime = cal.getTimeInMillis();
                                if (chosenTime <= System.currentTimeMillis()) {
                                    Toast.makeText(MainActivity.this, "אנא בחר שעה עתידית לתזכורת", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (callback != null) {
                                    callback.onReminderSelected(chosenTime);
                                } else if (note != null) {
                                    note.reminderTimestamp = chosenTime;
                                    dbHelper.updateNote(note);
                                    scheduleReminder(note);
                                    Toast.makeText(MainActivity.this, R.string.reminder_set_success, Toast.LENGTH_SHORT).show();
                                    loadNotes();
                                }
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                    );
                    tpd.show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dpd.show();
    }

    private void scheduleReminder(Note note) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_NOTE_ID, note.id);
        intent.putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, note.title);
        intent.putExtra(ReminderReceiver.EXTRA_NOTE_CONTENT, note.content);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getBroadcast(this, (int) note.id, intent, flags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, note.reminderTimestamp, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, note.reminderTimestamp, pi);
        }
    }

    private void cancelReminder(Note note) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pi = PendingIntent.getBroadcast(this, (int) note.id, intent, flags);
        am.cancel(pi);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
