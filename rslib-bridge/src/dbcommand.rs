use std::collections::HashMap;
use std::sync::Mutex;

use anki::backend_proto::{DbResponse, DbResult};

// Handles global variables for DbResponse streaming
// COULD_BE_BETTER: Consider converting this into an object returned from the function
// (accessible via JNI) - more idiomatic, but probably less clean than this.

// COULD_BE_BETTER: make DBResponse.DbResult non-optional

use i64 as backend_pointer;
use i64 as dbresponse_pointer;

lazy_static! {
    // backend_pointer => DbResponse pointer
    static ref HASHMAP: Mutex<HashMap<backend_pointer, i64>> = {
        Mutex::new(HashMap::new())
    };
}

pub(crate) unsafe fn flush_cache(ptr : &backend_pointer) {
    let mut map = HASHMAP.lock().unwrap();
    let entry = map.remove_entry(ptr);
    match entry {
        Some(x) => {
            let raw = x.1 as *mut DbResponse;
            Box::from_raw(raw);
        },
        None => { }
    }

}

pub(crate) fn insert_cache(ptr : backend_pointer, result : DbResponse) {
    let mut map = HASHMAP.lock().unwrap();
    map.insert(ptr,  Box::into_raw(Box::new(result)) as dbresponse_pointer);
}

pub(crate) unsafe fn get_next(ptr : backend_pointer, offset : usize, to_take : usize ) -> Option<DbResponse> {
    let map = HASHMAP.lock().unwrap();

    let result_ptr = *map.get(&ptr)?;

    let current_result = &mut *(result_ptr as *mut DbResponse);

    let result = DbResult { rows: current_result.result.as_ref().unwrap_or(&DbResult { rows: Vec::new()} ).rows.iter().skip(offset).take(to_take).cloned().collect() };

    if result.rows.is_empty() {
        flush_cache(&ptr)
    }

    let trimmed_result = DbResponse { result: Some(result), sequence_number: current_result.sequence_number, row_count: current_result.row_count };

    Some(trimmed_result)
}

static mut SEQUENCE_NUMBER: i32 = 0;

pub(crate) unsafe fn next_sequence_number() -> i32 {
    SEQUENCE_NUMBER = SEQUENCE_NUMBER + 1;
    SEQUENCE_NUMBER
}