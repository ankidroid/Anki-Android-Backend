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

import org.json.JSONArray;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import BackendProto.Backend;
import BackendProto.Sqlite;

/**
 * Ensures that a single thread accesses RustBackend at the same time.
 * This is because rslib-bridge currently has no distinction between threads, and handles the state of
 * transactions.
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

    private final ReentrantLock lock = new ReentrantLock();
    private final BackendV1 backend;

    public BackendMutex(BackendV1 backend) {
        this.backend = backend;
    }

    @Override
    public void beginTransaction() {
        lock.lock();
        backend.beginTransaction();
    }

    @Override
    public void commitTransaction() {
        try {
            backend.commitTransaction();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void rollbackTransaction() {
        try {
            backend.rollbackTransaction();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public JSONArray fullQuery(String query, Object... bindArgs) {
        try {
            lock.lock();
            return backend.fullQuery(query, bindArgs);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int executeGetRowsAffected(String sql, Object... bindArgs) {
        try {
            lock.lock();
            return backend.executeGetRowsAffected(sql, bindArgs);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long insertForId(String sql, Object... bindArgs) {
        try {
            lock.lock();
            return backend.insertForId(sql, bindArgs);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String[] getColumnNames(String sql) {
        try {
            lock.lock();
            return backend.getColumnNames(sql);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeDatabase() {
        try {
            lock.lock();
            backend.closeDatabase();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getPath() {
        try {
            lock.lock();
            return backend.getPath();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Sqlite.DBResponse getPage(int page) {
        try {
            lock.lock();
            return backend.getPage(page);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Sqlite.DBResponse fullQueryProto(String query, Object... bindArgs) {
        try {
            lock.lock();
            return backend.fullQueryProto(query, bindArgs);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getCurrentRowCount() {
        try {
            lock.lock();
            return backend.getCurrentRowCount();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cancelCurrentProtoQuery() {
        try {
            lock.lock();
            backend.cancelCurrentProtoQuery();
        } finally {
            lock.unlock();
        }
    }

    // RustBackend Implementation

    @Override
    public Backend.Progress latestProgress() {
        try {
            lock.lock();
            return backend.latestProgress();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setWantsAbort() {
        try {
            lock.lock();
            backend.setWantsAbort();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.ExtractAVTagsOut extractAVTags(@Nullable String text, boolean questionSide) {
        try {
            lock.lock();
            return backend.extractAVTags(text, questionSide);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.ExtractLatexOut extractLatex(@Nullable String text, boolean svg, boolean expandClozes) {
        try {
            lock.lock();
            return backend.extractLatex(text, svg, expandClozes);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.EmptyCardsReport getEmptyCards() {
        try {
            lock.lock();
            return backend.getEmptyCards();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.RenderCardOut renderExistingCard(long cardId, boolean browser) {
        try {
            lock.lock();
            return backend.renderExistingCard(cardId, browser);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.RenderCardOut renderUncommittedCard(@Nullable Backend.Note note, int cardOrd, @Nullable ByteString template, boolean fillEmpty) {
        try {
            lock.lock();
            return backend.renderUncommittedCard(note, cardOrd, template, fillEmpty);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.String stripAVTags(String args) {
        try {
            lock.lock();
            return backend.stripAVTags(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.SearchCardsOut searchCards(@Nullable String search, @Nullable Backend.SortOrder order) {
        try {
            lock.lock();
            return backend.searchCards(search, order);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.SearchNotesOut searchNotes(@Nullable String search) {
        try {
            lock.lock();
            return backend.searchNotes(search);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.UInt32 findAndReplace(List<Long> nids, @Nullable String search, @Nullable String replacement, boolean regex, boolean matchCase, @Nullable String fieldName) {
        try {
            lock.lock();
            return backend.findAndReplace(nids, search, replacement, regex, matchCase, fieldName);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Int32 localMinutesWest(long args) {
        try {
            lock.lock();
            return backend.localMinutesWest(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setLocalMinutesWest(int args) {
        try {
            lock.lock();
            backend.setLocalMinutesWest(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.SchedTimingTodayOut schedTimingToday() {
        try {
            lock.lock();
            return backend.schedTimingToday();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.String studiedToday(int cards, double seconds) {
        try {
            lock.lock();
            return backend.studiedToday(cards, seconds);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.String congratsLearnMessage(float nextDue, int remaining) {
        try {
            lock.lock();
            return backend.congratsLearnMessage(nextDue, remaining);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateStats(long deckId, int newDelta, int reviewDelta, int millisecondDelta) {
        try {
            lock.lock();
            backend.updateStats(deckId, newDelta, reviewDelta, millisecondDelta);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void extendLimits(long deckId, int newDelta, int reviewDelta) {
        try {
            lock.lock();
            backend.extendLimits(deckId, newDelta, reviewDelta);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.CountsForDeckTodayOut countsForDeckToday(long did) {
        try {
            lock.lock();
            return backend.countsForDeckToday(did);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.String cardStats(long cid) {
        try {
            lock.lock();
            return backend.cardStats(cid);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.GraphsOut graphs(@Nullable String search, int days) {
        try {
            lock.lock();
            return backend.graphs(search, days);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.CheckMediaOut checkMedia() {
        try {
            lock.lock();
            return backend.checkMedia();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void trashMediaFiles(List<String> fnames) {
        try {
            lock.lock();
            backend.trashMediaFiles(fnames);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.String addMediaFile(@Nullable String desiredName, @Nullable ByteString data) {
        try {
            lock.lock();
            return backend.addMediaFile(desiredName, data);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void emptyTrash() {
        try {
            lock.lock();
            backend.emptyTrash();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void restoreTrash() {
        try {
            lock.lock();
            backend.restoreTrash();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.DeckID addOrUpdateDeckLegacy(@Nullable ByteString deck, boolean preserveUsnAndMtime) {
        try {
            lock.lock();
            return backend.addOrUpdateDeckLegacy(deck, preserveUsnAndMtime);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.DeckTreeNode deckTree(long now, long topDeckId) {
        try {
            lock.lock();
            return backend.deckTree(now, topDeckId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json deckTreeLegacy() {
        try {
            lock.lock();
            return backend.deckTreeLegacy();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json getAllDecksLegacy() {
        try {
            lock.lock();
            return backend.getAllDecksLegacy();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.DeckID getDeckIDByName(String name) {
        try {
            lock.lock();
            return backend.getDeckIDByName(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json getDeckLegacy(long did) {
        try {
            lock.lock();
            return backend.getDeckLegacy(did);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.DeckNames getDeckNames(boolean skipEmptyDefault, boolean includeFiltered) {
        try {
            lock.lock();
            return backend.getDeckNames(skipEmptyDefault, includeFiltered);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json newDeckLegacy(boolean args) {
        try {
            lock.lock();
            return backend.newDeckLegacy(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeDeck(long args) {
        try {
            lock.lock();
            backend.removeDeck(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.DeckConfigID addOrUpdateDeckConfigLegacy(@Nullable ByteString config, boolean preserveUsnAndMtime) {
        try {
            lock.lock();
            return backend.addOrUpdateDeckConfigLegacy(config, preserveUsnAndMtime);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json allDeckConfigLegacy() {
        try {
            lock.lock();
            return backend.allDeckConfigLegacy();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json getDeckConfigLegacy(long dConfId) {
        try {
            lock.lock();
            return backend.getDeckConfigLegacy(dConfId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json newDeckConfigLegacy() {
        try {
            lock.lock();
            return backend.newDeckConfigLegacy();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeDeckConfig(long dConfId) {
        try {
            lock.lock();
            backend.removeDeckConfig(dConfId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Card getCard(long cid) {
        try {
            lock.lock();
            return backend.getCard(cid);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateCard(Backend.Card args) {
        try {
            lock.lock();
            backend.updateCard(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.CardID addCard(Backend.Card args) {
        try {
            lock.lock();
            return backend.addCard(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeCards(List<Long> cardIds) {
        try {
            lock.lock();
            backend.removeCards(cardIds);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Note newNote(long noteTypidId) {
        try {
            lock.lock();
            return backend.newNote(noteTypidId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.NoteID addNote(@Nullable Backend.Note note, long deckId) {
        try {
            lock.lock();
            return backend.addNote(note, deckId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateNote(Backend.Note args) {
        try {
            lock.lock();
            backend.updateNote(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Note getNote(long nid) {
        try {
            lock.lock();
            return backend.getNote(nid);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeNotes(List<Long> noteIds, List<Long> cardIds) {
        try {
            lock.lock();
            backend.removeNotes(noteIds, cardIds);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.UInt32 addNoteTags(List<Long> nids, @Nullable String tags) {
        try {
            lock.lock();
            return backend.addNoteTags(nids, tags);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.UInt32 updateNoteTags(List<Long> nids, @Nullable String tags, @Nullable String replacement, boolean regex) {
        try {
            lock.lock();
            return backend.updateNoteTags(nids, tags, replacement, regex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.ClozeNumbersInNoteOut clozeNumbersInNote(Backend.Note args) {
        try {
            lock.lock();
            return backend.clozeNumbersInNote(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void afterNoteUpdates(List<Long> nids, boolean markNotesModified, boolean generateCards) {
        try {
            lock.lock();
            backend.afterNoteUpdates(nids, markNotesModified, generateCards);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.FieldNamesForNotesOut fieldNamesForNotes(List<Long> nids) {
        try {
            lock.lock();
            return backend.fieldNamesForNotes(nids);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.NoteIsDuplicateOrEmptyOut noteIsDuplicateOrEmpty(Backend.Note args) {
        try {
            lock.lock();
            return backend.noteIsDuplicateOrEmpty(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeID addOrUpdateNotetype(@Nullable ByteString json, boolean preserveUsnAndMtime) {
        try {
            lock.lock();
            return backend.addOrUpdateNotetype(json, preserveUsnAndMtime);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json getStockNotetypeLegacy(@Nullable Backend.StockNoteType kind) {
        try {
            lock.lock();
            return backend.getStockNotetypeLegacy(kind);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json getNotetypeLegacy(long noteTypeId) {
        try {
            lock.lock();
            return backend.getNotetypeLegacy(noteTypeId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeNames getNotetypeNames() {
        try {
            lock.lock();
            return backend.getNotetypeNames();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeUseCounts getNotetypeNamesAndCounts() {
        try {
            lock.lock();
            return backend.getNotetypeNamesAndCounts();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.NoteTypeID getNotetypeIDByName(String name) {
        try {
            lock.lock();
            return backend.getNotetypeIDByName(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeNotetype(long noteTypeId) {
        try {
            lock.lock();
            backend.removeNotetype(noteTypeId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void openCollection(@Nullable String collectionPath, @Nullable String mediaFolderPath, @Nullable String mediaDbPath, @Nullable String logPath) {
        try {
            lock.lock();
            backend.openCollection(collectionPath, mediaFolderPath, mediaDbPath, logPath);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void closeCollection(boolean downgradeToSchema11) {
        try {
            lock.lock();
            backend.closeCollection(downgradeToSchema11);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.CheckDatabaseOut checkDatabase() {
        try {
            lock.lock();
            return backend.checkDatabase();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void beforeUpload() {
        try {
            lock.lock();
            backend.beforeUpload();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.String translateString(Backend.TranslateStringIn args) {
        try {
            lock.lock();
            return backend.translateString(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.String formatTimespan(float seconds, @Nullable Backend.FormatTimespanIn.Context context) {
        try {
            lock.lock();
            return backend.formatTimespan(seconds, context);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json i18nResources() {
        try {
            lock.lock();
            return backend.i18nResources();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Bool registerTags(@Nullable String tags, boolean preserveUsn, int usn, boolean clearFirst) {
        try {
            lock.lock();
            return backend.registerTags(tags, preserveUsn, usn, clearFirst);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.AllTagsOut allTags() {
        try {
            lock.lock();
            return backend.allTags();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json getConfigJson(String args) {
        try {
            lock.lock();
            return backend.getConfigJson(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setConfigJson(@Nullable String key, @Nullable ByteString valueJson) {
        try {
            lock.lock();
            backend.setConfigJson(key, valueJson);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeConfig(String args) {
        try {
            lock.lock();
            backend.removeConfig(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setAllConfig(Backend.Json args) {
        try {
            lock.lock();
            backend.setAllConfig(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Json getAllConfig() {
        try {
            lock.lock();
            return backend.getAllConfig();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Backend.Preferences getPreferences() {
        try {
            lock.lock();
            return backend.getPreferences();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setPreferences(Backend.Preferences args) {
        try {
            lock.lock();
            backend.setPreferences(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void openAnkiDroidCollection(Backend.OpenCollectionIn args) {
        try {
            lock.lock();
            backend.openAnkiDroidCollection(args);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isOpen() {
        try {
            lock.lock();
            return backend.isOpen();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            lock.lock();
            backend.close();
        } finally {
            lock.unlock();
        }
    }
}
