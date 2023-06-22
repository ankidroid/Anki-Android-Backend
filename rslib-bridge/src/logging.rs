//! A simple adaptor that takes log messages from the backend and sends them to
//! the Android logs.
//! It also captures stdout/stderr output, and feeds it to logcat, to make it
//! easier to debug issues with dbg!()/println!()

use std::{
    io::{BufRead, BufReader},
    time::Duration,
};

use android_logger::{Config, FilterBuilder};
use anki::error::Result;
use gag::BufferRedirect;
use log::LevelFilter;
use once_cell::sync::OnceCell;
use tracing::{debug, info};

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
                Ok(_) => {
                    if !should_ignore_line(&buf) {
                        debug!("STDOUT: {}", buf)
                    }
                }
                Err(err) => debug!("STDERR: {}", err),
            }
        }
    });
}

fn should_ignore_line(buf: &str) -> bool {
    buf.starts_with("s_glBindAttribLocation")
}

pub(crate) fn setup_logging() {
    static ONCE: OnceCell<()> = OnceCell::new();
    ONCE.get_or_init(|| {
        _ = redirect_io();

        let filter = format!(
            "{},rsdroid::logging=debug",
            std::env::var("RUST_LOG").unwrap_or_else(|_| "error".into())
        );
        android_logger::init_once(
            Config::default()
                .with_max_level(LevelFilter::Debug)
                .with_filter(FilterBuilder::new().parse(&filter).build()),
        );

        info!("rsdroid logging enabled");
    });
}
