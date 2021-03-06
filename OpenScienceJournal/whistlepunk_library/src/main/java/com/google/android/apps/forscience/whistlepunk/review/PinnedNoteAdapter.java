/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.android.apps.forscience.whistlepunk.review;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.apps.forscience.whistlepunk.AccessibilityUtils;
import com.google.android.apps.forscience.whistlepunk.ElapsedTimeFormatter;
import com.google.android.apps.forscience.whistlepunk.ExternalAxisController;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.metadata.Label;
import com.google.android.apps.forscience.whistlepunk.metadata.PictureLabel;
import com.google.android.apps.forscience.whistlepunk.metadata.SensorTriggerLabel;
import com.google.android.apps.forscience.whistlepunk.metadata.TextLabel;
import com.google.android.apps.forscience.whistlepunk.metadata.TriggerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for a recycler view of pinned notes.
 */
class PinnedNoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "PinnedNoteAdapter";
    private static final int TYPE_TEXT_NOTE = 0;
    private static final int TYPE_PICTURE_NOTE = 1;
    private static final int TYPE_NO_NOTES = 2;
    private static final int TYPE_TRIGGER_NOTE = 3;
    private static final int TYPE_UNKNOWN = -1;

    /**
     * An interface for listening to when a pinned note should be edited or deleted.
     */
    public interface ListItemEditListener {
        void onListItemEdit(Label item);
        void onListItemDelete(Label item);
    }

    /**
     * An interface for listening to when a pinned note is clicked.
     */
    public interface ListItemClickListener {
        // Anywhere on the item was clicked.
        void onListItemClicked(Label item);

        // The picture of a picture label was clicked
        void onPictureItemClicked(PictureLabel item);
    }

    public class NoteHolder extends RecyclerView.ViewHolder {
        TextView mDurationText;
        ImageButton mMenuButton;
        TextView mText;
        ImageView mImage;
        TextView mAutoText;

        public NoteHolder(View v) {
            super(v);
            mDurationText = (TextView) v.findViewById(R.id.duration_text);
            mMenuButton = (ImageButton) v.findViewById(R.id.note_menu_button);
            mText = (TextView) v.findViewById(R.id.note_text);
            mImage = (ImageView) v.findViewById(R.id.note_image);
            mAutoText = (TextView) v.findViewById(R.id.auto_note_text);
        }
    }

    public class NoNotesHolder extends RecyclerView.ViewHolder {
        public NoNotesHolder(View v) {
            super(v);
        }
    }

    private ArrayList<Label> mPinnedNotes = new ArrayList<>();
    private long mStartTimestamp;
    private long mEndTimestamp;
    private ListItemEditListener mEditListener;
    private ListItemClickListener mClickListener;

    public PinnedNoteAdapter(ArrayList<Label> pinnedNotes, long startTimestamp, long endTimestamp) {
        mPinnedNotes = pinnedNotes;
        mStartTimestamp = startTimestamp;
        mEndTimestamp = endTimestamp;
    }

    public void updateRunTimestamps(long startTimestamp, long endTimestamp) {
        mStartTimestamp = startTimestamp;
        mEndTimestamp = endTimestamp;
        notifyDataSetChanged();
    }

    public void setListItemModifyListener(ListItemEditListener editListener) {
        mEditListener = editListener;
    }

    public void setListItemClickListener(ListItemClickListener clickListener) {
        mClickListener = clickListener;
    }

    public List<Label> getPinnedNotes() {
        return mPinnedNotes;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_TEXT_NOTE:
            case TYPE_PICTURE_NOTE:
            case TYPE_TRIGGER_NOTE:
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.pinned_note_item, parent, false);
                return new NoteHolder(v);
            case TYPE_NO_NOTES:
                v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.no_pinned_notes_item, parent, false);
                return new NoNotesHolder(v);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (mPinnedNotes.size() == 0) {
            return;
        }

        final NoteHolder noteHolder = (NoteHolder) holder;
        final Label label = mPinnedNotes.get(position);
        noteHolder.mDurationText.setText(getNoteTimeText(label, mStartTimestamp));
        noteHolder.mDurationText.setContentDescription(getNoteTimeContentDescription(
                label.getTimeStamp(), mStartTimestamp, noteHolder.mDurationText.getContext()));
        String text = "";
        if (label instanceof TextLabel) {
            text = ((TextLabel) label).getText();
            noteHolder.mImage.setVisibility(View.GONE);
            noteHolder.mAutoText.setVisibility(View.GONE);
        } else if (label instanceof PictureLabel) {
            text = ((PictureLabel) label).getCaption();
            noteHolder.mImage.setVisibility(View.VISIBLE);
            noteHolder.mAutoText.setVisibility(View.GONE);
            Glide.with(noteHolder.mImage.getContext())
                    .load(((PictureLabel) label).getFilePath())
                    .into(noteHolder.mImage);
            noteHolder.mImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onPictureItemClicked((PictureLabel) label);
                }
            });
        } else if (label instanceof SensorTriggerLabel) {
            text = ((SensorTriggerLabel) label).getCustomText();
            noteHolder.mImage.setVisibility(View.GONE);
            noteHolder.mAutoText.setVisibility(View.VISIBLE);
            String autoText = ((SensorTriggerLabel) label).getAutogenText();
            TriggerHelper.populateAutoTextViews(noteHolder.mAutoText, autoText,
                    R.drawable.ic_label_black_18dp, noteHolder.mAutoText.getResources());
        }
        if (!TextUtils.isEmpty(text)) {
            noteHolder.mText.setText(text);
            noteHolder.mText.setTextColor(
                    noteHolder.mText.getResources().getColor(R.color.text_color_black));
        } else {
            noteHolder.mText.setText(noteHolder.mText.getResources().getString(
                    label instanceof PictureLabel ? R.string.picture_note_caption_hint :
                            R.string.pinned_note_placeholder_text));
            noteHolder.mText.setTextColor(noteHolder.mText.getResources().getColor(
                    R.color.text_color_light_grey));
            // Increase the touchable area for the "Add Note" prompt.
            AccessibilityUtils.setTouchDelegateToMinAccessibleSize(noteHolder.mText);
        }
        if (mEditListener != null) {
            // Only allow editing of notes with empty text onclick.
            if ((label instanceof PictureLabel &&
                    TextUtils.isEmpty(((PictureLabel) label).getCaption())) ||
                    (label instanceof TextLabel &&
                            TextUtils.isEmpty(((TextLabel) label).getText()))) {
                noteHolder.mText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mEditListener.onListItemEdit(label);
                    }
                });
            }
            noteHolder.mMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = noteHolder.mMenuButton.getContext();
                    PopupMenu popup = new PopupMenu(context, noteHolder.mMenuButton);
                    popup.getMenuInflater().inflate(R.menu.menu_note, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            int itemId = item.getItemId();
                            if (itemId == R.id.btn_edit_note) {
                                mEditListener.onListItemEdit(label);
                                return true;
                            } else if (itemId == R.id.btn_delete_note) {
                                mEditListener.onListItemDelete(label);
                                return true;
                            }
                            return false;
                        }
                    });
                    popup.show();
                }
            });
        }
        // Notes out of range are not clickable.
        if (mStartTimestamp <= label.getTimeStamp() && label.getTimeStamp() < mEndTimestamp) {
            noteHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onListItemClicked(label);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        // If there are no notes, we show a "no notes" view, so the
        // size is always at least 1.
        return Math.max(1, mPinnedNotes.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (mPinnedNotes.size() == 0 && position == 0) {
            return TYPE_NO_NOTES;
        }
        if (mPinnedNotes.get(position) instanceof TextLabel) {
            return TYPE_TEXT_NOTE;
        }
        if (mPinnedNotes.get(position) instanceof PictureLabel) {
            return TYPE_PICTURE_NOTE;
        }
        if (mPinnedNotes.get(position) instanceof SensorTriggerLabel) {
            return TYPE_TRIGGER_NOTE;
        }
        return TYPE_UNKNOWN;
    }

    public void editLabel(Label label) {
        int position = findLabelIndexById(label.getLabelId());
        if (position != -1) {
            mPinnedNotes.remove(position);
            notifyItemRemoved(position);
        }
        insertNote(label);
    }

    private int findLabelIndexById(String id) {
        for (int i = 0; i < mPinnedNotes.size(); i++) {
            if (TextUtils.equals(mPinnedNotes.get(i).getLabelId(), id)) {
                return i;
            }
        }
        return -1;
    }

    public void insertNote(Label label) {
        int size = mPinnedNotes.size();
        long timestamp = label.getTimeStamp();
        for (int i = 0; i < size; i++) {
            if (timestamp < mPinnedNotes.get(i).getTimeStamp()) {
                mPinnedNotes.add(i, label);
                notifyItemInserted(i);
                return;
            }
        }
        mPinnedNotes.add(size, label);
        if (size == 0) {
            notifyItemChanged(size);
        } else {
            notifyItemInserted(size);
        }
    }

    public void deleteNote(Label label) {
        int position = mPinnedNotes.indexOf(label);
        if (position == -1) {
            return;
        }
        mPinnedNotes.remove(position);
        if (mPinnedNotes.size() != 0) {
            notifyItemRemoved(position);
        } else {
            notifyItemChanged(position);
        }
    }

    public static String getNoteTimeText(Label label, long startTimestamp) {
        return getNoteTimeText(label.getTimeStamp(), startTimestamp);
    }

    public static String getNoteTimeText(long labelTimestamp, long startTimestamp) {
        long elapsedTimeSeconds = Math.round(
                (labelTimestamp - startTimestamp) / RunReviewFragment.MILLIS_IN_A_SECOND);
        if (elapsedTimeSeconds < 0) {
            // TOOD: String resource: Localization for negative values?
            return "-" + DateUtils.formatElapsedTime(-1 * elapsedTimeSeconds);
        }
        return DateUtils.formatElapsedTime(elapsedTimeSeconds);
    }

    public static String getNoteTimeContentDescription(long currentTimestamp,
            long runStartTimestamp, Context context) {
        return ElapsedTimeFormatter.getInstance(context).formatForAccessibility(
                (currentTimestamp - runStartTimestamp) / ExternalAxisController.MS_IN_SEC);
    }

    public void onDestroy() {
        mClickListener = null;
        mEditListener = null;
    }
}
