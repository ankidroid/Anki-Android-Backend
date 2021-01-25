#[macro_use]
extern crate lazy_static;

use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jbyteArray, jint, jlong, jobjectArray, jarray};

use anki::backend_proto as pb;
use pb::OpenCollectionIn;
use crate::sqlite::{open_collection_ankidroid, insert_for_id, query_for_affected};

use anki::backend::{init_backend, anki_error_to_proto_error, Backend};
use crate::ankidroid::AnkiDroidBackend;

// allows encode/decode
use prost::Message;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::any::Any;
use anki::err::AnkiError;
use anki::i18n::I18n;

use anki::backend_proto::{DbResult, DbResponse};
use core::result;
use crate::backend_proto::{DroidBackendService, LocalMinutesWestIn, SchedTimingTodayIn, SchedTimingTodayOut2, LocalMinutesWestOut};
use anki::sched::cutoff;
use anki::timestamp::TimestampSecs;
use anki::sched::cutoff::SchedTimingToday;

mod dbcommand;
mod sqlite;
mod ankidroid;
mod backend_proto;

// TODO: Use a macro to handle panics to reduce code duplication

// FUTURE_EXTENSION: Allow DB_COMMAND_NUM_ROWS to be variable to allow tuning of memory usage
// Maybe also change this to a per-MB value if it's easy to stream-serialise to protobuf until a
// memory limit is hit.

// MAINTENANCE: This must manually be kept in sync with the Java
const DB_COMMAND_NUM_ROWS: usize = 1000;

impl From<SchedTimingToday> for SchedTimingTodayOut2 {
    fn from(data: SchedTimingToday) -> Self {
        SchedTimingTodayOut2 {
            days_elapsed: data.days_elapsed,
            next_day_at: data.next_day_at
        }
    }
}

impl backend_proto::DroidBackendService for Backend {
    fn sched_timing_today_legacy(&self, input: SchedTimingTodayIn) -> Result<SchedTimingTodayOut2, AnkiError> {
        let result = cutoff::sched_timing_today(
            TimestampSecs::from(input.created_secs),
            TimestampSecs::from(input.now_secs),
            Some(input.created_mins_west),
            Some(input.now_mins_west),
            Some(input.rollover_hour as u8)
        );
        Ok(SchedTimingTodayOut2::from(result))
    }

