//! A simple adaptor that takes log messages from the backend and sends them to
//! the Android logs.

use android_logger::Config;
use log::Level;
use slog::*;
use std::{fmt, result};

pub struct AndroidSerializer;

impl Serializer for AndroidSerializer {
    fn emit_arguments(&mut self, key: Key, val: &fmt::Arguments<'_>) -> Result {
        log::debug!("{}={}", key, val);
        Ok(())
    }
}

pub struct AndroidDrain;

impl Drain for AndroidDrain {
    type Ok = ();
    type Err = ();

    fn log(
        &self,
        record: &Record<'_>,
        values: &OwnedKVList,
    ) -> result::Result<Self::Ok, Self::Err> {
        log::debug!("{}", record.msg());

        record
            .kv()
            .serialize(record, &mut AndroidSerializer)
            .unwrap();
        values.serialize(record, &mut AndroidSerializer).unwrap();

        Ok(())
    }
}

pub(crate) fn setup_logging() -> Logger {
    android_logger::init_once(Config::default().with_min_level(Level::Debug));
    Logger::root(AndroidDrain {}.fuse(), slog_o!())
}
