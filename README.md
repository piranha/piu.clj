# paste.in.ua

`piu.clj` is an open source application powering [paste.in.ua](https://paste.in.ua) - a simple pastebin service.

It's written in Clojure and compiles down to a single binary using GraalVM with Native Image plugin. See [releases](https://github.com/piranha/piu.clj/releases) for the compiled binary.

## Features

- 800 lines of code (including HTML templates)
- simple file based store
- short random paste id
- editing your own paste
- click a line number and then press shift and click another one and you got a [highlight](https://paste.in.ua/4yb9/#8-13)
- a [few shortcuts](https://paste.in.ua/about/#shortcuts)
- all of [Highlight.js](https://highlightjs.org/static/demo/) supported languages
- JSON [pretty-printing](https://paste.in.ua/dsfq/?pretty)
- [rendering Markdown](https://paste.in.ua/4yb9/render/)
- a simple [API](https://paste.in.ua/about/#api) and, as a result:
  - a [command-line utility](https://paste.in.ua/piu)
  - an [Emacs utility](https://paste.in.ua/piu.el)
