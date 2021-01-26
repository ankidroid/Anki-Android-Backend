use std::collections::HashMap;
use std::sync::Mutex;

use anki::backend_proto::{DbResponse, DbResult};

// Handles global variables for DbResponse streaming
// COULD_BE_BETTER: Consider converting this into an object returned from the function
// (accessible via JNI) - more idiomatic, but probably less clean than this.

// COULD_BE_BETTER: make DBResponse.DbResult non-optional

use i64 as backend_pointer;
use i64 as dbresponse_pointer;

use itertools::Itertools;

lazy_static! {
    // backend_pointer => Map<sequenceNumber, DbResponse pointer>
    static ref HASHMAP: Mutex<HashMap<backend_pointer, HashMap<i32, i64>>> = {
        Mutex::new(HashMap::new())
    };
}

pub(crate) unsafe fn flush_cache(ptr : &backend_pointer, sequence_number : i32) {
    let mut map = HASHMAP.lock().unwrap();
    let entries = map.get_mut(ptr);
    match entries {
        Some(seq_to_ptr) => {
            let entry = seq_to_ptr.remove_entry(&sequence_number);
            match entry {
                Some(ptr) => {
                    let raw = ptr.1 as *mut DbResponse;
                    Box::from_raw(raw);
                }
                None => { }
            }
        }
        None => { }
    }
}


pub(crate) unsafe fn flush_all(ptr: &backend_pointer) {
    let mut map = HASHMAP.lock().unwrap();

    // clear the map
    let entries = map.remove_entry(ptr);

    match entries {
        Some(seq_to_ptr_map) => {
            // then clear each value
            for val in seq_to_ptr_map.1.values() {
                let raw = (*val) as *mut DbResponse;
                Box::from_raw(raw);
            }
        }
        None => { }
    }
}

pub(crate) fn active_sequences(ptr : backend_pointer) -> Vec<i32> {
    let mut map = HASHMAP.lock().unwrap();

    match map.get_mut(&ptr) {
        Some(x) => {
            let keys = x.keys();
            keys.into_iter().map(|i| *i).collect_vec()
        },
        None => {
            Vec::new()
        }
    }
}

pub(crate) fn insert_cache(ptr : backend_pointer, result : DbResponse) {
    let mut map = HASHMAP.lock().unwrap();

    match map.get_mut(&ptr) {
        Some(_) => { },
        None => {
            let map2 : HashMap<i32, i64> = HashMap::new();
            map.insert(ptr, map2);
        }
    };

    let out_hash_map = map.get_mut(&ptr).unwrap();

    out_hash_map.insert(result.sequence_number,  Box::into_raw(Box::new(result)) as dbresponse_pointer);
}

pub(crate) unsafe fn get_next(ptr : backend_pointer, sequence_number : i32, offset : usize, to_take : usize) -> Option<DbResponse> {
    let map = HASHMAP.lock().unwrap();

    let result_map = map.get(&ptr)?;

    let backend_ptr = *result_map.get(&sequence_number)?;

    let current_result = &mut *(backend_ptr as *mut DbResponse);

    let result = DbResult { rows: current_result.result.as_ref().unwrap_or(&DbResult { rows: Vec::new()} ).rows.iter().skip(offset).take(to_take).cloned().collect() };

    if result.rows.is_empty() {
        flush_cache(&ptr, sequence_number)
    }

    let trimmed_result = DbResponse { result: Some(result), sequence_number: current_result.sequence_number, row_count: current_result.row_count };

    Some(trimmed_result)
}

static mut SEQUENCE_NUMBER: i32 = 0;

pub(crate) unsafe fn next_sequence_number() -> i32 {
    SEQUENCE_NUMBER = SEQUENCE_NUMBER + 1;
    SEQUENCE_NUMBER
}