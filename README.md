# blogger

Simple personal blog application built with Clojure(Script) and Reagent.

Supports multiple users, but doesn't have separate user roles or edit/delete rights assigned per blog entry.

To register user navigate to #/auth/register and login thorough #/auth/login. Once logged in you can edit anything from
blog entries to site header.

Test deploy is live on heroku: [https://tranquil-fortress-64511.herokuapp.com/](https://tranquil-fortress-64511.herokuapp.com/)

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
To start figwheel, run `(start-fw)`

To compile sass to css run `lein sass watch` in terminal. It would be smarter to add figwheel script for this,
but I haven't gotten around to it yet.

Resources folder has default env variables. You can create your own dev-config.edn to project root to replace them.


## License

MIT