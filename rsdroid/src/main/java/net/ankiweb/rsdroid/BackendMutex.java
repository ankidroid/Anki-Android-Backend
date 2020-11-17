/*
 * Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.ankiweb.rsdroid;

import androidx.annotation.Nullable;

import com.google.protobuf.ByteString;

import net.ankiweb.rsdroid.database.SQLHandler;

import org.json.JSONArray;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import BackendProto.Backend;

/**
 * Ensures that a single thread accesses RustBackend at the same time.
 * This is because rslib-bridge currently has no distinction between threads, and handles the state of
 * transactions. Parallel
 */
public class BackendMutex implements BackendV1 {
    // This class exists as the Rust backend uses a single connection for SQLite, rather than a connection pool
    // This means that SQL can occur cross-threads.
    // There are a few problems with this:
    // * When inside a transaction, another thread can add commands, or close the transaction
    // * Commands can either be sent from the Java, or from the Rust.
    // * We have no knowledge about whether a Rust command will start a transaction

    // We handle this using a mutex and some invariants:
    // * If a transaction is held by a thread, have the thread keep the mutex until the transaction is closed
    // * Only one Rust command can run at a time - already true as with_col in rust uses a mutex, but we'll lock on the Java side

    private final ReentrantLock mLock = new ReentrantLock();
    private final BackendV1 mBackend;

    public BackendMutex(BackendV1 mWrapped) {
        this.mBackend = mWrapped;
    }

    @Override
    public void beginTransaction() {
        mLock.lock();
        mBackend.beginTransaction();
    }

