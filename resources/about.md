<style>body { max-width: 60em; margin: 0 auto; }</style>

That's a little simple pastebin, open for everyone. If you'd like to run your own instance, sources are on [Github](https://github.com/piranha/piu.clj).

## Features

### Selections

There is a neat feature - you can click on any line and it'll be highlighted. If
you click any other line while pressing `Shift`, you'll get a highlighted
block. Your whole selection will be stored in URL, so you can highlight
something and send URL to somebody to get their attention.

### Shortcuts

There are a few shortcuts:

- Ctrl-Enter (⌘-Enter on Mac): create a paste (but only if the textarea is not
  empty)
- Ctrl-J: jump to a lexer selection
- Ctrl-N: create new paste (from "viewing paste" screen)

### Prettification

If you have pasted JSON there is an option to prettify it - look at top-right
corner.

### Markdown

If you have pasted Markdown document, there is an option to render it as HTML -
look at top-right corner.

### API

API is dead simple, it's just a POST request to `http://paste.in.ua/` with
single required parameter - `data`. Supply a `lexer` to pre-select a lexer. It's
default to `guess` (which can select surprisingly weird stuff).

## Tools

### Command Line

You can install command line [utility](/piu) like that:

```
curl -so ~/bin/piu http://paste.in.ua/piu && chmod +x ~/bin/piu
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
your Emacs's `load-path`. Instruction is in comments at the top of the file.
