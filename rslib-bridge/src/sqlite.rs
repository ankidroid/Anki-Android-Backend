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
use anki::media::database::open_or_create;
use anki::backend::Backend;
use anki::{backend_proto as pb, log};

pub type Result<T> = std::result::Result<T, AnkiError>;

pub fn open_collection_no_update<P: Into<PathBuf>>(
    path: P,
    media_folder: P,
    media_db: P,
    server: bool,
    i18n: I18n,
    log: Logger,
) -> Result<Collection> {
    let col_path = path.into();
    let storage = open_or_create_no_update(&col_path, &i18n, server)?;

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

pub fn open_collection_ankidroid(backend : &Backend, input: pb::OpenCollectionIn) -> Result<pb::Empty> {
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

    let new_col = open_collection_no_update(
        input.collection_path,
        input.media_folder_path,
        input.media_db_path,
        false,
        backend.i18n.clone(),
        logger,
    )?;

    *col = Some(new_col);

    Ok(().into())
}


pub(crate) fn open_or_create_no_update(path: &Path, i18n: &I18n, server: bool) -> Result<SqliteStorage> {
    let db = anki::storage::sqlite::open_or_create_collection_db(path)?;
    let (create, ver) = anki::storage::sqlite::schema_version(&db)?;

    const SCHEMA_ANKIDROID_VERSION: u8 = 11;
    let err = match ver {
        v if v < SCHEMA_ANKIDROID_VERSION => Some(DBErrorKind::FileTooOld),
        v if v > SCHEMA_ANKIDROID_VERSION => Some(DBErrorKind::FileTooNew),
        _ => None,
    };
    if let Some(kind) = err {
        return Err(AnkiError::DBError {
            info: "".to_string(),
            kind,
        });
    }
    if create {
        return Err(AnkiError::DBError {
            info: "".to_string(),
            kind: DBErrorKind::Other,
        });
    }

    let storage = SqliteStorage { db };

    Ok(storage)
}
