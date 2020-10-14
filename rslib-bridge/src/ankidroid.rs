use anki::backend::{init_backend, anki_error_to_proto_error, Backend as RustBackend};

pub(crate) struct AnkiDroidBackend {
   pub backend: RustBackend,
}
impl AnkiDroidBackend {
    pub fn new(backend: RustBackend) -> AnkiDroidBackend {
        AnkiDroidBackend { backend }
    }
}