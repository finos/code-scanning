### Hello World in Mozilla's Rust language

This is trivial Hello World example written in Mozilla's Rust language.
This example comes from https://doc.rust-lang.org/book/getting-started.html


## Setup

Tested on `Ubuntu 18.04.2 LTS` (formerly Ubuntu 16.04.1 LTS)

Install these packages on Ubuntu:

```bash
sudo apt-get install git rustc cargo
```
Clone this project using:

```bash
mkdir ~/projects
cd ~/projects
git clone https://github.com/hpaluch/rust-hello-world.git
cd rust-hello-world
```

Updating from Ubuntu 16 to Ubuntu 18:
Issue this command to update to recent dependencies:
```bash
cargo update
```

## Build

Invoke for this project directory:

```bash
cargo build
```

## Run

Invoke this command from project directory:

```bash
cargo run
```

## Examples

### Date example

Printing current date/time:
* `chrono` depndency was added to `Cargo.toml` 
  from https://docs.rs/chrono/0.4.6/chrono/
* and example snippet was copied
  from https://rust-lang-nursery.github.io/rust-cookbook/datetime/parse.html#display-formatted-date-and-time

These two files were modified:
* [Cargo.toml](https://github.com/hpaluch/rust-hello-world/commit/3d4b250b81c6b2f7e0d2f5d0267f50e850acca6f#diff-80398c5faae3c069e4e6aa2ed11b28c0)
* [main.rs](https://github.com/hpaluch/rust-hello-world/commit/3d4b250b81c6b2f7e0d2f5d0267f50e850acca6f#diff-639fbc4ef05b315af92b4d836c31b023)

### Original hello-world

Can be still fetched using this tag: https://github.com/hpaluch/rust-hello-world/tree/t-just-hello-world



### Resources

* https://doc.rust-lang.org/book/getting-started.html
* https://www.rust-lang.org/en-US/ About Mozilla's Rust language
* https://www.wired.com/2016/03/epic-story-dropboxs-exodus-amazon-cloud-empire/ DropBox using Rust


