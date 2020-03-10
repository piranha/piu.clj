// finding out keyCode:
// http://www.cambiaresearch.com/articles/15/javascript-char-codes-key-codes

function addShortcut(keyCode, mods, callback) {
    mods = mods || {};
    listenTo(document, 'keydown', function(e) {
        var modsMatched = ((e.ctrlKey  == !!mods.ctrl) &&
                           (e.shiftKey == !!mods.shift) &&
                           (e.altKey   == !!mods.alt) &&
                           (e.metaKey  == !!mods.meta));
        var received = e.keyCode !== undefined ? e.keyCode : e.which;
        if (modsMatched && received == keyCode) {
            callback(e);
            e.preventDefault();
        }
    });
}

var $id = function(x) { return document.getElementById(x); };
var $qs = function(x) { return document.querySelector(x); };
var $qsa = function(x) { return document.querySelectorAll(x); };
var forEach = function (array, callback, scope) {
    for (var i = 0; i < array.length; i++) {
        callback.call(scope || array, array[i], i);
    }
};

function listenTo(els, type, fn) {
    if (!els) return;
    if (els && els.nodeType) return listenTo([els], type, fn);

    for (var i = 0; i < els.length; i++) {
        els[i].addEventListener(type, fn);
    }
}

function absHeight(el) {
    if (el == document.body)
        return Math.max(document.body.offsetHeight,
                        document.documentElement.offsetHeight);
    return parseInt(window.getComputedStyle(el).height, 10);
}

function selectHotLang() {
    var selected = $qs('.hot.selected'),
        next = $qs('.hot[rel=' + lexers.value + ']');
    selected && selected.classList.remove('selected');
    next && next.classList.add('selected');
}

document.addEventListener('DOMContentLoaded', function() {
    var lexers = $id('lexers');
    var text = $id('text');

    lexers.addEventListener('change', selectHotLang);
    selectHotLang();

    listenTo($qsa('.hot'), 'click', function(e) {
        e.preventDefault();
        lexers.value = e.target.rel;
        selectHotLang();
        text.focus();
    });

    listenTo($id('wrap'), 'click', function(e) {
        e.preventDefault();
        $id('content').classList.toggle('wrap');
    });

    // resize textarea to fill maximum area without adding a scrollbar
    if (text) {
        var newheight = window.innerHeight - absHeight(document.body) + absHeight(text);
        if (newheight > absHeight(text)) {
            text.style.height = newheight + 'px';
        }
    }

    function submit(e) {
        if (!text.value.replace(/^\s+|\s+$/g, '')) { return; }
        $qs('form') && $qs('form').submit();
    };

    addShortcut(13, {ctrl: true}, submit); // ctrl+enter
    addShortcut(13, {meta: true}, submit); // cmd+enter
    addShortcut(74, {ctrl: true}, function() { // ctrl+j
        lexers.focus();
    });
    addShortcut(78, {ctrl: true}, function() { // ctrl+n
        document.location.href = '/';
    });

    hlter.run();

    // highlight hovered lines
    listenTo($qsa('.line'), 'mouseover', function(e) {
        $id(e.target.dataset.line).classList.add('over');
    });
    listenTo($qsa('.line'), 'mouseout', function(e) {
        $id(e.target.dataset.line).classList.remove('over');
    });
});

hlter = {
    last_location: null,    // which location we're following right now
    run_interval_every: 50, // time in ms that the URL is queried for changes
    selecting: null,        // if user is selecting lines, first line
    current: null,          // current line or line number if any
    modifier: 0,            // if ctrl or shift is pressed
    // shift - 16, ctrl - 17
    isModifier: function(code) { return [16].indexOf(code) + 1; },
    bit: function(code) { return 1 << (code - 16); },

    run: function() {
        var app = this;
        this.lineselection(true);
        this._interval = setInterval(function() {
            app.check.apply(app);
        }, this.run_interval_every);

        listenTo($qsa('.line'), 'mousedown', function(e) {
            if (e.which == 1) app.selectStart(this.rel);
        });
        listenTo($qsa('.line'), 'mouseup', function(e) {
            app.selectEnd(this.rel);
        });
        listenTo($qsa('.code'), 'mouseup', function(e) {
            app.selectEnd(this.id);
        });

        listenTo($qsa('.code'), 'mousemove', function() { app.current = this.id; });
        listenTo($qsa('.line'), 'mousemove', function() { app.current = this.rel; });

        // yay bitwise :P
        listenTo(document, 'keydown', function(e) {
            if (app.isModifier(e.keyCode)) {
                app.modifier |= app.bit(e.keyCode);
            }
        });
        listenTo(document, 'keyup', function(e) {
            if (app.isModifier(e.keyCode)) {
                app.modifier ^= app.bit(e.keyCode);
            }
        });
    },

    selectStart: function(id) { this.selecting = id; },
    selectEnd: function(id) { this.select(id); },

    select: function(end) {
        if (!this.selecting) { return; }

        var range = this.selecting;
        if (end && end != this.selecting) {
            range += ':' + end;
        }

        if (this.modifier && window.location.hash) {
            window.location.hash += ',' + range;
        } else {
            window.location.hash = range;
        }

        this.selecting = null;
    },

    ongoingselection: function() {
        forEach($qsa('.selecting'), function(el) {
            el.classList.remove('selecting');
        });
        if (!(this.selecting && this.current)) { return; }

        var range = this.range(this.selecting, this.current, 'a-');
        forEach(range, function(id) {
            $id(id).classList.add('selecting');
        });
    },

    lineselection: function(onload) {
        forEach($qsa('.selected'), function(el) {
            el.classList.remove('selected');
        });
        var specifiers = this.location().split(',');
        var first;
        for (var i = 0, l = specifiers.length; i < l; i++) {
            var pair = this.getPair(specifiers[i]);
            var range = this.range(pair[0], pair[1], '');
            forEach(range, function(id) {
                $id(id).classList.add('selected');
            });

            if (onload && (!first || first > pair[0])) {
                first = pair[0];
            }
        }

        if (onload && first) {
            document.body.scrollTop += $id('' + first).getBoundingClientRect().top;
        }
    },

    int: function(v) { return parseInt(v, 10); },

    getPair: function(x) {
        var pair = x.split(':').map(this.int);
        if (pair[0] > pair[1]) {
            return [pair[1], pair[0]];
        }
        return pair;
    },

    range: function(start, end, prefix) {
        start = parseInt(start, 10);
        end = parseInt(end, 10);
        if (!start) { return []; }
        if (!end) { end = start; }
        if (prefix == undefined) { prefix = '#'; }

        if (start > end) {
            end += start;
            start = end - start;
            end = end - start;
        }

        var range = [];
        for (var i = start; i <= end; i++) {
            range.push(prefix + i);
        }
        return range;
    },

    location: function() {
        return window.location.hash.slice(1);
    },

    check: function() {
        this.ongoingselection();
        var location = this.location();
        if (location != this.last_location) {
            this.last_location = location;
            this.lineselection();
        }
    }
};