    fn local_minutes_west_legacy(&self, input : LocalMinutesWestIn) -> Result<LocalMinutesWestOut, AnkiError> {
        let out = LocalMinutesWestOut {
            mins_west: cutoff::local_minutes_west_for_stamp(input.collection_creation_time)
        };
        Ok(out)
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_openBackend(
    env: JNIEnv,
    _: JClass,
    args: jbyteArray) -> jlong {
    // TODO: This does not handle panics - we currently return a pointer - convert to protobuf

    let rust_backend = init_backend(env.convert_byte_array(args).unwrap().as_slice()).unwrap();
    let backend = AnkiDroidBackend::new(rust_backend);


    Box::into_raw(Box::new(backend)) as jlong
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_closeBackend(
    _env: JNIEnv,
    _: JClass,
    args: jlong) -> jlong {
    // TODO: This does not handle panics - we currently return a pointer - convert to protobuf

    let raw = args as *mut AnkiDroidBackend;
    Box::from_raw(raw);

    1
}


#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_openCollection(
    env: JNIEnv,
    _: JClass,
    backend_ptr: jlong,
    args: jbyteArray) -> jbyteArray {

    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let in_bytes = env.convert_byte_array(args).unwrap();

        let command = OpenCollectionIn::decode(in_bytes.as_slice()).unwrap();

        let ret = open_collection_ankidroid(&backend.backend,command).and_then(|empty| {
            let mut out_bytes = Vec::new();
            empty.encode(&mut out_bytes)?;
            Ok(out_bytes)
        }).map_err(|err| {
            let backend_err = anki_error_to_proto_error(err, &backend.backend.i18n);
            let mut bytes = Vec::new();
            backend_err.encode(&mut bytes).unwrap();
            bytes
        });

        match ret {
            Ok(_s) => env.byte_array_from_slice(_s.as_slice()).unwrap(),
            Err(_err) => env.byte_array_from_slice(_err.as_slice()).unwrap(),
        }
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_command(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    command: jint,
    args: jbyteArray,
) -> jbyteArray {

    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let command: u32 = command as u32;
        let in_bytes = env.convert_byte_array(args).unwrap();

        // We might want to later change this to append a bit to the head of the stream to specify
        // the return type.

        match backend.backend.run_command_bytes(command, &in_bytes) {
            Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
            Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
        }
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}


#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_executeAnkiDroidCommand(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    command: jint,
    args: jbyteArray,
) -> jbyteArray {

    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let command: u32 = command as u32;
        let in_bytes = env.convert_byte_array(args).unwrap();

        match run_ad_command_bytes(backend, command, &in_bytes) {
            Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
            Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
        }
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}

pub(crate) fn run_ad_command_bytes(backend: &mut AnkiDroidBackend, method: u32, input: &[u8]) -> result::Result<Vec<u8>, Vec<u8>> {
    backend.backend.run_command_bytes2_inner_ad(method, input).map_err(|err| {
        let backend_err = anki_error_to_proto_error(err, &backend.backend.i18n);
        let mut bytes = Vec::new();
        backend_err.encode(&mut bytes).unwrap();
        bytes
    })
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_fullDatabaseCommand(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    input : jbyteArray
) -> jbyteArray {

    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let in_bytes =  env.convert_byte_array(input).unwrap();

        // Don't map the error for now
        let out_res = backend.backend.run_db_command_bytes(&in_bytes);

        match out_res {
            Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
            Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
        }
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_databaseGetNextResultPage(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    page: i32
) -> jbyteArray {

    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {

        let next_page = dbcommand::get_next(backend_ptr, (page as usize) * DB_COMMAND_NUM_ROWS as usize, DB_COMMAND_NUM_ROWS).unwrap();


        let mut out_bytes = Vec::new();
        next_page.encode(&mut out_bytes).unwrap();
        env.byte_array_from_slice(&out_bytes).unwrap()
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_cancelCurrentProtoQuery(
    _: JNIEnv,
    _: JClass,
    backend_ptr : jlong
) {
    dbcommand::flush_cache(&backend_ptr);
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_databaseCommand(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    input : jbyteArray
) -> jbyteArray {

    dbcommand::flush_cache(&backend_ptr);

    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let in_bytes =  env.convert_byte_array(input).unwrap();

        // Normally we'd want this as a Vec<u8>, but 
        let out_res = backend.backend.run_db_command_proto(&in_bytes);

        match out_res {
            Ok(db_result) => {
                let trimmed = trim_and_cache_remaining(backend_ptr, db_result, dbcommand::next_sequence_number());

                let mut out_bytes = Vec::new();
                trimmed.encode(&mut out_bytes).unwrap();
                env.byte_array_from_slice(&out_bytes).unwrap()
            }
            Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
        }
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}

// We define these here to avoid the need for a union of positive return values.
#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_sqlInsertForId(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    input : jbyteArray
) -> jbyteArray {


    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let in_bytes = env.convert_byte_array(input).unwrap();

        let out_res = insert_for_id(&in_bytes, backend);

        match out_res {
            Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
            Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
        }
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }


}

// We define these here to avoid the need for a union of positive return values.
#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_sqlQueryForAffected(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    input : jbyteArray
) -> jbyteArray {

    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let in_bytes = env.convert_byte_array(input).unwrap();

        let out_res = query_for_affected(&in_bytes, backend);

        match out_res {
            Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
            Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
        }
    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}


// We define these here to avoid the need for a union of positive return values.
#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_getColumnNames(
    env: JNIEnv,
    _: JClass,
    backend_ptr : jlong,
    input : JString
) -> jarray {
    let backend = to_backend(backend_ptr);

    let result = catch_unwind(AssertUnwindSafe(|| {

        let ret = backend.backend.with_col(|col| {

            let str : String = env.get_string(input).expect("Couldn't get java string!").into();
            let stmt = col.storage.db.prepare(&str)?;
            let names = stmt.column_names();

            let array: jobjectArray = env
                .new_object_array(
                    names.len() as i32,
                    env.find_class("java/lang/String").unwrap(),
                    *env.new_string("").unwrap(),
                )
                .unwrap();


            for (i, name) in names.iter().enumerate() {
                env.set_object_array_element(
                    array,
                    i as i32,
                    *env.new_string(&name)
                        .unwrap()
                        .to_owned(),
                )
                    .expect("Could not perform set_object_array_element on array element.");
            }
            Ok(array)
        });

        match ret {
            Ok(_s) => _s,
            // This may be incorrect
            Err(_) => *JObject::null()
        }

    }));

    match result {
        Ok(_s) => _s,
        Err(err) => panic_to_bytes(env,err.as_ref(), &backend.backend.i18n)
    }
}

unsafe fn to_backend(ptr: jlong) -> &'static mut AnkiDroidBackend {
    // TODO: This is not unwindable, but we can't hard-crash as Android won't send it to ACRA
    // As long as the FatalError is sent below, we're OK
    &mut *(ptr as *mut AnkiDroidBackend)
}

fn panic_to_bytes(env: JNIEnv , s: &(dyn Any + Send), i18n: &I18n) -> jbyteArray {
    let ret = panic_to_anki_error(s);
    let backend_err = anki_error_to_proto_error(ret, i18n);
    let mut bytes = Vec::new();
    backend_err.encode(&mut bytes).unwrap();
    env.byte_array_from_slice(bytes.as_slice()).unwrap()
}

/**
Store the data in the cache if there's more than DB_COMMAND_NUM_ROWS.<br/>
Returns: The data capped to DB_COMMAND_NUM_ROWS
*/
fn trim_and_cache_remaining(backend_ptr: i64, values: DbResult, sequence_number: i32) -> DbResponse {
    let row_count = values.rows.len() as i32;
    if values.rows.len() > DB_COMMAND_NUM_ROWS {
        let result = values.rows.iter().take(DB_COMMAND_NUM_ROWS).cloned().collect();
        let to_store = DbResponse { result: Some(values), sequence_number, row_count };
        dbcommand::insert_cache(backend_ptr, to_store);

        DbResponse { result: Some(DbResult { rows: result }), sequence_number, row_count }
    } else {
        DbResponse { result: Some(values), sequence_number, row_count }
    }
}

fn panic_to_anki_error(s: &(dyn Any + Send)) -> AnkiError {
    if let Some(msg) = s.downcast_ref::<String>(){
        AnkiError::FatalError {
            info: msg.to_string()
        }
    } else {
        AnkiError::FatalError {
            info: "panic with no info".to_string()
        }
    }
}

