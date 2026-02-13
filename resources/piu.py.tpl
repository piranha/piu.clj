#!/usr/bin/env python3

from optparse import OptionParser
from fnmatch import fnmatch
import os, sys, urllib.request, urllib.parse, re

EXTMAP = #extmap#
LEXERS = #lexers#

LEXERMAP = {'emacs-lisp': 'lisp',
            'nxml': 'xml',
            'html': 'xml',
            'sh': 'bash'}

URI = 'https://paste.in.ua/'
mode_re = re.compile(r'-\*-.*mode: (?P<mode>[\w\.\-]+).*-\*-', re.I)

def findlexer(fn, default=None):
    fn = os.path.basename(fn)
    for pat, lexer in EXTMAP.items():
        if fnmatch(fn, pat):
            return lexer
    return default

def guess_lexer(data, default):
    if not data.strip():
        print('abort: no data')
        sys.exit(1)

    if '\033' in data:
        return 'ansi'

    lines = data.splitlines()

    # shebang
    line = lines[0]
    if line.startswith('#!'):
        executable = os.path.basename(line.split()[0][2:])
        if executable == 'env':
            executable = line.split()[1]
        return LEXERMAP.get(executable, executable)

    # file variables appear only in first two lines of file
    for line in lines[:2]:
        if mode_re.search(line):
            mode = mode_re.search(line).group('mode')
            return LEXERMAP.get(mode, mode)

    # check if it's a diff
    probably = False
    for line in lines:
        if line.startswith('--- '):
            probably = True
        elif line.startswith('+++ ') and probably:
            return 'diff'
        else:
            probably = False
    return default

def print_lexers(*args, **kwargs):
    print('\n'.join(LEXERS))
    sys.exit()

def paste(data, lexer):
    post = {'data': data, 'lexer': lexer}
    data = urllib.parse.urlencode(post).encode('utf-8')
    return urllib.request.urlopen(URI, data).url

def result(url):
    print(url)
    utils = 'xclip pbcopy'.split()
    for util in utils:
        # not because 0 is success
        if not os.system('which %s > /dev/null 2>&1' % util):
            os.system('printf %s | %s' % (url, util))
            print('url copied to clipboard using %s' % util)

def main():
    usage = 'usage: cat file | %prog  or  %prog file'
    parser = OptionParser(usage)
    parser.add_option('-t', '--type', type='string', default='',
                      help='input file type')
    parser.add_option('', '--types', action='callback', callback=print_lexers,
                      help='print available file types')
    opts, args = parser.parse_args()

    if not len(args):
        # is not a tty - we have data in stdin awaiting
        if not sys.stdin.isatty():
            data = sys.stdin.read()
        else:
            sys.exit(parser.print_help())
        lexer = opts.type
    else:
        data = open(args[0]).read()
        lexer = opts.type or findlexer(args[0])
    lexer = lexer or guess_lexer(data, 'plaintext')

    if lexer not in LEXERS:
        print('abort: {} is not a valid file type'.format(lexer))
        sys.exit(1)

    result(paste(data, lexer))

if __name__ == '__main__':
    main()
