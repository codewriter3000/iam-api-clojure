# iam-clj-api

An Identity Access Manager API built with Leiningen.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Setup
1. Run `git clone https://github.com/codewriter3000/identity-access-manager-clj.git`.
2. Run `lein install`.
3. Create a PostgreSQL database. You'll insert connection properties in step 5.
4. Create a `env.clj` file directly under the `src/` directory.
5. Your `env.clj` file should look something like this:
```
(ns env)

(def _
  {:DATABASE_TYPE "postgres"
   :DATABASE_NAME "iam_test"
   :DATABASE_USER "postgres"
   :DATABASE_HOST "localhost"
   :DATABASE_PORT "5432"
   :DATABASE_PASS "Password1!"})
```
6. Run `lein test` and make sure all of the tests pass. If anything fails, open an issue.

## Running

To start a web server for the application, run:

    lein run

## License

Copyright © 2025 FIXME
