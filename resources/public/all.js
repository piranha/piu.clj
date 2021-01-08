// finding out keyCode:
// http://www.cambiaresearch.com/articles/15/javascript-char-codes-key-codes

var $id = document.getElementById.bind(document);
var $qs = document.querySelector.bind(document);
var $qsa = document.querySelectorAll.bind(document);


function on(els, type, fn) {
    if (!els) return;
    if (els && els.nodeType) return on([els], type, fn);

    for (var i = 0; i < els.length; i++) {
        els[i].addEventListener(type, fn);
    }
}


function addShortcut(keyCode, mods, callback) {
    mods = mods || {};
    on(document, 'keydown', function(e) {
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


/// Resize textarea to fill maximum area without adding a scrollbar

document.addEventListener('DOMContentLoaded', function () {
    var text = $id('text');
    if (!text) return;

    function absHeight(el) {
        if (el == document.body) {
            return Math.max(document.body.offsetHeight,
                            document.documentElement.offsetHeight);
        }
        return parseInt(window.getComputedStyle(el).height, 10);
    }

    var newheight = window.innerHeight - absHeight(document.body) + absHeight(text);
    if (newheight > absHeight(text)) {
        text.style.height = newheight + 'px';
    }
});


/// Shortcuts

document.addEventListener('DOMContentLoaded', function () {
    var text = $id('text');

    function submit(e) {
        if (!text || !text.value.replace(/^\s+|\s+$/g, '')) { return; }
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

});


/// Lexers

document.addEventListener('DOMContentLoaded', function () {
    var lexers = $id('lexers');
    if (!lexers) return;

    function selectHotLang() {
        var selected = $qs('.hot.selected'),
            next = $qs('.hot[rel=' + lexers.value + ']');
        selected && selected.classList.remove('selected');
        next && next.classList.add('selected');
    }

    lexers.addEventListener('change', selectHotLang);
    selectHotLang();

    on($qsa('.hot'), 'click', function(e) {
        e.preventDefault();
        lexers.value = e.target.rel;
        selectHotLang();
        text.focus();
    });
});


/// Right side controls

document.addEventListener('DOMContentLoaded', function () {
    on($id('wrap'), 'click', function(e) {
        e.preventDefault();
        $id('content').classList.toggle('wrap');
    });
});


/// Highlighting

document.addEventListener('DOMContentLoaded', function() {
  var table = $qs('table.highlight');
  if (!table) return;

  on(table, 'mouseover', function(e) {
    if (e.target.className == 'line') {
      $id(e.target.dataset.line).classList.add('over');
    }
  });

  on(table, 'mouseout', function(e) {
    if (e.target.className == 'line') {
      $id(e.target.dataset.line).classList.remove('over');
    }
  });

  on(table, 'mousedown', highlightClicks.bind(this, table));
  setHighlight();
});


/// Parse URL to determine what should be highlighted
function targetSelection() {
    var hash = window.location.hash.slice(1);
    var pair = hash.split('-');
    var start = pair[0] && parseInt(pair[0], 10);
    var end = pair[1] && parseInt(pair[1], 10) || start;

    if (!start) {
        return;
    } else if (start > end) {
        return {start: end, end: start};
    } else {
        return {start: start, end: end};
    }
}

/// Highlight whatever URL tells us
function setHighlight() {
    [].forEach.call($qsa('.selected'), function(el) {
        el.classList.remove('selected');
    });

    var sel = targetSelection(sel);
    if (!sel) return;

    for (var i = sel.start; i <= sel.end; i++) {
        $id(i).classList.add('selected');
    }
}

/// Handler to highlight whatever user clicks
function highlightClicks(table, e) {
  if (!(e.target.className == 'line')) return;

  var line = parseInt(e.target.dataset.line, 10);

  if (!e.shiftKey) {
    window.location.hash = '#' + line;
  } else {
    var sel = targetSelection();
    if (line < sel.start) {
      sel.start = line;
    } else if (line > sel.end) {
      sel.end = line;
    }
    window.location.hash = '#' + sel.start + '-' + sel.end;
  }

  setHighlight();
}