    @Override
    public void commitTransaction() {
        try {
            mBackend.commitTransaction();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void rollbackTransaction() {
        try {
            mBackend.rollbackTransaction();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public JSONArray fullQuery(String query, Object... bindArgs) {
        try {
            mLock.lock();
            return mBackend.fullQuery(query, bindArgs);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public int executeGetRowsAffected(String sql, Object... bindArgs) {
        try {
            mLock.lock();
            return mBackend.executeGetRowsAffected(sql, bindArgs);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public long insertForId(String sql, Object... bindArgs) {
        try {
            mLock.lock();
            return mBackend.insertForId(sql, bindArgs);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public String[] getColumnNames(String sql) {
        try {
            mLock.lock();
            return mBackend.getColumnNames(sql);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void closeDatabase() {
        try {
            mLock.lock();
            mBackend.closeDatabase();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public String getPath() {
        try {
            mLock.lock();
            return mBackend.getPath();
        } finally {
            mLock.unlock();
        }
    }

    // RustBackend Implementation

    @Override
    public Backend.Progress latestProgress() {
        try {
            mLock.lock();
            return mBackend.latestProgress();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setWantsAbort() {
        try {
            mLock.lock();
            mBackend.setWantsAbort();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.ExtractAVTagsOut extractAVTags(@Nullable String text, boolean questionSide) {
        try {
            mLock.lock();
            return mBackend.extractAVTags(text, questionSide);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.ExtractLatexOut extractLatex(@Nullable String text, boolean svg, boolean expandClozes) {
        try {
            mLock.lock();
            return mBackend.extractLatex(text, svg, expandClozes);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.EmptyCardsReport getEmptyCards() {
        try {
            mLock.lock();
            return mBackend.getEmptyCards();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.RenderCardOut renderExistingCard(long cardId, boolean browser) {
        try {
            mLock.lock();
            return mBackend.renderExistingCard(cardId, browser);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.RenderCardOut renderUncommittedCard(@Nullable Backend.Note note, int cardOrd, @Nullable ByteString template, boolean fillEmpty) {
        try {
            mLock.lock();
            return mBackend.renderUncommittedCard(note, cardOrd, template, fillEmpty);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.String stripAVTags(Backend.String args) {
        try {
            mLock.lock();
            return mBackend.stripAVTags(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.SearchCardsOut searchCards(@Nullable String search, @Nullable Backend.SortOrder order) {
        try {
            mLock.lock();
            return mBackend.searchCards(search, order);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.SearchNotesOut searchNotes(@Nullable String search) {
        try {
            mLock.lock();
            return mBackend.searchNotes(search);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.UInt32 findAndReplace(List<Long> nids, @Nullable String search, @Nullable String replacement, boolean regex, boolean matchCase, @Nullable String fieldName) {
        try {
            mLock.lock();
            return mBackend.findAndReplace(nids, search, replacement, regex, matchCase, fieldName);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Int32 localMinutesWest(Backend.Int64 args) {
        try {
            mLock.lock();
            return mBackend.localMinutesWest(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setLocalMinutesWest(Backend.Int32 args) {
        try {
            mLock.lock();
            mBackend.setLocalMinutesWest(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.SchedTimingTodayOut schedTimingToday() {
        try {
            mLock.lock();
            return mBackend.schedTimingToday();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.String studiedToday(int cards, double seconds) {
        try {
            mLock.lock();
            return mBackend.studiedToday(cards, seconds);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.String congratsLearnMessage(float nextDue, int remaining) {
        try {
            mLock.lock();
            return mBackend.congratsLearnMessage(nextDue, remaining);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void updateStats(long deckId, int newDelta, int reviewDelta, int millisecondDelta) {
        try {
            mLock.lock();
            mBackend.updateStats(deckId, newDelta, reviewDelta, millisecondDelta);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void extendLimits(long deckId, int newDelta, int reviewDelta) {
        try {
            mLock.lock();
            mBackend.extendLimits(deckId, newDelta, reviewDelta);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.CountsForDeckTodayOut countsForDeckToday(Backend.DeckID args) {
        try {
            mLock.lock();
            return mBackend.countsForDeckToday(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.String cardStats(Backend.CardID args) {
        try {
            mLock.lock();
            return mBackend.cardStats(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.GraphsOut graphs(@Nullable String search, int days) {
        try {
            mLock.lock();
            return mBackend.graphs(search, days);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.CheckMediaOut checkMedia() {
        try {
            mLock.lock();
            return mBackend.checkMedia();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void trashMediaFiles(List<String> fnames) {
        try {
            mLock.lock();
            mBackend.trashMediaFiles(fnames);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.String addMediaFile(@Nullable String desiredName, @Nullable ByteString data) {
        try {
            mLock.lock();
            return mBackend.addMediaFile(desiredName, data);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void emptyTrash() {
        try {
            mLock.lock();
            mBackend.emptyTrash();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void restoreTrash() {
        try {
            mLock.lock();
            mBackend.restoreTrash();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.DeckID addOrUpdateDeckLegacy(@Nullable ByteString deck, boolean preserveUsnAndMtime) {
        try {
            mLock.lock();
            return mBackend.addOrUpdateDeckLegacy(deck, preserveUsnAndMtime);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.DeckTreeNode deckTree(long now, long topDeckId) {
        try {
            mLock.lock();
            return mBackend.deckTree(now, topDeckId);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json deckTreeLegacy() {
        try {
            mLock.lock();
            return mBackend.deckTreeLegacy();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json getAllDecksLegacy() {
        try {
            mLock.lock();
            return mBackend.getAllDecksLegacy();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.DeckID getDeckIDByName(Backend.String args) {
        try {
            mLock.lock();
            return mBackend.getDeckIDByName(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json getDeckLegacy(Backend.DeckID args) {
        try {
            mLock.lock();
            return mBackend.getDeckLegacy(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.DeckNames getDeckNames(boolean skipEmptyDefault, boolean includeFiltered) {
        try {
            mLock.lock();
            return mBackend.getDeckNames(skipEmptyDefault, includeFiltered);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json newDeckLegacy(Backend.Bool args) {
        try {
            mLock.lock();
            return mBackend.newDeckLegacy(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void removeDeck(Backend.DeckID args) {
        try {
            mLock.lock();
            mBackend.removeDeck(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.DeckConfigID addOrUpdateDeckConfigLegacy(@Nullable ByteString config, boolean preserveUsnAndMtime) {
        try {
            mLock.lock();
            return mBackend.addOrUpdateDeckConfigLegacy(config, preserveUsnAndMtime);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json allDeckConfigLegacy() {
        try {
            mLock.lock();
            return mBackend.allDeckConfigLegacy();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json getDeckConfigLegacy(Backend.DeckConfigID args) {
        try {
            mLock.lock();
            return mBackend.getDeckConfigLegacy(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json newDeckConfigLegacy() {
        try {
            mLock.lock();
            return mBackend.newDeckConfigLegacy();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void removeDeckConfig(Backend.DeckConfigID args) {
        try {
            mLock.lock();
            mBackend.removeDeckConfig(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Card getCard(Backend.CardID args) {
        try {
            mLock.lock();
            return mBackend.getCard(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void updateCard(Backend.Card args) {
        try {
            mLock.lock();
            mBackend.updateCard(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.CardID addCard(Backend.Card args) {
        try {
            mLock.lock();
            return mBackend.addCard(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void removeCards(List<Long> cardIds) {
        try {
            mLock.lock();
            mBackend.removeCards(cardIds);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Note newNote(Backend.NoteTypeID args) {
        try {
            mLock.lock();
            return mBackend.newNote(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.NoteID addNote(@Nullable Backend.Note note, long deckId) {
        try {
            mLock.lock();
            return mBackend.addNote(note, deckId);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void updateNote(Backend.Note args) {
        try {
            mLock.lock();
            mBackend.updateNote(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Note getNote(Backend.NoteID args) {
        try {
            mLock.lock();
            return mBackend.getNote(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void removeNotes(List<Long> noteIds, List<Long> cardIds) {
        try {
            mLock.lock();
            mBackend.removeNotes(noteIds, cardIds);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.UInt32 addNoteTags(List<Long> nids, @Nullable String tags) {
        try {
            mLock.lock();
            return mBackend.addNoteTags(nids, tags);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.UInt32 updateNoteTags(List<Long> nids, @Nullable String tags, @Nullable String replacement, boolean regex) {
        try {
            mLock.lock();
            return mBackend.updateNoteTags(nids, tags, replacement, regex);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.ClozeNumbersInNoteOut clozeNumbersInNote(Backend.Note args) {
        try {
            mLock.lock();
            return mBackend.clozeNumbersInNote(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void afterNoteUpdates(List<Long> nids, boolean markNotesModified, boolean generateCards) {
        try {
            mLock.lock();
            mBackend.afterNoteUpdates(nids, markNotesModified, generateCards);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.FieldNamesForNotesOut fieldNamesForNotes(List<Long> nids) {
        try {
            mLock.lock();
            return mBackend.fieldNamesForNotes(nids);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.NoteIsDuplicateOrEmptyOut noteIsDuplicateOrEmpty(Backend.Note args) {
        try {
            mLock.lock();
            return mBackend.noteIsDuplicateOrEmpty(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeID addOrUpdateNotetype(@Nullable ByteString json, boolean preserveUsnAndMtime) {
        try {
            mLock.lock();
            return mBackend.addOrUpdateNotetype(json, preserveUsnAndMtime);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json getStockNotetypeLegacy(@Nullable Backend.StockNoteType kind) {
        try {
            mLock.lock();
            return mBackend.getStockNotetypeLegacy(kind);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json getNotetypeLegacy(Backend.NoteTypeID args) {
        try {
            mLock.lock();
            return mBackend.getNotetypeLegacy(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeNames getNotetypeNames() {
        try {
            mLock.lock();
            return mBackend.getNotetypeNames();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeUseCounts getNotetypeNamesAndCounts() {
        try {
            mLock.lock();
            return mBackend.getNotetypeNamesAndCounts();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeID getNotetypeIDByName(Backend.String args) {
        try {
            mLock.lock();
            return mBackend.getNotetypeIDByName(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void removeNotetype(Backend.NoteTypeID args) {
        try {
            mLock.lock();
            mBackend.removeNotetype(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void openCollection(@Nullable String collectionPath, @Nullable String mediaFolderPath, @Nullable String mediaDbPath, @Nullable String logPath) {
        try {
            mLock.lock();
            mBackend.openCollection(collectionPath, mediaFolderPath, mediaDbPath, logPath);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void closeCollection(boolean downgradeToSchema11) {
        try {
            mLock.lock();
            mBackend.closeCollection(downgradeToSchema11);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.CheckDatabaseOut checkDatabase() {
        try {
            mLock.lock();
            return mBackend.checkDatabase();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void beforeUpload() {
        try {
            mLock.lock();
            mBackend.beforeUpload();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.String translateString(Backend.TranslateStringIn args) {
        try {
            mLock.lock();
            return mBackend.translateString(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.String formatTimespan(float seconds, @Nullable Backend.FormatTimespanIn.Context context) {
        try {
            mLock.lock();
            return mBackend.formatTimespan(seconds, context);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json i18nResources() {
        try {
            mLock.lock();
            return mBackend.i18nResources();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Bool registerTags(@Nullable String tags, boolean preserveUsn, int usn, boolean clearFirst) {
        try {
            mLock.lock();
            return mBackend.registerTags(tags, preserveUsn, usn, clearFirst);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.AllTagsOut allTags() {
        try {
            mLock.lock();
            return mBackend.allTags();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json getConfigJson(Backend.String args) {
        try {
            mLock.lock();
            return mBackend.getConfigJson(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setConfigJson(@Nullable String key, @Nullable ByteString valueJson) {
        try {
            mLock.lock();
            mBackend.setConfigJson(key, valueJson);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void removeConfig(Backend.String args) {
        try {
            mLock.lock();
            mBackend.removeConfig(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setAllConfig(Backend.Json args) {
        try {
            mLock.lock();
            mBackend.setAllConfig(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Json getAllConfig() {
        try {
            mLock.lock();
            return mBackend.getAllConfig();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public Backend.Preferences getPreferences() {
        try {
            mLock.lock();
            return mBackend.getPreferences();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void setPreferences(Backend.Preferences args) {
        try {
            mLock.lock();
            mBackend.setPreferences(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void openAnkiDroidCollection(Backend.OpenCollectionIn args) {
        try {
            mLock.lock();
            mBackend.openAnkiDroidCollection(args);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public boolean isOpen() {
        try {
            mLock.lock();
            return mBackend.isOpen();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            mLock.lock();
            mBackend.close();
        } finally {
            mLock.unlock();
        }
    }
}
