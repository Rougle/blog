# blogger

Simple home page & blog application.

Generated using Luminus version "3.46"

## Prerequisites

Without docker:

- PostgreSQL
- [Leiningen][1] 2.0 or above

[1]: https://github.com/technomancy/leiningen

With docker:

None.

## Running

Run `docker-compose up` and connect to port 7000 with nREPL.
To start the app, run `(start)`
To start figwheel, run `(use 'figwheel-sidecar.repl-api)` and `(start-figwheel!)`

## License

MIT