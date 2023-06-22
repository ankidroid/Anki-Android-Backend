// Copyright: Ankitects Pty Ltd and contributors
// License: GNU AGPL, version 3 or later; http://www.gnu.org/licenses/agpl.html

pub mod proto;

use std::{env, path::PathBuf};

use anyhow::Result;
use prost_reflect::DescriptorPool;

fn main() -> Result<()> {
    let descriptors_path = env::var("DESCRIPTORS_BIN").ok().map(PathBuf::from).unwrap();
    println!("cargo:rerun-if-changed={}", descriptors_path.display());
    let pool = DescriptorPool::decode(std::fs::read(descriptors_path)?.as_ref())?;
    let (_, services) = anki_proto_gen::get_services(&pool);
    proto::write_kotlin_interface(&services)?;

    Ok(())
}
