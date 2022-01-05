<style>body { max-width: 60em; margin: 0 auto; }</style>

That's a little simple pastebin, open for everyone. If you'd like to run your
own instance, sources are on [Github](https://github.com/piranha/piu.clj).

## Features

### Selections

There is a neat feature - you can click on any line and it'll be highlighted. If
you click any other line while pressing `Shift`, you'll get a highlighted
block. Your whole selection will be stored in the URL, so you can highlight
something and send that URL to somebody to get their attention.

### Shortcuts

There are a few shortcuts:

- `Ctrl-Enter` (`⌘-Enter` on a Mac): create a paste (but only if the textarea is not
  empty)
- `Ctrl-J`: jump to a lexer selection
- `Ctrl-N`: create a new paste (from "viewing paste" screen)

### Prettification

If you have pasted JSON there is an option to prettify it — look at the top-right
corner.

### Markdown

If you have pasted document of type `markdown`, there is an option to render it as HTML —
look at the top-right corner. In fact, this about screen is rendered using same
functionality.

### API

API is dead simple, it's just a POST form-encoded request to
`https://paste.in.ua/` with a single required parameter - `data`. Supply a
`lexer` to pre-select a lexer. It's default to `guess` (which can select
surprisingly weird stuff).

## Tools

### Curl

```
cat > ~/bin/piu <<EOF
#!/bin/sh
curl --data-urlencode 'data@-' -w '%{redirect_url}\n' https://paste.in.ua/
EOF
chmod +x ~/bin/piu
```

### Command Line Client

You can install command line [utility](/piu) like that:

```
curl -so ~/bin/piu https://paste.in.ua/piu && chmod +x ~/bin/piu
```

Features:

- determine file type by extension
- determine file type by analyzing input data
- automatically copy url to clipboard using either xclip or pbcopy (whichever is
  available on your system)

Usage:

```
> piu somefile.py
> cat file | piu
> git show HEAD | piu
```

### Emacs

There is a [small module](/piu.el) for Emacs; download and put it somewhere in
your Emacs's `load-path`. Instruction is in the comments at the top of the file.
