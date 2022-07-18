// printing current date from: https://rust-lang-nursery.github.io/rust-cookbook/datetime/parse.html#display-formatted-date-and-time

extern crate chrono;
use chrono::{DateTime, Utc};


fn main() {
    let now: DateTime<Utc> = Utc::now();

    println!("UTC now is: {}", now);
    println!("UTC now in RFC 2822 is: {}", now.to_rfc2822());
    println!("UTC now in RFC 3339 is: {}", now.to_rfc3339());
    println!("UTC now in a custom format is: {}", now.format("%a %b %e %T %Y"));
}

