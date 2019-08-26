
FROM clojure:lein

RUN mkdir -p /usr/src/app

COPY project.clj /usr/src/app

WORKDIR /usr/src/app

RUN lein deps
