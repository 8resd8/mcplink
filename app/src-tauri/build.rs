fn main() {
    let project_root = std::env::var("CARGO_MANIFEST_DIR").unwrap();
    let env_path = std::path::Path::new(&project_root).join("..").join(".env");

    if env_path.exists() {

        match dotenvy::from_path_iter(&env_path) {
            Ok(iter) => {
                for item in iter {
                    match item {
                        Ok((key, value)) => {
                        }
                        Err(e) => {
                            eprintln!(
                                "Error: Failed to parse an item from .env file at {}. Details: {}",
                                env_path.display(),
                                e
                            );
                        }
                    }
                }
                eprintln!(
                    "Successfully processed .env file from: {}",
                    env_path.display()
                );
            }
            Err(e) => {
                eprintln!(
                    "Error: Failed to create iterator for .env file at {}. Details: {}",
                    env_path.display(),
                    e
                );
            }
        }
    } else {
        eprintln!(
            "Warning: .env file not found at {}. Skipping .env loading.",
            env_path.display()
        );
    }

    tauri_build::build();
}
