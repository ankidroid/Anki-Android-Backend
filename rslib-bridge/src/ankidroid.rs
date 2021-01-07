use anki::backend::Backend as RustBackend;

pub(crate) struct AnkiDroidBackend {
   pub backend: RustBackend,
}

impl AnkiDroidBackend {
    pub fn new(backend: RustBackend) -> AnkiDroidBackend {
        AnkiDroidBackend { backend }
    }
}