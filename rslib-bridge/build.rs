use std::fmt::Write;
use std::path::Path;
use std::process::Command;

// TODO: See if we can reference anki/build.rs so there's no code duplication

struct CustomGenerator {}

fn write_method_enum(buf: &mut String, service: &prost_build::Service) {
    buf.push_str(
        r#"
use num_enum::TryFromPrimitive;
#[derive(PartialEq,TryFromPrimitive)]
#[repr(u32)]
pub enum BackendMethod {
"#,
    );
    for (idx, method) in service.methods.iter().enumerate() {
        writeln!(buf, "    {} = {},", method.proto_name, idx + 1).unwrap();
    }
    buf.push_str("}\n\n");
}

fn write_method_trait(buf: &mut String, service: &prost_build::Service) {
    buf.push_str(
        r#"
use prost::Message;
pub type BackendResult<T> = std::result::Result<T, anki::err::AnkiError>;
pub trait DroidBackendService {
    fn run_command_bytes2_inner_ad(&self, method: u32, input: &[u8]) -> std::result::Result<Vec<u8>, anki::err::AnkiError> {
        match method {
"#,
    );

    for (idx, method) in service.methods.iter().enumerate() {
        write!(
            buf,
            concat!("            ",
            "{idx} => {{ let input = {input_type}::decode(input)?;\n",
            "let output = self.{rust_method}(input)?;\n",
            "let mut out_bytes = Vec::new(); output.encode(&mut out_bytes)?; Ok(out_bytes) }}, "),
            idx = idx + 1,
            input_type = method.input_type,
            rust_method = method.name
        )
        .unwrap();
    }
    buf.push_str(
        r#"
            _ => Err(anki::err::AnkiError::invalid_input("invalid command")),
        }
    }
"#,
    );

    for method in &service.methods {
        write!(
            buf,
            concat!(
                "    fn {method_name}(&self, input: {input_type}) -> ",
                "BackendResult<{output_type}>;\n"
            ),
            method_name = method.name,
            input_type = method.input_type,
            output_type = method.output_type
        )
        .unwrap();
    }
    buf.push_str("}\n");
}

impl prost_build::ServiceGenerator for CustomGenerator {
    fn generate(&mut self, service: prost_build::Service, buf: &mut String) {
        write_method_enum(buf, &service);
        write_method_trait(buf, &service);
    }
}

fn service_generator() -> Box<dyn prost_build::ServiceGenerator> {
    Box::new(CustomGenerator {})
}


fn main() -> std::io::Result<()> {
    // output protobuf generated code
    println!("cargo:rerun-if-changed=proto/AdBackend.proto");
	
    let mut config = prost_build::Config::new();
    config
        // we avoid default OUT_DIR for now, as it breaks code completion
        .out_dir("src")
        .service_generator(service_generator())
        .compile_protos(&["proto/AdBackend.proto"], &["proto"])
        .unwrap();

    if let Err(e) = std::env::var("DONT_RUSTFMT") {
        assert_eq!(e, std::env::VarError::NotPresent);
        println!("Using rustfmt to format src/backend_proto.rs");
        // rustfmt the protobuf code
        let rustfmt = Command::new("rustfmt")
            .arg(Path::new("src/backend_proto.rs"))
            .status()
            .unwrap();

        assert!(rustfmt.success(), "rustfmt backend_proto.rs failed");
    }

    Ok(())
}