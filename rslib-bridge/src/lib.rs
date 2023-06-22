#![allow(clippy::missing_safety_doc)]

use std::{
    any::Any,
    panic::{catch_unwind, AssertUnwindSafe},
};

use anki::{
    backend::{init_backend, Backend},
    error::Result,
};
use anki_proto::{
    backend::{backend_error, BackendError},
    generic::Int64,
};
use jni::{
    objects::{JClass, JObject},
    sys::{jarray, jbyteArray, jint, jlong},
    JNIEnv,
};
use prost::Message;

mod logging;

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_openBackend(
    env: JNIEnv,
    _: JClass,
    args: jbyteArray,
) -> jarray {
    logging::setup_logging();

    let input = env.convert_byte_array(args).unwrap();
    let result = init_backend(&input)
        .map(|backend| {
            let backend_ptr = Box::into_raw(Box::new(backend)) as i64;
            Int64 { val: backend_ptr }.encode_to_vec()
        })
        .map_err(|err| {
            BackendError {
                message: err,
                kind: backend_error::Kind::InvalidInput as i32,
                ..Default::default()
            }
            .encode_to_vec()
        });
    pack_result(result, &env)
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_closeBackend(
    _env: JNIEnv,
    _: JClass,
    args: jlong,
) {
    let raw = args as *mut Backend;
    drop(Box::from_raw(raw));
}

#[no_mangle]
pub unsafe extern "C" fn Java_net_ankiweb_rsdroid_NativeMethods_runMethodRaw(
    env: JNIEnv,
    _: JClass,
    backend_ptr: jlong,
    service: jint,
    method: jint,
    args: jbyteArray,
) -> jbyteArray {
    let backend = to_backend(backend_ptr);
    let service: u32 = service as u32;
    let method: u32 = method as u32;
    let input = env.convert_byte_array(args).unwrap();
    with_packed_result(&env, || backend.run_service_method(service, method, &input))
}

unsafe fn to_backend(ptr: jlong) -> &'static mut Backend {
    &mut *(ptr as *mut Backend)
}

macro_rules! null_on_error {
    ($arg:expr) => {
        match ($arg) {
            Ok(ok) => ok,
            Err(_) => return JObject::null().into_inner(),
        }
    };
}

/// Run provided func and pack result into jarray. Catches panics.
fn with_packed_result<F>(env: &JNIEnv, func: F) -> jarray
where
    F: FnOnce() -> Result<Vec<u8>, Vec<u8>>,
{
    let result = match catch_unwind(AssertUnwindSafe(func)) {
        Ok(result) => result,
        Err(panic) => Err(panic_to_backend_error(panic).encode_to_vec()),
    };
    pack_result(result, env)
}

/// Pack Result<okBytes, errBytes> into jArray[okBytes, null] | jarray[null, errBytes] | null
/// Null returned in case conversion to a jbyteArray fails (eg low mem),
fn pack_result(result: Result<Vec<u8>, Vec<u8>>, env: &JNIEnv) -> jarray {
    // create the outer 2-element array
    let byte_array_class = null_on_error!(env.find_class("[B"));
    let outer_array = null_on_error!(env.new_object_array(2, byte_array_class, JObject::null()));
    // pack return/error into bytearrays
    let elems = match result {
        Ok(msg) => (
            null_on_error!(env.byte_array_from_slice(&msg)),
            JObject::null().into_inner(),
        ),
        Err(err) => (
            JObject::null().into_inner(),
            null_on_error!(env.byte_array_from_slice(&err)),
        ),
    };
    // pack into outer
    null_on_error!(env.set_object_array_element(outer_array, 0, elems.0));
    null_on_error!(env.set_object_array_element(outer_array, 1, elems.1));

    outer_array
}

fn panic_to_backend_error(panic: Box<dyn Any + Send>) -> BackendError {
    let message = match panic.downcast_ref::<&'static str>() {
        Some(msg) => *msg,
        None => match panic.downcast_ref::<String>() {
            Some(msg) => msg.as_str(),
            None => "unknown panic",
        },
    }
    .to_string();
    BackendError {
        kind: backend_error::Kind::AnkidroidPanicError as i32,
        message,
        ..Default::default()
    }
}
