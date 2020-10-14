use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jbyteArray, jint, jlong, jstring, jobjectArray, jarray};

use anki::backend_proto as pb;
use pb::OpenCollectionIn;
use crate::sqlite::{open_collection_ankidroid, insert_for_id, query_for_affected};

use anki::backend::{init_backend, anki_error_to_proto_error};
use crate::ankidroid::AnkiDroidBackend;

// allows encode/decode
use prost::Message;

mod mmap64;
mod sqlite;
mod ankidroid;

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_openBackend(
    env: JNIEnv,
    _: JClass,
    args: jbyteArray) -> jlong {

    let rust_backend = init_backend(env.convert_byte_array(args).unwrap().as_slice()).unwrap();
    let backend = AnkiDroidBackend::new(rust_backend);


    Box::into_raw(Box::new(backend)) as jlong
}




#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_openCollection(
    env: JNIEnv,
    _: JClass,
    backend : jlong,
    args: jbyteArray) -> jbyteArray {

    let backend = &mut *(backend as *mut AnkiDroidBackend);
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
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_command(
    env: JNIEnv,
    _: JClass,
    backend : jlong,
    command: jint,
    args: jbyteArray,
) -> jbyteArray {

    let backend = &mut *(backend as *mut AnkiDroidBackend);
    let command: u32 = command as u32;
    let in_bytes = env.convert_byte_array(args).unwrap();

    // We might want to later change this to append a bit to the head of the stream to specify
    // the return type.
    match backend.backend.run_command_bytes(command, &in_bytes) {
        Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
        Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_fullDatabaseCommand(
    env: JNIEnv,
    _: JClass,
    backend : jlong,
    input : jbyteArray
) -> jbyteArray {
    let backend = &mut *(backend as *mut AnkiDroidBackend);
    let in_bytes =  env.convert_byte_array(input).unwrap();

    // Don't map the error for now
    let out_res = backend.backend.run_db_command_bytes(&in_bytes);

    match out_res {
        Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
        Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
    }
}

// We define these here to avoid the need for a union of positive return values.
#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_sqlInsertForId(
    env: JNIEnv,
    _: JClass,
    backend : jlong,
    input : jbyteArray
) -> jbyteArray {
    let backend = &mut *(backend as *mut AnkiDroidBackend);
    let in_bytes = env.convert_byte_array(input).unwrap();

    let out_res = insert_for_id(&in_bytes, backend);

    match out_res {
        Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
        Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
    }
}

// We define these here to avoid the need for a union of positive return values.
#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_sqlQueryForAffected(
    env: JNIEnv,
    _: JClass,
    backend : jlong,
    input : jbyteArray
) -> jbyteArray {
    let backend = &mut *(backend as *mut AnkiDroidBackend);
    let in_bytes = env.convert_byte_array(input).unwrap();

    let out_res = query_for_affected(&in_bytes, backend);

    match out_res {
        Ok(_s) => env.byte_array_from_slice(&_s).unwrap(),
        Err(_err) => env.byte_array_from_slice(&_err).unwrap(),
    }
}


// We define these here to avoid the need for a union of positive return values.
#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_getColumnNames(
    env: JNIEnv,
    _: JClass,
    backend : jlong,
    input : JString
) -> jarray {
    let backend = &mut *(backend as *mut AnkiDroidBackend);

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
}

