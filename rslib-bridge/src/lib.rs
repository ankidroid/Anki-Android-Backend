use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jbyteArray, jint, jlong, jobjectArray, jarray};

use anki::backend_proto as pb;
use pb::OpenCollectionIn;
use crate::sqlite::{open_collection_ankidroid, insert_for_id, query_for_affected};

use anki::backend::{init_backend, anki_error_to_proto_error};
use crate::ankidroid::AnkiDroidBackend;

// allows encode/decode
use prost::Message;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::any::Any;
use anki::err::AnkiError;
use anki::i18n::I18n;

mod sqlite;
mod ankidroid;

// TODO: Use a macro to handle panics to reduce code duplication

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

