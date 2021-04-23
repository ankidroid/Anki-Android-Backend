/*
While porting: open_collection upgrades the collection in a non-backwards compatible manner.

We should be able to perform a database migration to V13 as we're past SQLite 3.9



 */

use std::path::{PathBuf, Path};
use anki::i18n::I18n;
use anki::log::Logger;
use anki::collection::{Collection, CollectionState};
use anki::err::{DBErrorKind, AnkiError};
use anki::storage::SqliteStorage;
use anki::backend::{Backend, anki_error_to_proto_error};
use anki::{backend_proto as pb, log};
use anki::config;

use rusqlite::{params, NO_PARAMS};


// allows encode/decode
use prost::Message;

use crate::ankidroid::AnkiDroidBackend;

#[derive(Deserialize)]
struct DBArgs {
    sql: String,
    args: Vec<anki::backend::dbproxy::SqlValue>
}
use serde_derive::Deserialize;
use anki::sched::cutoff::v1_creation_date;

pub type AnkiResult<T> = std::result::Result<T, AnkiError>;

pub fn open_collection_no_update<P: Into<PathBuf>>(
    path: P,
    media_folder: P,
    media_db: P,
    server: bool,
    i18n: I18n,
    log: Logger,
    min_schama: u8,
    max_schema: u8
) -> AnkiResult<Collection> {
    let col_path = path.into();
    let storage = open_or_create_no_update(&col_path, &i18n, server, min_schama, max_schema)?;

    let col = Collection {
        storage,
        col_path,
        media_folder: media_folder.into(),
        media_db: media_db.into(),
        i18n,
        log,
        server,
        state: CollectionState::default(),
    };

    Ok(col)
}

pub fn open_collection_ankidroid(backend : &Backend, input: pb::OpenCollectionIn) -> AnkiResult<pb::Empty> {
    let mut col = backend.col.lock().unwrap();
    if col.is_some() {
        return Err(AnkiError::CollectionAlreadyOpen);
    }

    let mut path = input.collection_path.clone();
    path.push_str(".log");

    let log_path = match input.log_path.as_str() {
        "" => None,
        path => Some(path),
    };
    let logger = log::default_logger(log_path)?;

    const SCHEMA_ANKIDROID_VERSION: u8 = 11;

    let new_col = open_collection_no_update(
        input.collection_path,
        input.media_folder_path,
        input.media_db_path,
        false,
        backend.i18n.clone(),
        logger,
        SCHEMA_ANKIDROID_VERSION,
        SCHEMA_ANKIDROID_VERSION
    )?;

    *col = Some(new_col);

    Ok(().into())
}

pub fn get_open_collection_for_downgrade(collection_path: String) -> AnkiResult<Collection> {
    let logger = log::default_logger(None)?;

    const SCHEMA_ANKIDROID_MAX_VERSION: u8 = 16;

    open_collection_no_update(
        collection_path,
        "".to_owned(),
        "".to_owned(),
        false,
        I18n::new(&[""], "", logger.clone()),
        logger,
        SCHEMA_ANKIDROID_MAX_VERSION,
        SCHEMA_ANKIDROID_MAX_VERSION
    )
}


pub(crate) fn open_or_create_no_update(path: &Path, _i18n: &I18n, _server: bool, current_schema: u8, max_schema : u8) -> AnkiResult<SqliteStorage> {
    let db = anki::storage::sqlite::open_or_create_collection_db(path)?;

    let (create, ver) = anki::storage::sqlite::schema_version(&db)?;
    // Requery uses "TRUNCATE" by default if WAL is not enabled.
    // We copy this behaviour here. See https://github.com/ankidroid/Anki-Android/pull/7977 for
    // analysis. We may be able to enable WAL at a later time.
    db.pragma_update(None, "journal_mode", &"TRUNCATE")?;

    let err = match ver {
        v if v < current_schema => Some(DBErrorKind::FileTooOld),
        v if v > max_schema => Some(DBErrorKind::FileTooNew),
        _ => None,
    };
    if let Some(kind) = err {
        return Err(AnkiError::DBError {
            info: "Got Schema".to_owned() + &*ver.to_string(),
            kind,
        });
    }

    if !create {
        return Ok(SqliteStorage { db })
    }


    db.execute("begin exclusive", NO_PARAMS)?;
    db.execute_batch(include_str!("../../anki/rslib/src/storage/schema11.sql"))?;
    // start at schema 11, then upgrade below
    let crt = v1_creation_date();
    db.execute(
        "update col set crt=?, scm=?, ver=?, conf=?",
        params![
                crt,
                crt * 1000,
                current_schema,
                &config::schema11_config_as_string()
            ],
    )?;

    let storage = SqliteStorage { db };

    // storage.add_default_deck_config(i18n)?;
    // storage.add_default_deck(i18n)?;
    // storage.add_stock_notetypes(i18n)?;

    storage.commit_trx()?;


    Ok(storage)
}


fn get_args(in_bytes: &Vec<u8>) -> AnkiResult<DBArgs> {
    let ret : DBArgs = serde_json::from_slice(&in_bytes)?;
    Ok(ret)
}


pub(crate) fn insert_for_id(in_bytes: &Vec<u8>, backend: &mut AnkiDroidBackend) -> Result<Vec<u8>, Vec<u8>> {

    let req = get_args(&in_bytes)
        .and_then(|req| {
            backend.backend.with_col(|col| {
                col.storage.db.execute(&req.sql, req.args)?;
                Ok(col.storage.db.last_insert_rowid())
            })
        })
        .map_err(|err| {
            let backend_err = anki_error_to_proto_error(err, &backend.backend.i18n);
            let mut bytes = Vec::new();
            backend_err.encode(&mut bytes).unwrap();
            bytes
        })?;

    let mut out_bytes : Vec<u8> = Vec::new();
    pb::Int64 { val: req }.encode(&mut out_bytes);
    Ok(out_bytes)
}

pub(crate) fn query_for_affected(in_bytes: &Vec<u8>, backend: &mut AnkiDroidBackend) -> Result<Vec<u8>, Vec<u8>> {

    let req = get_args(&in_bytes)
        .and_then(|req| {
            backend.backend.with_col(|col| {
                Ok(col.storage.db.execute(&req.sql, req.args)?)
            })
        })
        .map_err(|err| {
            let backend_err = anki_error_to_proto_error(err, &backend.backend.i18n);
            let mut bytes = Vec::new();
            backend_err.encode(&mut bytes).unwrap();
            bytes
        })?;

    let mut out_bytes : Vec<u8> = Vec::new();
    let as_i32 : i32 = req as i32;
    pb::Int32 { val: as_i32 }.encode(&mut out_bytes);
    Ok(out_bytes)
}
