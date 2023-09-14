/* jshint esversion: 8 */

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


function offsetTop(el) {
  var offset = 0;
  do {
    offset += el.offsetTop;
  } while (el = el.offsetParent);
  return offset;
}


// finding out keyCode:
// http://www.cambiaresearch.com/articles/15/javascript-char-codes-key-codes
function addShortcut(keyCode, mods, callback) {
  mods = mods || {};
  document.addEventListener('keydown', function(e) {
    var modsMatched = ((e.ctrlKey  == !!mods.ctrl)  &&
                       (e.shiftKey == !!mods.shift) &&
                       (e.altKey   == !!mods.alt)   &&
                       (e.metaKey  == !!mods.meta));
    var received = e.keyCode !== undefined ? e.keyCode : e.which;
    if (modsMatched && received == keyCode) {
      callback(e);
      e.preventDefault();
    }
  });
}


/// Shortcuts

document.addEventListener('DOMContentLoaded', function () {
  var text = $id('text');

  function submit(e) {
    if ((text || text.value.replace(/^\s+|\s+$/g, '').length) &&
        $qs('form')) {
      $qs('form').submit();
    }
  }

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

document.addEventListener('DOMContentLoaded', function() {
  on($id('wrap'), 'click', function(e) {
    e.preventDefault();
    $id('content').classList.toggle('wrap');
  });
});


/// Highlighting

document.addEventListener('DOMContentLoaded', function() {
  var table = $qs('table.highlight');
  if (!table) return;

  on(table, 'mousedown', highlightClicks.bind(this, table));
  setHighlight();
  scrollIntoView();
});


window.addEventListener('popstate', function() {
  setHighlight();
  scrollIntoView();
});


/// To local date time
document.addEventListener('DOMContentLoaded', function() {
  [].forEach.call($qsa('time'), function(t) {
    var d = new Date(t.dateTime);
    t.innerText = d.toLocaleString();
  });
});

function asLexer(e) {
  if (e.target.value !== e.target.defaultValue) {
    window.location.search = '?as=' + encodeURIComponent(e.target.value);
  }
}

/// Parse URL to determine what should be highlighted
function parseSelectionPair(s) {
  var pair = s.split('-');
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

function getSelections() {
  var hash = window.location.hash.slice(1);
  if (!hash)
    return [];
  var bits = hash.split(',');
  return bits.map(parseSelectionPair);
}

function encodeSelections(sels) {
  return sels
    .map(sel => sel.start == sel.end ? sel.start : sel.start + '-' + sel.end)
    .join(',');
}

/// Highlight whatever URL tells us
function setHighlight() {
  var sels = getSelections();

  [].forEach.call($qsa('.selected'), function(el) {
    el.classList.remove('selected');
  });

  for (var j = 0; j < sels.length; j++) {
    for (var i = sels[j].start; i <= sels[j].end; i++) {
      let el = $id(i);
      el.classList.add('selected');
      el.previousElementSibling.classList.add('selected');
    }
  }
  return sels;
}


function scrollIntoView() {
  var sels = getSelections().sort();
  if (!sels.length) return;

  var sel = sels[0];
  // Browser scroll on hash change is hard to determine, so we just override it
  // with our own logic. Timeout is needed to perform scroll after browser.
  setTimeout(function() {
    var el = $id(sel.start);
    var offset = offsetTop(el);
    window.scroll(0, offset);
  }, 1);
}


/// Handler to highlight whatever user clicks
function highlightClicks(table, e) {
  if (e.target.className != 'line') return;

  var line = parseInt(e.target.dataset.line, 10);

  var sels = getSelections();
  var sel = sels[sels.length - 1];
  if (e.shiftKey && sel) {
    if (line < sel.start) {
      sel.start = line;
    } else if (line > sel.end) {
      sel.end = line;
    }
  } else if (e.metaKey || e.ctrlKey) {
    sels.push({start: line, end: line});
  } else {
    sels = [{start: line, end: line}];
  }

  // we have updated selection in-place, so should be okay
  window.history.pushState(null, null, '#' + encodeSelections(sels));
  setHighlight();
}
