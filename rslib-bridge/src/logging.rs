//! A simple adaptor that takes log messages from the backend and sends them to
//! the Android logs.
//! It also captures stdout/stderr output, and feeds it to logcat, to make it
//! easier to debug issues with dbg!()/println!()

use android_logger::Config;
use log::Level;
use slog::*;
use std::io::{BufRead, BufReader};
use std::time::Duration;
use std::{fmt, result};

use gag::BufferRedirect;

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

fn redirect_io() -> Result<()> {
    monitor_io_handle(BufferRedirect::stdout()?);
    monitor_io_handle(BufferRedirect::stderr()?);
    Ok(())
}

fn monitor_io_handle(handle: BufferRedirect) {
    let mut handle = BufReader::new(handle);

    std::thread::spawn(move || {
        let mut buf = String::new();
        loop {
            buf.truncate(0);
            match handle.read_line(&mut buf) {
                Ok(0) => {
                    // currently EOF
                    std::thread::sleep(Duration::from_secs(1));
                }
                Ok(_) => log::debug!("{}", buf),
                Err(err) => log::debug!("stdio err: {}", err),
            }
        }
    });
}

pub(crate) fn setup_logging() -> Logger {
    // failure is expected after the first backend invocation
    let _ = redirect_io();
    android_logger::init_once(Config::default().with_min_level(Level::Debug));
    Logger::root(AndroidDrain {}.fuse(), slog_o!())
}
